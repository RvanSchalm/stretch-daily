// StretchApp — phone-shaped React app. Render with theme + tweaks props.

const { useState: aUS, useEffect: aUE, useMemo: aUM } = React;

function StretchApp({ theme, tweaks }) {
  const [tab, setTab] = aUS(tweaks.__tab || "dashboard");
  const [mode, setMode] = aUS(tweaks.sessionState || "overview");
  const [sessionList, setSessionList] = aUS(window.TODAY_SESSION);
  const [benchCarousel, setBenchCarousel] = aUS(false);

  // Sync mode + tab when tweaks change (so Tweaks panel controls each artboard)
  aUE(() => { setMode(tweaks.sessionState || "overview"); }, [tweaks.sessionState]);
  aUE(() => { if (tweaks.__tab) setTab(tweaks.__tab); }, [tweaks.__tab]);

  // When user taps the Session tab show overview;
  // "Begin session" flips mode to 'running'.
  const startSession = () => setMode("running");
  const completeSession = () => setMode("done");
  const exitSession = () => { setMode("overview"); setTab("dashboard"); };

  const swapAt = (idx, newEx) => {
    const next = [...sessionList];
    next[idx] = newEx;
    setSessionList(next);
  };

  const goToTab = t => {
    setTab(t);
    if (t !== "session") setMode("overview");
  };

  const vars = window.applyTweaks(theme.vars, tweaks);
  const styleVars = {};
  for (const [k, v] of Object.entries(vars)) styleVars[k] = v;

  let screen = null;
  if (benchCarousel) {
    screen = <BenchmarkCarousel onClose={() => setBenchCarousel(false)}/>;
  } else if (tab === "session" && mode === "running") {
    screen = <SessionPlayer sessionList={sessionList}
                            onExit={exitSession}
                            onComplete={completeSession}/>;
  } else if (tab === "session" && mode === "done") {
    const totalSec = sessionList.reduce((a,e) => a + (e.unilateral ? e.total*2 : e.total), 0);
    screen = <SessionDoneScreen onExit={exitSession}
                                stats={{count: sessionList.length,
                                        minutes: Math.round(totalSec/60),
                                        streak: window.MOCK_STREAK}}/>;
  }

  return (
    <div style={{
      ...styleVars,
      width:"100%",height:"100%",background:"var(--bg)",color:"var(--ink)",
      fontFamily:"var(--font-body)",position:"relative",overflow:"hidden",
      display:"flex",flexDirection:"column",
    }}>
      <StatusBar/>
      <div style={{flex:1,overflowY:"auto",overflowX:"hidden",position:"relative"}}>
        {tab === "dashboard" && (
          <DashboardScreen theme={theme} tweaks={tweaks}
                           goTo={goToTab}
                           openBenchmarkCarousel={() => setBenchCarousel(true)}/>
        )}
        {tab === "session" && mode === "overview" && (
          <SessionOverviewScreen theme={theme} tweaks={tweaks}
                                 startSession={startSession}
                                 sessionList={sessionList}
                                 onSwap={swapAt}/>
        )}
        {tab === "log" && (
          <BenchmarkLogScreen theme={theme} tweaks={tweaks}
                              openBenchmarkCarousel={() => setBenchCarousel(true)}/>
        )}
        {tab === "chart" && <AnalyticsScreen theme={theme} tweaks={tweaks}/>}
        {tab === "settings" && <SettingsScreen theme={theme} tweaks={tweaks}/>}
      </div>
      <BottomNav active={tab} setActive={goToTab} flavor={theme.flavor}/>
      {screen}
    </div>
  );
}

window.StretchApp = StretchApp;
