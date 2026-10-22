---
version: 1
slug: "app-src-main-java-com-autopaymax-ui-theme-theme-kt"
primary_target: "app/src/main/java/com/autopaymax/ui/theme/Theme.kt"
related_targets: ["app/src/main/java/com/autopaymax/ui/dashboard/DashboardScreen.kt","app/src/main/java/com/autopaymax/ui/autopay/AutoPayScreen.kt","app/src/main/java/com/autopaymax/ui/passbook/PassbookScreen.kt","app/src/main/java/com/autopaymax/ui/components/CommonComponents.kt"]
---

## Direction contract

THESIS: AutoPayMax is an instrument you trust at a glance to tell you what's about to charge you — refuses the generic fintech-dashboard default (gradient hero card, colored text-pill badges, Inter-adjacent default type) in favor of a calm, authoritative cockpit reading.

OWN-WORLD: Deep near-black instrument-panel ground in dark theme, pale instrument-white ground in light theme, both restrained. Luminous off-white numerals for money. One accent (the existing orange family, `#FF7600` tonal family, carried through Material3 color roles rather than raw hex) reserved for the primary instrument glow and interactive accents. Status expressed only through three reserved lights — steady green (active/on-track), amber (pending/needs attention), red (overdue) — used nowhere else. Huge instrument-grade numerals for money (Material `displayLarge`/`headlineLarge` roles) against small terse placard labels (`labelSmall`/`labelMedium`), no in-between sizes. Cards read as instrument bezels: flat surfaces, hairline borders, tonal elevation, no drop shadows, no gradients except the one hero glow. Material3 components throughout (filled/tonal buttons, chips, bottom nav, snackbars, bottom sheets) — the metaphor lives in color, type, and motion, never in invented controls or navigation.

STORY: The user opens the app and reads their financial exposure the way a pilot reads a six-pack: one glance, no interpretation needed. They scan mandate rows like a cross-check sweep — everything nominal (green) except what needs a decision (amber/red) — and act on exactly those.

FIRST VIEWPORT: Dashboard. Top: existing AutoPayTopHeader. Hero instrument: dark bezel card, "TOTAL MONTHLY" placard label, huge luminous total, a single steady/amber/red status summary chip below it ("6 active" / "1 needs attention"), no decorative gradient circles. Below: mandate list, each row a compact instrument line — status light + merchant name + amount right-aligned in tabular numerals, mandate source (Gmail/Play/Manual/Inferred) as a small secondary placard tag, never competing visually with the status light.

FORM: Flight Instrument Panel (challenger world "aircraft night instrument six-pack", source id `signals-instruments-night-flight-six-pack`). Won the direction roll fused against assigned candidate "ATM/POS receipt terminal" on both audience identification and product clarity. Raises donated from declined challengers: reveal-without-effort motion (from the Miura-fold deployable sheet — data appears fully resolved on load, no skeleton-loader drama, matching the product's "automatic detection, not manual entry" positioning) and scale-contrast-as-hierarchy (from the variable font specimen — huge numerals vs. tiny labels carry hierarchy alone). Seed key: 71c5bc75.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance.
