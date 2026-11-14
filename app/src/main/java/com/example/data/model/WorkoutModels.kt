package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale
import kotlin.math.pow

@IgnoreExtraProperties
data class PersonalRecordEntry(
    val exerciseId: String = "",
    val exerciseName: String = "",
    val equipmentTag: String = "",
    val bestWeightKg: Double = 0.0,
    val bestReps: Int = 0,
    val achievedDateLabel: String = ""
)

@IgnoreExtraProperties
data class WeightSample(
    val dateLabel: String = "",
    val weightKg: Double = 0.0
)

@IgnoreExtraProperties
data class WorkoutProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val heightCm: Double = 172.0,
    val weightKg: Double = 72.0,
    val calorieTarget: Int = 2400,
    val activeMinutesTarget: Int = 60,
    val proteinTargetG: Int = 140,
    val caloriesToday: Int = 520,
    val activeMinutesToday: Int = 24,
    val proteinTodayG: Int = 42,
    val streakDays: Int = 3,
    val lastWorkoutDateIso: String = "",
    val personalRecords: List<PersonalRecordEntry> = defaultInitialPrs(),
    val weightHistory: List<WeightSample> = listOf(
        WeightSample("W1", 74.0),
        WeightSample("W2", 73.2),
        WeightSample("W3", 72.5),
        WeightSample("Now", 72.0)
    ),
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val bmiRatio: Double
        get() = calculateBmi(weightKg, heightCm)

    val bmiLabel: String
        get() = bmiCategory(bmiRatio)

    companion object {
        fun calculateBmi(weightKg: Double, heightCm: Double): Double {
            if (weightKg <= 0.0 || heightCm <= 40.0) return 0.0
            val heightM = heightCm / 100.0
            val raw = weightKg / heightM.pow(2.0)
            return ((raw * 10.0).toInt()) / 10.0
        }

        fun bmiCategory(bmi: Double): String = when {
            bmi <= 0.0 -> "Not Set"
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Optimal Fit"
            bmi < 30.0 -> "Overweight"
            else -> "Obese"
        }

        fun formatBmi(bmi: Double): String =
            if (bmi <= 0.0) "--" else String.format(Locale.US, "%.1f", bmi)

        fun defaultInitialPrs(): List<PersonalRecordEntry> = listOf(
            PersonalRecordEntry(
                exerciseId = "jerai_45_leg_press",
                exerciseName = "45° Leg Press",
                equipmentTag = "Jerai Leg Press Station",
                bestWeightKg = 140.0,
                bestReps = 12,
                achievedDateLabel = "Floor PR"
            ),
            PersonalRecordEntry(
                exerciseId = "smith_incline_press",
                exerciseName = "Smith Incline Press",
                equipmentTag = "Jerai Smith Rack (5kg–25kg Bumper Plates)",
                bestWeightKg = 60.0,
                bestReps = 8,
                achievedDateLabel = "Floor PR"
            ),
            PersonalRecordEntry(
                exerciseId = "db_single_arm_row",
                exerciseName = "Dumbbell Row",
                equipmentTag = "Jerai Round Rubber Dumbbells (up to 40kg)",
                bestWeightKg = 30.0,
                bestReps = 10,
                achievedDateLabel = "Floor PR"
            )
        )
    }
}

@IgnoreExtraProperties
data class WorkoutLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val workoutTitle: String = "Full Gym Push & Pull",
    val durationSeconds: Long = 0L,
    val totalVolumeKg: Double = 0.0,
    val caloriesBurned: Int = 0,
    val completedSets: Int = 0,
    val exercisesCompleted: Int = 0,
    val prAchievedLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class ExerciseZone(val label: String) {
    ALL("All"),
    JERAI_MACHINES("Jerai Machines"),
    CABLE_TOWER("Cable Tower"),
    SMITH_AND_RACKS("Smith & Racks"),
    DUMBBELLS("Dumbbells (up to 40kg)"),
    CARDIO_AND_TURF("Cardio & Turf")
}

data class ExerciseCatalogEntry(
    val id: String,
    val name: String,
    val zone: ExerciseZone,
    val equipmentTag: String,
    val muscleGroup: String,
    val defaultWeightKg: Double,
    val defaultReps: Int,
    val formCues: List<String>,
    val swapAlternativeIds: List<String>
)

data class WorkoutSetEntry(
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val completed: Boolean = false,
    val previousLiftText: String
)

data class ActiveStationExercise(
    val catalogId: String,
    val name: String,
    val equipmentTag: String,
    val zone: ExerciseZone,
    val previousLiftSummary: String,
    val sets: List<WorkoutSetEntry>
)

object DudhnoiFloorInventory {

    val catalog: List<ExerciseCatalogEntry> = listOf(
        // 1. Pre-loaded active workout station #1
        ExerciseCatalogEntry(
            id = "smith_flat_bench",
            name = "Smith Machine Flat Bench Press",
            zone = ExerciseZone.SMITH_AND_RACKS,
            equipmentTag = "Jerai Fitness Smith Rack + Olympic Bumper Plates (5kg–25kg)",
            muscleGroup = "Chest • Triceps • Front Delts",
            defaultWeightKg = 50.0,
            defaultReps = 10,
            formCues = listOf(
                "Align mid-sternum directly beneath the guided bar path with feet flat on the floor.",
                "Retract shoulder blades into the adjustable bench and stack wrists vertically over elbows.",
                "Control the eccentric 2 seconds to chest height before pressing up without locking elbows."
            ),
            swapAlternativeIds = listOf("jerai_plate_chest_press", "db_flat_bench", "db_incline_press")
        ),
        // 2. Pre-loaded active workout station #2
        ExerciseCatalogEntry(
            id = "cable_crossover_fly",
            name = "Seated Cable Crossover / Chest Fly",
            zone = ExerciseZone.CABLE_TOWER,
            equipmentTag = "Jerai Fitness Dual Cable Crossover Tower",
            muscleGroup = "Inner Chest • Anterior Deltoid",
            defaultWeightKg = 15.0,
            defaultReps = 12,
            formCues = listOf(
                "Set both pulleys at shoulder height and plant feet in a staggered athletic stance.",
                "Maintain a fixed ~15° soft bend at the elbows through the entire arc.",
                "Sweep handles together across the lower sternum and pause for a 1-second contraction."
            ),
            swapAlternativeIds = listOf("jerai_pec_deck_fly", "smith_flat_bench", "jerai_plate_chest_press")
        ),
        // 3. Pre-loaded active workout station #3
        ExerciseCatalogEntry(
            id = "db_incline_press",
            name = "Incline Dumbbell Press",
            zone = ExerciseZone.DUMBBELLS,
            equipmentTag = "Jerai Round Rubber Dumbbells (2.5kg–40kg) + Adjustable Bench",
            muscleGroup = "Upper Chest • Anterior Delts",
            defaultWeightKg = 20.0,
            defaultReps = 10,
            formCues = listOf(
                "Set the adjustable bench backrest to a 30°–45° incline angle with glutes anchored.",
                "Tuck elbows roughly 45° from your torso to keep glenohumeral joints protected.",
                "Lower dumbbells to outer-chest depth in a balanced arc and drive straight overhead."
            ),
            swapAlternativeIds = listOf("smith_incline_press", "jerai_plate_chest_press", "db_flat_bench")
        ),
        // 4. Pre-loaded active workout station #4
        ExerciseCatalogEntry(
            id = "jerai_45_leg_press",
            name = "45-Degree Plate-Loaded Leg Press",
            zone = ExerciseZone.JERAI_MACHINES,
            equipmentTag = "Jerai Fitness 45° Leg Press Station",
            muscleGroup = "Quads • Glutes • Adductors",
            defaultWeightKg = 120.0,
            defaultReps = 12,
            formCues = listOf(
                "Place feet hip-width apart at mid-sled height and grip side stability handles tightly.",
                "Keep lower back and tailbone pressed flat against the pad throughout descent.",
                "Track knees directly over middle toes and stop just shy of full knee lockout."
            ),
            swapAlternativeIds = listOf("smith_squat_rdl", "jerai_leg_extension", "jerai_leg_curl")
        ),
        // 5. Pre-loaded active workout station #5
        ExerciseCatalogEntry(
            id = "jerai_leg_extension",
            name = "Seated Leg Extension / Hamstring Curl",
            zone = ExerciseZone.JERAI_MACHINES,
            equipmentTag = "Jerai Selectorized Leg Machine",
            muscleGroup = "Quadriceps • Hamstrings",
            defaultWeightKg = 45.0,
            defaultReps = 12,
            formCues = listOf(
                "Align the knee joint axis cleanly with the red pivot cam on the Jerai selectorized frame.",
                "Rest shin roller pad just above the ankle crease and keep torso upright against backrest.",
                "Extend smoothly without swinging your hips, holding peak quad squeeze for 1 second."
            ),
            swapAlternativeIds = listOf("jerai_leg_curl", "jerai_45_leg_press", "smith_squat_rdl")
        ),
        // Additional floor inventory
        ExerciseCatalogEntry(
            id = "jerai_leg_curl",
            name = "Prone / Seated Leg Curl",
            zone = ExerciseZone.JERAI_MACHINES,
            equipmentTag = "Jerai Selectorized Leg Curl Machine",
            muscleGroup = "Hamstrings • Calves",
            defaultWeightKg = 40.0,
            defaultReps = 12,
            formCues = listOf(
                "Position the roller pad above the Achilles tendon with knees clear of the bench edge.",
                "Brace hips down into the pad so the lower back does not arch during heavy curls.",
                "Control the 3-second return phase to maximize hamstring tension."
            ),
            swapAlternativeIds = listOf("jerai_leg_extension", "jerai_45_leg_press")
        ),
        ExerciseCatalogEntry(
            id = "jerai_pec_deck_fly",
            name = "Pec Deck / Rear Delt Fly",
            zone = ExerciseZone.JERAI_MACHINES,
            equipmentTag = "Jerai Dual-Cam Pec Deck & Rear Delt Stack",
            muscleGroup = "Pectorals • Posterior Delts",
            defaultWeightKg = 35.0,
            defaultReps = 12,
            formCues = listOf(
                "Adjust seat height so handles stay level with mid-chest (or shoulder height for rear delts).",
                "Pin shoulders down away from your ears and avoid rounding forward.",
                "Bring forearms smoothly toward midline and control back to a comfortable chest stretch."
            ),
            swapAlternativeIds = listOf("cable_crossover_fly", "db_incline_press")
        ),
        ExerciseCatalogEntry(
            id = "jerai_lat_pulldown_row",
            name = "Lat Pulldown & Seated Row Stack",
            zone = ExerciseZone.JERAI_MACHINES,
            equipmentTag = "Jerai Commercial Selectorized Lat/Row Tower",
            muscleGroup = "Lats • Rhomboids • Biceps",
            defaultWeightKg = 50.0,
            defaultReps = 10,
            formCues = listOf(
                "Lock thighs securely under the foam rollers (or brace feet on row footplate) before pulling.",
                "Initiate each rep by depressing shoulder blades down and back, not yanking with biceps.",
                "Pull bar smoothly to upper collarbone with proud chest and controlled upward stretch."
            ),
            swapAlternativeIds = listOf("db_single_arm_row", "cable_face_pull_triceps")
        ),
        ExerciseCatalogEntry(
            id = "jerai_plate_chest_press",
            name = "Plate-Loaded Chest Press",
            zone = ExerciseZone.JERAI_MACHINES,
            equipmentTag = "Jerai Plate-Loaded Converging Chest Press",
            muscleGroup = "Mid Chest • Triceps",
            defaultWeightKg = 60.0,
            defaultReps = 10,
            formCues = listOf(
                "Adjust seat so horizontal handles line up cleanly with mid-pectoral crease.",
                "Keep shoulders tucked back against the pad and wrists neutral on grips.",
                "Drive forward smoothly without shrugging shoulders toward the neck."
            ),
            swapAlternativeIds = listOf("smith_flat_bench", "db_flat_bench", "db_incline_press")
        ),
        ExerciseCatalogEntry(
            id = "jerai_plate_shoulder_press",
            name = "Plate-Loaded Shoulder Press",
            zone = ExerciseZone.JERAI_MACHINES,
            equipmentTag = "Jerai Plate-Loaded Overhead Press Station",
            muscleGroup = "Anterior & Lateral Delts • Triceps",
            defaultWeightKg = 40.0,
            defaultReps = 10,
            formCues = listOf(
                "Keep ribcage braced down and lumbar spine lightly touching the vertical backrest.",
                "Start grips right above clavicle level with elbows positioned slightly forward of torso.",
                "Press straight overhead until arms are tall without flaring ribs."
            ),
            swapAlternativeIds = listOf("db_shoulder_press", "smith_incline_press")
        ),
        ExerciseCatalogEntry(
            id = "cable_face_pull_triceps",
            name = "Cable Face Pull & Rope Pushdown",
            zone = ExerciseZone.CABLE_TOWER,
            equipmentTag = "Jerai Dual Cable Tower + Rope Attachment",
            muscleGroup = "Rear Delts • Upper Back • Triceps",
            defaultWeightKg = 20.0,
            defaultReps = 15,
            formCues = listOf(
                "Pin upper arms tightly alongside your ribcage for pushdowns, or elbows high for face pulls.",
                "Split the rope ends outward at peak contraction while keeping wrists neutral.",
                "Avoid torso sway by engaging glutes and abs throughout the set."
            ),
            swapAlternativeIds = listOf("jerai_lat_pulldown_row", "jerai_pec_deck_fly")
        ),
        ExerciseCatalogEntry(
            id = "smith_incline_press",
            name = "Smith Incline Press",
            zone = ExerciseZone.SMITH_AND_RACKS,
            equipmentTag = "Jerai Smith Rack (5kg–25kg Bumper Plates) + Adjustable Bench",
            muscleGroup = "Upper Chest • Front Delts",
            defaultWeightKg = 50.0,
            defaultReps = 8,
            formCues = listOf(
                "Position 30° bench so bar descends right below the collarbone.",
                "Use an even grip slightly wider than shoulder width and rotate hooks smoothly to unrack.",
                "Keep forearms vertical from side and front views through every rep."
            ),
            swapAlternativeIds = listOf("db_incline_press", "jerai_plate_chest_press", "smith_flat_bench")
        ),
        ExerciseCatalogEntry(
            id = "smith_squat_rdl",
            name = "Smith Machine Squats & RDLs",
            zone = ExerciseZone.SMITH_AND_RACKS,
            equipmentTag = "Jerai Smith Machine + Safety Catches",
            muscleGroup = "Quads • Glutes • Hamstrings",
            defaultWeightKg = 70.0,
            defaultReps = 8,
            formCues = listOf(
                "Set safety catches below parallel squat depth before loading Olympic bumper plates.",
                "Step feet 10cm ahead of the bar line for balanced torso posture on squats.",
                "Keep chest lifted and drive through midfoot and heel uniformly."
            ),
            swapAlternativeIds = listOf("jerai_45_leg_press", "jerai_leg_extension", "jerai_leg_curl")
        ),
        ExerciseCatalogEntry(
            id = "barbell_ez_curl_floor",
            name = "EZ-Curl & Straight Barbell Floor Station",
            zone = ExerciseZone.SMITH_AND_RACKS,
            equipmentTag = "Jerai Olympic Straight Bar & EZ-Curl Rack",
            muscleGroup = "Biceps • Forearms • Upper Back",
            defaultWeightKg = 25.0,
            defaultReps = 10,
            formCues = listOf(
                "Grip angled EZ curves to keep wrists comfortable and elbow joints tracking forward slightly.",
                "Keep elbows stationary at your sides rather than swinging torso backward.",
                "Lower under full control until arms reach soft extension."
            ),
            swapAlternativeIds = listOf("db_single_arm_row", "cable_face_pull_triceps")
        ),
        ExerciseCatalogEntry(
            id = "db_flat_bench",
            name = "Flat Dumbbell Press",
            zone = ExerciseZone.DUMBBELLS,
            equipmentTag = "Jerai Round Rubber Dumbbells (2.5kg–40kg) + Flat Bench",
            muscleGroup = "Mid Chest • Triceps",
            defaultWeightKg = 22.5,
            defaultReps = 10,
            formCues = listOf(
                "Kick dumbbells cleanly to chest with thighs before lying back onto the flat bench.",
                "Keep knuckles pointed at the ceiling with a neutral 45° elbow tuck.",
                "Lower until biceps pass torso plane gently without bouncing at bottom."
            ),
            swapAlternativeIds = listOf("smith_flat_bench", "jerai_plate_chest_press", "db_incline_press")
        ),
        ExerciseCatalogEntry(
            id = "db_single_arm_row",
            name = "Dumbbell Row",
            zone = ExerciseZone.DUMBBELLS,
            equipmentTag = "Jerai Round Rubber Dumbbells (up to 40kg) + Bench",
            muscleGroup = "Lats • Mid Back • Rear Delts",
            defaultWeightKg = 25.0,
            defaultReps = 10,
            formCues = listOf(
                "Brace opposite hand and knee solidly on the bench with a flat, neutral spine.",
                "Pull the dumbbell back toward your hip pocket rather than straight up to your chest.",
                "Avoid twisting your torso; keep shoulders square to the floor."
            ),
            swapAlternativeIds = listOf("jerai_lat_pulldown_row", "cable_face_pull_triceps")
        ),
        ExerciseCatalogEntry(
            id = "db_shoulder_press",
            name = "Seated Dumbbell Shoulder Press & Laterals",
            zone = ExerciseZone.DUMBBELLS,
            equipmentTag = "Jerai Dumbbells (2.5kg–40kg) + Upright Bench",
            muscleGroup = "Shoulders • Upper Triceps",
            defaultWeightKg = 15.0,
            defaultReps = 12,
            formCues = listOf(
                "Set bench backrest to ~80° vertical and press lower back gently into support.",
                "Keep dumbbells slightly in front of ear plane to respect scapular biomechanics.",
                "Control descent back to chin/ear height before initiating the next press."
            ),
            swapAlternativeIds = listOf("jerai_plate_shoulder_press", "db_incline_press")
        ),
        ExerciseCatalogEntry(
            id = "aerofit_treadmill_run",
            name = "Aerofit Commercial Treadmill Zone",
            zone = ExerciseZone.CARDIO_AND_TURF,
            equipmentTag = "Aerofit Commercial Incline Treadmill",
            muscleGroup = "Cardiovascular Endurance • Lower Body",
            defaultWeightKg = 0.0,
            defaultReps = 15,
            formCues = listOf(
                "Clip the safety stop tether and begin with a 2-minute 4.5 km/h brisk warm-up walk.",
                "Maintain tall posture looking straight ahead rather than holding side rails on incline.",
                "Land midfoot directly under your hips at a brisk cadence."
            ),
            swapAlternativeIds = listOf("aerofit_cycle_bay", "turf_agility_drills")
        ),
        ExerciseCatalogEntry(
            id = "aerofit_cycle_bay",
            name = "Aerofit Conditioning Cycle Bay",
            zone = ExerciseZone.CARDIO_AND_TURF,
            equipmentTag = "Aerofit Commercial Upright / Spin Bike",
            muscleGroup = "VO2 Conditioning • Quads",
            defaultWeightKg = 0.0,
            defaultReps = 15,
            formCues = listOf(
                "Set saddle height to hip-bone level so knees have a 25°–30° bend at bottom stroke.",
                "Keep core braced and shoulders relaxed away from handlebars.",
                "Alternate 30-second high-cadence intervals with 60-second steady recovery."
            ),
            swapAlternativeIds = listOf("aerofit_treadmill_run", "turf_agility_drills")
        ),
        ExerciseCatalogEntry(
            id = "turf_agility_drills",
            name = "Open Turf & Core Conditioning Lane",
            zone = ExerciseZone.CARDIO_AND_TURF,
            equipmentTag = "Battle Box Dudhnoi Open Turf & Conditioning Mats",
            muscleGroup = "Full Body Conditioning • Core",
            defaultWeightKg = 10.0,
            defaultReps = 12,
            formCues = listOf(
                "Keep center of gravity low during lateral shuffles and agility cone cuts.",
                "Brace abdominal wall tightly during loaded carries and plank transitions.",
                "Drive hips forward explosively while keeping heels rooted."
            ),
            swapAlternativeIds = listOf("aerofit_treadmill_run", "aerofit_cycle_bay")
        )
    )

    fun byId(id: String): ExerciseCatalogEntry? = catalog.find { it.id == id }

    fun swapAlternativesFor(catalogId: String): List<ExerciseCatalogEntry> {
        val current = byId(catalogId) ?: return emptyList()
        return current.swapAlternativeIds.mapNotNull { byId(it) }
    }

    fun buildDefaultPushPullSession(): List<ActiveStationExercise> {
        val stationIds = listOf(
            "smith_flat_bench" to "Prev: 50kg × 10 • 3 sets",
            "cable_crossover_fly" to "Prev: 15kg × 12 • 3 sets",
            "db_incline_press" to "Prev: 20kg × 10 • 3 sets",
            "jerai_45_leg_press" to "Prev: 120kg × 12 • 3 sets",
            "jerai_leg_extension" to "Prev: 45kg × 12 • 3 sets"
        )
        return stationIds.mapNotNull { (id, prevSummary) ->
            val entry = byId(id) ?: return@mapNotNull null
            ActiveStationExercise(
                catalogId = entry.id,
                name = entry.name,
                equipmentTag = entry.equipmentTag,
                zone = entry.zone,
                previousLiftSummary = prevSummary,
                sets = (1..3).map { setNum ->
                    WorkoutSetEntry(
                        setNumber = setNum,
                        weightKg = entry.defaultWeightKg,
                        reps = entry.defaultReps,
                        completed = false,
                        previousLiftText = "${entry.defaultWeightKg.toInt()}kg × ${entry.defaultReps}"
                    )
                }
            )
        }
    }
}
