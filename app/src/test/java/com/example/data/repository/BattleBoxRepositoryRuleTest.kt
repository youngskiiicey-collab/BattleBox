package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.GymConfig
import com.example.data.model.GymPlan
import com.example.data.model.NotificationPreference
import com.example.data.model.PaymentRequest
import com.example.security.DeviceFix
import com.example.security.QrTokenSigner
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class BattleBoxRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun onboardingAndKioskCheckIn_validWithinGeofence_succeedsAndObservesAttendance() = runBlocking {
        val uniqueEmail = "member_${UUID.randomUUID().toString().take(8)}@test.com"
        val uid = signInTestUser(uniqueEmail)
        val repository = BattleBoxRepository(firestore)

        val onboardRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.completeOnboarding(
                displayName = "Rohan Kapoor",
                email = uniqueEmail,
                phone = "+919876543210",
                emergencyContact = "Priya +919811112222",
                photoUrl = null,
                isBootstrappedOwner = false
            )
        }
        assertTrue(onboardRes.isSuccess)

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfile(uid).first { it != null }
        }!!
        assertEquals("member", profile.role)
        assertEquals("basic", profile.planTier)
        assertTrue(profile.dpdpConsentAccepted)
        assertTrue(profile.healthWaiverAccepted)
        assertTrue("Expected consentAcceptedAt server timestamp", profile.consentAcceptedAt != null)

        val consentReceipts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMyConsents(uid).first { it.isNotEmpty() }
        }
        assertEquals(1, consentReceipts.size)
        val receipt = consentReceipts.first()
        assertEquals(uid, receipt.userId)
        assertTrue(receipt.dpdpConsentAccepted)
        assertTrue(receipt.healthWaiverAccepted)
        assertTrue("Expected consent acceptedAt server timestamp", receipt.acceptedAt != null)

        val kioskQr = QrTokenSigner.generateKioskToken()
        val gymConfig = GymConfig()
        val insideFix = DeviceFix(
            latitude = gymConfig.latitude,
            longitude = gymConfig.longitude,
            accuracyMeters = 12.0
        )

        val outcome = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.performKioskSelfCheckIn(
                scannedQrPayload = kioskQr,
                currentUser = profile,
                gymConfig = gymConfig,
                deviceFix = insideFix
            )
        }
        assertTrue("Expected CheckedIn but got $outcome", outcome is CheckInOutcome.CheckedIn)

        val records = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMyAttendance(uid).first { it.isNotEmpty() }
        }
        assertEquals(1, records.size)
        assertEquals("checked_in", records.first().status)
    }

    @Test
    fun kioskCheckIn_outsideGeofence_rejectsAndLogsRejection() = runBlocking {
        val uniqueEmail = "far_${UUID.randomUUID().toString().take(8)}@test.com"
        val uid = signInTestUser(uniqueEmail)
        val repository = BattleBoxRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.completeOnboarding(
                displayName = "Far Member",
                email = uniqueEmail,
                phone = null,
                emergencyContact = null,
                photoUrl = null,
                isBootstrappedOwner = false
            )
        }
        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfile(uid).first { it != null }
        }!!

        val kioskQr = QrTokenSigner.generateKioskToken()
        val farFix = DeviceFix(latitude = 28.5000, longitude = 77.2000, accuracyMeters = 10.0)
        val outcome = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.performKioskSelfCheckIn(
                scannedQrPayload = kioskQr,
                currentUser = profile,
                gymConfig = GymConfig(),
                deviceFix = farFix
            )
        }
        assertTrue(outcome is CheckInOutcome.Rejected)
    }

    @Test
    fun observeMyAttendance_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = BattleBoxRepository(firestore)

        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeMyAttendance("any_uid").first()
            }
        } catch (e: Exception) {
            caught = e
        }
        val fsEx = caught?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeAllUsers_nonAdminMember_failsWithPermissionDenied() = runBlocking {
        val uniqueEmail = "nonadmin_${UUID.randomUUID().toString().take(8)}@test.com"
        signInTestUser(uniqueEmail)
        val repository = BattleBoxRepository(firestore)

        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeAllUsers().first()
            }
        } catch (e: Exception) {
            caught = e
        }
        val fsEx = caught?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun phase2_checkoutIntentAndWhatsAppOptIn_succeed_whileClientPaymentSpoofDenied() = runBlocking {
        val uniqueEmail = "p2_${UUID.randomUUID().toString().take(8)}@test.com"
        val uid = signInTestUser(uniqueEmail)
        val coreRepo = BattleBoxRepository(firestore)
        val p2Repo = Phase2Repository(firestore, coreRepo)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            coreRepo.completeOnboarding(
                displayName = "Meera Nair",
                email = uniqueEmail,
                phone = "+919800001122",
                emergencyContact = null,
                photoUrl = null,
                isBootstrappedOwner = false
            )
        }
        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            coreRepo.observeUserProfile(uid).first { it != null }
        }!!

        val premiumPlan = GymPlan.defaultSeedPlans().first { it.id == "premium_monthly" }
        val intentRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            p2Repo.createCheckoutIntent(
                user = profile,
                plan = premiumPlan,
                paymentMode = PaymentRequest.MODE_UPI_AUTOPAY,
                preferredUpiApp = "Google Pay"
            )
        }
        assertTrue("createCheckoutIntent failed: ${intentRes.exceptionOrNull()}", intentRes.isSuccess)

        val prefRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            p2Repo.saveNotificationPreference(
                NotificationPreference(
                    userId = uid,
                    whatsappOptIn = true,
                    whatsappPhone = "+919800001122",
                    expiryReminder3d = true,
                    dueTodayReminder = true,
                    paymentAlerts = true,
                    gymClosedAlerts = true,
                    preDebitAlerts = true,
                    pushEnabled = true
                )
            )
        }
        assertTrue("saveNotificationPreference failed: ${prefRes.exceptionOrNull()}", prefRes.isSuccess)

        val savedPref = withTimeout(FLOW_TIMEOUT_MS) {
            p2Repo.observeNotificationPreference(uid).first { it.whatsappOptIn }
        }
        assertEquals("+919800001122", savedPref.whatsappPhone)

        // Attempt to write directly into payments (only Cloud Functions webhooks / Admins may write)
        var spoofError: Exception? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("payments").document("pay_client_spoof").set(
                    mapOf(
                        "userId" to uid,
                        "memberName" to profile.displayName,
                        "memberEmail" to profile.email,
                        "planId" to premiumPlan.id,
                        "planName" to premiumPlan.name,
                        "tier" to premiumPlan.tier,
                        "baseAmountInr" to 2542L,
                        "gstAmountInr" to 457L,
                        "totalAmountInr" to 2999L,
                        "currency" to "INR",
                        "paymentMode" to PaymentRequest.MODE_ONE_TIME,
                        "razorpayOrderId" to "order_fake",
                        "razorpayPaymentId" to "pay_fake",
                        "webhookEventId" to "evt_fake",
                        "status" to "captured",
                        "invoiceNumber" to "BB-SPOOF",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            spoofError = e
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, spoofError?.unwrapFirestoreException()?.code)
    }

    @Test
    fun observeLiveAttendanceRoster_regularMember_failsWithPermissionDenied() = runBlocking {
        val uniqueEmail = "member_roster_${UUID.randomUUID().toString().take(8)}@test.com"
        signInTestUser(uniqueEmail)
        val repository = BattleBoxRepository(firestore)

        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeLiveAttendanceRoster().first()
            }
        } catch (e: Exception) {
            caught = e
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught?.unwrapFirestoreException()?.code)
    }

    @Test
    fun trainerQrScan_validMemberQr_storesAttendanceAndUpdatesLiveOccupancy() = runBlocking {
        val repository = BattleBoxRepository(firestore)
        val gymConfig = GymConfig()
        val insideFix = DeviceFix(gymConfig.latitude, gymConfig.longitude, accuracyMeters = 10.0)

        // 1. Onboard member Riya
        val memberEmail = "riya_${UUID.randomUUID().toString().take(8)}@test.com"
        val memberUid = signInTestUser(memberEmail)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.completeOnboarding(
                displayName = "Riya Basumatary",
                email = memberEmail,
                phone = null,
                emergencyContact = null,
                photoUrl = null,
                isBootstrappedOwner = false
            )
        }
        val memberProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfile(memberUid).first { it != null }
        }!!

        // 2. Onboard Trainer Vikram, then promote via verified Bootstrapped Owner
        val trainerEmail = "trainer_${UUID.randomUUID().toString().take(8)}@test.com"
        val trainerUid = signInTestUser(trainerEmail)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.completeOnboarding(
                displayName = "Coach Vikram",
                email = trainerEmail,
                phone = null,
                emergencyContact = null,
                photoUrl = null,
                isBootstrappedOwner = false
            )
        }
        val trainerProfileBefore = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfile(trainerUid).first { it != null }
        }!!

        signInVerifiedBootstrappedOwner()
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.completeOnboarding(
                displayName = "Gym Owner",
                email = "youngskiiicey@gmail.com",
                phone = null,
                emergencyContact = null,
                photoUrl = null,
                isBootstrappedOwner = true
            )
        }
        val promoteRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.adminUpdateUser(
                targetUser = trainerProfileBefore,
                newRole = "trainer",
                newPlanTier = "basic",
                newMembershipStatus = "active",
                newAssignedTrainerId = null
            )
        }
        assertTrue("Promote trainer failed: ${promoteRes.exceptionOrNull()}", promoteRes.isSuccess)

        // 3. Sign back in as Trainer Vikram and scan Riya's rotating QR pass
        signInTestUser(trainerEmail)
        val memberQr = QrTokenSigner.generateMemberToken(memberProfile.userId)
        val scanOutcome = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.performTrainerQrScanCheckIn(
                scannedMemberQr = memberQr,
                scannerFix = insideFix,
                gymConfig = gymConfig
            )
        }
        assertTrue("Expected CheckedIn for trainer scan but got $scanOutcome", scanOutcome is CheckInOutcome.CheckedIn)
        val checkedIn = scanOutcome as CheckInOutcome.CheckedIn
        assertEquals("Riya Basumatary", checkedIn.memberName)

        // 4. Verify live occupancy roster includes Riya with trainer_scan mode
        val liveRoster = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeLiveAttendanceRoster().first { list -> list.any { it.userId == memberUid } }
        }
        val riyaRecord = liveRoster.first { it.userId == memberUid }
        assertEquals("trainer_scan", riyaRecord.mode)
        assertEquals(trainerUid, riyaRecord.verifiedByUid)

        // 5. Trainer checks out Riya and live occupancy updates
        val checkOutRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.checkOutSession(riyaRecord.id, isAdminAction = false, targetUserName = "Riya Basumatary")
        }
        assertTrue("Trainer checkout failed: ${checkOutRes.exceptionOrNull()}", checkOutRes.isSuccess)

        val updatedRoster = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeLiveAttendanceRoster().first { list -> list.none { it.userId == memberUid } }
        }
        assertTrue(updatedRoster.none { it.userId == memberUid })
    }

    @Test
    fun adminFetchMonthlyAttendanceRecords_returnsCurrentMonthLogsAndWritesAuditLog() = runBlocking {
        val repository = BattleBoxRepository(firestore)
        val gymConfig = GymConfig()
        val insideFix = DeviceFix(gymConfig.latitude, gymConfig.longitude, accuracyMeters = 10.0)

        // 1. Member performs valid kiosk check-in in the current IST month
        val memberEmail = "csv_member_${UUID.randomUUID().toString().take(8)}@test.com"
        val memberUid = signInTestUser(memberEmail)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.completeOnboarding(
                displayName = "Bikash Rabha",
                email = memberEmail,
                phone = null,
                emergencyContact = null,
                photoUrl = null,
                isBootstrappedOwner = false
            )
        }
        val memberProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfile(memberUid).first { it != null }
        }!!
        val kioskQr = QrTokenSigner.generateKioskToken("main")
        val checkInOutcome = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.performKioskSelfCheckIn(
                scannedQrPayload = kioskQr,
                currentUser = memberProfile,
                gymConfig = gymConfig,
                deviceFix = insideFix
            )
        }
        assertTrue("Expected CheckedIn but got $checkInOutcome", checkInOutcome is CheckInOutcome.CheckedIn)

        // 2. Bootstrapped Admin queries current IST month records for CSV export
        signInVerifiedBootstrappedOwner()
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.completeOnboarding(
                displayName = "Gym Owner",
                email = "youngskiiicey@gmail.com",
                phone = null,
                emergencyContact = null,
                photoUrl = null,
                isBootstrappedOwner = true
            )
        }
        val (curYear, curMonth) = com.example.utils.AttendanceCsvExporter.currentYearMonthIst()
        val exportResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.fetchMonthlyAttendanceRecords(curYear, curMonth)
        }
        assertTrue("Monthly export query failed: ${exportResult.exceptionOrNull()}", exportResult.isSuccess)
        val records = exportResult.getOrThrow()
        assertTrue("Expected Bikash Rabha record in monthly export", records.any { it.userId == memberUid })

        val csv = com.example.utils.AttendanceCsvExporter.toCsv(records)
        assertTrue(csv.contains("Bikash Rabha"))

        // 3. Verify ATTENDANCE_CSV_EXPORTED audit entry was recorded
        val auditLogs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeAuditLogs().first { list -> list.any { it.action == "ATTENDANCE_CSV_EXPORTED" } }
        }
        assertTrue(auditLogs.any { it.action == "ATTENDANCE_CSV_EXPORTED" })
    }

    private suspend fun signInVerifiedBootstrappedOwner() {
        val ownerEmail = "youngskiiicey@gmail.com"
        val uid = signInTestUser(ownerEmail)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val url = java.net.URL(
                "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/projects/demo-no-project/accounts:update"
            )
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer owner")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            val body = """{"localId":"$uid","emailVerified":true}"""
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            check(code in 200..299) { "Auth emulator admin update failed with HTTP $code" }
        }
        auth.signOut()
        auth.signInWithEmailAndPassword(ownerEmail, "password123").await()
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
