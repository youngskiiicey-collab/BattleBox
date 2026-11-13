const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_member_1";
const BOB_UID = "bob_member_2";
const TRAINER_UID = "trainer_vikram_1";
const ADMIN_UID = "admin_owner_1";
const ADMIN_EMAIL = "youngskiiicey@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validMemberPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    displayName: "Arjun Verma",
    email: `${uid}@example.com`,
    role: "member",
    planTier: "basic",
    membershipStatus: "active",
    onboardingCompleted: true,
    termsVersion: "1.0-draft",
    dpdpConsentAccepted: true,
    healthWaiverAccepted: true,
    consentAcceptedAt: now,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validAttendancePayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  const expires = new Date(Date.now() + 3 * 3600 * 1000);
  return {
    userId: uid,
    memberName: "Arjun Verma",
    memberRole: "member",
    mode: "kiosk_qr",
    status: "checked_in",
    distanceMeters: 24.5,
    gpsAccuracyMeters: 12.0,
    latitude: 28.4211,
    longitude: 77.0436,
    verifiedByUid: uid,
    qrEpochSlot: 123456,
    checkInAt: now,
    expiresAt: expires,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("gym_public").doc("status").get());
  await assertFails(unauthDb.collection("attendance").where("userId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("audit_logs").get());
});

test("Member can create own profile only with role=member and planTier=basic (blocks privilege escalation)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();

  // Self-escalation to admin fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(validMemberPayload(ALICE_UID, { role: "admin" }))
  );

  // Self-escalation to premium fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(validMemberPayload(ALICE_UID, { planTier: "premium" }))
  );

  // Ghost field fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(validMemberPayload(ALICE_UID, { isVerified: true }))
  );

  // Valid member creation succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validMemberPayload(ALICE_UID))
  );
});

test("Member cannot read another member's profile or list all users", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).set(validMemberPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").get());
});

test("Member cannot self-escalate role or membershipStatus on update", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set(validMemberPayload(ALICE_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({ role: "trainer", updatedAt: new Date(Date.now() - 500) })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({ displayName: "Arjun V.", updatedAt: new Date(Date.now() - 500) })
  );
});

test("Trainer can read ONLY assigned members", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set(
      validMemberPayload(ALICE_UID, { assignedTrainerId: TRAINER_UID })
    );
    await ctx.firestore().collection("users").doc(BOB_UID).set(validMemberPayload(BOB_UID));
  });

  const trainerDb = testEnv.authenticatedContext(TRAINER_UID).firestore();
  await assertSucceeds(trainerDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    trainerDb.collection("users").where("assignedTrainerId", "==", TRAINER_UID).get()
  );
  await assertFails(trainerDb.collection("users").doc(BOB_UID).get());
  await assertFails(trainerDb.collection("users").get());
});

test("Email spoofing test: unverified admin email is rejected", async () => {
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(spoofDb.collection("audit_logs").get());
});

test("Bootstrapped Admin can list users, update roles, and write immutable audit logs", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set(validMemberPayload(ALICE_UID));
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      role: "trainer",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  const now = new Date(Date.now() - 500);
  await assertSucceeds(
    adminDb.collection("audit_logs").doc("log_1").set({
      actorUid: ADMIN_UID,
      actorEmail: ADMIN_EMAIL,
      action: "ROLE_UPDATED",
      targetUid: ALICE_UID,
      details: "Promoted Alice to trainer",
      createdAt: now,
    })
  );
  // Audit logs are immutable
  await assertFails(adminDb.collection("audit_logs").doc("log_1").update({ details: "tampered" }));
  await assertFails(adminDb.collection("audit_logs").doc("log_1").delete());
});

test("Attendance: member can query own records, check in/out, and cannot reopen terminal checkout", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("attendance").doc("att_alice_1").set(validAttendancePayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("attendance").where("userId", "==", ALICE_UID).get()
  );

  // Bob cannot read Alice's attendance or list all attendance
  await assertFails(bobDb.collection("attendance").doc("att_alice_1").get());
  await assertFails(bobDb.collection("attendance").get());

  const now = new Date(Date.now() - 500);
  await assertSucceeds(
    aliceDb.collection("attendance").doc("att_alice_1").update({
      status: "checked_out",
      checkOutAt: now,
      updatedAt: now,
    })
  );

  // Terminal state lock: member cannot revert checked_out -> checked_in
  await assertFails(
    aliceDb.collection("attendance").doc("att_alice_1").update({
      status: "checked_in",
      updatedAt: now,
    })
  );
});

test("Consents are immutable and isolated per user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 500);

  await assertSucceeds(
    aliceDb.collection("consents").doc(`${ALICE_UID}_1_0`).set({
      userId: ALICE_UID,
      termsVersion: "1.0-draft",
      dpdpConsentAccepted: true,
      healthWaiverAccepted: true,
      acceptedAt: now,
      createdAt: now,
    })
  );

  await assertFails(bobDb.collection("consents").doc(`${ALICE_UID}_1_0`).get());
  await assertFails(aliceDb.collection("consents").doc(`${ALICE_UID}_1_0`).delete());
});

test("Phase 2 Payments: member cannot spoof paid payments or self-activate subscriptions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 500);

  // Member CANNOT write to payments directly (webhook-only)
  await assertFails(
    aliceDb.collection("payments").doc("pay_spoof_1").set({
      userId: ALICE_UID,
      memberName: "Alice",
      memberEmail: "alice@example.com",
      planId: "premium_monthly",
      planName: "Premium Monthly",
      tier: "premium",
      baseAmountInr: 2542,
      gstAmountInr: 457,
      totalAmountInr: 2999,
      currency: "INR",
      paymentMode: "one_time_order",
      razorpayOrderId: "order_fake",
      razorpayPaymentId: "pay_fake",
      webhookEventId: "evt_fake",
      status: "captured",
      invoiceNumber: "BB-FAKE",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Member CAN create a checkout intent ONLY with status=created
  await assertSucceeds(
    aliceDb.collection("payment_requests").doc("req_1").set({
      userId: ALICE_UID,
      memberName: "Alice",
      memberEmail: "alice@example.com",
      planId: "premium_monthly",
      planName: "Premium Monthly",
      tier: "premium",
      billingCycle: "monthly",
      paymentMode: "upi_autopay_subscription",
      amountInr: 2999,
      currency: "INR",
      preferredUpiApp: "Google Pay",
      status: "created",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(bobDb.collection("payment_requests").doc("req_1").get());

  // Seed an active subscription via privileged context
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("subscriptions").doc("sub_alice_1").set({
      userId: ALICE_UID,
      planId: "premium_monthly",
      planName: "Premium Monthly",
      tier: "premium",
      billingCycle: "monthly",
      paymentMode: "upi_autopay_subscription",
      amountInr: 2999,
      status: "active",
      upiHandle: "alice@okicici",
      razorpaySubscriptionId: "sub_rzp_123",
      preDebitNotified: false,
      autoRenew: true,
      updatedAt: now,
    });
  });

  // Member CANNOT mutate tier or re-activate; CAN only request cancellation
  await assertFails(
    aliceDb.collection("subscriptions").doc("sub_alice_1").update({
      tier: "basic",
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("subscriptions").doc("sub_alice_1").update({
      status: "cancellation_requested",
      autoRenew: false,
      updatedAt: now,
    })
  );
});

test("Phase 2 Notifications & Announcements: member isolates own opt-in, only Admin posts announcements", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 500);

  await assertSucceeds(
    aliceDb.collection("notification_preferences").doc(ALICE_UID).set({
      userId: ALICE_UID,
      whatsappOptIn: true,
      whatsappPhone: "+919876543210",
      expiryReminder3d: true,
      dueTodayReminder: true,
      paymentAlerts: true,
      gymClosedAlerts: true,
      preDebitAlerts: true,
      pushEnabled: true,
      updatedAt: now,
    })
  );
  await assertFails(bobDb.collection("notification_preferences").doc(ALICE_UID).get());

  // Member cannot post gym announcements; Admin can and members can read
  await assertFails(
    aliceDb.collection("gym_announcements").doc("ann_1").set({
      statusType: "closed",
      title: "Fake Closure",
      message: "Do not allow member spam",
      effectiveHours: "Today",
      targetAudience: "all_members_and_trainers",
      sendPushNotification: true,
      sendWhatsappClosure: false,
      active: true,
      createdByUid: ALICE_UID,
      createdByEmail: "alice@example.com",
      createdAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("gym_announcements").doc("ann_1").set({
      statusType: "holiday",
      title: "Republic Day Special Hours",
      message: "Open morning session only today.",
      effectiveHours: "06:00–10:00 IST",
      targetAudience: "all_members_and_trainers",
      sendPushNotification: true,
      sendWhatsappClosure: true,
      active: true,
      createdByUid: ADMIN_UID,
      createdByEmail: ADMIN_EMAIL,
      createdAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("gym_announcements").get());
});

test("Trainer QR Attendance Scanner & Live Occupancy: trainer can scan member QR, read live roster, and check out; member cannot spoof another member check-in", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(TRAINER_UID).set(
      validMemberPayload(TRAINER_UID, { role: "trainer", displayName: "Coach Vikram" })
    );
    await ctx.firestore().collection("users").doc(ALICE_UID).set(
      validMemberPayload(ALICE_UID, { displayName: "Alice Sangma" })
    );
  });

  const trainerDb = testEnv.authenticatedContext(TRAINER_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Trainer can verify member profile on QR scan, but cannot list entire users collection
  await assertSucceeds(trainerDb.collection("users").doc(ALICE_UID).get());
  await assertFails(trainerDb.collection("users").get());

  // Trainer scans Alice's QR and stores attendance record in Firestore
  await assertSucceeds(
    trainerDb.collection("attendance").doc("att_trainer_scan_1").set(
      validAttendancePayload(ALICE_UID, {
        memberName: "Alice Sangma",
        mode: "trainer_scan",
        verifiedByUid: TRAINER_UID,
      })
    )
  );

  // Trainer can monitor live checked-in occupancy roster
  await assertSucceeds(
    trainerDb.collection("attendance").where("status", "==", "checked_in").get()
  );

  // Regular member Bob CANNOT query live roster or forge check-in for Alice
  await assertFails(
    bobDb.collection("attendance").where("status", "==", "checked_in").get()
  );
  await assertFails(
    bobDb.collection("attendance").doc("att_bob_spoof").set(
      validAttendancePayload(ALICE_UID, {
        mode: "trainer_scan",
        verifiedByUid: BOB_UID,
      })
    )
  );

  // Trainer can check out Alice from the floor
  const now = new Date(Date.now() - 500);
  await assertSucceeds(
    trainerDb.collection("attendance").doc("att_trainer_scan_1").update({
      status: "checked_out",
      checkOutAt: now,
      updatedAt: now,
    })
  );

  // Alice can still view her own checked-out record
  await assertSucceeds(aliceDb.collection("attendance").doc("att_trainer_scan_1").get());
});

