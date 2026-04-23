// The 5 top-level screens. Reach window globals for data + components.

const { useState: uS, useEffect: uE, useMemo: uM, useRef: uR } = React;

// ─────────────────────────────────────────────
// 1. DASHBOARD
// ─────────────────────────────────────────────
function DashboardScreen({ theme, tweaks, goTo, openBenchmarkCarousel }) {
  const sessions = tweaks.populated ? window.MOCK_SESSIONS : [];
  const streak = tweaks.populated ? window.MOCK_STREAK : 0;
  const totalMin = tweaks.populated ? window.MOCK_TOTAL_MINUTES : 0;
  const today = window.MOCK_TODAY;
  // "Next benchmark day" = 1st of next month
  const todayD = new Date(today);
  const nextBench = new Date(todayD.getFullYear(), todayD.getMonth() + 1, 1);
  const daysUntil = Math.round((nextBench - todayD) / (1000*60*60*24));

  const isBold = theme.flavor.headerStyle === "bold-block";
  const isSerif = theme.flavor.headerStyle === "serif-hero";

  const todayLabel = todayD.toLocaleDateString('en', { weekday:'long', month:'long', day:'numeric' });

  // Session duration estimate
  const estSec = window.TODAY_SESSION.reduce((a, e) => a + (e.unilateral ? e.total*2 : e.total), 0);
  const estMin = Math.round(estSec / 60);

  return (
    <div style={{padding:"6px 20px 100px"}}>
      {/* Top row */}
      <div style={{display:"flex",justifyContent:"space-between",alignItems:"center",
                   padding:"14px 0 6px"}}>
        <div style={{fontFamily:"var(--font-mono)",fontSize:10.5,letterSpacing:1.2,
                     textTransform:"uppercase",color:"var(--ink-3)"}}>
          {todayLabel}
        </div>
        <div style={{display:"flex",alignItems:"center",gap:5,
                     fontFamily:"var(--font-mono)",fontSize:11,color:"var(--accent)",
                     fontWeight:600}}>
          <Icon name="flame" size={14}/>
          {streak} day{streak===1?"":"s"}
        </div>
      </div>

      {/* Hero title */}
      <h1 style={{
        fontFamily: isSerif || isBold ? "var(--font-display)" : "var(--font-body)",
        fontSize: isBold ? 44 : (isSerif ? 40 : 30),
        fontWeight: isBold ? 500 : (isSerif ? 400 : 600),
        lineHeight: 1.02,
        letterSpacing: isSerif ? -1 : -0.5,
        margin:"6px 0 4px",
        color:"var(--ink)",
      }}>
        {<>Stretch Daily</>}
      </h1>
      <div style={{color:"var(--ink-2)",fontSize:14,marginBottom:18,
                   fontFamily:"var(--font-body)",textWrap:"pretty"}}>
        {window.TODAY_SESSION.length} exercises · ~{estMin} minutes · mixed full body
      </div>

      {/* Benchmark banner */}
      {tweaks.benchmarkBannerOn && (
        <div onClick={openBenchmarkCarousel} style={{
          background: isBold ? "var(--bg-2)" : "var(--accent-soft)",
          color: isBold ? "var(--accent-ink)" : "var(--ink)",
          borderRadius:"var(--radius-md)",
          padding:"14px 16px",marginBottom:14,cursor:"pointer",
          display:"flex",alignItems:"center",gap:12,
          border: theme.flavor.cardStyle === "hairline" ? "1px solid var(--line)" : "none",
        }}>
          <div style={{
            width:36,height:36,borderRadius:"var(--radius-sm)",
            background: isBold ? "var(--accent-2)" : "var(--accent)",
            color: isBold ? "var(--ink)" : "var(--accent-ink)",
            display:"flex",alignItems:"center",justifyContent:"center",flexShrink:0,
          }}>
            <Icon name="sparkle" size={18}/>
          </div>
          <div style={{flex:1,minWidth:0}}>
            <div style={{fontFamily:"var(--font-body)",fontSize:13,fontWeight:600,
                         letterSpacing:-0.1}}>
              Benchmark day — 2 overdue
            </div>
            <div style={{fontSize:11.5,opacity:0.78,marginTop:2,
                         fontFamily:"var(--font-body)"}}>
              Thomas Test & Apley haven't been logged for April
            </div>
          </div>
          <Icon name="chevronR" size={18}/>
        </div>
      )}

      {/* Start session card — big */}
      <div onClick={() => goTo("session")} style={{
        background: isBold ? "var(--bg-2)" : "var(--surface)",
        color: isBold ? "var(--accent-ink)" : "var(--ink)",
        borderRadius:"var(--radius-lg)",
        padding:"20px 20px 22px",marginBottom:18,cursor:"pointer",
        border: theme.flavor.cardStyle === "hairline" && !isBold ? "1px solid var(--line)" : "none",
        boxShadow: theme.flavor.cardStyle === "soft" ? "0 2px 0 var(--line-2), 0 8px 24px -16px rgba(0,0,0,.15)" : "none",
        position:"relative",overflow:"hidden",
      }}>
        <div style={{display:"flex",justifyContent:"space-between",alignItems:"flex-start"}}>
          <div>
            <div style={{fontFamily:"var(--font-mono)",fontSize:10,letterSpacing:1.2,
                         textTransform:"uppercase",opacity:0.7,marginBottom:10}}>
              Today's session
            </div>
            <div style={{fontFamily: isSerif ? "var(--font-display)" : "var(--font-body)",
                         fontSize: isSerif ? 26 : 22,fontWeight: isSerif?400:600,
                         letterSpacing:-0.4,marginBottom:4}}>
              {estMin} minutes
            </div>
            <div style={{fontSize:12,opacity:0.72,fontFamily:"var(--font-body)"}}>
              Extra focus: <b>{window.WEAKEST_CATEGORY}</b>
            </div>
          </div>
          <div style={{
            width:58,height:58,borderRadius:"var(--radius-pill)",
            background: isBold ? "var(--accent-2)" : "var(--accent)",
            color: isBold ? "var(--ink)" : "var(--accent-ink)",
            display:"flex",alignItems:"center",justifyContent:"center",
            flexShrink:0,
          }}>
            <Icon name="play" size={22} stroke={0}/>
          </div>
        </div>
        {/* Little category dots */}
        <div style={{display:"flex",gap:4,marginTop:18}}>
          {window.TODAY_SESSION.map((e,i) => (
            <div key={i} style={{
              flex:1,height:5,borderRadius:3,
              background: window.CATEGORY_TINTS[e.category],
              opacity: 0.9,
            }}/>
          ))}
        </div>
      </div>

      {/* This week */}
      <Section label="This week">
        <WeekStrip sessions={sessions} today={today}/>
      </Section>

      {/* KPIs */}
      <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:10,marginTop:18}}>
        <KpiCard theme={theme} label="Streak" value={streak} suffix="days" icon="flame"/>
        <KpiCard theme={theme} label="Total time" value={totalMin} suffix="min" icon="clock"/>
        <KpiCard theme={theme} label="Sessions" value={sessions.length} suffix="logged" icon="check"/>
        <KpiCard theme={theme} label="Next benchmark" value={daysUntil} suffix={daysUntil===1?"day":"days"} icon="calendar"/>
      </div>

    </div>
  );
}

function Section({ label, children, style }) {
  return (
    <div style={{marginTop:14, ...style}}>
      <div style={{
        fontFamily:"var(--font-mono)",fontSize:10,letterSpacing:1.4,
        textTransform:"uppercase",color:"var(--ink-3)",marginBottom:10,
      }}>{label}</div>
      {children}
    </div>
  );
}

function KpiCard({ theme, label, value, suffix, icon }) {
  const isBlocky = theme.flavor.cardStyle === "blocky";
  const isHairline = theme.flavor.cardStyle === "hairline";
  return (
    <div style={{
      background: "var(--surface)",
      border: isHairline ? "1px solid var(--line)" : "none",
      borderRadius:"var(--radius-md)",
      padding:"13px 14px",
      boxShadow: !isHairline && !isBlocky ? "0 1px 0 var(--line-2)" : "none",
    }}>
      <div style={{display:"flex",justifyContent:"space-between",alignItems:"center",marginBottom:6}}>
        <div style={{fontFamily:"var(--font-mono)",fontSize:9.5,letterSpacing:1,
                     textTransform:"uppercase",color:"var(--ink-3)"}}>{label}</div>
        <div style={{color:"var(--ink-3)"}}><Icon name={icon} size={14}/></div>
      </div>
      <div style={{display:"flex",alignItems:"baseline",gap:5}}>
        <div style={{fontFamily:"var(--font-display)",fontSize:26,fontWeight:500,
                     color:"var(--ink)",letterSpacing:-0.5,lineHeight:1}}>{value}</div>
        <div style={{fontSize:11,color:"var(--ink-3)",fontFamily:"var(--font-body)"}}>{suffix}</div>
      </div>
    </div>
  );
}

function MonthlyRhythm({ sessions, today }){
  // Last 6 months, count of sessions per month
  const todayD = new Date(today);
  const months = [];
  for (let i = 5; i >= 0; i--){
    const d = new Date(todayD.getFullYear(), todayD.getMonth() - i, 1);
    const ym = d.getFullYear() + "-" + String(d.getMonth()+1).padStart(2,"0");
    const count = sessions.filter(s => s.date.startsWith(ym)).length;
    months.push({ ym, count, label: d.toLocaleString('en',{month:'short'}) });
  }
  const max = Math.max(...months.map(m => m.count), 1);
  return (
    <div style={{display:"flex",gap:6,alignItems:"flex-end",height:56}}>
      {months.map((m,i) => (
        <div key={i} style={{flex:1,display:"flex",flexDirection:"column",alignItems:"stretch",gap:5}}>
          <div style={{height:40,display:"flex",alignItems:"flex-end"}}>
            <div style={{
              width:"100%",
              height:`${(m.count/max)*100}%`,
              background:"var(--accent)",opacity: i===5 ? 1 : 0.45,
              borderRadius:"var(--radius-xs)",
              minHeight:2,
            }}/>
          </div>
          <div style={{fontSize:9.5,fontFamily:"var(--font-mono)",color:"var(--ink-3)",
                       textAlign:"center",letterSpacing:0.4}}>{m.label}</div>
        </div>
      ))}
    </div>
  );
}

// ─────────────────────────────────────────────
// 2. SESSION overview (pre-start list)
// ─────────────────────────────────────────────
function SessionOverviewScreen({ theme, tweaks, startSession, sessionList, onSwap, onRemove }) {
  const [swapFor, setSwapFor] = uS(null);
  const estSec = sessionList.reduce((a, e) => a + (e.unilateral ? e.total*2 : e.total), 0);
  const estMin = Math.round(estSec / 60);

  return (
    <div style={{padding:"6px 20px 100px"}}>
      {/* Header */}
      <div style={{padding:"14px 0 12px"}}>
        <div style={{fontFamily:"var(--font-mono)",fontSize:10,letterSpacing:1.2,
                     textTransform:"uppercase",color:"var(--ink-3)",marginBottom:6}}>
          Today's session
        </div>
        <h2 style={{
          fontFamily:"var(--font-display)",fontSize:32,fontWeight:500,
          letterSpacing:-0.6,margin:"0 0 4px",color:"var(--ink)",
        }}>
          {estMin} min<span style={{fontSize:18,color:"var(--ink-3)",marginLeft:6}}>
            · {sessionList.length} moves
          </span>
        </h2>
        <div style={{fontSize:13,color:"var(--ink-2)",fontFamily:"var(--font-body)",textWrap:"pretty"}}>
          Tap any exercise to see cues, or{" "}
          <Icon name="swap" size={12}/> to swap for another in the same category.
        </div>
      </div>

      {/* Category spread */}
      <div style={{display:"flex",gap:6,padding:"8px 0 16px"}}>
        {sessionList.map((e,i) => (
          <div key={i} style={{flex:1}} title={e.category}>
            <div style={{
              height:4,borderRadius:2,
              background: window.CATEGORY_TINTS[e.category],
            }}/>
          </div>
        ))}
      </div>

      {/* Exercise list */}
      <div style={{display:"flex",flexDirection:"column",gap:8}}>
        {sessionList.map((ex, i) => (
          <ExerciseRow key={ex.id+"_"+i} ex={ex} idx={i+1} theme={theme}
                       onSwap={() => setSwapFor(i)}/>
        ))}
      </div>

      {/* Primary CTA */}
      <button onClick={startSession} style={{
        width:"100%",marginTop:22,
        background:"var(--accent)",color:"var(--accent-ink)",border:"none",
        borderRadius:"var(--radius-pill)",padding:"18px",
        fontFamily:"var(--font-body)",fontSize:15,fontWeight:600,letterSpacing:0.2,
        cursor:"pointer",display:"flex",alignItems:"center",justifyContent:"center",gap:10,
      }}>
        <Icon name="play" size={18} stroke={0}/> Begin session
      </button>

      <Sheet open={swapFor !== null} onClose={() => setSwapFor(null)}
             title={swapFor !== null ? `Swap "${sessionList[swapFor].name}"` : ""}>
        {swapFor !== null && (
          <SwapPicker current={sessionList[swapFor]}
                      onPick={e => { onSwap(swapFor, e); setSwapFor(null); }}/>
        )}
      </Sheet>
    </div>
  );
}

function ExerciseRow({ ex, idx, theme, onSwap }){
  const [expanded, setExpanded] = uS(false);
  const isHairline = theme.flavor.cardStyle === "hairline";
  return (
    <div style={{
      background:"var(--surface)",
      border: isHairline ? "1px solid var(--line)" : "none",
      borderRadius:"var(--radius-md)",
      overflow:"hidden",
      boxShadow: !isHairline ? "0 1px 0 var(--line-2)" : "none",
    }}>
      <div style={{display:"flex",alignItems:"center",gap:12,padding:"10px 12px",cursor:"pointer"}}
           onClick={() => setExpanded(!expanded)}>
        <ExerciseTile exercise={ex} size="sm"/>
        <div style={{flex:1,minWidth:0}}>
          <div style={{display:"flex",alignItems:"baseline",gap:6,marginBottom:2}}>
            <span style={{fontFamily:"var(--font-mono)",fontSize:10,color:"var(--ink-3)",
                          letterSpacing:0.4}}>{String(idx).padStart(2,"0")}</span>
            <span style={{fontFamily:"var(--font-body)",fontSize:13.5,fontWeight:600,
                          color:"var(--ink)",letterSpacing:-0.1}}>{ex.name}</span>
          </div>
          <div style={{display:"flex",gap:5,alignItems:"center",flexWrap:"wrap"}}>
            <CatChip category={ex.category} theme={theme}/>
            <span style={{fontSize:10.5,color:"var(--ink-3)",fontFamily:"var(--font-mono)"}}>
              {ex.timed ? `${ex.target}s` : `${ex.target} reps`}
              {ex.unilateral ? " · L/R" : ""}
            </span>
          </div>
        </div>
        <button onClick={e => { e.stopPropagation(); onSwap(); }} style={{
          background:"var(--bg-2)",border:"none",width:34,height:34,
          borderRadius:"var(--radius-sm)",cursor:"pointer",
          display:"flex",alignItems:"center",justifyContent:"center",
          color:"var(--ink-2)",
        }}>
          <Icon name="swap" size={15}/>
        </button>
      </div>
      {expanded && (
        <div style={{padding:"0 14px 14px",borderTop:"1px solid var(--line-2)"}}>
          <div style={{fontFamily:"var(--font-mono)",fontSize:9.5,letterSpacing:1.2,
                       textTransform:"uppercase",color:"var(--ink-3)",margin:"12px 0 6px"}}>
            Form cues
          </div>
          <div style={{fontSize:12.5,color:"var(--ink-2)",lineHeight:1.5,
                       fontFamily:"var(--font-body)",textWrap:"pretty"}}>
            {ex.cues}
          </div>
        </div>
      )}
    </div>
  );
}

function CatChip({ category, theme }){
  const tint = window.CATEGORY_TINTS[category];
  const isSolid = theme.flavor.chipStyle === "solid-mint";
  const isOutline = theme.flavor.chipStyle === "outline";
  return (
    <span style={{
      fontFamily:"var(--font-mono)",fontSize:9.5,letterSpacing:0.5,
      textTransform:"uppercase",fontWeight:600,
      padding:"2px 8px",borderRadius:"var(--radius-pill)",
      background: isOutline ? "transparent" : tint,
      color: isSolid ? "var(--ink)" : "var(--ink)",
      border: isOutline ? `1px solid ${tint}` : "none",
    }}>{category}</span>
  );
}

function SwapPicker({ current, onPick }) {
  const options = window.EXERCISES.filter(e => e.category === current.category && e.id !== current.id);
  return (
    <div style={{display:"flex",flexDirection:"column",gap:8}}>
      {options.map(e => (
        <button key={e.id} onClick={() => onPick(e)} style={{
          display:"flex",alignItems:"center",gap:12,
          background:"var(--bg)",border:"none",padding:"10px",
          borderRadius:"var(--radius-md)",cursor:"pointer",textAlign:"left",
        }}>
          <ExerciseTile exercise={e} size="sm"/>
          <div style={{flex:1,minWidth:0}}>
            <div style={{fontFamily:"var(--font-body)",fontSize:13.5,fontWeight:600,color:"var(--ink)"}}>
              {e.name}
            </div>
            <div style={{fontSize:10.5,color:"var(--ink-3)",fontFamily:"var(--font-mono)"}}>
              {e.difficulty} · {e.timed ? `${e.target}s` : `${e.target} reps`}
            </div>
          </div>
          <Icon name="chevronR" size={16}/>
        </button>
      ))}
    </div>
  );
}

// ─────────────────────────────────────────────
// 3. BENCHMARK LOGGING
// ─────────────────────────────────────────────
function BenchmarkLogScreen({ theme, tweaks, openBenchmarkCarousel }) {
  const [openBm, setOpenBm] = uS(null);
  const [expanded, setExpanded] = uS(new Set());
  const history = tweaks.populated ? window.MOCK_BENCHMARK_HISTORY : {};
  const isBold = theme.flavor.headerStyle === "bold-block";

  const toggle = id => {
    const n = new Set(expanded);
    if (n.has(id)) n.delete(id); else n.add(id);
    setExpanded(n);
  };

  return (
    <div style={{padding:"6px 20px 100px"}}>
      <div style={{padding:"14px 0 4px"}}>
        <div style={{fontFamily:"var(--font-mono)",fontSize:10,letterSpacing:1.2,
                     textTransform:"uppercase",color:"var(--ink-3)",marginBottom:6}}>
          Monthly benchmarks
        </div>
        <h2 style={{fontFamily:"var(--font-display)",fontSize:30,fontWeight:500,
                    letterSpacing:-0.6,margin:"0 0 6px",color:"var(--ink)"}}>
          Log & review
        </h2>
        <div style={{fontSize:13,color:"var(--ink-2)",fontFamily:"var(--font-body)",textWrap:"pretty"}}>
          Ten benchmarks across seven categories.
          Logged on the 1st of each month.
        </div>
      </div>

      {/* Carousel CTA */}
      <button onClick={openBenchmarkCarousel} style={{
        marginTop:14,marginBottom:18,width:"100%",
        background:"var(--accent-soft)",color:"var(--ink)",border:"none",
        borderRadius:"var(--radius-md)",padding:"13px 14px",cursor:"pointer",
        display:"flex",alignItems:"center",gap:12,
        fontFamily:"var(--font-body)",textAlign:"left",
      }}>
        <div style={{width:34,height:34,borderRadius:"var(--radius-sm)",
                     background:"var(--accent)",color:"var(--accent-ink)",
                     display:"flex",alignItems:"center",justifyContent:"center",flexShrink:0}}>
          <Icon name="sparkle" size={16}/>
        </div>
        <div style={{flex:1}}>
          <div style={{fontSize:13.5,fontWeight:600,letterSpacing:-0.1}}>Start benchmark day</div>
          <div style={{fontSize:11,opacity:0.75,marginTop:1}}>Walk through all 10, one at a time</div>
        </div>
        <Icon name="chevronR" size={16}/>
      </button>

      {/* Grouped list */}
      {window.CATEGORIES.map(cat => {
        const bms = window.BENCHMARKS.filter(b => b.category === cat);
        if (!bms.length) return null;
        return (
          <div key={cat} style={{marginBottom:18}}>
            <div style={{display:"flex",alignItems:"center",gap:8,marginBottom:8}}>
              <div style={{width:10,height:10,borderRadius:3,
                           background:window.CATEGORY_TINTS[cat]}}/>
              <div style={{fontFamily:"var(--font-mono)",fontSize:10,letterSpacing:1.4,
                           textTransform:"uppercase",color:"var(--ink-2)",fontWeight:600}}>
                {cat}
              </div>
            </div>
            <div style={{display:"flex",flexDirection:"column",gap:8}}>
              {bms.map(bm => (
                <BenchmarkRow key={bm.id} bm={bm}
                              hist={history[bm.id]||[]}
                              expanded={expanded.has(bm.id)}
                              onToggle={() => toggle(bm.id)}
                              onLog={() => setOpenBm(bm)}
                              theme={theme}/>
              ))}
            </div>
          </div>
        );
      })}

      <Sheet open={!!openBm} onClose={() => setOpenBm(null)}
             title={openBm?.name}>
        {openBm && <LogForm bm={openBm} onClose={() => setOpenBm(null)}/>}
      </Sheet>
    </div>
  );
}

function BenchmarkRow({ bm, hist, expanded, onToggle, onLog, theme }){
  const isHairline = theme.flavor.cardStyle === "hairline";
  const last = hist[hist.length - 1];
  const prev = hist[hist.length - 2];
  const band = last ? window.bandForValue(bm, last.value) : null;
  const prevBand = prev ? window.bandForValue(bm, prev.value) : null;
  const trend = (band !== null && prevBand !== null) ? band - prevBand : 0;

  const todayD = new Date(window.MOCK_TODAY);
  const currentMonth = todayD.getFullYear() + "-" + String(todayD.getMonth()+1).padStart(2,"0");
  const logged = hist.find(h => h.date.startsWith(currentMonth));
  const overdue = todayD.getDate() >= 1 && !logged;

  return (
    <div style={{
      background:"var(--surface)",
      border: isHairline ? "1px solid var(--line)" : "none",
      borderRadius:"var(--radius-md)",
      overflow:"hidden",
      boxShadow: !isHairline ? "0 1px 0 var(--line-2)" : "none",
    }}>
      <div style={{padding:"12px 14px",display:"flex",alignItems:"center",gap:12}}>
        <div style={{flex:1,minWidth:0}}>
          <div style={{fontFamily:"var(--font-body)",fontSize:13.5,fontWeight:600,
                       color:"var(--ink)",letterSpacing:-0.1,display:"flex",gap:6,alignItems:"center"}}>
            {bm.name}
            {overdue && (
              <span style={{fontSize:9.5,fontFamily:"var(--font-mono)",
                            color:"var(--warn)",background:"rgba(154,90,30,0.12)",
                            padding:"1px 6px",borderRadius:"var(--radius-pill)",
                            letterSpacing:0.5,textTransform:"uppercase",fontWeight:600}}>
                Overdue
              </span>
            )}
          </div>
          <div style={{display:"flex",gap:10,alignItems:"center",marginTop:4}}>
            {last ? (
              <>
                <span style={{fontFamily:"var(--font-display)",fontSize:17,fontWeight:500,color:"var(--ink)",lineHeight:1}}>
                  {last.value}<span style={{fontSize:11,marginLeft:2,color:"var(--ink-3)",fontFamily:"var(--font-body)"}}>
                    {bm.unit}
                  </span>
                </span>
                <BandPill band={band} bm={bm}/>
                <Sparkline values={hist.map(h => h.value)} w={50} h={14}/>
              </>
            ) : (
              <span style={{fontSize:11,color:"var(--ink-3)",fontFamily:"var(--font-body)",fontStyle:"italic"}}>
                Not logged yet
              </span>
            )}
          </div>
        </div>
        <button onClick={onLog} style={{
          background:"var(--accent)",color:"var(--accent-ink)",border:"none",
          padding:"7px 12px",borderRadius:"var(--radius-pill)",cursor:"pointer",
          fontFamily:"var(--font-body)",fontSize:11.5,fontWeight:600,
          display:"flex",alignItems:"center",gap:4,
        }}>
          <Icon name="plus" size={13}/> Log
        </button>
        <button onClick={onToggle} style={{
          background:"none",border:"none",cursor:"pointer",color:"var(--ink-3)",
          transform: expanded ? "rotate(180deg)" : "none",transition:"transform .2s",
        }}>
          <Icon name="chevronD" size={18}/>
        </button>
      </div>
      {expanded && (
        <div style={{borderTop:"1px solid var(--line-2)",padding:"12px 14px",
                     background:"var(--bg)"}}>
          <div style={{fontSize:11.5,color:"var(--ink-2)",lineHeight:1.5,marginBottom:10,
                       fontFamily:"var(--font-body)",textWrap:"pretty"}}>
            {bm.description}
          </div>
          <div style={{fontFamily:"var(--font-mono)",fontSize:9.5,letterSpacing:1,
                       textTransform:"uppercase",color:"var(--ink-3)",marginBottom:6}}>
            Bands
          </div>
          <div style={{display:"flex",flexDirection:"column",gap:3,marginBottom:10}}>
            {bm.bands.map((b,i) => (
              <div key={i} style={{display:"flex",gap:8,alignItems:"center",fontSize:11}}>
                <div style={{
                  width:8,height:8,borderRadius:2,
                  background: i === band ? "var(--accent)" : "var(--line)",
                  flexShrink:0,
                }}/>
                <span style={{fontFamily:"var(--font-body)",fontWeight: i===band?600:400,color: i===band?"var(--ink)":"var(--ink-2)",minWidth:85}}>
                  {b.label}
                </span>
                <span style={{fontFamily:"var(--font-mono)",color:"var(--ink-3)",fontSize:10.5}}>
                  {b.hint}
                </span>
              </div>
            ))}
          </div>
          {hist.length > 0 && (
            <>
              <div style={{fontFamily:"var(--font-mono)",fontSize:9.5,letterSpacing:1,
                           textTransform:"uppercase",color:"var(--ink-3)",marginBottom:6,marginTop:8}}>
                History
              </div>
              <div style={{display:"flex",flexDirection:"column",gap:3}}>
                {hist.slice().reverse().map((h,i) => {
                  const d = new Date(h.date);
                  return (
                    <div key={i} style={{display:"flex",justifyContent:"space-between",
                                          alignItems:"center",fontSize:11.5,
                                          padding:"6px 0",
                                          borderBottom: i === hist.length-1 ? "none" : "1px solid var(--line-2)",
                                          fontFamily:"var(--font-body)",color:"var(--ink-2)"}}>
                      <span>{d.toLocaleDateString('en',{month:'short',year:'numeric'})}</span>
                      <span style={{display:"flex",gap:8,alignItems:"center"}}>
                        <b style={{color:"var(--ink)"}}>{h.value} {bm.unit}</b>
                        <BandPill band={window.bandForValue(bm, h.value)} bm={bm} small/>
                      </span>
                    </div>
                  );
                })}
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}

function BandPill({ band, bm, small }){
  if (band === null || band === undefined) return null;
  const b = bm.bands[band];
  const colors = [
    "oklch(0.72 0.09 30)",
    "oklch(0.78 0.07 55)",
    "oklch(0.82 0.04 100)",
    "oklch(0.78 0.08 140)",
    "oklch(0.7 0.1 150)",
  ];
  return (
    <span style={{
      fontFamily:"var(--font-mono)",fontSize: small ? 9 : 9.5,
      letterSpacing:0.5,textTransform:"uppercase",fontWeight:600,
      padding: small ? "1px 6px" : "2px 7px",
      borderRadius:"var(--radius-pill)",
      background: colors[band],color:"#2a2a25",
    }}>{b.label}</span>
  );
}

function LogForm({ bm, onClose }){
  const [value, setValue] = uS(bm.kind === "category" ? 3 : "");
  return (
    <div>
      <div style={{fontSize:12.5,color:"var(--ink-2)",lineHeight:1.5,marginBottom:14,
                   fontFamily:"var(--font-body)",textWrap:"pretty"}}>
        {bm.description}
      </div>

      {bm.kind === "number" ? (
        <div style={{marginBottom:14}}>
          <div style={{fontFamily:"var(--font-mono)",fontSize:10,letterSpacing:1.2,
                       textTransform:"uppercase",color:"var(--ink-3)",marginBottom:6}}>
            Your score
          </div>
          <div style={{display:"flex",alignItems:"baseline",gap:8}}>
            <input type="number" value={value} onChange={e=>setValue(e.target.value)}
                   placeholder="0"
                   style={{flex:1,background:"var(--bg)",border:"1px solid var(--line)",
                           borderRadius:"var(--radius-md)",
                           padding:"14px 16px",fontFamily:"var(--font-display)",
                           fontSize:30,fontWeight:500,color:"var(--ink)",outline:"none",
                           textAlign:"center"}}/>
            <span style={{fontSize:16,color:"var(--ink-3)",fontFamily:"var(--font-body)"}}>{bm.unit}</span>
          </div>
        </div>
      ) : (
        <div style={{marginBottom:14}}>
          <div style={{fontFamily:"var(--font-mono)",fontSize:10,letterSpacing:1.2,
                       textTransform:"uppercase",color:"var(--ink-3)",marginBottom:8}}>
            Pick your level
          </div>
          <div style={{display:"flex",flexDirection:"column",gap:6}}>
            {bm.bands.map((b,i) => {
              const isSel = (b.value || (i+1)) === value;
              return (
                <button key={i} onClick={() => setValue(b.value || (i+1))} style={{
                  background: isSel ? "var(--accent)" : "var(--bg)",
                  color: isSel ? "var(--accent-ink)" : "var(--ink)",
                  border:"none",padding:"12px 14px",borderRadius:"var(--radius-md)",
                  cursor:"pointer",textAlign:"left",display:"flex",justifyContent:"space-between",
                  alignItems:"center",fontFamily:"var(--font-body)",
                }}>
                  <span style={{fontWeight:600,fontSize:13}}>{b.label}</span>
                  <span style={{fontSize:11,opacity:isSel?0.85:0.65,fontFamily:"var(--font-mono)"}}>
                    {b.hint}
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      )}

      <div style={{display:"flex",gap:8,marginTop:18}}>
        <button onClick={onClose} style={{
          flex:1,background:"var(--bg-2)",color:"var(--ink)",border:"none",
          padding:"14px",borderRadius:"var(--radius-pill)",cursor:"pointer",
          fontFamily:"var(--font-body)",fontSize:13,fontWeight:600,
        }}>Cancel</button>
        <button onClick={onClose} style={{
          flex:2,background:"var(--accent)",color:"var(--accent-ink)",border:"none",
          padding:"14px",borderRadius:"var(--radius-pill)",cursor:"pointer",
          fontFamily:"var(--font-body)",fontSize:13,fontWeight:600,
          display:"flex",alignItems:"center",justifyContent:"center",gap:6,
        }}><Icon name="check" size={15}/> Save entry</button>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────
// 4. ANALYTICS — line charts per benchmark
// ─────────────────────────────────────────────
function AnalyticsScreen({ theme, tweaks }){
  const history = tweaks.populated ? window.MOCK_BENCHMARK_HISTORY : {};
  const [filter, setFilter] = uS("all");
  const cats = ["all", ...window.CATEGORIES];
  return (
    <div style={{padding:"6px 20px 100px"}}>
      <div style={{padding:"14px 0 4px"}}>
        <div style={{fontFamily:"var(--font-mono)",fontSize:10,letterSpacing:1.2,
                     textTransform:"uppercase",color:"var(--ink-3)",marginBottom:6}}>
          Progress
        </div>
        <h2 style={{fontFamily:"var(--font-display)",fontSize:30,fontWeight:500,
                    letterSpacing:-0.6,margin:"0 0 6px",color:"var(--ink)"}}>
          Six months in.
        </h2>
        <div style={{fontSize:13,color:"var(--ink-2)",fontFamily:"var(--font-body)",textWrap:"pretty"}}>
          Each chart is one benchmark. The colored strip marks its category.
        </div>
      </div>

      {/* Category filter */}
      <div style={{display:"flex",gap:6,overflowX:"auto",padding:"14px 0 2px",margin:"0 -20px",
                   paddingLeft:20,paddingRight:20}}>
        {cats.map(c => (
          <button key={c} onClick={() => setFilter(c)} style={{
            background: filter === c ? "var(--accent)" : "var(--bg-2)",
            color: filter === c ? "var(--accent-ink)" : "var(--ink-2)",
            border:"none",padding:"7px 12px",borderRadius:"var(--radius-pill)",
            cursor:"pointer",fontFamily:"var(--font-mono)",fontSize:10,
            letterSpacing:0.5,textTransform:"uppercase",fontWeight:600,flexShrink:0,
          }}>{c === "all" ? "All" : c}</button>
        ))}
      </div>

      <div style={{display:"flex",flexDirection:"column",gap:14,marginTop:14}}>
        {window.BENCHMARKS
          .filter(bm => filter === "all" || bm.category === filter)
          .map(bm => (
          <BenchmarkChartCard key={bm.id} bm={bm} hist={history[bm.id]||[]} theme={theme}/>
        ))}
      </div>
    </div>
  );
}

function BenchmarkChartCard({ bm, hist, theme }){
  const isHairline = theme.flavor.cardStyle === "hairline";
  const last = hist[hist.length-1];
  const first = hist[0];
  const delta = last && first ? (last.value - first.value) : 0;
  const pos = bm.better === "higher" ? delta > 0 : delta < 0;

  return (
    <div style={{
      background:"var(--surface)",
      border: isHairline ? "1px solid var(--line)" : "none",
      borderRadius:"var(--radius-md)",
      overflow:"hidden",position:"relative",
      boxShadow: !isHairline ? "0 1px 0 var(--line-2)" : "none",
    }}>
      {/* category tint strip */}
      <div style={{
        height:3,background:window.CATEGORY_TINTS[bm.category],width:"100%",
      }}/>
      <div style={{padding:"12px 14px 4px"}}>
        <div style={{display:"flex",justifyContent:"space-between",alignItems:"flex-start",gap:8}}>
          <div style={{flex:1,minWidth:0}}>
            <div style={{fontFamily:"var(--font-mono)",fontSize:9.5,letterSpacing:1.2,
                         textTransform:"uppercase",color:"var(--ink-3)"}}>{bm.category}</div>
            <div style={{fontFamily:"var(--font-body)",fontSize:14,fontWeight:600,
                         color:"var(--ink)",letterSpacing:-0.1,marginTop:2}}>{bm.name}</div>
          </div>
          {last && (
            <div style={{textAlign:"right"}}>
              <div style={{fontFamily:"var(--font-display)",fontSize:22,fontWeight:500,
                           color:"var(--ink)",lineHeight:1,letterSpacing:-0.4}}>
                {last.value}
                <span style={{fontSize:11,marginLeft:2,color:"var(--ink-3)",
                              fontFamily:"var(--font-body)"}}>{bm.unit}</span>
              </div>
              {first && delta !== 0 && (
                <div style={{fontFamily:"var(--font-mono)",fontSize:10,marginTop:3,
                             color: pos ? "var(--accent)" : "var(--warn)"}}>
                  {delta > 0 ? "+" : ""}{Math.round(delta*10)/10} {bm.unit} · 6mo
                </div>
              )}
            </div>
          )}
        </div>
        <div style={{marginTop:8,marginLeft:-6,marginRight:-6}}>
          <BigChart series={hist} bm={bm} tint={window.CATEGORY_TINTS[bm.category]}
                    height={130} width={340}/>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────
// 5. SETTINGS
// ─────────────────────────────────────────────
function SettingsScreen({ theme, tweaks, setTweaks }){
  const [sound, setSound] = uS(true);
  const [dataOpen, setDataOpen] = uS(false);

  const totalExercises = window.EXERCISES.length;
  const totalBench = window.BENCHMARKS.length;
  const sessions = window.MOCK_SESSIONS.length;

  return (
    <div style={{padding:"6px 20px 100px"}}>
      <div style={{padding:"14px 0 18px"}}>
        <div style={{fontFamily:"var(--font-mono)",fontSize:10,letterSpacing:1.2,
                     textTransform:"uppercase",color:"var(--ink-3)",marginBottom:6}}>
          Settings
        </div>
        <h2 style={{fontFamily:"var(--font-display)",fontSize:30,fontWeight:500,
                    letterSpacing:-0.6,margin:0,color:"var(--ink)"}}>
          Your app, offline.
        </h2>
      </div>

      <SettingGroup label="Preferences">
        <SettingToggle label="Timer sound effects" desc="Chimes at exercise transitions"
                       icon="sound" value={sound} onChange={setSound}/>
      </SettingGroup>

      <SettingGroup label="Your data" style={{marginTop:18}}>
        <SettingRow icon="download" label="Export data"
                    desc="Save a .json backup of sessions and benchmarks"
                    onClick={() => setDataOpen(true)}/>
        <SettingRow icon="upload" label="Import data"
                    desc="Restore from a previous export"
                    onClick={() => setDataOpen(true)}/>
        <SettingRow icon="trash" label="Delete all data"
                    desc="Clear everything on this device"
                    destructive
                    onClick={() => setDataOpen(true)}/>
      </SettingGroup>

      <SettingGroup label="Library" style={{marginTop:18}}>
        <ReadonlyRow label="Exercises" value={`${totalExercises} across 7 categories`}/>
        <ReadonlyRow label="Benchmarks" value={`${totalBench} tests`}/>
        <ReadonlyRow label="Sessions logged" value={`${sessions} total`}/>
      </SettingGroup>

      <div style={{textAlign:"center",padding:"28px 0 10px",
                   fontFamily:"var(--font-mono)",fontSize:10,letterSpacing:1.2,
                   color:"var(--ink-3)",textTransform:"uppercase"}}>
        Stretch Daily · v3 · local only
      </div>
    </div>
  );
}

function SettingGroup({ label, children, style }){
  return (
    <div style={style}>
      <div style={{fontFamily:"var(--font-mono)",fontSize:10,letterSpacing:1.4,
                   textTransform:"uppercase",color:"var(--ink-3)",marginBottom:8,paddingLeft:4}}>
        {label}
      </div>
      <div style={{background:"var(--surface)",borderRadius:"var(--radius-md)",
                   border:"1px solid var(--line-2)",overflow:"hidden"}}>
        {children}
      </div>
    </div>
  );
}

function SettingToggle({ label, desc, icon, value, onChange }){
  return (
    <div style={{padding:"12px 14px",display:"flex",alignItems:"center",gap:12,
                 borderBottom:"1px solid var(--line-2)"}}>
      <div style={{color:"var(--ink-2)"}}><Icon name={icon} size={18}/></div>
      <div style={{flex:1,minWidth:0}}>
        <div style={{fontFamily:"var(--font-body)",fontSize:13,fontWeight:600,color:"var(--ink)"}}>
          {label}
        </div>
        <div style={{fontSize:11,color:"var(--ink-3)",fontFamily:"var(--font-body)",marginTop:1}}>
          {desc}
        </div>
      </div>
      <button onClick={() => onChange(!value)} style={{
        width:38,height:22,borderRadius:11,border:"none",cursor:"pointer",
        background: value ? "var(--accent)" : "var(--line)",
        position:"relative",padding:0,flexShrink:0,transition:"background .2s",
      }}>
        <div style={{
          position:"absolute",top:2,left: value ? 18 : 2,
          width:18,height:18,borderRadius:9,background:"#fff",
          transition:"left .2s",boxShadow:"0 1px 3px rgba(0,0,0,0.2)",
        }}/>
      </button>
    </div>
  );
}

function SettingRow({ icon, label, desc, onClick, destructive }){
  return (
    <button onClick={onClick} style={{
      width:"100%",padding:"12px 14px",display:"flex",alignItems:"center",gap:12,
      background:"none",border:"none",borderBottom:"1px solid var(--line-2)",
      cursor:"pointer",textAlign:"left",
      color: destructive ? "var(--warn)" : "var(--ink)",
    }}>
      <Icon name={icon} size={18}/>
      <div style={{flex:1,minWidth:0}}>
        <div style={{fontFamily:"var(--font-body)",fontSize:13,fontWeight:600}}>
          {label}
        </div>
        <div style={{fontSize:11,color:"var(--ink-3)",fontFamily:"var(--font-body)",marginTop:1}}>
          {desc}
        </div>
      </div>
      <Icon name="chevronR" size={16}/>
    </button>
  );
}

function ReadonlyRow({ label, value }){
  return (
    <div style={{padding:"10px 14px",display:"flex",justifyContent:"space-between",
                 alignItems:"center",borderBottom:"1px solid var(--line-2)"}}>
      <span style={{fontFamily:"var(--font-body)",fontSize:13,color:"var(--ink-2)"}}>{label}</span>
      <span style={{fontFamily:"var(--font-mono)",fontSize:11,color:"var(--ink-3)"}}>{value}</span>
    </div>
  );
}

Object.assign(window, {
  DashboardScreen, SessionOverviewScreen, BenchmarkLogScreen,
  AnalyticsScreen, SettingsScreen,
});
