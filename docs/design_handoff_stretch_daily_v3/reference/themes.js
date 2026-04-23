// Three visual directions. Each provides:
//  - name / tagline
//  - CSS variables injected as inline style on the phone root
//  - font family stacks (Google Fonts loaded in index.html)
// Tweak-adjustable values are merged in at render time.

window.THEMES = {
  // Direction A — "Grove"
  // Warm paper, forest green accents, editorial serif headings, soft card vocabulary.
  grove: {
    id: "grove",
    name: "Grove",
    tagline: "Warm paper · forest · serif headings",
    vars: {
      "--bg":           "#f3efe5",
      "--bg-2":         "#ebe6d8",
      "--surface":      "#fbf8f0",
      "--surface-2":    "#f6f2e6",
      "--ink":          "#1f2a1d",
      "--ink-2":        "#4a5144",
      "--ink-3":        "#7f8478",
      "--line":         "rgba(31,42,29,0.10)",
      "--line-2":       "rgba(31,42,29,0.06)",
      "--accent":       "#2e4a2a",
      "--accent-2":     "#3d6138",
      "--accent-ink":   "#f3efe5",
      "--accent-soft":  "oklch(0.86 0.04 140)",
      "--warn":         "#9a5a1e",
      "--radius-xs":    "8px",
      "--radius-sm":    "12px",
      "--radius-md":    "18px",
      "--radius-lg":    "24px",
      "--radius-pill":  "999px",
      "--font-display": "'Playfair Display', 'Fraunces', Georgia, serif",
      "--font-body":    "'Manrope', -apple-system, system-ui, sans-serif",
      "--font-mono":    "'JetBrains Mono', ui-monospace, monospace",
    },
    flavor: {
      headerStyle: "serif-hero",     // big serif title with eyebrow
      cardStyle:   "soft",            // soft cards with subtle shadow
      chipStyle:   "pill-tinted",
      navStyle:    "labeled",
    },
  },

  // Direction B — "Sage"
  // Cool sage + cream, extra-quiet sans-first, outlined icons, more air, hairline divider aesthetic.
  sage: {
    id: "sage",
    name: "Sage",
    tagline: "Cool sage · minimal sans · hairlines",
    vars: {
      "--bg":           "#f2eee4",
      "--bg-2":         "#e8e4d9",
      "--surface":      "#f9f6ec",
      "--surface-2":    "#ede9de",
      "--ink":          "#252823",
      "--ink-2":        "#54584d",
      "--ink-3":        "#8a8d82",
      "--line":         "rgba(37,40,35,0.12)",
      "--line-2":       "rgba(37,40,35,0.06)",
      "--accent":       "#5c7a4a",
      "--accent-2":     "#4a6741",
      "--accent-ink":   "#f5f3ea",
      "--accent-soft":  "oklch(0.88 0.045 135)",
      "--warn":         "#a6632a",
      "--radius-xs":    "8px",
      "--radius-sm":    "12px",
      "--radius-md":    "16px",
      "--radius-lg":    "22px",
      "--radius-pill":  "999px",
      "--font-display": "'Manrope', -apple-system, system-ui, sans-serif",
      "--font-body":    "'Manrope', -apple-system, system-ui, sans-serif",
      "--font-mono":    "'JetBrains Mono', ui-monospace, monospace",
    },
    flavor: {
      headerStyle: "quiet-sans",
      cardStyle:   "hairline",
      chipStyle:   "outline",
      navStyle:    "icons-only",
    },
  },

  // Direction C — "Moss"
  // Confident & bold — dark forest dashboard blocks, mint highlights, more color blocking.
  moss: {
    id: "moss",
    name: "Moss",
    tagline: "Bold blocks · dark forest · mint accents",
    vars: {
      "--bg":           "#f5f2e7",
      "--bg-2":         "#1f2a1d",     // bold dark block
      "--surface":      "#ffffff",
      "--surface-2":    "#eae5d3",
      "--ink":          "#1f2a1d",
      "--ink-2":        "#4a5144",
      "--ink-3":        "#7c8172",
      "--line":         "rgba(31,42,29,0.14)",
      "--line-2":       "rgba(31,42,29,0.07)",
      "--accent":       "#1f2a1d",
      "--accent-2":     "#b6d3a4",     // mint
      "--accent-ink":   "#f5f2e7",
      "--accent-soft":  "oklch(0.89 0.055 135)",
      "--warn":         "#b6713a",
      "--radius-xs":    "10px",
      "--radius-sm":    "14px",
      "--radius-md":    "20px",
      "--radius-lg":    "28px",
      "--radius-pill":  "999px",
      "--font-display": "'Playfair Display', 'Fraunces', Georgia, serif",
      "--font-body":    "'Manrope', -apple-system, system-ui, sans-serif",
      "--font-mono":    "'JetBrains Mono', ui-monospace, monospace",
    },
    flavor: {
      headerStyle: "bold-block",
      cardStyle:   "blocky",
      chipStyle:   "solid-mint",
      navStyle:    "labeled",
    },
  },
};

// Default tweakable overrides
window.DEFAULT_TWEAKS = {
  accentShade: "default",   // 'sage' / 'forest' / 'olive' / 'moss' / 'default'
  bgTone:      "default",   // 'warm' / 'cool' / 'paper' / 'default'
  radius:      "default",   // 'sharp' / 'medium' / 'pill' / 'default'
  fontPair:    "default",   // 'serif+sans' / 'sans-only' / 'softserif' / 'default'
  benchmarkBannerOn: true,
  populated:  true,
  sessionState: "overview", // 'overview' | 'running' | 'done'
};

window.applyTweaks = function(baseVars, tweaks){
  const out = {...baseVars};

  if (tweaks.accentShade && tweaks.accentShade !== "default"){
    const map = {
      sage:   {"--accent":"#5c7a4a","--accent-2":"#4a6741"},
      forest: {"--accent":"#2e4a2a","--accent-2":"#3d6138"},
      olive:  {"--accent":"#5f6638","--accent-2":"#737a3f"},
      moss:   {"--accent":"#3b5a3a","--accent-2":"#b6d3a4"},
    };
    Object.assign(out, map[tweaks.accentShade] || {});
  }
  if (tweaks.bgColor){
    out["--bg"] = tweaks.bgColor;
  }
  if (tweaks.bgTone && tweaks.bgTone !== "default"){
    const map = {
      warm:  {"--bg":"#f4efe0","--bg-2":"#ebe5d1","--surface":"#fcf8eb","--surface-2":"#f6f1de"},
      cool:  {"--bg":"#edeee9","--bg-2":"#e3e5df","--surface":"#f6f7f3","--surface-2":"#ebeee7"},
      bright:{"--bg":"#fbfaf5","--bg-2":"#f1efe7","--surface":"#ffffff","--surface-2":"#f6f4ec"},
      stone: {"--bg":"#e9e6dc","--bg-2":"#ddd9cb","--surface":"#f0ede3","--surface-2":"#e3dfd1"},
    };
    Object.assign(out, map[tweaks.bgTone] || {});
  }
  if (tweaks.radius && tweaks.radius !== "default"){
    const map = {
      sharp:  {"--radius-xs":"2px","--radius-sm":"3px","--radius-md":"4px","--radius-lg":"6px"},
      medium: {"--radius-xs":"8px","--radius-sm":"12px","--radius-md":"16px","--radius-lg":"22px"},
      pill:   {"--radius-xs":"14px","--radius-sm":"18px","--radius-md":"24px","--radius-lg":"32px"},
    };
    Object.assign(out, map[tweaks.radius] || {});
  }
  if (tweaks.fontPair && tweaks.fontPair !== "default"){
    const map = {
      "serif+sans": {
        "--font-display":"'Playfair Display', Georgia, serif",
        "--font-body":"'Manrope', system-ui, sans-serif"
      },
      "sans-only": {
        "--font-display":"'Manrope', system-ui, sans-serif",
        "--font-body":"'Manrope', system-ui, sans-serif"
      },
      "softserif": {
        "--font-display":"'Fraunces', 'Playfair Display', Georgia, serif",
        "--font-body":"'Manrope', system-ui, sans-serif"
      },
    };
    Object.assign(out, map[tweaks.fontPair] || {});
  }
  return out;
};
