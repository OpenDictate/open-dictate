---
name: OpenDictate
description: A dark, instrument-like visual system that makes speech becoming text feel immediate and trustworthy.
colors:
  ink: "#0b1020"
  ink-deep: "#070b16"
  panel: "#11182a"
  panel-raised: "#172137"
  white: "#f7f9fc"
  fog: "#aab5c8"
  line: "#2c3953"
  mint: "#7de4c4"
  mint-deep: "#45cba3"
  mint-hover: "#9aefd5"
  coral: "#ff8d7f"
typography:
  display:
    fontFamily: "Unbounded, Arial, sans-serif"
    fontSize: "clamp(48px, 6.35vw, 92px)"
    fontWeight: 500
    lineHeight: 0.98
    letterSpacing: "-0.04em"
  headline:
    fontFamily: "Unbounded, Arial, sans-serif"
    fontSize: "clamp(42px, 5vw, 72px)"
    fontWeight: 500
    lineHeight: 1.08
    letterSpacing: "-0.04em"
  title:
    fontFamily: "Unbounded, Arial, sans-serif"
    fontSize: "22px"
    fontWeight: 500
    lineHeight: 1.25
    letterSpacing: "-0.03em"
  body:
    fontFamily: "Golos Text, Segoe UI, sans-serif"
    fontSize: "17px"
    fontWeight: 400
    lineHeight: 1.55
  label:
    fontFamily: "SFMono-Regular, Cascadia Code, Roboto Mono, monospace"
    fontSize: "11px"
    fontWeight: 400
    lineHeight: 1.55
    letterSpacing: "0.06em"
  button:
    fontFamily: "Golos Text, Segoe UI, sans-serif"
    fontSize: "17px"
    fontWeight: 600
    lineHeight: 1.55
rounded:
  wave: "3px"
  small: "8px"
  inset-control: "9px"
  control: "12px"
  control-group: "13px"
  panel: "16px"
  full: "999px"
spacing:
  control-inset: "4px"
  compact: "8px"
  small: "12px"
  control: "20px"
  panel: "28px"
  block: "40px"
  section-mobile: "92px"
  section-desktop: "130px"
components:
  button-primary:
    backgroundColor: "{colors.mint}"
    textColor: "{colors.ink-deep}"
    typography: "{typography.button}"
    rounded: "{rounded.control}"
    padding: "0 20px"
    height: "56px"
  button-primary-hover:
    backgroundColor: "{colors.mint-hover}"
    textColor: "{colors.ink-deep}"
    rounded: "{rounded.control}"
  button-secondary:
    backgroundColor: "transparent"
    textColor: "{colors.white}"
    typography: "{typography.button}"
    rounded: "{rounded.control}"
    padding: "0 20px"
    height: "56px"
  mic:
    backgroundColor: "{colors.mint}"
    textColor: "{colors.ink}"
    rounded: "{rounded.full}"
    size: "68px"
  mic-listening:
    backgroundColor: "{colors.coral}"
    textColor: "{colors.ink}"
    rounded: "{rounded.full}"
    size: "68px"
  mode-live:
    backgroundColor: "{colors.mint}"
    textColor: "{colors.ink-deep}"
    typography: "{typography.label}"
    rounded: "{rounded.inset-control}"
    padding: "0 12px"
    height: "46px"
  mode-accurate:
    backgroundColor: "{colors.coral}"
    textColor: "{colors.ink-deep}"
    typography: "{typography.label}"
    rounded: "{rounded.inset-control}"
    padding: "0 12px"
    height: "46px"
  dictation-field:
    backgroundColor: "{colors.panel}"
    textColor: "{colors.white}"
    rounded: "{rounded.panel}"
    padding: "25px 92px 30px 28px"
---

# Design System: OpenDictate

## Overview

**Creative North Star: "The Live Dictation Instrument"**

OpenDictate presents speech as a precise, observable input process rather than an abstract AI effect. Deep navy surfaces, hairline structure, compact instrument labels, a live caret, and the five-bar waveform make the interface feel focused, technical, and calm. Mint carries the active voice signal; coral appears only when a mode or state needs meaningful contrast.

This file is the repository-wide authority for web and public brand surfaces. The Android app shares the navy–mint–coral character, but its current Compose palette intentionally differs at several values and its controls, typography scale, insets, motion, and 48 dp touch targets remain Material 3-native. Do not mechanically transplant web pixels or fonts into Android.

**Key Characteristics:**

- Deep navy work surfaces with no light-theme inversion.
- Mint waveform geometry and caret behavior as the signature motif.
- Restrained coral reserved for Accurate/listening contrast and focus.
- Unbounded display statements, Golos body copy, and monospaced utility labels.
- Hairline ledgers and routes instead of floating marketing cards.
- Direct, literal diagrams and interactions instead of decorative product mockups.

## Colors

The palette is a dark technical field with luminous, deliberately scarce signals.

### Primary

- **Signal Mint:** The main action, live state, caret, waveform, active status, and positive emphasis.
- **Deep Signal Mint:** A darker companion available for denser brand applications; do not substitute it for the brighter interactive signal by default.
- **Air Mint:** Hover feedback for filled mint controls.

### Secondary

- **Mode Coral:** Accurate-mode contrast, replay/listening state, focus outlines, and cautionary diagrams. It is a state distinction, not a second general-purpose brand fill.

### Neutral

- **Working Ink:** The default page canvas.
- **Deep Ink:** The deepest chrome and the text/icon color on luminous fills.
- **Instrument Panel:** Bounded fields and subtle contained surfaces.
- **Raised Panel:** The stronger tonal layer for selected or emphasized dark surfaces.
- **Signal White:** Primary text.
- **Interface Fog:** Secondary copy and inactive controls.
- **Hairline Slate:** Dividers, routes, borders, and structural drawing.

**The Two-Signal Rule.** Mint means live, primary, or device-side activity; coral means a deliberate alternate mode, active replay, or focus. Never decorate with both indiscriminately.

**The Dark-Only Rule.** Web and brand surfaces are authored as a true dark scheme, not as colors awaiting automatic inversion.

## Typography

**Display Font:** Unbounded (Arial fallback)

**Body Font:** Golos Text (Segoe UI fallback)
**Label/Mono Font:** SFMono-Regular (Cascadia Code and Roboto Mono fallbacks)

**Character:** Wide, engineered display type creates unmistakable statements; the body face stays warm and readable; mono labels make modes, metadata, models, and routes feel like operating cues. Local Latin and Cyrillic font files are part of the system, so English and Russian retain the same hierarchy.

### Hierarchy

- **Display:** Hero statements only; balanced wrapping, very tight leading, and restrained weight.
- **Headline:** Major section propositions and the closing call to action.
- **Title:** Compact component or rail headings.
- **Body:** Explanations and supporting prose; keep readable line lengths and use fog for secondary copy.
- **Label:** Uppercase metadata, navigation, mode IDs, and technical annotations.

**The Three-Voice Rule.** Unbounded states the proposition, Golos explains it, and mono labels the instrument. Do not swap their jobs.

## Layout

The desktop page is centered at a maximum width of 1280px with 24px outer gutters. The hero is an asymmetric two-column work surface: fluid narrative and live field on the left, a 340px action rail on the right. Subsequent content uses ledgers, split diagrams, and full-width hairline boundaries rather than grids of detached cards. Desktop sections generally breathe with 130px vertical padding.

At 1040px and below, the container caps at 900px with 18px gutters, primary navigation disappears in favor of a compact APK control, the hero and privacy split stack, four setup columns become two, and ledger metadata reflows. At 700px and below, gutters become 16px, sections use 92px vertical padding, ledgers and setup steps become one column, the data route rotates from horizontal to vertical, and closing actions become full-width stacked controls. The center guide disappears on small screens.

**The Working-Surface Rule.** Organize information as one continuous instrument panel separated by rules; do not replace the ledger with a generic feature-card mosaic.

## Elevation & Depth

The web system uses no box shadows. Depth comes from dark tonal layers, 1px hairlines, contained fields, and occasional overlap such as the microphone crossing the dictation-field edge. Hover elevation is a small vertical translation, never a shadow bloom.

**The Flat-by-Default Rule.** A surface earns separation through tone and structure. Never add ambient card shadows or glass effects.

## Shapes

The form language mixes gently rounded rectangular controls with a perfect circular microphone and status dots. Fields use the largest recurring corner, primary controls use a medium corner, and segmented-control selections sit inside a slightly larger rounded frame. Hairlines stay square and precise; wave bars use tiny rounded ends.

**The Shape-Hierarchy Rule.** Curves belong to touchable controls and live signal geometry. Content organization remains rectilinear and ruled.

## Components

### Buttons

- **Primary:** A full-width or content-width mint action, 56px tall, with dark text and a 12px corner.
- **Hover / Active / Focus:** Hover shifts to Air Mint and lifts 3px; active settles 1px and may scale to 0.97 where tactile; focus is a 3px coral outline offset by 4px.
- **Secondary:** Transparent with a hairline border; hover raises it 3px and fills with the panel tone.

### Cards / Containers

- **Dictation Field:** An Instrument Panel surface with a 1px Hairline Slate border, 16px corners, metadata at the top, and a large cumulative transcript below.
- **Information Structures:** Proofs, modes, and setup steps are ledger rows divided by hairlines, not free-floating cards.
- **Shadow Strategy:** None; use tonal contrast and border geometry.

### Navigation

Desktop navigation uses uppercase mono links in Interface Fog, turning mint on hover. At the tablet breakpoint the text navigation disappears and a compact bordered APK action appears; the brand and language switch remain. All keyboard focus uses the shared coral outline.

### Mode Switch

The two-option control has a Hairline Slate frame, a 13px outer corner, 4px inset, and 46px options. Live selects mint; Accurate selects coral. `aria-pressed` is the state source, and Left/Right arrows move between options.

### Dictation Signal

The circular microphone contains the five-bar mark. It is mint at rest and coral while replaying; its bars animate with staggered 760ms alternate pulses, while the caret blinks every 900ms. The demo builds a cumulative phrase word by word and runs once after load. Under reduced motion, it resolves immediately and all CSS motion collapses to 1ms.

## Do's and Don'ts

### Do:

- **Do** make the waveform, caret, route lines, and state labels explain how speech reaches text.
- **Do** use hairline structure and tonal layers to organize dense information.
- **Do** preserve the bilingual Latin/Cyrillic font setup and responsive English/Russian copy behavior.
- **Do** keep every web control keyboard-visible and honor `prefers-reduced-motion`.
- **Do** theme Android through Material 3 roles and components while retaining its existing Compose palette and 48 dp minimum targets.

### Don't:

- **Don't** introduce generic feature-card grids, detached phone mockups, stock AI imagery, gradients, glassmorphism, or ambient shadows.
- **Don't** use coral as routine decoration or let it compete with mint for the primary action.
- **Don't** replace Unbounded headlines with default sans type or use Unbounded for paragraphs.
- **Don't** flatten desktop composition onto mobile; preserve the explicit 1040px and 700px reflows.
- **Don't** copy web pixel values, web fonts, or hover-only affordances directly into native Android UI.
