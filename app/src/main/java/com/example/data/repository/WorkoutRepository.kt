package com.example.data.repository

import com.example.data.model.PersonalRecordEntry
import com.example.data.model.WeightSample
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutProfile
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class WorkoutRepository(private val db: FirebaseFirestore) {

    private fun requireUserId(): String =
        Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Workout Mode data.")

    fun observeWorkoutProfile(uid: String): Flow<WorkoutProfile> =
        db.collection("workout_profiles").document(uid)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(WorkoutProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?.copy(id = snapshot.id)
                        ?: WorkoutProfile(id = uid, userId = uid)
                } else {
                    WorkoutProfile(id = uid, userId = uid)
                }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.GET, "workout_profiles/$uid")
                throw error
            }

    fun observeMyWorkoutLogs(uid: String): Flow<List<WorkoutLog>> =
        db.collection("workout_logs")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(WorkoutLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "workout_logs")
                throw error
            }

    suspend fun saveWorkoutProfile(profile: WorkoutProfile): Result<Unit> = runCatching {
        val uid = requireUserId()
        val ref = db.collection("workout_profiles").document(uid)
        val payload = mapOf(
            "userId" to uid,
            "heightCm" to profile.heightCm.coerceIn(100.0, 240.0),
            "weightKg" to profile.weightKg.coerceIn(25.0, 250.0),
            "calorieTarget" to profile.calorieTarget.coerceIn(800, 8000),
            "activeMinutesTarget" to profile.activeMinutesTarget.coerceIn(10, 300),
            "proteinTargetG" to profile.proteinTargetG.coerceIn(20, 400),
            "caloriesToday" to profile.caloriesToday.coerceIn(0, 10000),
            "activeMinutesToday" to profile.activeMinutesToday.coerceIn(0, 600),
            "proteinTodayG" to profile.proteinTodayG.coerceIn(0, 500),
            "streakDays" to profile.streakDays.coerceAtLeast(0),
            "lastWorkoutDateIso" to profile.lastWorkoutDateIso.take(32),
            "personalRecords" to profile.personalRecords.take(15).map { pr ->
                mapOf(
                    "exerciseId" to pr.exerciseId,
                    "exerciseName" to pr.exerciseName,
                    "equipmentTag" to pr.equipmentTag,
                    "bestWeightKg" to pr.bestWeightKg,
                    "bestReps" to pr.bestReps,
                    "achievedDateLabel" to pr.achievedDateLabel
                )
            },
            "weightHistory" to profile.weightHistory.takeLast(12).map { sample ->
                mapOf(
                    "dateLabel" to sample.dateLabel,
                    "weightKg" to sample.weightKg
                )
            },
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

    suspend fun recordQuickMacroEntry(
        current: WorkoutProfile,
        addCalories: Int,
        addProteinG: Int,
        addActiveMinutes: Int
    ): Result<WorkoutProfile> = runCatching {
        val updated = current.copy(
            userId = current.userId.ifBlank { requireUserId() },
            caloriesToday = (current.caloriesToday + addCalories).coerceAtLeast(0),
            proteinTodayG = (current.proteinTodayG + addProteinG).coerceAtLeast(0),
            activeMinutesToday = (current.activeMinutesToday + addActiveMinutes).coerceAtLeast(0)
        )
        saveWorkoutProfile(updated).getOrThrow()
        updated
    }

    suspend fun updateBodyMetrics(
        current: WorkoutProfile,
        newWeightKg: Double,
        newHeightCm: Double
    ): Result<WorkoutProfile> = runCatching {
        val labelFmt = SimpleDateFormat("dd MMM", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        val todayLabel = labelFmt.format(Date())
        val updatedHistory = (current.weightHistory.filterNot { it.dateLabel == todayLabel } +
            WeightSample(dateLabel = todayLabel, weightKg = newWeightKg)).takeLast(8)

        val updated = current.copy(
            userId = current.userId.ifBlank { requireUserId() },
            weightKg = newWeightKg.coerceIn(25.0, 250.0),
            heightCm = newHeightCm.coerceIn(100.0, 240.0),
            weightHistory = updatedHistory
        )
        saveWorkoutProfile(updated).getOrThrow()
        updated
    }

    suspend fun completeAndSaveWorkoutSession(
        currentProfile: WorkoutProfile,
        workoutTitle: String,
        durationSeconds: Long,
        totalVolumeKg: Double,
        completedSets: Int,
        exercisesCompleted: Int,
        updatedPrs: List<PersonalRecordEntry>,
        prAchievedLabel: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val logId = "wlog_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val logRef = db.collection("workout_logs").document(logId)
        val minutes = (durationSeconds / 60L).toInt().coerceAtLeast(1)
        val estimatedKcal = ((minutes * 7) + (totalVolumeKg * 0.04).toInt()).coerceIn(20, 1800)

        val logPayload = mapOf(
            "userId" to uid,
            "workoutTitle" to workoutTitle.ifBlank { "Full Gym Push & Pull" }.take(80),
            "durationSeconds" to durationSeconds.coerceAtLeast(0L),
            "totalVolumeKg" to totalVolumeKg.coerceAtLeast(0.0),
            "caloriesBurned" to estimatedKcal,
            "completedSets" to completedSets.coerceAtLeast(0),
            "exercisesCompleted" to exercisesCompleted.coerceAtLeast(0),
            "prAchievedLabel" to prAchievedLabel.take(120),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            logRef.set(logPayload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, logRef.path)
            throw e
        }

        val isoFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        val todayIso = isoFmt.format(Date())
        val nextStreak = if (currentProfile.lastWorkoutDateIso == todayIso) {
            currentProfile.streakDays.coerceAtLeast(1)
        } else {
            currentProfile.streakDays + 1
        }
        val nextProfile = currentProfile.copy(
            userId = uid,
            caloriesToday = currentProfile.caloriesToday + estimatedKcal,
            activeMinutesToday = currentProfile.activeMinutesToday + minutes,
            streakDays = nextStreak,
            lastWorkoutDateIso = todayIso,
            personalRecords = updatedPrs
        )
        saveWorkoutProfile(nextProfile).getOrThrow()
        logId
    }
}
