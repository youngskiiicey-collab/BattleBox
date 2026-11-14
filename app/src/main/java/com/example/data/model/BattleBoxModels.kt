package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String? = null,
    val emergencyContact: String? = null,
    val photoUrl: String? = null,
    val role: String = ROLE_MEMBER,
    val planTier: String = PLAN_BASIC,
    val membershipStatus: String = STATUS_ACTIVE,
    val assignedTrainerId: String? = null,
    val onboardingCompleted: Boolean = false,
    val termsVersion: String = CURRENT_TERMS_VERSION,
    val dpdpConsentAccepted: Boolean = false,
    val healthWaiverAccepted: Boolean = false,
    @ServerTimestamp val consentAcceptedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        const val ROLE_MEMBER = "member"
        const val ROLE_TRAINER = "trainer"
        const val ROLE_ADMIN = "admin"

        const val PLAN_BASIC = "basic"
        const val PLAN_PREMIUM = "premium"

        const val STATUS_ACTIVE = "active"
        const val STATUS_FROZEN = "frozen"
        const val STATUS_EXPIRED = "expired"
        const val STATUS_PENDING = "pending"

        const val CURRENT_TERMS_VERSION = "1.0-draft"
    }
}

@IgnoreExtraProperties
data class GymConfig(
    @DocumentId val id: String = "",
    val gymName: String = "Battle Box : FFF",
    val mapsUrl: String = "https://maps.app.goo.gl/4ji9NFtZwpXZSiTN9",
    val latitude: Double = 25.9833,
    val longitude: Double = 90.7833,
    val geofenceRadiusM: Double = 100.0,
    val maxGpsAccuracyM: Double = 50.0,
    val autoCheckoutHours: Double = 3.0,
    val morningOpen: String = "05:30",
    val morningClose: String = "10:00",
    val eveningOpen: String = "16:30",
    val eveningClose: String = "21:30",
    val timezone: String = "Asia/Kolkata",
    val currency: String = "INR",
    val dataRegion: String = "asia-south1 (Mumbai)",
    val manualStatusOverride: String = "auto",
    val maxCapacity: Long = 40L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class GymPublicStatus(
    @DocumentId val id: String = "",
    val inGymCount: Long = 0L,
    val isOpen: Boolean = true,
    val statusLabel: String = "OPEN",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val memberName: String = "",
    val memberRole: String = UserProfile.ROLE_MEMBER,
    val mode: String = MODE_KIOSK_QR,
    val status: String = STATUS_CHECKED_IN,
    val distanceMeters: Double = 0.0,
    val gpsAccuracyMeters: Double = 0.0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val verifiedByUid: String = "",
    val qrEpochSlot: Long = 0L,
    @ServerTimestamp val checkInAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val checkOutAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        const val MODE_FRONT_DESK = "front_desk"
        const val MODE_TRAINER_SCAN = "trainer_scan"
        const val MODE_KIOSK_QR = "kiosk_qr"

        const val STATUS_CHECKED_IN = "checked_in"
        const val STATUS_CHECKED_OUT = "checked_out"
        const val STATUS_AUTO_CHECKED_OUT = "auto_checked_out"
    }
}

@IgnoreExtraProperties
data class AttendanceRejection(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mode: String = AttendanceRecord.MODE_KIOSK_QR,
    val reason: String = "",
    val distanceMeters: Double = 0.0,
    val gpsAccuracyMeters: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AuditLogEntry(
    @DocumentId val id: String = "",
    val actorUid: String = "",
    val actorEmail: String = "",
    val action: String = "",
    val targetUid: String? = null,
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ConsentRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val termsVersion: String = UserProfile.CURRENT_TERMS_VERSION,
    val dpdpConsentAccepted: Boolean = false,
    val healthWaiverAccepted: Boolean = false,
    @ServerTimestamp val acceptedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
