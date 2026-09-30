---
name: OpenDictate
description: The dark public website system, with a scoped native Android settings override.
colors:
  ink: "#080808"
  ink-deep: "#030303"
  panel: "#171717"
  panel-raised: "#242424"
  white: "#ffffff"
  fog: "#c6c6c6"
  line: "#363636"
  signal: "#ffffff"
  signal-deep: "#e8e8e8"
  signal-hover: "#f0f0f0"
  secondary: "#e3e3e3"
typography:
  webDisplay:
    fontFamily: "Literata, Georgia, serif"
    fontSize: "clamp(52px, 5.2vw, 76px)"
    fontWeight: 500
    lineHeight: 1.12
    letterSpacing: "-0.025em"
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
  macos-heading:
    fontFamily: "system-ui, -apple-system, sans-serif"
    fontSize: "27pt"
    fontWeight: 600
  macos-navigation:
    fontFamily: "system-ui, -apple-system, sans-serif"
    fontSize: "13pt"
    fontWeight: 400
  macos-brand:
    fontFamily: "system-ui, -apple-system, sans-serif"
    fontSize: "15pt"
    fontWeight: 600
rounded:
  wave: "3px"
  small: "8px"
  inset-control: "9px"
  control: "12px"
  control-group: "13px"
  panel: "16px"
  full: "999px"
  macos-navigation: "7pt"
  macos-editor: "8pt"
spacing:
  control-inset: "4px"
  compact: "8px"
  small: "12px"
  control: "20px"
  panel: "28px"
  block: "40px"
  section-mobile: "92px"
  section-desktop: "130px"
  macos-rail: "20pt"
  macos-body: "28pt"
components:
  button-primary:
    backgroundColor: "{colors.signal}"
    textColor: "{colors.ink-deep}"
    typography: "{typography.button}"
    rounded: "{rounded.control}"
    padding: "0 20px"
    height: "56px"
  button-primary-hover:
    backgroundColor: "{colors.signal-hover}"
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
    backgroundColor: "{colors.signal}"
    textColor: "{colors.ink}"
    rounded: "{rounded.full}"
    size: "68px"
  mic-listening:
    backgroundColor: "{colors.secondary}"
    textColor: "{colors.ink}"
    rounded: "{rounded.full}"
    size: "68px"
  mode-live:
    backgroundColor: "{colors.signal}"
    textColor: "{colors.ink-deep}"
    typography: "{typography.label}"
    rounded: "{rounded.inset-control}"
    padding: "0 12px"
    height: "46px"
  mode-accurate:
    backgroundColor: "{colors.secondary}"
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

The OpenDictate website presents speech as a precise, observable input process rather than an abstract AI effect. Black surfaces, graphite layers, compact instrument labels, a live caret, and the waveform demo make the interface feel focused and calm. A white microphone is the app mark and the primary action. Silver distinguishes alternate states.

The frontmatter and the following instrument rules describe the public website. Android settings follow the scoped override under Components and the surface brief at `.impeccable/surfaces/android-settings.md`. Settings use native system typography and follow Android’s light/dark appearance. The dictation overlay and launcher mark retain their existing identity. Do not mechanically transplant web pixels or fonts into Android.

macOS adapts the same white microphone and graphite identity through SwiftUI/AppKit system colors, system typography, SF Symbols, and native controls. Its dark settings surface is compact and direct; selected controls and keyboard focus retain the macOS accent. Native measurements use points, and the web's display fonts, control dimensions, and hover motion do not apply to this surface.

**Key Characteristics:**

- Black website work surfaces with no light-theme inversion.
- White microphone mark, white primary actions, and a white live caret.
- Restrained silver reserved for alternate modes and state contrast.
- The GitHub Pages site pairs Literata headings with Golos body copy; Android settings use system sans-serif roles.
- Hairline ledgers and routes instead of floating marketing cards.
- Direct, literal diagrams and interactions instead of decorative product mockups.
- System typography and native controls on macOS, with English/Russian copy and visible keyboard focus.

## Colors

The palette is a dark technical field with luminous, deliberately scarce signals.

### Primary

- **Signal White:** The main action, live state, caret, waveform, active status, and positive emphasis.
- **Soft White:** A quieter companion for dense surfaces.
- **Hover White:** Subtle press feedback for filled controls.

### Secondary

- **Mode Silver:** Accurate-mode contrast, replay/listening state, and focus outlines. It is a state distinction, not a second general-purpose brand fill.

### Neutral

- **Working Ink:** The default page canvas.
- **Deep Ink:** The deepest chrome and the text/icon color on luminous fills.
- **Instrument Panel:** Bounded fields and subtle contained surfaces.
- **Raised Panel:** The stronger tonal layer for selected or emphasized dark surfaces.
- **Signal White:** Primary text.
- **Interface Fog:** Secondary copy and inactive controls.
- **Hairline Slate:** Dividers, routes, borders, and structural drawing.

### Native macOS

The native surface fixes `preferredColorScheme(.dark)` and uses semantic system roles rather than hard-coded copies of the web palette. `windowBackgroundColor` supplies the graphite body and recording indicator; `controlBackgroundColor` supplies the sidebar and message footer; `textBackgroundColor` supplies the dictionary editor. Primary and secondary text use SwiftUI's corresponding foreground styles. Sidebar selection is a white overlay at 9% opacity.

The recording indicator uses `Color.red` for its finish control and `Color.white` for the stop glyph. These roles are scoped to the recording indicator.

System accent color remains on selected segmented controls, enabled switches, picker focus, and keyboard focus rings. The review screenshots show the standard blue accent; its value belongs to macOS rather than a new OpenDictate brand color.

**The Contrast Rule.** On the website, white marks the primary action and live activity; silver marks a deliberate alternate state. Use shape and labels as well as shade to distinguish state.

**The Dark-Only Rule.** The public website is authored as a true dark scheme, not as colors awaiting automatic inversion. This rule does not apply to Android settings.

**The Native Controls Rule.** On macOS, keep semantic system colors and native accent/focus states. Do not force white brand fills onto platform controls.

## Typography

**Website display token:** Unbounded (Arial fallback); the shipped GitHub Pages surface uses the Literata override below.

**GitHub Pages display font:** Literata (Georgia fallback). The site uses it for the hero, section headings, and wordmark. Its calmer letterforms give the download page a more considered tone while keeping the black instrument palette and direct copy.

**Body Font:** Golos Text (Segoe UI fallback)
**Label/Mono Font:** SFMono-Regular (Cascadia Code and Roboto Mono fallbacks)

**Character:** Wide, engineered display type creates unmistakable statements; the body face stays warm and readable; mono labels make modes, metadata, models, and routes feel like operating cues. Local Latin and Cyrillic font files are part of the system, so English and Russian retain the same hierarchy.

### Hierarchy

- **Display:** Hero statements only; balanced wrapping, very tight leading, and restrained weight.
- **Headline:** Major section propositions and the closing call to action.
- **Title:** Compact component or rail headings.
- **Body:** Explanations and supporting prose; keep readable line lengths and use fog for secondary copy.
- **Label:** Uppercase metadata, navigation, mode IDs, and technical annotations.

**The Three-Voice Rule.** Display type states the proposition, Golos explains it, and mono labels the instrument. On GitHub Pages, Literata fills the display role in both Latin and Cyrillic. Android settings do not use this three-font system.

### Native macOS

SwiftUI system type carries all interface copy. The native hierarchy uses the scoped heading, navigation, and brand tokens above; selected navigation changes to medium weight. Body, callout, and caption text retain their native semantic styles. Shortcut labels use the system monospaced design. English and Russian share the same hierarchy and wrap supporting copy vertically.

**The Native Type Rule.** Keep macOS in system typography. The website display/body pairing is specific to that surface.

## Layout

The desktop page is centered at a maximum width of 1280px with 24px outer gutters. The hero is an asymmetric two-column work surface: fluid narrative and live field on the left, a 340px action rail on the right. Subsequent content uses ledgers, split diagrams, and full-width hairline boundaries rather than grids of detached cards. Desktop sections generally breathe with 130px vertical padding.

At 1040px and below, the container caps at 900px with 18px gutters, primary navigation disappears in favor of a compact APK control, the hero and privacy split stack, four setup columns become two, and ledger metadata reflows. At 700px and below, gutters become 16px, sections use 92px vertical padding, ledgers and setup steps become one column, the data route rotates from horizontal to vertical, and closing actions become full-width stacked controls. The center guide disappears on small screens.

**The Working-Surface Rule.** Organize information as one continuous instrument panel separated by rules; do not replace the ledger with a generic feature-card mosaic.

### Native macOS

The settings window uses a fixed sidebar (190pt) and a scrollable content column separated by a native divider. The scoped rail/body padding tokens set the inset rhythm; recurring content groups use native vertical stacks and dividers rather than detached cards. Resizing keeps the rail fixed and lets the content scroll, including the longer Russian copy. Window dimensions and the four-page information architecture are recorded in `.impeccable/surfaces/apps-macos.md`.

## Elevation & Depth

The web system uses no box shadows. Depth comes from dark tonal layers, 1px hairlines, contained fields, and occasional overlap such as the microphone crossing the dictation-field edge. Hover elevation is a small vertical translation, never a shadow bloom.

**The Flat-by-Default Rule.** A surface earns separation through tone and structure. Never add ambient card shadows or glass effects.

macOS settings content follows the same flat structure through system graphite layers and dividers. Native window chrome retains the operating system's elevation. The recording indicator is an AppKit panel with its native shadow enabled; this is floating status feedback, not card elevation within the settings content.

## Shapes

The form language mixes gently rounded rectangular controls with a perfect circular microphone and status dots. Fields use the largest recurring corner, primary controls use a medium corner, and segmented-control selections sit inside a slightly larger rounded frame. Hairlines stay square and precise; wave bars use tiny rounded ends.

**The Shape-Hierarchy Rule.** Curves belong to touchable controls and live signal geometry. Content organization remains rectilinear and ruled.

macOS keeps native rounded fields, switches, buttons, and segmented controls. Only the sidebar selection and dictionary editor use the scoped custom corner tokens above. The recording indicator uses a `Capsule` background and a circular finish control. SF Symbols provide the navigation and status geometry. The packaged app icon is a code-drawn white microphone on a near-black rounded square, adapted from the existing mark by `apps/macos/scripts/generate-icon.swift`; its raster sizes and `.icns` are build output.

## Components

### Buttons

- **Primary:** A full-width or content-width signal action, 56px tall, with dark text and a 12px corner.
- **Hover / Active / Focus:** Hover shifts to a slightly softer white and lifts 3px; active settles 1px and may scale to 0.97 where tactile; focus is a 3px silver outline offset by 4px.
- **Secondary:** Transparent with a hairline border; hover raises it 3px and fills with the panel tone.

macOS uses native SwiftUI buttons at the system's default or small control size. Save and setup actions remain plainly labeled; disabled states, destructive actions, and confirmation alerts use platform behavior. The web button dimensions and lift do not apply.

### Native Inputs / Fields

macOS uses rounded-border secure and search fields, native pop-up pickers, switches, and a segmented Live/Accurate control. The dictionary uses a native text editor with the scoped editor corner. Fields retain native selection, keyboard focus, and text editing. Localized Edit menu commands route Undo, Redo, Cut, Copy, Paste, and Select All through the AppKit responder chain.

### Cards / Containers

- **Dictation Field:** An Instrument Panel surface with a 1px Hairline Slate border, 16px corners, metadata at the top, and a large cumulative transcript below.
- **Information Structures:** Proofs, modes, and setup steps are ledger rows divided by hairlines, not free-floating cards.
- **Shadow Strategy:** None; use tonal contrast and border geometry.

### Navigation

Desktop navigation uses uppercase mono links in Interface Fog, turning signal on hover. At the tablet breakpoint the text navigation disappears and a compact bordered APK action appears; the brand and language switch remain. All keyboard focus uses the shared secondary outline.

macOS navigation uses system type and SF Symbols in a persistent rail. The selected row gains a tonal background and medium type weight; keyboard focus remains a distinct native ring and may appear on a different row from selection. Navigation labels keep sentence case rather than the web's uppercase mono treatment.

### Mode Switch

The two-option control has a Hairline Slate frame, a 13px outer corner, 4px inset, and 46px options. Live selects signal; Accurate selects secondary. `aria-pressed` is the state source, and Left/Right arrows move between options.

### Dictation Signal

The public demo uses a five-bar signal inside its microphone control. It is white at rest and silver while replaying; its bars animate with staggered 760ms alternate pulses, while the caret blinks every 900ms. The Android launcher retains the white microphone mark on black; the settings header uses the active theme foreground. Under reduced motion, the web demo resolves immediately and all CSS motion collapses to 1ms.

### Android Settings Override

Android settings are a modern, minimalist Operate surface. `SettingsTheme.kt` defines static neutral Material 3 light and dark schemes selected by `isSystemInDarkTheme()`; there is no manual theme selector or dynamic wallpaper palette. These settings rules override the website tokens above.

| Role | Light | Dark |
| --- | --- | --- |
| Canvas | `#F3F4F6` | `#101114` |
| Group surface | `#FFFFFF` | `#1B1D22` |
| Inset controls | `#EEF0F3` | `#282B32` |
| Primary text | `#202329` | `#F2F3F5` |
| Supporting text | `#626975` | `#A9B0BC` |
| Primary action | `#22252B` | `#F2F3F5` |
| Divider | `#E4E7EC` | `#30343C` |

Use native Material 3 sans-serif typography for headings, labels, and body copy; monospace is reserved for model IDs. A centered single column caps at 680 dp with 20 dp content gutters. Groups have 16 dp corners, 16 dp row padding, and inset 0.5 dp dividers. Separation comes from tone and spacing; surface tint is transparent. Rows grow with text, action rows have a 76 dp minimum, toggle rows an 80 dp minimum, and controls retain at least 48 dp targets. Respect font scaling, status/navigation bars, and keyboard insets rather than importing website breakpoints.

The signature component is a compact readiness disclosure above the settings groups: it exposes API key, microphone, and Accessibility setup on demand and reports active dictation state. Dictation groups mode, recognition model, languages, punctuation, and dictionary. Keyboard controls group transformation, selected-text dictionary actions, and app exclusions. Advanced contains timeout and model refresh; a test field and privacy note finish the page. Languages and dictionary open native modal sheets; model choices use native menus and timeout uses a dialog. Toggle the whole row with one switch semantic target; preserve selected-state checks, disabled/loading/error feedback, and keyboard-safe editing. See the surface brief for interaction details and the sidecar’s `extensions.androidSettings` for native theme roles.

### Native Recording Indicator

From preparation through recording, the macOS indicator is a dark capsule (44 × 22pt) containing only an SF `mic.fill` symbol (9pt) and a red circular finish control (14pt). A centered white stop square (4.5pt) sits inside its circular hit area (16pt); the microphone and control are separated by 6pt, with explicit 6pt horizontal content insets. Processing shows only a centered native circular `ProgressView` at mini control size on a dark capsule (22 × 22pt). Idle is hidden. There is no visible text, timer, waveform, or shortcut hint in the indicator.

The borderless, nonactivating AppKit panel retains its native shadow and sits centered on the current screen's visible frame, 28pt above its bottom edge. It cannot become key or main. The preparing/recording finish control invokes `model.stop()` without taking keyboard focus; only processing ignores mouse events. The finish control has localized accessibility label and help, while the spinner has a localized processing label. The menu bar retains a microphone at rest, waveform during an active session, and an ellipsis while processing.

The committed recording/processing captures at `.impeccable/review/macos-indicator-recording.png` and `.impeccable/review/macos-indicator-processing.png` document rc.1. The rc.4 refinement halves both dimensions and shows the finish control during preparation; production-view raster tests and offscreen native captures verify its geometry. The built SwiftUI/AppKit indicator is the visual authority.

## Do's and Don'ts

### Do:

- **Do** make the waveform, caret, route lines, and state labels explain how speech reaches text.
- **Do** use hairline structure and tonal layers to organize dense information.
- **Do** preserve the bilingual Latin/Cyrillic font setup and responsive English/Russian copy behavior.
- **Do** keep every web control keyboard-visible and honor `prefers-reduced-motion`.
- **Do** follow the Android settings override for system light/dark themes, native typography, and 48 dp minimum targets.
- **Do** retain macOS system type, semantic colors, native controls, and localized responder-chain text commands.
- **Do** keep the macOS recording indicator compact and nonactivating; its finish control must preserve keyboard focus in the user's app.

### Don't:

- **Don't** introduce generic feature-card grids, detached phone mockups, stock AI imagery, gradients, glassmorphism, or ambient shadows.
- **Don't** use secondary as routine decoration or let it compete with signal for the primary action.
- **Don't** use display faces for website paragraphs or replace the chosen website display face with a default system sans.
- **Don't** flatten desktop composition onto mobile; preserve the explicit 1040px and 700px reflows.
- **Don't** copy web pixel values, web fonts, or hover-only affordances directly into native Android UI.
- **Don't** transplant web fonts, control sizes, or decorative motion into the native macOS surface.
- **Don't** commit generated macOS icon rasters or `.icns`; keep the source generator as the brand asset's provenance.
