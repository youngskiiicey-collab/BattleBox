package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class GymPlan(
    @DocumentId val id: String = "",
    val name: String = "",
    val tier: String = UserProfile.PLAN_BASIC, // basic | premium
    val billingCycle: String = CYCLE_MONTHLY, // monthly | quarterly | annual
    val baseAmountInr: Long = 0L,
    val gstPercent: Long = 18L,
    val totalAmountInr: Long = 0L,
    val durationDays: Long = 30L,
    val supportsOneTime: Boolean = true,
    val supportsUpiAutopay: Boolean = true,
    val description: String = "",
    val active: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        const val CYCLE_MONTHLY = "monthly"
        const val CYCLE_QUARTERLY = "quarterly"
        const val CYCLE_ANNUAL = "annual"

        fun defaultSeedPlans(): List<GymPlan> = listOf(
            GymPlan(
                id = "basic_monthly",
                name = "Basic Monthly",
                tier = UserProfile.PLAN_BASIC,
                billingCycle = CYCLE_MONTHLY,
                baseAmountInr = 1270L,
                gstPercent = 18L,
                totalAmountInr = 1499L,
                durationDays = 30L,
                description = "Full floor access, heavy bags, rotating QR check-in & IST sessions."
            ),
            GymPlan(
                id = "basic_quarterly",
                name = "Basic Quarterly",
                tier = UserProfile.PLAN_BASIC,
                billingCycle = CYCLE_QUARTERLY,
                baseAmountInr = 3389L,
                gstPercent = 18L,
                totalAmountInr = 3999L,
                durationDays = 90L,
                description = "3-month Battle Box access with up to 14-day travel freeze eligibility."
            ),
            GymPlan(
                id = "basic_annual",
                name = "Basic Annual",
                tier = UserProfile.PLAN_BASIC,
                billingCycle = CYCLE_ANNUAL,
                baseAmountInr = 11864L,
                gstPercent = 18L,
                totalAmountInr = 13999L,
                durationDays = 365L,
                description = "12-month floor pass with up to 60-day freeze benefit."
            ),
            GymPlan(
                id = "premium_monthly",
                name = "Premium Monthly",
                tier = UserProfile.PLAN_PREMIUM,
                billingCycle = CYCLE_MONTHLY,
                baseAmountInr = 2542L,
                gstPercent = 18L,
                totalAmountInr = 2999L,
                durationDays = 30L,
                description = "Unlocks AI Coach, trainer workout/diet plans & advanced analytics."
            ),
            GymPlan(
                id = "premium_quarterly",
                name = "Premium Quarterly",
                tier = UserProfile.PLAN_PREMIUM,
                billingCycle = CYCLE_QUARTERLY,
                baseAmountInr = 6779L,
                gstPercent = 18L,
                totalAmountInr = 7999L,
                durationDays = 90L,
                description = "3 months Premium tier with AI Coach, custom plans & UPI AutoPay."
            ),
            GymPlan(
                id = "premium_annual",
                name = "Premium Annual",
                tier = UserProfile.PLAN_PREMIUM,
                billingCycle = CYCLE_ANNUAL,
                baseAmountInr = 22881L,
                gstPercent = 18L,
                totalAmountInr = 26999L,
                durationDays = 365L,
                description = "Best-value annual Premium membership with full analytics & AI Coach."
            )
        )
    }
}

@IgnoreExtraProperties
data class PaymentRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val memberName: String = "",
    val memberEmail: String = "",
    val planId: String = "",
    val planName: String = "",
    val tier: String = UserProfile.PLAN_BASIC,
    val billingCycle: String = GymPlan.CYCLE_MONTHLY,
    val paymentMode: String = MODE_ONE_TIME, // one_time_order | upi_autopay_subscription
    val amountInr: Long = 0L,
    val currency: String = "INR",
    val preferredUpiApp: String = "Google Pay",
    val status: String = STATUS_CREATED, // created | awaiting_backend_order
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        const val MODE_ONE_TIME = "one_time_order"
        const val MODE_UPI_AUTOPAY = "upi_autopay_subscription"

        const val STATUS_CREATED = "created"
        const val STATUS_AWAITING_BACKEND = "awaiting_backend_order"
    }
}

@IgnoreExtraProperties
data class MemberSubscription(
    @DocumentId val id: String = "",
    val userId: String = "",
    val planId: String = "",
    val planName: String = "",
    val tier: String = UserProfile.PLAN_BASIC,
    val billingCycle: String = GymPlan.CYCLE_MONTHLY,
    val paymentMode: String = PaymentRequest.MODE_UPI_AUTOPAY,
    val amountInr: Long = 0L,
    val status: String = STATUS_ACTIVE, // pending_auth | active | cancellation_requested | cancelled | expired
    val upiHandle: String = "Google Pay / UPI AutoPay",
    val razorpaySubscriptionId: String = "",
    val nextDebitAt: Timestamp? = null,
    val preDebitReminderAt: Timestamp? = null,
    val preDebitNotified: Boolean = false,
    val currentPeriodEnd: Timestamp? = null,
    val autoRenew: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        const val STATUS_PENDING_AUTH = "pending_auth"
        const val STATUS_ACTIVE = "active"
        const val STATUS_CANCEL_REQUESTED = "cancellation_requested"
        const val STATUS_CANCELLED = "cancelled"
        const val STATUS_EXPIRED = "expired"
    }
}

@IgnoreExtraProperties
data class PaymentRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val memberName: String = "",
    val memberEmail: String = "",
    val planId: String = "",
    val planName: String = "",
    val tier: String = UserProfile.PLAN_BASIC,
    val baseAmountInr: Long = 0L,
    val gstAmountInr: Long = 0L,
    val totalAmountInr: Long = 0L,
    val currency: String = "INR",
    val paymentMode: String = PaymentRequest.MODE_ONE_TIME,
    val razorpayOrderId: String = "",
    val razorpayPaymentId: String = "",
    val webhookEventId: String = "",
    val status: String = STATUS_PENDING_WEBHOOK, // pending_webhook | captured | failed | refunded
    val invoiceNumber: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        const val STATUS_PENDING_WEBHOOK = "pending_webhook"
        const val STATUS_CAPTURED = "captured"
        const val STATUS_FAILED = "failed"
        const val STATUS_REFUNDED = "refunded"
    }
}

@IgnoreExtraProperties
data class NotificationPreference(
    @DocumentId val id: String = "",
    val userId: String = "",
    val whatsappOptIn: Boolean = false,
    val whatsappPhone: String = "",
    val expiryReminder3d: Boolean = true,
    val dueTodayReminder: Boolean = true,
    val paymentAlerts: Boolean = true,
    val gymClosedAlerts: Boolean = true,
    val preDebitAlerts: Boolean = true,
    val pushEnabled: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class GymAnnouncement(
    @DocumentId val id: String = "",
    val statusType: String = TYPE_OPEN, // open | closed | special_hours | holiday
    val title: String = "",
    val message: String = "",
    val effectiveHours: String = "",
    val targetAudience: String = "all_members_and_trainers",
    val sendPushNotification: Boolean = true,
    val sendWhatsappClosure: Boolean = false,
    val active: Boolean = true,
    val createdByUid: String = "",
    val createdByEmail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    companion object {
        const val TYPE_OPEN = "open"
        const val TYPE_CLOSED = "closed"
        const val TYPE_SPECIAL_HOURS = "special_hours"
        const val TYPE_HOLIDAY = "holiday"
    }
}

@IgnoreExtraProperties
data class NotificationDeliveryLog(
    @DocumentId val id: String = "",
    val recipientUid: String = "",
    val recipientContact: String = "",
    val channel: String = CHANNEL_IN_APP, // whatsapp | fcm_push | in_app
    val templateName: String = "",
    val status: String = STATUS_QUEUED, // queued | sent | delivered | failed
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    companion object {
        const val CHANNEL_WHATSAPP = "whatsapp"
        const val CHANNEL_FCM_PUSH = "fcm_push"
        const val CHANNEL_IN_APP = "in_app"

        const val STATUS_QUEUED = "queued"
        const val STATUS_SENT = "sent"
        const val STATUS_DELIVERED = "delivered"
        const val STATUS_FAILED = "failed"
    }
}
