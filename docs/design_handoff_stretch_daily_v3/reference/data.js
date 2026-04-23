// Static data for Stretch Daily — exercises, benchmarks, and generated mock history.
// Everything is in-memory / localStorage; no network.

window.EXERCISES = [
  {id:"N01",name:"Chin Tucks",difficulty:"Beginner",cues:"Sit tall; pull chin straight back (turtle neck); hold 5s; do not tilt head",focus:"Deep Neck Flexors",category:"Neck",unilateral:false,timed:false,target:10,secPerRep:5,total:50},
  {id:"N02",name:"Isometric Rotation",difficulty:"Beginner",cues:"Place palm on cheek; try to turn head into hand; hold 5s; repeat sides",focus:"Sternocleidomastoid",category:"Neck",unilateral:true,timed:true,target:5,secPerRep:5,total:50},
  {id:"N03",name:"Upper Trap Stretch",difficulty:"Beginner",cues:"Sit tall; one hand behind back; tilt ear toward opposite shoulder; hold 30s",focus:"Upper Trapezius",category:"Neck",unilateral:true,timed:true,target:30,secPerRep:30,total:60},
  {id:"N04",name:"Neck Nods (Rotated)",difficulty:"Intermediate",cues:"Rotate head 45°; gently nod up/down; feel stretch in different angles",focus:"Levator Scapulae",category:"Neck",unilateral:true,timed:false,target:10,secPerRep:4,total:80},
  {id:"N05",name:"Resisted Extension",difficulty:"Intermediate",cues:"Place hands behind head; push head back into palms while resisting",focus:"Erector Spinae",category:"Neck",unilateral:false,timed:false,target:10,secPerRep:5,total:50},
  {id:"N06",name:"Banded Rotations",difficulty:"Advanced",cues:"Use a resistance band or towel; rotate head against light tension",focus:"Multi-planar Stability",category:"Neck",unilateral:true,timed:false,target:10,secPerRep:4,total:80},

  {id:"S01",name:"Scapular Wall Slides",difficulty:"Beginner",cues:"Back against wall; elbows/wrists touch wall; slide up in 'W' to 'Y' shape",focus:"Scapular Upward Rotation",category:"Shoulders",unilateral:false,timed:false,target:15,secPerRep:4,total:60},
  {id:"S02",name:"Doorway Pec Stretch",difficulty:"Beginner",cues:"Arm at 90° on frame; step forward; feel stretch in chest; hold 30s",focus:"Pectoralis Minor",category:"Shoulders",unilateral:true,timed:true,target:30,secPerRep:30,total:60},
  {id:"S03",name:"Shoulder Shrugs",difficulty:"Beginner",cues:"Shrug shoulders toward ears; hold 2s; release slowly",focus:"Levator Scapulae",category:"Shoulders",unilateral:false,timed:false,target:15,secPerRep:3,total:45},
  {id:"S04",name:"Prone Y-T-W",difficulty:"Intermediate",cues:"Lie face down; lift arms in Y, T, and W shapes; squeeze blades; hold 2s",focus:"Mid/Lower Traps",category:"Shoulders",unilateral:false,timed:false,target:10,secPerRep:6,total:60},
  {id:"S05",name:"External Rotation",difficulty:"Intermediate",cues:"Lie on side; elbow at 90° on ribs; rotate hand toward ceiling; use water bottle",focus:"Rotator Cuff",category:"Shoulders",unilateral:true,timed:false,target:12,secPerRep:4,total:96},
  {id:"S06",name:"Wall Facing Squat",difficulty:"Advanced",cues:"Face wall; arms overhead; squat deep without hands touching wall",focus:"Thoracic/Shoulder Link",category:"Shoulders",unilateral:false,timed:false,target:10,secPerRep:5,total:50},
  {id:"S07",name:"Incline Powell Raise",difficulty:"Advanced",cues:"Lie on side on couch; lift arm from floor to ceiling with control",focus:"Rear Deltoid",category:"Shoulders",unilateral:true,timed:false,target:12,secPerRep:4,total:96},

  {id:"W01",name:"Wrist Shakes",difficulty:"Beginner",cues:"Shake wrists loosely for 15s to increase circulation",focus:"Joint Lubrication",category:"Wrists",unilateral:false,timed:true,target:15,secPerRep:15,total:15},
  {id:"W02",name:"Wrist Pulses",difficulty:"Beginner",cues:"Hands flat on floor; fingers forward; pulse bodyweight forward",focus:"Extension ROM",category:"Wrists",unilateral:false,timed:false,target:20,secPerRep:2,total:40},
  {id:"W03",name:"Rice Bucket Grip",difficulty:"Beginner",cues:"Open/close hand in bucket of rice; rotate wrist against resistance",focus:"Intrinsic Strength",category:"Wrists",unilateral:false,timed:true,target:60,secPerRep:60,total:60},
  {id:"W04",name:"Palms-Up Pulses",difficulty:"Intermediate",cues:"Back of hands on floor; fingers toward knees; pulse back slowly",focus:"Wrist Flexion",category:"Wrists",unilateral:false,timed:false,target:20,secPerRep:2,total:40},
  {id:"W05",name:"Hammer Curls",difficulty:"Intermediate",cues:"Use water bottle; thumb up; curl toward shoulder with control",focus:"Brachioradialis",category:"Wrists",unilateral:true,timed:false,target:15,secPerRep:4,total:120},
  {id:"W06",name:"Fingertip Push-ups",difficulty:"Advanced",cues:"Perform push-up on fingertips; start on knees to scale difficulty",focus:"Digital/Wrist Stability",category:"Wrists",unilateral:false,timed:false,target:10,secPerRep:4,total:40},

  {id:"SP01",name:"Cat-Cow",difficulty:"Beginner",cues:"Hands/knees; arch back (cat); drop belly (cow); 10 reps",focus:"Segmental Mobility",category:"Spine",unilateral:false,timed:false,target:10,secPerRep:6,total:60},
  {id:"SP02",name:"QL Standing Tilt",difficulty:"Beginner",cues:"Stand tall; slide hand down side of thigh toward knee; return",focus:"Quadratus Lumborum",category:"Spine",unilateral:true,timed:false,target:15,secPerRep:4,total:120},
  {id:"SP03",name:"Thread the Needle",difficulty:"Intermediate",cues:"On all fours; reach arm under body; rotate shoulder to floor",focus:"Thoracic Rotation",category:"Spine",unilateral:true,timed:false,target:10,secPerRep:5,total:100},
  {id:"SP04",name:"Bird-Dog",difficulty:"Intermediate",cues:"Extend opposite arm/leg; keep back flat and core engaged",focus:"Rotary Stability",category:"Spine",unilateral:true,timed:false,target:10,secPerRep:6,total:120},
  {id:"SP05",name:"Elephant Walk",difficulty:"Intermediate",cues:"Hands on chair; alternate straightening legs; keep back neutral",focus:"Hamstring/Low Back",category:"Spine",unilateral:true,timed:false,target:30,secPerRep:2,total:120},
  {id:"SP06",name:"Jefferson Curl",difficulty:"Advanced",cues:"Roll down slowly; chin to chest; reach for toes; use light weight",focus:"Spinal Flexion",category:"Spine",unilateral:false,timed:false,target:10,secPerRep:8,total:80},
  {id:"SP07",name:"Dead Bug",difficulty:"Advanced",cues:"Lie on back; lower opposite arm/leg; keep lower back on floor",focus:"Core Stability",category:"Spine",unilateral:true,timed:false,target:10,secPerRep:4,total:80},

  {id:"H01",name:"90/90 Stretch",difficulty:"Beginner",cues:"Sit with legs at 90°; one in front, one to side; rotate torso",focus:"Internal Rotation",category:"Hips",unilateral:true,timed:true,target:60,secPerRep:60,total:120},
  {id:"H02",name:"Glute Bridge",difficulty:"Beginner",cues:"Lie on back; lift hips; squeeze glutes; keep shoulders down",focus:"Posterior Chain",category:"Hips",unilateral:false,timed:false,target:20,secPerRep:4,total:80},
  {id:"H03",name:"Butterfly Stretch",difficulty:"Beginner",cues:"Sit; feet together; press knees toward floor gently",focus:"Adductor Mobility",category:"Hips",unilateral:false,timed:true,target:60,secPerRep:60,total:60},
  {id:"H04",name:"Couch Stretch",difficulty:"Intermediate",cues:"One knee on couch/wall; other foot forward; stay upright",focus:"Rectus Femoris",category:"Hips",unilateral:true,timed:true,target:60,secPerRep:60,total:120},
  {id:"H05",name:"Pigeon on Bed",difficulty:"Intermediate",cues:"Front leg on bed; shin at 45°; lean forward slowly",focus:"Glute/Piriformis",category:"Hips",unilateral:true,timed:true,target:60,secPerRep:60,total:120},
  {id:"H06",name:"Thomas Stretch",difficulty:"Advanced",cues:"Lie on edge of bed; hug one knee; let other leg hang",focus:"Iliopsoas Lengthening",category:"Hips",unilateral:true,timed:true,target:60,secPerRep:60,total:120},
  {id:"H07",name:"Banded Hip Flexion",difficulty:"Advanced",cues:"Band on foot; lift knee toward chest against resistance",focus:"Hip Flexor Strength",category:"Hips",unilateral:true,timed:false,target:20,secPerRep:3,total:120},

  {id:"K01",name:"Backward Walk",difficulty:"Beginner",cues:"Walk backward 5-10 mins; toe-to-heel; pump the ground",focus:"Blood Flow/Warm-up",category:"Knees",unilateral:false,timed:true,target:600,secPerRep:1,total:600},
  {id:"K02",name:"Tibialis Raise",difficulty:"Beginner",cues:"Back to wall; heels out 15cm; lift toes toward shins",focus:"Tibialis Anterior",category:"Knees",unilateral:false,timed:false,target:25,secPerRep:3,total:75},
  {id:"K03",name:"Poliquin Step-up",difficulty:"Intermediate",cues:"Stand on step; heel on wedge; drive knee over toe",focus:"VMO Activation",category:"Knees",unilateral:true,timed:false,target:25,secPerRep:3,total:150},
  {id:"K04",name:"Spanish Squat",difficulty:"Intermediate",cues:"Band behind knees; sit back 90°; keep shins vertical",focus:"Tendon Loading",category:"Knees",unilateral:false,timed:true,target:45,secPerRep:45,total:45},
  {id:"K05",name:"ATG Split Squat",difficulty:"Advanced",cues:"Front heel down; hamstring covers calf; back leg straight",focus:"Structural Integrity",category:"Knees",unilateral:true,timed:false,target:10,secPerRep:5,total:100},
  {id:"K06",name:"Nordic Curl",difficulty:"Advanced",cues:"Kneel; ankles secured; lower body slowly like a lever",focus:"Eccentric Hamstring",category:"Knees",unilateral:false,timed:false,target:5,secPerRep:8,total:40},
  {id:"K07",name:"Slant Squat",difficulty:"Advanced",cues:"Heels elevated on wedge; squat deep; hamstrings to calves",focus:"Quad Dominance",category:"Knees",unilateral:false,timed:false,target:15,secPerRep:4,total:60},

  {id:"A01",name:"Ankle Circles",difficulty:"Beginner",cues:"Sit; draw large circles with big toe; 20 reps each way",focus:"Joint Mobility",category:"Ankles",unilateral:true,timed:false,target:20,secPerRep:2,total:80},
  {id:"A02",name:"Soleus Stretch",difficulty:"Beginner",cues:"Lunge against wall; back knee bent; heel grounded",focus:"Deep Calf",category:"Ankles",unilateral:true,timed:true,target:60,secPerRep:60,total:120},
  {id:"A03",name:"Toe Yoga",difficulty:"Beginner",cues:"Lift big toe only; then small toes; alternate 20 times",focus:"Intrinsic Control",category:"Ankles",unilateral:false,timed:false,target:20,secPerRep:3,total:60},
  {id:"A04",name:"Calf Raise",difficulty:"Intermediate",cues:"Stand on one leg; lift heel high; 2s hold at top",focus:"Gastroc Strength",category:"Ankles",unilateral:true,timed:false,target:15,secPerRep:4,total:120},
  {id:"A05",name:"Knee-to-Wall Pulse",difficulty:"Advanced",cues:"Heel down; knee toward wall; push forward for 10s",focus:"Dorsiflexion ROM",category:"Ankles",unilateral:true,timed:false,target:15,secPerRep:4,total:120},
  {id:"A06",name:"Single Leg Hop",difficulty:"Advanced",cues:"Small springy hops; focus on stiff ankle contact",focus:"Elastic Recoil",category:"Ankles",unilateral:true,timed:false,target:30,secPerRep:1,total:60},
];

window.CATEGORIES = ["Neck","Shoulders","Wrists","Spine","Hips","Knees","Ankles"];

window.BENCHMARKS = [
  {id:"b_cerv_rot",name:"Cervical Rotation",category:"Neck",unit:"°",kind:"number",
   description:"Sit tall. Rotate your head to one side as if looking over your shoulder. Note the angle where your chin aligns with your collarbone without moving your shoulders.",
   bands:[{label:"Stiff",hint:"< 50°",max:50},{label:"Below avg",hint:"50–70°",max:70},{label:"Average",hint:"70–80°",max:80},{label:"Flexible",hint:"80–90°",max:90},{label:"Very flexible",hint:"> 90°",max:Infinity}],
   better:"higher"},
  {id:"b_apley",name:"Apley Scratch Test",category:"Shoulders",unit:"cm gap",kind:"number",
   description:"Reach one hand over your shoulder and the other behind your back. Measure the vertical gap between fingertips. Negative values = overlap.",
   bands:[{label:"Very flexible",hint:"Finger overlap",max:0},{label:"Flexible",hint:"0–5 cm",max:5},{label:"Average",hint:"5–10 cm",max:10},{label:"Below avg",hint:"10–15 cm",max:15},{label:"Stiff",hint:"> 15 cm",max:Infinity}],
   better:"lower"},
  {id:"b_thor_rot",name:"Seated Thoracic Rotation",category:"Spine",unit:"°",kind:"number",
   description:"Sit on a bench with a stick across your shoulders. Rotate your upper body as far as possible while keeping your hips and knees locked forward.",
   bands:[{label:"Stiff",hint:"< 25°",max:25},{label:"Below avg",hint:"25–35°",max:35},{label:"Average",hint:"35–45°",max:45},{label:"Flexible",hint:"45–55°",max:55},{label:"Very flexible",hint:"> 55°",max:Infinity}],
   better:"higher"},
  {id:"b_sit_reach",name:"Sit and Reach",category:"Spine",unit:"cm",kind:"number",
   description:"Sit on the floor with legs straight. Reach forward with both hands toward your toes. Measure the distance from your fingertips to your toes (past toes = negative).",
   bands:[{label:"Stiff",hint:"> 20 cm",max:Infinity,inverse:true},{label:"Below avg",hint:"11–20 cm",max:20},{label:"Average",hint:"1–10 cm",max:10},{label:"Flexible",hint:"-10–0 cm",max:0},{label:"Very flexible",hint:"< -10 cm",max:-10}],
   better:"lower"},
  {id:"b_thomas",name:"Thomas Test",category:"Hips",unit:"°",kind:"number",
   description:"Lie on the edge of a bed. Hug one knee to your chest. Let the other leg hang freely. Measure the angle of the hanging thigh relative to horizontal (negative = hangs down).",
   bands:[{label:"Stiff",hint:"> 15° up",max:Infinity,inverse:true},{label:"Below avg",hint:"5–15° up",max:15},{label:"Average",hint:"Horizontal",max:5},{label:"Flexible",hint:"-10°–0°",max:0},{label:"Very flexible",hint:"< -15°",max:-15}],
   better:"lower"},
  {id:"b_butterfly",name:"Butterfly Stretch",category:"Hips",unit:"cm",kind:"number",
   description:"Sit with the soles of your feet together. Pull your heels toward your groin and press your knees toward the floor. Measure the vertical distance from your knees to the ground.",
   bands:[{label:"Stiff",hint:"> 20 cm",max:Infinity,inverse:true},{label:"Below avg",hint:"15–20 cm",max:20},{label:"Average",hint:"10–15 cm",max:15},{label:"Flexible",hint:"5–10 cm",max:10},{label:"Very flexible",hint:"< 5 cm",max:5}],
   better:"lower"},
  {id:"b_atg",name:"ATG Split Squat",category:"Knees",unit:"score",kind:"category",
   description:"Step into a long lunge. Lower your hips until your front hamstring completely covers your calf. Rate your current level.",
   bands:[{label:"Stiff",hint:"12\"+ elevation needed",value:1},{label:"Below avg",hint:"6–10\" elevation",value:2},{label:"Average",hint:"Flat ground, knee bent",value:3},{label:"Flexible",hint:"Flat, knee off floor",value:4},{label:"Very flexible",hint:"Flat, leg straight",value:5}],
   better:"higher"},
  {id:"b_kneewall",name:"Knee-to-Wall",category:"Ankles",unit:"cm",kind:"number",
   description:"Place your big toe a measured distance from a wall. Keeping your heel down, drive your knee forward to touch the wall. Find the max distance where the heel stays grounded.",
   bands:[{label:"Stiff",hint:"< 5 cm",max:5},{label:"Below avg",hint:"5–8 cm",max:8},{label:"Average",hint:"8–10 cm",max:10},{label:"Flexible",hint:"10–13 cm",max:13},{label:"Very flexible",hint:"> 13 cm",max:Infinity}],
   better:"higher"},
  {id:"b_wrist_ext",name:"Wrist Extension",category:"Wrists",unit:"°",kind:"number",
   description:"Place your hand sideways on a table with your fingers forward. Move your hand away from your body until you reach your max pain-free angle.",
   bands:[{label:"Stiff",hint:"< 60°",max:60},{label:"Below avg",hint:"60–70°",max:70},{label:"Average",hint:"70–80°",max:80},{label:"Flexible",hint:"80–90°",max:90},{label:"Very flexible",hint:"> 90°",max:Infinity}],
   better:"higher"},
  {id:"b_wrist_flex",name:"Wrist Flexion",category:"Wrists",unit:"°",kind:"number",
   description:"Place your hand sideways on a table with your fingers forward. Move your hand toward your body until you reach your max pain-free angle.",
   bands:[{label:"Stiff",hint:"< 65°",max:65},{label:"Below avg",hint:"65–75°",max:75},{label:"Average",hint:"75–85°",max:85},{label:"Flexible",hint:"85–95°",max:95},{label:"Very flexible",hint:"> 95°",max:Infinity}],
   better:"higher"},
];

// Helper: given a benchmark and a numeric value, return the band index 0..4
window.bandForValue = function(bm, value){
  if (bm.kind === "category") {
    const b = bm.bands.find(b => b.value === value);
    return b ? bm.bands.indexOf(b) : 2;
  }
  // For "lower is better" we map numeric differently. We want to return an index 0..4
  // where 0 = Stiff and 4 = Very flexible, regardless of direction.
  // Use the bands array order AS AUTHORED (Stiff → Very flexible).
  if (bm.better === "higher") {
    for (let i = 0; i < bm.bands.length; i++){
      if (value <= bm.bands[i].max) return i;
    }
    return bm.bands.length - 1;
  } else {
    // lower is better — bands in order still Stiff → Very flexible.
    // For Apley (Stiff on top because Stiff = Inf gap), bands are ordered Very flexible → Stiff.
    // We wrote bands as Stiff→Very flexible semantically, but the max values require walking.
    // Simplest: walk from the "Very flexible" end down.
    // Each band below uses its authored order. We convert by: find first band where value fits.
    // Bands are in Stiff→Very flexible order semantically. The max of each represents the UPPER bound of that level for "higher-better" metrics.
    // For lower-better, we authored them so that a smaller value means a BETTER band. The max stored is the upper bound of the WORSE band.
    // Walk bands; each has "inverse" flag on the worst band. Use value ≤ max rule but invert mapping.
    // Simpler: per-benchmark we just compute with explicit thresholds.
    // Since we control author order and "lower is better" bands list thresholds in the range the metric takes,
    // we walk from best (last) to worst (first) and return first band matching:
    for (let i = bm.bands.length - 1; i >= 0; i--){
      const b = bm.bands[i];
      if (b.inverse) continue; // the "infinity worst" sentinel
      if (value <= b.max) return i;
    }
    return 0;
  }
};

// ─────────────────────────────────────────────
// Generate 6 months of mock session + benchmark data
// ─────────────────────────────────────────────
(function generateMockData(){
  const today = new Date(2026, 3, 23); // April 23, 2026 — matches system date
  const startDate = new Date(today);
  startDate.setMonth(startDate.getMonth() - 6);

  const sessions = [];
  // Simulate ~5 sessions/week with random skips.
  const d = new Date(startDate);
  // Seed-like pseudo-random
  let seed = 42;
  const rand = () => { seed = (seed * 9301 + 49297) % 233280; return seed / 233280; };

  while (d <= today) {
    const dow = d.getDay();
    // skip some days; more weekends off
    const skipProb = (dow === 0 || dow === 6) ? 0.45 : 0.2;
    if (rand() > skipProb) {
      const duration = 10 + Math.floor(rand() * 6); // 10–15 min
      sessions.push({
        date: d.toISOString().slice(0,10),
        minutes: duration,
        exerciseCount: 7 + Math.floor(rand() * 3),
      });
    }
    d.setDate(d.getDate() + 1);
  }

  // Mark today as NOT completed (so dashboard shows "Start today's session")
  const todayStr = today.toISOString().slice(0,10);
  const todayIdx = sessions.findIndex(s => s.date === todayStr);
  if (todayIdx >= 0) sessions.splice(todayIdx, 1);

  // Compute streak ending today (count back from yesterday)
  let streak = 0;
  const cursor = new Date(today);
  cursor.setDate(cursor.getDate() - 1);
  while (true) {
    const s = cursor.toISOString().slice(0,10);
    if (sessions.find(x => x.date === s)) {
      streak++;
      cursor.setDate(cursor.getDate() - 1);
    } else break;
  }

  window.MOCK_SESSIONS = sessions;
  window.MOCK_STREAK = streak;
  window.MOCK_TOTAL_MINUTES = sessions.reduce((a,s) => a + s.minutes, 0);
  window.MOCK_TOTAL_SESSIONS = sessions.length;
  window.MOCK_TODAY = todayStr;

  // Benchmark history — one entry per month start for ~6 months, trending toward better.
  // For "higher-better" benchmarks, numbers increase. For "lower-better", decrease.
  const history = {};
  const monthStarts = [];
  const ms = new Date(startDate);
  ms.setDate(1);
  while (ms <= today) {
    monthStarts.push(ms.toISOString().slice(0,10));
    ms.setMonth(ms.getMonth() + 1);
  }
  // Drop the latest month (hasn't been logged yet — it's April 1 & user hasn't done it)
  // Actually — we're April 23, so April 1 has passed. Include it.
  // But leave some benchmarks unlogged for this month to make the "benchmark day overdue" banner meaningful.

  const trends = {
    b_cerv_rot:   {start:62, slope:1.8, noise:3},
    b_apley:      {start:12, slope:-1.0, noise:1.5},
    b_thor_rot:   {start:28, slope:1.5, noise:2},
    b_sit_reach:  {start:14, slope:-1.8, noise:1.5},
    b_thomas:     {start:10, slope:-1.4, noise:1.5},
    b_butterfly:  {start:18, slope:-1.3, noise:1},
    b_atg:        {start:2,  slope:0.3, noise:0.3, cat:true},
    b_kneewall:   {start:6,  slope:0.5, noise:0.8},
    b_wrist_ext:  {start:58, slope:2.5, noise:3},
    b_wrist_flex: {start:68, slope:2.0, noise:3},
  };

  for (const bm of window.BENCHMARKS){
    const t = trends[bm.id];
    if (!t) continue;
    history[bm.id] = [];
    // Skip the most recent month for a couple of benchmarks to simulate "overdue"
    const skipLatest = (bm.id === "b_thomas" || bm.id === "b_apley");
    monthStarts.forEach((date, i) => {
      if (skipLatest && i === monthStarts.length - 1) return;
      let v = t.start + t.slope * i + (rand() - 0.5) * t.noise * 2;
      if (t.cat) v = Math.max(1, Math.min(5, Math.round(v)));
      else v = Math.round(v * 10) / 10;
      history[bm.id].push({date, value: v});
    });
  }
  window.MOCK_BENCHMARK_HISTORY = history;
})();

// Build "today's" session — 8 exercises with category diversity and benchmark-driven focus.
(function buildTodaySession(){
  // "Weak" categories based on most recent benchmark band
  const latestByCat = {};
  for (const bm of window.BENCHMARKS){
    const hist = window.MOCK_BENCHMARK_HISTORY[bm.id];
    if (!hist || !hist.length) continue;
    const last = hist[hist.length - 1];
    const band = window.bandForValue(bm, last.value);
    if (!(bm.category in latestByCat) || band < latestByCat[bm.category]) {
      latestByCat[bm.category] = band;
    }
  }
  // Pick 8: at least 1 from each of the 7 categories, plus one extra from the weakest.
  const weakestCat = Object.entries(latestByCat).sort((a,b) => a[1] - b[1])[0]?.[0] || "Hips";

  const picks = [];
  const used = new Set();
  // Deterministic pick: first unused exercise in each category
  for (const cat of window.CATEGORIES){
    const pool = window.EXERCISES.filter(e => e.category === cat && !used.has(e.id));
    // Pick a middle-ish difficulty one
    const pick = pool[Math.min(1, pool.length - 1)];
    if (pick) { picks.push(pick); used.add(pick.id); }
  }
  // Add one extra for the weakest category
  const extra = window.EXERCISES.find(e => e.category === weakestCat && !used.has(e.id));
  if (extra) picks.push(extra);

  window.TODAY_SESSION = picks;
  window.WEAKEST_CATEGORY = weakestCat;
})();

// Category colors (very restrained — all desaturated, each category gets a subtle tint)
window.CATEGORY_TINTS = {
  Neck:      "oklch(0.82 0.04 140)",
  Shoulders: "oklch(0.82 0.04 110)",
  Wrists:    "oklch(0.82 0.04 80)",
  Spine:     "oklch(0.82 0.04 170)",
  Hips:      "oklch(0.82 0.04 50)",
  Knees:     "oklch(0.82 0.04 200)",
  Ankles:    "oklch(0.82 0.04 25)",
};
