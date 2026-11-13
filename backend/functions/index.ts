import * as crypto from "node:crypto";
import { initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore, Timestamp } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { onCall, HttpsError, onRequest } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { defineSecret } from "firebase-functions/params";

initializeApp();
const REGION = "asia-south1";
const DB_ID = "ai-studio-android-battlebo-f9f1c6c4-69cd-46ca-9bcc-c54ee93a8e1b";
const db = getFirestore(DB_ID);

// Secrets managed exclusively via Google Cloud Secret Manager (NEVER in client apps)
const RAZORPAY_KEY_ID = defineSecret("RAZORPAY_KEY_ID");
const RAZORPAY_KEY_SECRET = defineSecret("RAZORPAY_KEY_SECRET");
const RAZORPAY_WEBHOOK_SECRET = defineSecret("RAZORPAY_WEBHOOK_SECRET");
const WHATSAPP_ACCESS_TOKEN = defineSecret("WHATSAPP_ACCESS_TOKEN");
const WHATSAPP_PHONE_NUMBER_ID = defineSecret("WHATSAPP_PHONE_NUMBER_ID");

// 1. Create Razorpay One-Time Order OR UPI AutoPay Subscription
export const createCheckoutSession = onCall(
  {
    region: REGION,
    enforceAppCheck: true,
    secrets: [RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET],
  },
  async (req) => {
    const uid = req.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Sign in required.");

    const { planId, paymentMode, upiApp } = req.data as {
      planId: string;
      paymentMode: "one_time_order" | "upi_autopay_subscription";
      upiApp?: string;
    };

    const planSnap = await db.collection("plans").doc(planId).get();
    if (!planSnap.exists) throw new HttpsError("not-found", "Plan not found.");
    const plan = planSnap.data()!;
    const amountPaise = Number(plan.totalAmountInr) * 100;

    const authHeader =
      "Basic " +
      Buffer.from(`${RAZORPAY_KEY_ID.value()}:${RAZORPAY_KEY_SECRET.value()}`).toString("base64");

    if (paymentMode === "one_time_order") {
      const rzpRes = await fetch("https://api.razorpay.com/v1/orders", {
        method: "POST",
        headers: { Authorization: authHeader, "Content-Type": "application/json" },
        body: JSON.stringify({
          amount: amountPaise,
          currency: "INR",
          receipt: `bb_${uid.slice(0, 8)}_${Date.now()}`,
          notes: { userId: uid, planId, tier: plan.tier, billingCycle: plan.billingCycle },
        }),
      });
      const order = (await rzpRes.json()) as { id: string };
      return { kind: "order", orderId: order.id, amountPaise, currency: "INR", keyId: RAZORPAY_KEY_ID.value() };
    }

    // UPI AutoPay Subscription (Google Pay / PhonePe / Paytm / BHIM)
    const rzpSubRes = await fetch("https://api.razorpay.com/v1/subscriptions", {
      method: "POST",
      headers: { Authorization: authHeader, "Content-Type": "application/json" },
      body: JSON.stringify({
        plan_id: plan.razorpayPlanId,
        total_count: plan.billingCycle === "annual" ? 5 : plan.billingCycle === "quarterly" ? 12 : 24,
        customer_notify: 1,
        notes: { userId: uid, planId, tier: plan.tier, preferredUpiApp: upiApp ?? "Google Pay" },
      }),
    });
    const subscription = (await rzpSubRes.json()) as { id: string };
    return {
      kind: "subscription",
      subscriptionId: subscription.id,
      amountPaise,
      currency: "INR",
      keyId: RAZORPAY_KEY_ID.value(),
    };
  }
);

// 2. Razorpay Webhook Handler: HMAC-SHA256 verification + strict idempotency + webhook-only payment status
export const razorpayWebhook = onRequest(
  {
    region: REGION,
    secrets: [RAZORPAY_WEBHOOK_SECRET, WHATSAPP_ACCESS_TOKEN, WHATSAPP_PHONE_NUMBER_ID],
  },
  async (req, res) => {
    const signature = req.header("x-razorpay-signature") ?? "";
    const rawBody = req.rawBody;
    const expected = crypto
      .createHmac("sha256", RAZORPAY_WEBHOOK_SECRET.value())
      .update(rawBody)
      .digest("hex");

    const sigBuf = Buffer.from(signature, "utf8");
    const expBuf = Buffer.from(expected, "utf8");
    if (sigBuf.length !== expBuf.length || !crypto.timingSafeEqual(sigBuf, expBuf)) {
      res.status(401).send("Invalid webhook signature");
      return;
    }

    const eventId = req.header("x-razorpay-event-id") ?? crypto.createHash("sha256").update(rawBody).digest("hex");
    const event = req.body as { event: string; payload: Record<string, any> };
    const eventRef = db.collection("webhook_events").doc(eventId);

    // Idempotent check & lock inside Firestore transaction
    const alreadyProcessed = await db.runTransaction(async (tx) => {
      const snap = await tx.get(eventRef);
      if (snap.exists) return true;
      tx.set(eventRef, {
        eventType: event.event,
        processedAt: FieldValue.serverTimestamp(),
      });
      return false;
    });

    if (alreadyProcessed) {
      res.status(200).json({ ok: true, idempotentDuplicate: true });
      return;
    }

    const paymentEntity = event.payload?.payment?.entity;
    const notes = paymentEntity?.notes ?? event.payload?.subscription?.entity?.notes ?? {};
    const uid: string | undefined = notes.userId;

    if (event.event === "payment.captured" && uid && paymentEntity) {
      const invoiceNo = `BB-${new Date().getFullYear()}-${String(Date.now()).slice(-5)}`;
      const totalInr = Math.round(Number(paymentEntity.amount) / 100);
      const baseInr = Math.round(totalInr / 1.18);

      await db.collection("payments").doc(paymentEntity.id).set({
        userId: uid,
        planId: notes.planId ?? "basic_monthly",
        planName: notes.planName ?? "Battle Box Plan",
        tier: notes.tier ?? "basic",
        baseAmountInr: baseInr,
        gstAmountInr: totalInr - baseInr,
        totalAmountInr: totalInr,
        currency: "INR",
        paymentMode: paymentEntity.method === "upi" ? "upi_autopay_subscription" : "one_time_order",
        razorpayOrderId: paymentEntity.order_id ?? "",
        razorpayPaymentId: paymentEntity.id,
        webhookEventId: eventId,
        status: "captured",
        invoiceNumber: invoiceNo,
        createdAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
      });

      await db.collection("users").doc(uid).update({
        planTier: notes.tier ?? "basic",
        membershipStatus: "active",
        updatedAt: FieldValue.serverTimestamp(),
      });

      await sendWhatsAppUtilityTemplate(
        uid,
        "battle_box_payment_success",
        [String(totalInr), String(notes.planName ?? "Battle Box"), invoiceNo, "Next Billing Cycle"],
        WHATSAPP_ACCESS_TOKEN.value(),
        WHATSAPP_PHONE_NUMBER_ID.value()
      );
    } else if (event.event === "payment.failed" && uid && paymentEntity) {
      const totalInr = Math.round(Number(paymentEntity.amount) / 100);
      await db.collection("payments").doc(paymentEntity.id).set({
        userId: uid,
        planId: notes.planId ?? "",
        planName: notes.planName ?? "Battle Box Plan",
        tier: notes.tier ?? "basic",
        baseAmountInr: 0,
        gstAmountInr: 0,
        totalAmountInr: totalInr,
        currency: "INR",
        paymentMode: "one_time_order",
        razorpayOrderId: paymentEntity.order_id ?? "",
        razorpayPaymentId: paymentEntity.id,
        webhookEventId: eventId,
        status: "failed",
        invoiceNumber: "",
        createdAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
      });

      await sendWhatsAppUtilityTemplate(
        uid,
        "battle_box_payment_failed",
        ["Member", String(totalInr), String(notes.planName ?? "Battle Box"), paymentEntity.error_description ?? "Declined"],
        WHATSAPP_ACCESS_TOKEN.value(),
        WHATSAPP_PHONE_NUMBER_ID.value()
      );
    }

    res.status(200).json({ ok: true });
  }
);

// 3. Cancel Razorpay UPI AutoPay Subscription
export const cancelUpiAutopay = onCall(
  { region: REGION, enforceAppCheck: true, secrets: [RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET] },
  async (req) => {
    const uid = req.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Sign in required.");
    const { subscriptionDocId } = req.data as { subscriptionDocId: string };
    const subRef = db.collection("subscriptions").doc(subscriptionDocId);
    const subSnap = await subRef.get();
    if (!subSnap.exists || subSnap.data()?.userId !== uid) {
      throw new HttpsError("permission-denied", "Subscription does not belong to user.");
    }
    const rzpSubId = subSnap.data()?.razorpaySubscriptionId as string;
    const authHeader =
      "Basic " +
      Buffer.from(`${RAZORPAY_KEY_ID.value()}:${RAZORPAY_KEY_SECRET.value()}`).toString("base64");

    await fetch(`https://api.razorpay.com/v1/subscriptions/${rzpSubId}/cancel`, {
      method: "POST",
      headers: { Authorization: authHeader, "Content-Type": "application/json" },
      body: JSON.stringify({ cancel_at_cycle_end: 1 }),
    });

    await subRef.update({
      status: "cancelled",
      autoRenew: false,
      updatedAt: FieldValue.serverTimestamp(),
    });
    return { cancelled: true };
  }
);

// 4. Daily 09:00 IST Scheduled Reminders (Expiry in 3 days, Due Today, RBI 24h Pre-Debit Notice)
export const dailyBillingAndExpiryCron = onSchedule(
  {
    region: REGION,
    schedule: "0 9 * * *",
    timeZone: "Asia/Kolkata",
    secrets: [WHATSAPP_ACCESS_TOKEN, WHATSAPP_PHONE_NUMBER_ID],
  },
  async () => {
    const nowMs = Date.now();
    const in3DaysStart = Timestamp.fromMillis(nowMs + 2 * 86400_000);
    const in3DaysEnd = Timestamp.fromMillis(nowMs + 4 * 86400_000);

    const expiringSoon = await db
      .collection("subscriptions")
      .where("status", "==", "active")
      .where("currentPeriodEnd", ">=", in3DaysStart)
      .where("currentPeriodEnd", "<=", in3DaysEnd)
      .get();

    for (const doc of expiringSoon.docs) {
      const sub = doc.data();
      await sendWhatsAppUtilityTemplate(
        sub.userId,
        "battle_box_expiry_3_days",
        ["Athlete", sub.planName, sub.currentPeriodEnd.toDate().toLocaleDateString("en-IN")],
        WHATSAPP_ACCESS_TOKEN.value(),
        WHATSAPP_PHONE_NUMBER_ID.value()
      );
    }

    // RBI 24h pre-debit notifications
    const tomorrowEnd = Timestamp.fromMillis(nowMs + 26 * 3600_000);
    const dueForPreDebit = await db
      .collection("subscriptions")
      .where("autoRenew", "==", true)
      .where("preDebitNotified", "==", false)
      .where("nextDebitAt", "<=", tomorrowEnd)
      .get();

    for (const doc of dueForPreDebit.docs) {
      const sub = doc.data();
      await sendWhatsAppUtilityTemplate(
        sub.userId,
        "battle_box_due_today",
        ["Athlete", sub.planName, String(sub.amountInr), sub.nextDebitAt.toDate().toLocaleDateString("en-IN")],
        WHATSAPP_ACCESS_TOKEN.value(),
        WHATSAPP_PHONE_NUMBER_ID.value()
      );
      await doc.ref.update({ preDebitNotified: true, updatedAt: FieldValue.serverTimestamp() });
    }
  }
);

// 5. FCM Push Multicast + Optional WhatsApp Closed Alert on New Gym Announcement
export const onGymAnnouncementCreated = onDocumentCreated(
  {
    region: REGION,
    database: DB_ID,
    document: "gym_announcements/{announcementId}",
    secrets: [WHATSAPP_ACCESS_TOKEN, WHATSAPP_PHONE_NUMBER_ID],
  },
  async (event) => {
    const ann = event.data?.data();
    if (!ann || !ann.active) return;

    if (ann.sendPushNotification) {
      const prefs = await db.collection("notification_preferences").where("pushEnabled", "==", true).get();
      const tokens = prefs.docs.map((d) => d.data().fcmToken as string).filter(Boolean);
      if (tokens.length > 0) {
        await getMessaging().sendEachForMulticast({
          tokens,
          notification: { title: `Battle Box: ${ann.title}`, body: ann.message },
          data: { statusType: ann.statusType, effectiveHours: ann.effectiveHours ?? "" },
        });
      }
    }

    if (ann.sendWhatsappClosure && (ann.statusType === "closed" || ann.statusType === "holiday")) {
      const waPrefs = await db
        .collection("notification_preferences")
        .where("whatsappOptIn", "==", true)
        .where("gymClosedAlerts", "==", true)
        .get();
      for (const doc of waPrefs.docs) {
        await sendWhatsAppUtilityTemplate(
          doc.id,
          "battle_box_gym_closed",
          [String(ann.statusType).toUpperCase(), String(ann.title), String(ann.effectiveHours || ann.message)],
          WHATSAPP_ACCESS_TOKEN.value(),
          WHATSAPP_PHONE_NUMBER_ID.value()
        );
      }
    }
  }
);

async function sendWhatsAppUtilityTemplate(
  uid: string,
  templateName: string,
  params: string[],
  accessToken: string,
  phoneNumberId: string
): Promise<void> {
  const prefSnap = await db.collection("notification_preferences").doc(uid).get();
  const pref = prefSnap.data();
  if (!pref?.whatsappOptIn || !pref?.whatsappPhone) {
    await db.collection("notification_delivery_logs").add({
      recipientUid: uid,
      recipientContact: "",
      channel: "whatsapp",
      templateName,
      status: "failed",
      details: "Skipped: User has not opted in to WhatsApp notifications.",
      createdAt: FieldValue.serverTimestamp(),
    });
    return;
  }

  const res = await fetch(`https://graph.facebook.com/v21.0/${phoneNumberId}/messages`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      messaging_product: "whatsapp",
      to: String(pref.whatsappPhone).replace(/\D/g, ""),
      type: "template",
      template: {
        name: templateName,
        language: { code: "en" },
        components: [
          {
            type: "body",
            parameters: params.map((text) => ({ type: "text", text })),
          },
        ],
      },
    }),
  });

  await db.collection("notification_delivery_logs").add({
    recipientUid: uid,
    recipientContact: pref.whatsappPhone,
    channel: "whatsapp",
    templateName,
    status: res.ok ? "sent" : "failed",
    details: res.ok ? "Dispatched via Meta WhatsApp Cloud API" : `HTTP ${res.status}`,
    createdAt: FieldValue.serverTimestamp(),
  });
}
