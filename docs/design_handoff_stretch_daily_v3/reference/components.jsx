// Shared components — exercise placeholder, sparkline, bottom nav, chips, modal.
// All reach window.* (themes, data) — load order matters.

// components.jsx — top-level React hook destructure removed to avoid name collisions
// across Babel <script> files. Import hooks inline inside each function.

// ─────────────────────────────────────────────
// ExerciseTile — "honest" placeholder: diagonal stripe rectangle with
// category tint. Labeled with category only. Scales to parent size.
// ─────────────────────────────────────────────
function ExerciseTile({ exercise, size = "sm" }) {
  const tint = window.CATEGORY_TINTS[exercise.category] || "#ccc";
  const sizes = {
    xs: { w: 44,  h: 44,  fz: 9  },
    sm: { w: 56,  h: 56,  fz: 10 },
    md: { w: 80,  h: 80,  fz: 11 },
    lg: { w: 260, h: 220, fz: 14 },
    xl: { w: 320, h: 260, fz: 15 },
  };
  const s = sizes[size] || sizes.sm;
  const stripeId = `stripes-${exercise.id}-${size}`;
  return (
    <div style={{
      position:"relative", width:s.w, height:s.h, borderRadius:"var(--radius-sm)",
      background: tint, overflow:"hidden", flexShrink:0,
    }}>
      <svg viewBox={`0 0 ${s.w} ${s.h}`} width={s.w} height={s.h}
           style={{display:"block",position:"absolute",inset:0}}>
        <defs>
          <pattern id={stripeId} width="8" height="8" patternUnits="userSpaceOnUse"
                   patternTransform="rotate(45)">
            <rect width="8" height="8" fill="transparent"/>
            <rect width="3" height="8" fill="rgba(255,255,255,0.35)"/>
          </pattern>
        </defs>
        <rect width="100%" height="100%" fill={`url(#${stripeId})`}/>
      </svg>
      <div style={{
        position:"absolute", inset:0, padding: size==="lg"||size==="xl" ? 16 : 6,
        display:"flex", flexDirection:"column", justifyContent:"flex-end",
        fontFamily:"var(--font-mono)", fontSize:s.fz, color:"rgba(31,42,29,0.6)",
        lineHeight:1.2,
      }}>
        {(size==="lg"||size==="xl") && (
          <div style={{display:"flex",justifyContent:"space-between",alignItems:"flex-end"}}>
            <span style={{opacity:0.9,fontSize:s.fz+1}}>{exercise.category}</span>
            <span style={{opacity:0.7}}>.webp placeholder</span>
          </div>
        )}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────
// Sparkline — tiny SVG line. accepts points [{x,y}] already normalized
// values only (numbers). Optional fill under line.
// ─────────────────────────────────────────────
function Sparkline({ values, w=60, h=20, stroke="var(--accent)", fill=null, strokeWidth=1.5 }) {
  if (!values || values.length < 2) return <div style={{width:w,height:h}}/>;
  const min = Math.min(...values), max = Math.max(...values);
  const range = max - min || 1;
  const pts = values.map((v,i) => {
    const x = (i / (values.length - 1)) * (w - 2) + 1;
    const y = h - 1 - ((v - min) / range) * (h - 2);
    return [x, y];
  });
  const d = pts.map((p,i) => (i===0?"M":"L") + p[0].toFixed(1) + "," + p[1].toFixed(1)).join(" ");
  const dFill = d + ` L ${(w-1).toFixed(1)},${(h-1).toFixed(1)} L 1,${(h-1).toFixed(1)} Z`;
  return (
    <svg viewBox={`0 0 ${w} ${h}`} width={w} height={h} style={{display:"block"}}>
      {fill && <path d={dFill} fill={fill} opacity={0.3}/>}
      <path d={d} fill="none" stroke={stroke} strokeWidth={strokeWidth}
            strokeLinejoin="round" strokeLinecap="round"/>
    </svg>
  );
}

// ─────────────────────────────────────────────
// BigChart — large line chart with axes for analytics screen.
// values: [{date, value}]. bands: benchmark bands for tint bg.
// ─────────────────────────────────────────────
function BigChart({ series, bm, tint, height=140, width=320 }) {
  if (!series || series.length < 2){
    return <div style={{height, display:"flex",alignItems:"center",justifyContent:"center",
      color:"var(--ink-3)",fontSize:12,fontFamily:"var(--font-body)"}}>Not enough data yet</div>;
  }
  const values = series.map(s => s.value);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const pad = (max - min) * 0.25 || 1;
  const yMin = min - pad, yMax = max + pad;
  const W = width, H = height;
  const padL = 28, padR = 12, padT = 8, padB = 20;
  const plotW = W - padL - padR;
  const plotH = H - padT - padB;
  const x = i => padL + (i / (series.length - 1)) * plotW;
  const y = v => padT + plotH - ((v - yMin) / (yMax - yMin)) * plotH;

  const d = series.map((s,i) => (i===0?"M":"L") + x(i).toFixed(1) + "," + y(s.value).toFixed(1)).join(" ");
  const dFill = d + ` L ${x(series.length-1).toFixed(1)},${padT+plotH} L ${padL},${padT+plotH} Z`;

  // Y axis labels
  const yTicks = [yMax, (yMax+yMin)/2, yMin];

  // X axis: show first & last month labels
  const fmt = (dstr) => {
    const d = new Date(dstr);
    return d.toLocaleString('en', {month:"short"});
  };

  // Last point marker
  const lastX = x(series.length - 1);
  const lastY = y(series[series.length-1].value);

  return (
    <svg viewBox={`0 0 ${W} ${H}`} width="100%" height={H} style={{display:"block"}}>
      {/* tint background */}
      <rect x={padL} y={padT} width={plotW} height={plotH} fill={tint} opacity={0.25} rx={4}/>
      {/* gridlines */}
      {yTicks.map((v,i) => (
        <line key={i} x1={padL} x2={W-padR} y1={y(v)} y2={y(v)}
              stroke="var(--line-2)" strokeDasharray={i===1?"":"2 3"} strokeWidth={0.6}/>
      ))}
      {/* y labels */}
      {yTicks.map((v,i) => (
        <text key={i} x={padL-4} y={y(v)+3} textAnchor="end" fontSize={8.5}
              fill="var(--ink-3)" fontFamily="var(--font-mono)">{Math.round(v*10)/10}</text>
      ))}
      {/* fill */}
      <path d={dFill} fill="var(--accent)" opacity={0.08}/>
      {/* line */}
      <path d={d} fill="none" stroke="var(--accent)" strokeWidth={1.5}
            strokeLinejoin="round" strokeLinecap="round"/>
      {/* points */}
      {series.map((s,i) => (
        <circle key={i} cx={x(i)} cy={y(s.value)} r={1.8} fill="var(--accent)"/>
      ))}
      {/* last point highlight */}
      <circle cx={lastX} cy={lastY} r={3.5} fill="var(--surface)" stroke="var(--accent)" strokeWidth={1.6}/>
      {/* x labels */}
      <text x={padL} y={H-5} fontSize={8.5} fill="var(--ink-3)" fontFamily="var(--font-mono)">
        {fmt(series[0].date)}
      </text>
      <text x={W-padR} y={H-5} textAnchor="end" fontSize={8.5} fill="var(--ink-3)" fontFamily="var(--font-mono)">
        {fmt(series[series.length-1].date)}
      </text>
    </svg>
  );
}

// ─────────────────────────────────────────────
// Activity heatmap — 7-day strip
// ─────────────────────────────────────────────
function WeekStrip({ sessions, today }) {
  const days = [];
  const t = new Date(today);
  for (let i = 6; i >= 0; i--){
    const d = new Date(t); d.setDate(t.getDate() - i);
    const key = d.toISOString().slice(0,10);
    const s = sessions.find(x => x.date === key);
    days.push({
      d, key, label: d.toLocaleDateString('en',{weekday:'narrow'}),
      done: !!s,
      isToday: key === today,
    });
  }
  return (
    <div style={{display:"flex",gap:6,width:"100%"}}>
      {days.map((d,i) => (
        <div key={i} style={{flex:1,display:"flex",flexDirection:"column",alignItems:"center",gap:6}}>
          <div style={{
            fontSize:10,fontFamily:"var(--font-mono)",color:"var(--ink-3)",
            letterSpacing:0.5,textTransform:"uppercase",
          }}>{d.label}</div>
          <div style={{
            width:"100%", aspectRatio:"1",
            borderRadius:"var(--radius-xs)",
            background: d.done ? "var(--accent)" : "var(--line-2)",
            border: d.isToday ? "1.5px dashed var(--accent)" : "none",
            opacity: d.done ? 1 : 0.9,
            display:"flex",alignItems:"center",justifyContent:"center",
            color: d.done ? "var(--accent-ink)" : "var(--ink-3)",
            fontSize:10,fontFamily:"var(--font-mono)",
          }}>
            {d.isToday ? "·" : (d.done ? "✓" : "")}
          </div>
        </div>
      ))}
    </div>
  );
}

// ─────────────────────────────────────────────
// Icons (outlined, 20px)
// ─────────────────────────────────────────────
const Icon = ({ name, size=20, stroke=1.6 }) => {
  const s = size;
  const sw = { strokeWidth: stroke, stroke:"currentColor", fill:"none", strokeLinecap:"round", strokeLinejoin:"round" };
  const paths = {
    home:      <><path d="M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1v-9z"/></>,
    session:   <><circle cx="12" cy="12" r="9"/><path d="M10 9l5 3-5 3z" fill="currentColor" stroke="none"/></>,
    log:       <><path d="M5 4h11l3 3v13a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z"/><path d="M8 12h8M8 16h6M8 8h5"/></>,
    chart:     <><path d="M3 20V6M3 20h18M7 15l3-4 3 3 5-7"/></>,
    settings:  <><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1A1.7 1.7 0 0 0 4.6 9a1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3h0a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8v0a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"/></>,
    swap:      <><path d="M7 4l-3 3 3 3M4 7h11a4 4 0 0 1 4 4v1M17 20l3-3-3-3M20 17H9a4 4 0 0 1-4-4v-1"/></>,
    play:      <><path d="M8 5l12 7-12 7z" fill="currentColor" stroke="none"/></>,
    pause:     <><rect x="6" y="5" width="4" height="14" fill="currentColor" stroke="none"/><rect x="14" y="5" width="4" height="14" fill="currentColor" stroke="none"/></>,
    chevronR:  <><path d="M9 6l6 6-6 6"/></>,
    chevronL:  <><path d="M15 6l-9 6 9 6"/></>,
    chevronD:  <><path d="M6 9l6 6 6-6"/></>,
    close:     <><path d="M6 6l12 12M18 6L6 18"/></>,
    plus:      <><path d="M12 5v14M5 12h14"/></>,
    check:     <><path d="M4 12l5 5 11-11"/></>,
    flame:     <><path d="M12 22s7-3.5 7-10c0-4-3-6-3-6s0 3-2 5c-1.5-2-3-3-3-3s-1 2-2 4-3 3-3 6c0 2.5 1.5 4 6 4z"/></>,
    calendar:  <><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18M8 3v4M16 3v4"/></>,
    clock:     <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    download:  <><path d="M12 4v11M7 11l5 5 5-5M5 20h14"/></>,
    upload:    <><path d="M12 20V9M7 13l5-5 5 5M5 4h14"/></>,
    trash:     <><path d="M4 7h16M10 11v6M14 11v6M6 7l1 13a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1l1-13M9 7V4h6v3"/></>,
    sound:     <><path d="M3 10v4h4l5 4V6L7 10H3zM16 8a5 5 0 0 1 0 8M19 5a9 9 0 0 1 0 14"/></>,
    info:      <><circle cx="12" cy="12" r="9"/><path d="M12 16v-5M12 8h.01"/></>,
    sparkle:   <><path d="M12 3v4M12 17v4M3 12h4M17 12h4M6.3 6.3l2.8 2.8M14.9 14.9l2.8 2.8M6.3 17.7l2.8-2.8M14.9 9.1l2.8-2.8"/></>,
    skip:      <><path d="M5 4l10 8-10 8zM19 5v14"/></>,
  };
  return (
    <svg viewBox="0 0 24 24" width={s} height={s} {...sw}>{paths[name]}</svg>
  );
};

// ─────────────────────────────────────────────
// Bottom nav
// ─────────────────────────────────────────────
function BottomNav({ active, setActive, flavor }) {
  const items = [
    { id:"dashboard", label:"Today",      icon:"home" },
    { id:"session",   label:"Session",    icon:"session" },
    { id:"log",       label:"Log",        icon:"log" },
    { id:"chart",     label:"Progress",   icon:"chart" },
    { id:"settings",  label:"Settings",   icon:"settings" },
  ];
  const showLabels = flavor?.navStyle !== "icons-only";
  return (
    <nav style={{
      position:"absolute",bottom:0,left:0,right:0,
      background:"var(--surface)",borderTop:"1px solid var(--line)",
      padding:"8px 4px calc(8px + env(safe-area-inset-bottom, 0px))",
      display:"flex",justifyContent:"space-around",alignItems:"stretch",
      zIndex:10,
    }}>
      {items.map(it => {
        const on = active === it.id;
        return (
          <button key={it.id} onClick={() => setActive(it.id)} style={{
            flex:1,background:"none",border:"none",cursor:"pointer",
            display:"flex",flexDirection:"column",alignItems:"center",gap:3,
            padding:"6px 4px",borderRadius:"var(--radius-sm)",
            color: on ? "var(--accent)" : "var(--ink-3)",
            transition:"color .15s",
            fontFamily:"var(--font-body)",
          }}>
            <Icon name={it.icon} size={20} stroke={on ? 2 : 1.6}/>
            {showLabels && (
              <span style={{fontSize:9.5, letterSpacing:0.3, fontWeight: on ? 600 : 500}}>
                {it.label}
              </span>
            )}
          </button>
        );
      })}
    </nav>
  );
}

// ─────────────────────────────────────────────
// Modal / Sheet — bottom sheet
// ─────────────────────────────────────────────
function Sheet({ open, onClose, children, title }) {
  if (!open) return null;
  return (
    <div onClick={onClose} style={{
      position:"absolute",inset:0,background:"rgba(20,25,18,0.4)",
      zIndex:100,display:"flex",alignItems:"flex-end",
      animation:"fadeIn .2s ease",
    }}>
      <div onClick={e => e.stopPropagation()} style={{
        width:"100%",background:"var(--surface)",
        borderTopLeftRadius:"var(--radius-lg)",borderTopRightRadius:"var(--radius-lg)",
        maxHeight:"88%",overflowY:"auto",
        animation:"slideUp .25s ease",
        boxShadow:"0 -8px 28px rgba(0,0,0,0.18)",
      }}>
        <div style={{padding:"10px",display:"flex",justifyContent:"center"}}>
          <div style={{width:38,height:4,borderRadius:2,background:"var(--line)"}}/>
        </div>
        {title && (
          <div style={{
            padding:"0 20px 12px",
            fontFamily:"var(--font-display)",fontSize:22,fontWeight:500,
            color:"var(--ink)",letterSpacing:-0.3,
          }}>{title}</div>
        )}
        <div style={{padding:"0 20px 28px"}}>{children}</div>
      </div>
    </div>
  );
}

// Status bar mockup (battery, time, etc)
function StatusBar() {
  return (
    <div style={{
      height:28,display:"flex",alignItems:"center",justifyContent:"space-between",
      padding:"0 18px",fontFamily:"var(--font-body)",fontSize:12,color:"var(--ink)",
      fontWeight:600,flexShrink:0,
    }}>
      <span>9:41</span>
      <div style={{display:"flex",gap:5,alignItems:"center",opacity:0.9}}>
        <svg width="14" height="10" viewBox="0 0 14 10" fill="currentColor">
          <rect x="0" y="7" width="2" height="3" rx="0.5"/>
          <rect x="4" y="5" width="2" height="5" rx="0.5"/>
          <rect x="8" y="2.5" width="2" height="7.5" rx="0.5"/>
          <rect x="12" y="0" width="2" height="10" rx="0.5"/>
        </svg>
        <svg width="12" height="10" viewBox="0 0 12 10" fill="none" stroke="currentColor" strokeWidth="1.2">
          <path d="M1 4 Q6 0 11 4 M3 6 Q6 4 9 6 M5 8 Q6 7 7 8"/>
        </svg>
        <svg width="20" height="10" viewBox="0 0 20 10" fill="none" stroke="currentColor" strokeWidth="1">
          <rect x="0.5" y="1" width="16" height="8" rx="1.5"/>
          <rect x="2" y="2.5" width="11" height="5" rx="0.5" fill="currentColor"/>
          <rect x="17" y="3.5" width="1.5" height="3" rx="0.5" fill="currentColor"/>
        </svg>
      </div>
    </div>
  );
}

Object.assign(window, {
  ExerciseTile, Sparkline, BigChart, WeekStrip, Icon, BottomNav, Sheet, StatusBar,
});
