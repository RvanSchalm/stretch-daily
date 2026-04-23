// Full-screen session player — exercise → timer → next, plus benchmark carousel.

const { useState: sUS, useEffect: sUE, useRef: sUR } = React;

function SessionPlayer({ sessionList, onExit, onComplete }){
  const [idx, setIdx] = sUS(0);
  const [side, setSide] = sUS("L");            // only relevant for unilateral
  const ex = sessionList[idx];
  const [running, setRunning] = sUS(false);
  const [secondsLeft, setSecondsLeft] = sUS(ex.timed ? ex.target : 0);
  const intervalRef = sUR(null);

  // Reset timer whenever exercise or side changes
  sUE(() => {
    setRunning(false);
    setSecondsLeft(ex.timed ? ex.target : 0);
    return () => clearInterval(intervalRef.current);
  }, [idx, side]);

  // Tick
  sUE(() => {
    if (!running) { clearInterval(intervalRef.current); return; }
    if (!ex.timed) return;
    intervalRef.current = setInterval(() => {
      setSecondsLeft(s => {
        if (s <= 1) {
          clearInterval(intervalRef.current);
          setRunning(false);
          return 0;
        }
        return s - 1;
      });
    }, 1000);
    return () => clearInterval(intervalRef.current);
  }, [running, idx, side]);

  const total = sessionList.length;
  const progressPct = ((idx) / total) * 100;

  const next = () => {
    if (ex.unilateral && side === "L") {
      setSide("R");
      return;
    }
    if (idx < total - 1) {
      setSide("L");
      setIdx(idx + 1);
    } else {
      onComplete();
    }
  };

  const prev = () => {
    if (ex.unilateral && side === "R") { setSide("L"); return; }
    if (idx > 0) { setSide("L"); setIdx(idx - 1); }
  };

  const mmss = s => {
    const m = Math.floor(s/60), r = s%60;
    return String(m).padStart(2,"0") + ":" + String(r).padStart(2,"0");
  };

  // Form cues — split on semicolons
  const cues = ex.cues.split(";").map(c => c.trim()).filter(Boolean);

  return (
    <div style={{
      position:"absolute",inset:0,background:"var(--bg)",zIndex:200,
      display:"flex",flexDirection:"column",
    }}>
      {/* Top bar */}
      <div style={{padding:"16px 20px 10px",display:"flex",alignItems:"center",gap:12}}>
        <button onClick={onExit} style={{background:"var(--bg-2)",border:"none",
          width:32,height:32,borderRadius:"var(--radius-pill)",cursor:"pointer",
          display:"flex",alignItems:"center",justifyContent:"center",color:"var(--ink)"}}>
          <Icon name="close" size={16}/>
        </button>
        <div style={{flex:1}}>
          <div style={{display:"flex",gap:3}}>
            {sessionList.map((_,i) => (
              <div key={i} style={{
                flex:1,height:3,borderRadius:2,
                background: i < idx ? "var(--accent)" : (i===idx ? "var(--accent)" : "var(--line)"),
                opacity: i === idx ? 1 : (i < idx ? 0.55 : 1),
              }}/>
            ))}
          </div>
          <div style={{fontFamily:"var(--font-mono)",fontSize:9.5,letterSpacing:1.2,
                       textTransform:"uppercase",color:"var(--ink-3)",marginTop:6,
                       display:"flex",justifyContent:"space-between"}}>
            <span>Move {idx+1} of {total}</span>
            <span>{ex.category}</span>
          </div>
        </div>
      </div>

      {/* Animation placeholder */}
      <div style={{padding:"6px 20px 12px"}}>
        <div style={{
          width:"100%",aspectRatio:"4/3",borderRadius:"var(--radius-md)",
          background: window.CATEGORY_TINTS[ex.category],
          position:"relative",overflow:"hidden",
        }}>
          <svg viewBox="0 0 100 75" width="100%" height="100%"
               preserveAspectRatio="xMidYMid slice" style={{display:"block",position:"absolute",inset:0}}>
            <defs>
              <pattern id={"ply-" + ex.id} width="8" height="8" patternUnits="userSpaceOnUse"
                       patternTransform="rotate(45)">
                <rect width="8" height="8" fill="transparent"/>
                <rect width="3" height="8" fill="rgba(255,255,255,0.35)"/>
              </pattern>
            </defs>
            <rect width="100" height="75" fill={`url(#ply-${ex.id})`}/>
          </svg>
          <div style={{position:"absolute",inset:0,padding:14,
                       display:"flex",flexDirection:"column",justifyContent:"flex-end",
                       fontFamily:"var(--font-mono)",color:"rgba(31,42,29,0.65)",fontSize:10.5}}>
            <div style={{display:"flex",justifyContent:"space-between",alignItems:"flex-end"}}>
              <span style={{opacity:0.85,letterSpacing:0.3}}>{ex.category} · animation</span>
              <span style={{opacity:0.6}}>.webp placeholder</span>
            </div>
          </div>
        </div>
      </div>

      {/* Exercise info */}
      <div style={{padding:"0 20px 12px"}}>
        <h2 style={{fontFamily:"var(--font-display)",fontSize:26,fontWeight:500,
                    letterSpacing:-0.5,margin:"2px 0 4px",color:"var(--ink)"}}>
          {ex.name}
        </h2>
      </div>

      {/* Cues — always visible */}
      <div style={{padding:"0 20px 14px"}}>
        <ul style={{margin:0,padding:"0 0 0 16px",display:"flex",flexDirection:"column",gap:4,
                    listStyle:"none"}}>
          {cues.slice(0,4).map((c,i) => (
            <li key={i} style={{display:"flex",gap:8,alignItems:"flex-start",
                                fontSize:12.5,color:"var(--ink-2)",lineHeight:1.45,
                                fontFamily:"var(--font-body)"}}>
              <span style={{color:"var(--accent)",fontWeight:700,flexShrink:0,marginTop:1}}>—</span>
              {c}
            </li>
          ))}
        </ul>
      </div>

      <div style={{flex:1}}/>

      {/* Big timer / rep — with L/R indicator inline */}
      <div style={{padding:"0 20px 14px",textAlign:"center"}}>
        {ex.unilateral && (
          <div style={{
            display:"inline-flex",alignItems:"center",gap:6,
            padding:"4px 10px",borderRadius:"var(--radius-pill)",
            background: window.CATEGORY_TINTS[ex.category],
            color:"var(--ink)",
            fontFamily:"var(--font-mono)",fontSize:10,letterSpacing:1.5,
            textTransform:"uppercase",fontWeight:600,marginBottom:6,
          }}>
            <span style={{opacity: side==="L" ? 1 : 0.3}}>Left</span>
            <span style={{opacity:0.4}}>—</span>
            <span style={{opacity: side==="R" ? 1 : 0.3}}>Right</span>
          </div>
        )}
        {ex.timed ? (
          <div style={{fontFamily:"var(--font-display)",fontSize:76,fontWeight:500,
                       color:"var(--ink)",letterSpacing:-3,lineHeight:1}}>
            {mmss(secondsLeft)}
          </div>
        ) : (
          <div style={{fontFamily:"var(--font-display)",fontSize:56,fontWeight:500,
                       color:"var(--ink)",letterSpacing:-1.5,lineHeight:1}}>
            {ex.target}<span style={{fontSize:16,color:"var(--ink-3)",marginLeft:8,
                                     fontFamily:"var(--font-body)",fontWeight:500}}>reps</span>
          </div>
        )}
        <div style={{fontFamily:"var(--font-mono)",fontSize:10,letterSpacing:1.4,
                     textTransform:"uppercase",color:"var(--ink-3)",marginTop:8}}>
          {ex.timed ? (running ? "Hold the position" : "Ready when you are") :
                      "Tap Next when complete"}
        </div>
      </div>

      {/* Controls */}
      <div style={{padding:"0 20px calc(20px + env(safe-area-inset-bottom, 0px))",
                   display:"flex",gap:10,alignItems:"center"}}>
        <button onClick={prev} disabled={idx===0 && !(ex.unilateral && side==="R")} style={{
          width:52,height:52,borderRadius:"var(--radius-pill)",
          background:"var(--bg-2)",color:"var(--ink)",border:"none",cursor:"pointer",
          display:"flex",alignItems:"center",justifyContent:"center",
          opacity: (idx===0 && !(ex.unilateral && side==="R")) ? 0.4 : 1,
          flexShrink:0,
        }}>
          <Icon name="chevronL" size={18}/>
        </button>
        {ex.timed ? (
          <button onClick={() => setRunning(!running)} style={{
            flex:1,background:"var(--accent)",color:"var(--accent-ink)",border:"none",
            borderRadius:"var(--radius-pill)",padding:"18px",cursor:"pointer",
            fontFamily:"var(--font-body)",fontSize:14,fontWeight:600,letterSpacing:0.3,
            display:"flex",alignItems:"center",justifyContent:"center",gap:8,
          }}>
            <Icon name={running ? "pause" : "play"} size={16} stroke={0}/>
            {running ? "Pause" : (secondsLeft === ex.target ? "Start" : "Resume")}
          </button>
        ) : (
          <div style={{flex:1,background:"var(--surface)",color:"var(--ink-3)",
                       border:"1px dashed var(--line)",borderRadius:"var(--radius-pill)",
                       padding:"18px",textAlign:"center",
                       fontFamily:"var(--font-mono)",fontSize:11,letterSpacing:1,
                       textTransform:"uppercase"}}>
            Perform at your own pace
          </div>
        )}
        <button onClick={next} style={{
          width:52,height:52,borderRadius:"var(--radius-pill)",
          background:"var(--ink)",color:"var(--bg)",border:"none",cursor:"pointer",
          display:"flex",alignItems:"center",justifyContent:"center",flexShrink:0,
        }}>
          <Icon name="chevronR" size={18}/>
        </button>
      </div>
    </div>
  );
}

function SessionDoneScreen({ onExit, stats }){
  return (
    <div style={{position:"absolute",inset:0,background:"var(--accent)",color:"var(--accent-ink)",
                 zIndex:200,display:"flex",flexDirection:"column",alignItems:"center",
                 justifyContent:"center",padding:30,textAlign:"center"}}>
      <div style={{fontFamily:"var(--font-mono)",fontSize:11,letterSpacing:1.5,
                   textTransform:"uppercase",opacity:0.7,marginBottom:10}}>
        Session complete
      </div>
      <div style={{fontFamily:"var(--font-display)",fontSize:54,fontWeight:500,
                   lineHeight:1,letterSpacing:-1.2,margin:"0 0 16px"}}>
        <em style={{fontStyle:"italic"}}>Well done.</em>
      </div>
      <div style={{fontSize:14,opacity:0.82,textWrap:"pretty",maxWidth:280,lineHeight:1.5,
                   fontFamily:"var(--font-body)"}}>
        You stretched {stats.count} areas in {stats.minutes} minutes.
        Streak's at <b>{stats.streak+1}</b> now.
      </div>
      <div style={{display:"flex",gap:10,marginTop:30,alignItems:"center",opacity:0.85}}>
        <div style={{width:40,height:2,background:"currentColor"}}/>
        <Icon name="flame" size={18}/>
        <div style={{width:40,height:2,background:"currentColor"}}/>
      </div>
      <button onClick={onExit} style={{
        marginTop:40,background:"var(--accent-ink)",color:"var(--accent)",border:"none",
        borderRadius:"var(--radius-pill)",padding:"14px 30px",cursor:"pointer",
        fontFamily:"var(--font-body)",fontSize:13,fontWeight:600,letterSpacing:0.3,
      }}>Back to today</button>
    </div>
  );
}

// ─────────────────────────────────────────────
// Benchmark carousel — one-at-a-time walkthrough
// ─────────────────────────────────────────────
function BenchmarkCarousel({ onClose }){
  const [i, setI] = sUS(0);
  const bms = window.BENCHMARKS;
  const bm = bms[i];
  const done = () => {
    if (i < bms.length - 1) setI(i+1);
    else onClose();
  };

  return (
    <div style={{position:"absolute",inset:0,background:"var(--bg)",zIndex:200,
                 display:"flex",flexDirection:"column"}}>
      <div style={{padding:"16px 20px 10px",display:"flex",alignItems:"center",gap:12}}>
        <button onClick={onClose} style={{background:"var(--bg-2)",border:"none",
          width:32,height:32,borderRadius:"var(--radius-pill)",cursor:"pointer",
          display:"flex",alignItems:"center",justifyContent:"center",color:"var(--ink)"}}>
          <Icon name="close" size={16}/>
        </button>
        <div style={{flex:1}}>
          <div style={{display:"flex",gap:3}}>
            {bms.map((_,j) => (
              <div key={j} style={{flex:1,height:3,borderRadius:2,
                background: j < i ? "var(--accent)" : (j===i ? "var(--accent)" : "var(--line)"),
                opacity: j === i ? 1 : (j < i ? 0.55 : 1),
              }}/>
            ))}
          </div>
          <div style={{fontFamily:"var(--font-mono)",fontSize:9.5,letterSpacing:1.2,
                       textTransform:"uppercase",color:"var(--ink-3)",marginTop:6}}>
            Benchmark {i+1} of {bms.length} · {bm.category}
          </div>
        </div>
      </div>
      <div style={{flex:1,overflowY:"auto",padding:"8px 20px 100px"}}>
        <h2 style={{fontFamily:"var(--font-display)",fontSize:30,fontWeight:500,
                    letterSpacing:-0.6,margin:"14px 0 6px",color:"var(--ink)"}}>
          {bm.name}
        </h2>
        <div style={{fontSize:13,color:"var(--ink-2)",marginBottom:14,lineHeight:1.5,
                     fontFamily:"var(--font-body)",textWrap:"pretty"}}>
          {bm.description}
        </div>
        <LogForm bm={bm} onClose={done}/>
      </div>
    </div>
  );
}

Object.assign(window, { SessionPlayer, SessionDoneScreen, BenchmarkCarousel });
