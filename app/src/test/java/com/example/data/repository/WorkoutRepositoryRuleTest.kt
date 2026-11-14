package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.GymConfig
import com.example.security.DeviceFix
import com.example.security.QrTokenSigner
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class WorkoutRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun workoutProfileAndWorkoutLogs_persistAndIsolateAcrossUsers() = runBlocking {
        val repo = WorkoutRepository(firestore)

        val aliceEmail = "workout_alice_${UUID.randomUUID().toString().take(8)}@test.com"
        val aliceUid = signInTestUser(aliceEmail)

        val initialProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeWorkoutProfile(aliceUid).first()
        }
        val metricRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updateBodyMetrics(initialProfile, newWeightKg = 70.0, newHeightCm = 175.0)
        }
        assertTrue("updateBodyMetrics failed: ${metricRes.exceptionOrNull()}", metricRes.isSuccess)
        val updated = metricRes.getOrThrow()
        assertEquals(22.8, updated.bmiRatio, 0.2)

        val saveLogRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.completeAndSaveWorkoutSession(
                currentProfile = updated,
                workoutTitle = "Full Gym Push & Pull",
                durationSeconds = 2100L,
                totalVolumeKg = 5400.0,
                completedSets = 15,
                exercisesCompleted = 5,
                updatedPrs = updated.personalRecords,
                prAchievedLabel = "45° Leg Press: 140kg x 12"
            )
        }
        assertTrue("completeAndSaveWorkoutSession failed: ${saveLogRes.exceptionOrNull()}", saveLogRes.isSuccess)

        val aliceLogs = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMyWorkoutLogs(aliceUid).first { it.isNotEmpty() }
        }
        assertEquals("Full Gym Push & Pull", aliceLogs.first().workoutTitle)

        // Second user Bob cannot read Alice's workout profile or logs
        val bobEmail = "workout_bob_${UUID.randomUUID().toString().take(8)}@test.com"
        signInTestUser(bobEmail)
        var deniedException: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeWorkoutProfile(aliceUid).first()
            }
        } catch (e: Exception) {
            deniedException = e
        }
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            deniedException?.unwrapFirestoreException()?.code
        )
    }

    @Test
    fun geofenceAutoOut_whenOutsideRadiusOrPoorGps_checksOutMember() = runBlocking {
        val coreRepo = BattleBoxRepository(firestore)
        val gymConfig = GymConfig(geofenceRadiusM = 100.0, maxGpsAccuracyM = 50.0)
        val insideFix = DeviceFix(gymConfig.latitude, gymConfig.longitude, accuracyMeters = 10.0)

        val email = "geo_auto_${UUID.randomUUID().toString().take(8)}@test.com"
        val uid = signInTestUser(email)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            coreRepo.completeOnboarding(
                displayName = "Geofence Member",
                email = email,
                phone = null,
                emergencyContact = null,
                photoUrl = null,
                isBootstrappedOwner = false
            )
        }
        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            coreRepo.observeUserProfile(uid).first { it != null }
        }!!
        val wallToken = QrTokenSigner.generatePermanentWallToken("main")
        val checkIn = withTimeout(DEFAULT_TIMEOUT_MS) {
            coreRepo.performKioskSelfCheckIn(
                scannedQrPayload = wallToken,
                currentUser = profile,
                gymConfig = gymConfig,
                deviceFix = insideFix
            )
        }
        assertTrue(checkIn is CheckInOutcome.CheckedIn)

        val activeRecord = withTimeout(FLOW_TIMEOUT_MS) {
            coreRepo.observeMyAttendance(uid).first { list -> list.any { it.status == "checked_in" } }
        }.first { it.status == "checked_in" }

        val outsideFix = DeviceFix(gymConfig.latitude + 0.01, gymConfig.longitude + 0.01, accuracyMeters = 12.0)
        val autoOutRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            coreRepo.autoCheckOutIfOutsideGeofence(activeRecord, outsideFix, gymConfig)
        }
        assertTrue("autoCheckOutIfOutsideGeofence failed: ${autoOutRes.exceptionOrNull()}", autoOutRes.isSuccess)
        assertFalse(autoOutRes.getOrThrow().isInsideGeofence)

        val updatedRecords = withTimeout(FLOW_TIMEOUT_MS) {
            coreRepo.observeMyAttendance(uid).first { list ->
                list.any { it.id == activeRecord.id && it.status == "auto_checked_out" }
            }
        }
        assertTrue(updatedRecords.any { it.id == activeRecord.id && it.status == "auto_checked_out" })
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
