package com.stretchdaily.app.core.database

import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.Exercise
import com.stretchdaily.app.core.model.FlexibilityTier

/**
 * One-time seed data for the Room database.
 *
 * Source: `Exercises.xlsx` and `Benchmarks V2.xlsx` design docs (gitignored).
 * Keep this in sync with those files manually — the spreadsheets remain the
 * source of truth for content tweaks.
 *
 * Notes:
 * - Cues are split on `;` and trimmed.
 * - For unilateral exercises, [Exercise.totalTime] already accounts for both
 *   sides (matches the spreadsheet column).
 * - Apley Scratch Test is treated as numeric in cm even though the
 *   spreadsheet labels it "Score" — the actual ranges are cm gaps.
 * - ATG Split Squat is the only CATEGORICAL benchmark; the user picks the
 *   tier whose qualitative description matches their depth.
 */
object DatabaseSeeder {

    fun exercises(): List<Exercise> = listOf(
        ex("N01", "Chin Tucks", Category.NECK,
            "Sit tall; pull chin straight back (turtle neck); hold 5s; do not tilt head",
            uni = false, timed = false, tgt = 10, spr = 5, tot = 50),
        ex("N02", "Isometric Rotation", Category.NECK,
            "Place palm on cheek; try to turn head into hand; hold 5s; repeat sides",
            uni = true, timed = true, tgt = 5, spr = 5, tot = 50),
        ex("N03", "Upper Trap Stretch", Category.NECK,
            "Sit tall; one hand behind back; tilt ear toward opposite shoulder; hold 30s",
            uni = true, timed = true, tgt = 30, spr = 30, tot = 60),
        ex("N04", "Neck Nods (Rotated)", Category.NECK,
            "Rotate head 45°; gently nod up/down; feel stretch in different angles",
            uni = true, timed = false, tgt = 10, spr = 4, tot = 80),
        ex("N05", "Resisted Extension", Category.NECK,
            "Place hands behind head; push head back into palms while resisting",
            uni = false, timed = false, tgt = 10, spr = 5, tot = 50),
        ex("N06", "Banded Rotations", Category.NECK,
            "Use a resistance band or towel; rotate head against light tension",
            uni = true, timed = false, tgt = 10, spr = 4, tot = 80),

        ex("S01", "Scapular Wall Slides", Category.SHOULDERS,
            "Back against wall; elbows/wrists touch wall; slide up in \"W\" to \"Y\" shape",
            uni = false, timed = false, tgt = 15, spr = 4, tot = 60),
        ex("S02", "Doorway Pec Stretch", Category.SHOULDERS,
            "Arm at 90° on frame; step forward; feel stretch in chest; hold 30s",
            uni = true, timed = true, tgt = 30, spr = 30, tot = 60),
        ex("S03", "Shoulder Shrugs", Category.SHOULDERS,
            "Shrug shoulders toward ears; hold 2s; release slowly",
            uni = false, timed = false, tgt = 15, spr = 3, tot = 45),
        ex("S04", "Prone Y-T-W", Category.SHOULDERS,
            "Lie face down; lift arms in Y, T, and W shapes; squeeze blades; hold 2s",
            uni = false, timed = false, tgt = 10, spr = 6, tot = 60),
        ex("S05", "External Rotation", Category.SHOULDERS,
            "Lie on side; elbow at 90° on ribs; rotate hand toward ceiling; use water bottle",
            uni = true, timed = false, tgt = 12, spr = 4, tot = 96),
        ex("S06", "Wall Facing Squat", Category.SHOULDERS,
            "Face wall; arms overhead; squat deep without hands touching wall",
            uni = false, timed = false, tgt = 10, spr = 5, tot = 50),
        ex("S07", "Incline Powell Raise", Category.SHOULDERS,
            "Lie on side on couch; lift arm from floor to ceiling with control",
            uni = true, timed = false, tgt = 12, spr = 4, tot = 96),

        ex("W01", "Wrist Shakes", Category.WRISTS,
            "Shake wrists loosely for 15s to increase circulation",
            uni = false, timed = true, tgt = 15, spr = 15, tot = 15),
        ex("W02", "Wrist Pulses", Category.WRISTS,
            "Hands flat on floor; fingers forward; pulse bodyweight forward",
            uni = false, timed = false, tgt = 20, spr = 2, tot = 40),
        ex("W03", "Rice Bucket Grip", Category.WRISTS,
            "Open/close hand in bucket of rice; rotate wrist against resistance",
            uni = false, timed = true, tgt = 60, spr = 60, tot = 60),
        ex("W04", "Palms-Up Pulses", Category.WRISTS,
            "Back of hands on floor; fingers toward knees; pulse back slowly",
            uni = false, timed = false, tgt = 20, spr = 2, tot = 40),
        ex("W05", "Hammer Curls", Category.WRISTS,
            "Use water bottle; thumb up; curl toward shoulder with control",
            uni = true, timed = false, tgt = 15, spr = 4, tot = 120),
        ex("W06", "Fingertip Push-ups", Category.WRISTS,
            "Perform push-up on fingertips; start on knees to scale difficulty",
            uni = false, timed = false, tgt = 10, spr = 4, tot = 40),

        ex("SP01", "Cat-Cow", Category.SPINE,
            "Hands/knees; arch back (cat); drop belly (cow); 10 reps",
            uni = false, timed = false, tgt = 10, spr = 6, tot = 60),
        ex("SP02", "QL Standing Tilt", Category.SPINE,
            "Stand tall; slide hand down side of thigh toward knee; return",
            uni = true, timed = false, tgt = 15, spr = 4, tot = 120),
        ex("SP03", "Thread the Needle", Category.SPINE,
            "On all fours; reach arm under body; rotate shoulder to floor",
            uni = true, timed = false, tgt = 10, spr = 5, tot = 100),
        ex("SP04", "Bird-Dog", Category.SPINE,
            "Extend opposite arm/leg; keep back flat and core engaged",
            uni = true, timed = false, tgt = 10, spr = 6, tot = 120),
        ex("SP05", "Elephant Walk", Category.SPINE,
            "Hands on chair; alternate straightening legs; keep back neutral",
            uni = true, timed = false, tgt = 30, spr = 2, tot = 120),
        ex("SP06", "Jefferson Curl", Category.SPINE,
            "Roll down slowly; chin to chest; reach for toes; use light weight",
            uni = false, timed = false, tgt = 10, spr = 8, tot = 80),
        ex("SP07", "Dead Bug", Category.SPINE,
            "Lie on back; lower opposite arm/leg; keep lower back on floor",
            uni = true, timed = false, tgt = 10, spr = 4, tot = 80),

        ex("H01", "90/90 Stretch", Category.HIPS,
            "Sit with legs at 90°; one in front, one to side; rotate torso",
            uni = true, timed = true, tgt = 60, spr = 60, tot = 120),
        ex("H02", "Glute Bridge", Category.HIPS,
            "Lie on back; lift hips; squeeze glutes; keep shoulders down",
            uni = false, timed = false, tgt = 20, spr = 4, tot = 80),
        ex("H03", "Butterfly Stretch", Category.HIPS,
            "Sit; feet together; press knees toward floor gently",
            uni = false, timed = true, tgt = 60, spr = 60, tot = 60),
        ex("H04", "Couch Stretch", Category.HIPS,
            "One knee on couch/wall; other foot forward; stay upright",
            uni = true, timed = true, tgt = 60, spr = 60, tot = 120),
        ex("H05", "Pigeon on Bed", Category.HIPS,
            "Front leg on bed; shin at 45°; lean forward slowly",
            uni = true, timed = true, tgt = 60, spr = 60, tot = 120),
        ex("H06", "Thomas Stretch", Category.HIPS,
            "Lie on edge of bed; hug one knee; let other leg hang",
            uni = true, timed = true, tgt = 60, spr = 60, tot = 120),
        ex("H07", "Banded Hip Flexion", Category.HIPS,
            "Band on foot; lift knee toward chest against resistance",
            uni = true, timed = false, tgt = 20, spr = 3, tot = 120),

        ex("K01", "Backward Walk", Category.KNEES,
            "Walk backward 5-10 mins; toe-to-heel; pump the ground",
            uni = false, timed = true, tgt = 120, spr = 1, tot = 120),
        ex("K02", "Tibialis Raise", Category.KNEES,
            "Back to wall; heels out 15cm; lift toes toward shins",
            uni = false, timed = false, tgt = 25, spr = 3, tot = 75),
        ex("K03", "Poliquin Step-up", Category.KNEES,
            "Stand on step; heel on wedge; drive knee over toe",
            uni = true, timed = false, tgt = 25, spr = 3, tot = 150),
        ex("K04", "Spanish Squat", Category.KNEES,
            "Band behind knees; sit back 90°; keep shins vertical",
            uni = false, timed = true, tgt = 45, spr = 45, tot = 45),
        ex("K05", "ATG Split Squat", Category.KNEES,
            "Front heel down; hamstring covers calf; back leg straight",
            uni = true, timed = false, tgt = 10, spr = 5, tot = 100),
        ex("K06", "Nordic Curl", Category.KNEES,
            "Kneel; ankles secured; lower body slowly like a lever",
            uni = false, timed = false, tgt = 5, spr = 8, tot = 40),
        ex("K07", "Slant Squat", Category.KNEES,
            "Heels elevated on wedge; squat deep; hamstrings to calves",
            uni = false, timed = false, tgt = 15, spr = 4, tot = 60),

        ex("A01", "Ankle Circles", Category.ANKLES,
            "Sit; draw large circles with big toe; 20 reps each way",
            uni = true, timed = false, tgt = 20, spr = 2, tot = 80),
        ex("A02", "Soleus Stretch", Category.ANKLES,
            "Lunge against wall; back knee bent; heel grounded",
            uni = true, timed = true, tgt = 60, spr = 60, tot = 120),
        ex("A03", "Toe Yoga", Category.ANKLES,
            "Lift big toe only; then small toes; alternate 20 times",
            uni = false, timed = false, tgt = 20, spr = 3, tot = 60),
        ex("A04", "Calf Raise", Category.ANKLES,
            "Stand on one leg; lift heel high; 2s hold at top",
            uni = true, timed = false, tgt = 15, spr = 4, tot = 120),
        ex("A05", "Knee-to-Wall Pulse", Category.ANKLES,
            "Heel down; knee toward wall; push forward for 10s",
            uni = true, timed = false, tgt = 15, spr = 4, tot = 120),
        ex("A06", "Single Leg Hop", Category.ANKLES,
            "Small springy hops; focus on stiff ankle contact",
            uni = true, timed = false, tgt = 30, spr = 1, tot = 60)
    )

    fun benchmarks(): List<Benchmark> = listOf(
        numericBenchmark(
            id = "BM_CERVICAL_ROTATION",
            name = "Cervical Rotation",
            category = Category.NECK,
            unit = "Degrees",
            description = "Sit tall. Rotate your head to one side as if looking over your shoulder. Note the angle where your chin aligns with your collarbone without moving your shoulders.",
            stiff = "< 50°",
            belowAverage = "50°–70°",
            average = "70°–80°",
            flexible = "80°–90°",
            veryFlexible = "> 90°"
        ),
        numericBenchmark(
            id = "BM_APLEY_SCRATCH",
            name = "Apley Scratch Test",
            category = Category.SHOULDERS,
            // Original sheet says "Score" but ranges are cm gaps; treat as numeric cm.
            unit = "cm",
            description = "Reach one hand over your shoulder and the other behind your back. Measure the gap (or overlap) between your fingertips.",
            stiff = "> 15 cm gap",
            belowAverage = "10–15 cm gap",
            average = "5–10 cm gap",
            flexible = "0–5 cm gap",
            veryFlexible = "< 0 cm (Finger Overlap)"
        ),
        numericBenchmark(
            id = "BM_THORACIC_ROTATION",
            name = "Seated Thoracic Rotation",
            category = Category.SPINE,
            unit = "Degrees",
            description = "Sit on a bench with a stick across your shoulders. Rotate your upper body as far as possible while keeping your hips and knees locked forward. Avoid leaning sideways.",
            stiff = "< 25°",
            belowAverage = "25°–35°",
            average = "35°–45°",
            flexible = "45°–55°",
            veryFlexible = "> 55°"
        ),
        numericBenchmark(
            id = "BM_SIT_AND_REACH",
            name = "Sit and Reach",
            category = Category.SPINE,
            unit = "cm",
            description = "Sit on the floor with legs straight. Reach forward with both hands toward your toes. Measure the distance from your fingertips to your toes (negative if past toes).",
            stiff = "> 20 cm",
            belowAverage = "11 to 20 cm",
            average = "1 to 10 cm",
            flexible = "-10 to 0 cm",
            veryFlexible = "< -10 cm"
        ),
        numericBenchmark(
            id = "BM_THOMAS_TEST",
            name = "Thomas Test",
            category = Category.HIPS,
            unit = "Degrees",
            description = "Lie on the edge of a bed. Hug one knee to your chest. Let the other leg hang freely. Measure the angle of the hanging thigh relative to the horizontal bed surface.",
            stiff = "Thigh > 15° up",
            belowAverage = "Thigh 5°–15° up",
            average = "Thigh horizontal",
            flexible = "Thigh 5°–10° down",
            veryFlexible = "Thigh > 15° down"
        ),
        numericBenchmark(
            id = "BM_BUTTERFLY",
            name = "Butterfly Stretch",
            category = Category.HIPS,
            unit = "cm",
            description = "Sit with the soles of your feet together. Pull your heels toward your groin and press your knees toward the floor. Measure the vertical distance from your knees to the ground.",
            stiff = "> 20 cm to floor",
            belowAverage = "15–20 cm",
            average = "10–15 cm",
            flexible = "5–10 cm",
            veryFlexible = "< 5 cm"
        ),
        // Only categorical benchmark — qualitative depth descriptions.
        Benchmark(
            id = "BM_ATG_SPLIT_SQUAT",
            name = "ATG Split Squat",
            category = Category.KNEES,
            description = "Step into a long lunge. Lower your hips until your front hamstring completely covers your calf. Pick the tier that matches your achievable depth.",
            unit = "Tier",
            inputType = BenchmarkInputType.CATEGORICAL,
            tierRanges = mapOf(
                FlexibilityTier.STIFF.name to "> 30 cm elevation",
                FlexibilityTier.BELOW_AVERAGE.name to "15–25 cm elevation",
                FlexibilityTier.AVERAGE.name to "Flat (knee bent)",
                FlexibilityTier.FLEXIBLE.name to "Flat (knee off ground)",
                FlexibilityTier.VERY_FLEXIBLE.name to "Flat (back leg straight)"
            )
        ),
        numericBenchmark(
            id = "BM_KNEE_TO_WALL",
            name = "Knee-to-Wall",
            category = Category.ANKLES,
            unit = "cm",
            description = "Place your big toe a measured distance from a wall. Keeping your heel down, drive your knee forward to touch the wall. Find the max distance where the heel stays grounded.",
            stiff = "< 5 cm",
            belowAverage = "5–8 cm",
            average = "8–10 cm",
            flexible = "10–13 cm",
            veryFlexible = "> 13 cm"
        ),
        numericBenchmark(
            id = "BM_WRIST_EXTENSION",
            name = "Wrist Extension",
            category = Category.WRISTS,
            unit = "Degrees",
            description = "Place your hand sideways on a table with your fingers forward. Move your hand away from your body until you reach your max pain-free angle.",
            stiff = "< 60°",
            belowAverage = "60°–70°",
            average = "70°–80°",
            flexible = "80°–90°",
            veryFlexible = "> 90°"
        ),
        numericBenchmark(
            id = "BM_WRIST_FLEXION",
            name = "Wrist Flexion",
            category = Category.WRISTS,
            unit = "Degrees",
            description = "Place your hand sideways on a table with your fingers forward. Move your hand toward your body until you reach your max pain-free angle.",
            stiff = "< 65°",
            belowAverage = "65°–75°",
            average = "75°–85°",
            flexible = "85°–95°",
            veryFlexible = "> 95°"
        )
    )

    private fun ex(
        id: String,
        name: String,
        category: Category,
        cuesText: String,
        uni: Boolean,
        timed: Boolean,
        tgt: Int,
        spr: Int,
        tot: Int
    ): Exercise = Exercise(
        id = id,
        name = name,
        category = category,
        cues = cuesText.split(";").map { it.trim() }.filter { it.isNotEmpty() },
        isUnilateral = uni,
        isTimed = timed,
        targetReps = if (timed) null else tgt,
        secondsPerRep = if (timed) null else spr,
        totalTime = tot
    )

    private fun numericBenchmark(
        id: String,
        name: String,
        category: Category,
        unit: String,
        description: String,
        stiff: String,
        belowAverage: String,
        average: String,
        flexible: String,
        veryFlexible: String
    ): Benchmark = Benchmark(
        id = id,
        name = name,
        category = category,
        description = description,
        unit = unit,
        inputType = BenchmarkInputType.NUMERIC,
        tierRanges = mapOf(
            FlexibilityTier.STIFF.name to stiff,
            FlexibilityTier.BELOW_AVERAGE.name to belowAverage,
            FlexibilityTier.AVERAGE.name to average,
            FlexibilityTier.FLEXIBLE.name to flexible,
            FlexibilityTier.VERY_FLEXIBLE.name to veryFlexible
        )
    )
}
