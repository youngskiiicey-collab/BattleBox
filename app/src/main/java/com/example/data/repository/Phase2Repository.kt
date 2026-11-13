package com.example.data.repository

import com.example.data.model.GymAnnouncement
import com.example.data.model.GymPlan
import com.example.data.model.MemberSubscription
import com.example.data.model.NotificationDeliveryLog
import com.example.data.model.NotificationPreference
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentRequest
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class Phase2Repository(
    private val db: FirebaseFirestore,
    private val coreRepository: BattleBoxRepository
) {
    private fun requireUid(): String =
        Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing billing or notifications.")

    fun observePlans(): Flow<List<GymPlan>> =
        db.collection("plans")
            .snapshots()
            .map { snapshot ->
                val list = snapshot.toObjects(GymPlan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.active }
                list.ifEmpty { GymPlan.defaultSeedPlans() }
                    .sortedWith(compareBy<GymPlan> { it.tier }.thenBy { it.durationDays })
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "plans")
                throw error
            }

    fun observeMyPaymentRequests(uid: String): Flow<List<PaymentRequest>> =
        db.collection("payment_requests")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PaymentRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "payment_requests")
                throw error
            }

    fun observeAllPaymentRequests(): Flow<List<PaymentRequest>> =
        db.collection("payment_requests")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PaymentRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    .take(40)
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "payment_requests")
                throw error
            }

    fun observeMySubscriptions(uid: String): Flow<List<MemberSubscription>> =
        db.collection("subscriptions")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(MemberSubscription::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "subscriptions")
                throw error
            }

    fun observeMyPayments(uid: String): Flow<List<PaymentRecord>> =
        db.collection("payments")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PaymentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "payments")
                throw error
            }

    fun observeNotificationPreference(uid: String): Flow<NotificationPreference> =
        db.collection("notification_preferences").document(uid)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(NotificationPreference::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: NotificationPreference(userId = uid)
                } else {
                    NotificationPreference(userId = uid)
                }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.GET, "notification_preferences/$uid")
                throw error
            }

    fun observeAnnouncements(): Flow<List<GymAnnouncement>> =
        db.collection("gym_announcements")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(GymAnnouncement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.active }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "gym_announcements")
                throw error
            }

    fun observeDeliveryLogs(uid: String, isAdmin: Boolean): Flow<List<NotificationDeliveryLog>> {
        val query = if (isAdmin) {
            db.collection("notification_delivery_logs")
        } else {
            db.collection("notification_delivery_logs").whereEqualTo("recipientUid", uid)
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(NotificationDeliveryLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    .take(30)
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "notification_delivery_logs")
                throw error
            }
    }

    suspend fun createCheckoutIntent(
        user: UserProfile,
        plan: GymPlan,
        paymentMode: String,
        preferredUpiApp: String
    ): Result<String> = runCatching {
        val uid = requireUid()
        val reqId = "req_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val ref = db.collection("payment_requests").document(reqId)
        val payload = mapOf(
            "userId" to uid,
            "memberName" to user.displayName.take(80),
            "memberEmail" to user.email.take(160),
            "planId" to plan.id,
            "planName" to plan.name.take(80),
            "tier" to plan.tier,
            "billingCycle" to plan.billingCycle,
            "paymentMode" to paymentMode,
            "amountInr" to plan.totalAmountInr,
            "currency" to "INR",
            "preferredUpiApp" to preferredUpiApp.take(60),
            "status" to PaymentRequest.STATUS_CREATED,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            ref.set(payload).await()
            reqId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            throw e
        }
    }

    suspend fun requestAutopayCancellation(subscriptionId: String): Result<Unit> = runCatching {
        val subRef = db.collection("subscriptions").document(subscriptionId)
        try {
            subRef.update(
                mapOf(
                    "status" to MemberSubscription.STATUS_CANCEL_REQUESTED,
                    "autoRenew" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, subRef.path)
            throw e
        }
    }

    suspend fun saveNotificationPreference(pref: NotificationPreference): Result<Unit> = runCatching {
        val uid = requireUid()
        val ref = db.collection("notification_preferences").document(uid)
        val payload = mapOf(
            "userId" to uid,
            "whatsappOptIn" to pref.whatsappOptIn,
            "whatsappPhone" to pref.whatsappPhone.trim().take(25),
            "expiryReminder3d" to pref.expiryReminder3d,
            "dueTodayReminder" to pref.dueTodayReminder,
            "paymentAlerts" to pref.paymentAlerts,
            "gymClosedAlerts" to pref.gymClosedAlerts,
            "preDebitAlerts" to pref.preDebitAlerts,
            "pushEnabled" to pref.pushEnabled,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            ref.set(payload, SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            throw e
        }
    }

    suspend fun adminSeedDefaultPlans(): Result<Unit> = runCatching {
        val plans = GymPlan.defaultSeedPlans()
        try {
            for (plan in plans) {
                db.collection("plans").document(plan.id).set(
                    mapOf(
                        "name" to plan.name,
                        "tier" to plan.tier,
                        "billingCycle" to plan.billingCycle,
                        "baseAmountInr" to plan.baseAmountInr,
                        "gstPercent" to plan.gstPercent,
                        "totalAmountInr" to plan.totalAmountInr,
                        "durationDays" to plan.durationDays,
                        "supportsOneTime" to plan.supportsOneTime,
                        "supportsUpiAutopay" to plan.supportsUpiAutopay,
                        "description" to plan.description,
                        "active" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            coreRepository.writeAuditLog(
                action = "PLANS_CATALOG_SEEDED",
                targetUid = null,
                details = "Seeded ${plans.size} Battle Box INR membership plans"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "plans")
            throw e
        }
    }

    suspend fun adminPostAnnouncement(
        statusType: String,
        title: String,
        message: String,
        effectiveHours: String,
        sendPush: Boolean,
        sendWhatsappClosure: Boolean
    ): Result<String> = runCatching {
        val user = Firebase.auth.currentUser
            ?: throw IllegalStateException("Admin must be signed in.")
        val annId = "ann_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val ref = db.collection("gym_announcements").document(annId)
        val payload = mapOf(
            "statusType" to statusType,
            "title" to title.trim().take(120),
            "message" to message.trim().take(500),
            "effectiveHours" to effectiveHours.trim().take(120),
            "targetAudience" to "all_members_and_trainers",
            "sendPushNotification" to sendPush,
            "sendWhatsappClosure" to sendWhatsappClosure,
            "active" to true,
            "createdByUid" to user.uid,
            "createdByEmail" to (user.email ?: "admin"),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            ref.set(payload).await()
            val logId = "dlog_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            db.collection("notification_delivery_logs").document(logId).set(
                mapOf(
                    "recipientUid" to "broadcast_all",
                    "recipientContact" to "all_members_and_trainers",
                    "channel" to if (sendWhatsappClosure) NotificationDeliveryLog.CHANNEL_WHATSAPP else NotificationDeliveryLog.CHANNEL_IN_APP,
                    "templateName" to if (sendWhatsappClosure) "battle_box_gym_closed" else "in_app_banner",
                    "status" to NotificationDeliveryLog.STATUS_QUEUED,
                    "details" to "Announcement '$title' ($statusType) posted; FCM/WhatsApp Cloud dispatch requires deployed Cloud Functions.",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            coreRepository.writeAuditLog(
                action = "ANNOUNCEMENT_POSTED",
                targetUid = null,
                details = "Posted $statusType notice: ${title.take(80)}"
            )
            annId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            throw e
        }
    }

    suspend fun adminDeactivateAnnouncement(announcementId: String): Result<Unit> = runCatching {
        val ref = db.collection("gym_announcements").document(announcementId)
        try {
            ref.update(mapOf("active" to false)).await()
            coreRepository.writeAuditLog(
                action = "ANNOUNCEMENT_CLEARED",
                targetUid = null,
                details = "Deactivated announcement $announcementId"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    /**
     * Admin-only sandbox reconciliation helper so admins can verify PDF GST invoice rendering,
     * UPI AutoPay pre-debit reminders, and audit logs before deploying Cloud Functions webhooks.
     */
    suspend fun adminReconcileSandboxPayment(request: PaymentRequest): Result<Unit> = runCatching {
        val payId = "pay_sbx_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val invoiceNo = "BB-SBX-${System.currentTimeMillis().toString().takeLast(6)}"
        val baseInr = (request.amountInr / 1.18).toLong()
        val gstInr = request.amountInr - baseInr

        try {
            db.collection("payments").document(payId).set(
                mapOf(
                    "userId" to request.userId,
                    "memberName" to request.memberName,
                    "memberEmail" to request.memberEmail,
                    "planId" to request.planId,
                    "planName" to request.planName,
                    "tier" to request.tier,
                    "baseAmountInr" to baseInr,
                    "gstAmountInr" to gstInr,
                    "totalAmountInr" to request.amountInr,
                    "currency" to "INR",
                    "paymentMode" to request.paymentMode,
                    "razorpayOrderId" to "order_sandbox_${request.id.takeLast(6)}",
                    "razorpayPaymentId" to payId,
                    "webhookEventId" to "evt_sandbox_${System.currentTimeMillis()}",
                    "status" to PaymentRecord.STATUS_CAPTURED,
                    "invoiceNumber" to invoiceNo,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (request.paymentMode == PaymentRequest.MODE_UPI_AUTOPAY) {
                val subId = "sub_${request.userId.take(10)}_${request.planId}"
                val nowMs = System.currentTimeMillis()
                val nextDebit = Timestamp(Date(nowMs + 20L * 3_600_000L)) // 20h away triggers 24h Pre-Debit banner
                val periodEnd = Timestamp(Date(nowMs + 30L * 86_400_000L))
                db.collection("subscriptions").document(subId).set(
                    mapOf(
                        "userId" to request.userId,
                        "planId" to request.planId,
                        "planName" to request.planName,
                        "tier" to request.tier,
                        "billingCycle" to request.billingCycle,
                        "paymentMode" to request.paymentMode,
                        "amountInr" to request.amountInr,
                        "status" to MemberSubscription.STATUS_ACTIVE,
                        "upiHandle" to "${request.preferredUpiApp} AutoPay",
                        "razorpaySubscriptionId" to "sub_sbx_${request.id.takeLast(6)}",
                        "nextDebitAt" to nextDebit,
                        "preDebitReminderAt" to Timestamp(Date(nowMs)),
                        "preDebitNotified" to true,
                        "currentPeriodEnd" to periodEnd,
                        "autoRenew" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            db.collection("users").document(request.userId).update(
                mapOf(
                    "planTier" to request.tier,
                    "membershipStatus" to "active",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            coreRepository.writeAuditLog(
                action = "SANDBOX_WEBHOOK_RECONCILED",
                targetUid = request.userId,
                details = "Admin reconciled sandbox invoice $invoiceNo (${request.planName}, INR ${request.amountInr})"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "payments/$payId")
            throw e
        }
    }
}
