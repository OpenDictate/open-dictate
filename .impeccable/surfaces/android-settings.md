---
version: 1
slug: "android-settings"
primary_target: "apps/android/app/src/main/java/com/opendictate/app/ui/SettingsScreen.kt"
related_targets: ["apps/android/app/src/main/java/com/opendictate/app/ui/SettingsTheme.kt"]
---

SCOPE: Android settings redesign. Mode: Operate. The existing dictation overlay, launcher mark, and website identity are outside this surface.

AUDIENCE: Android users configuring direct, system-wide dictation, checking readiness, and testing insertion before returning to their current app. Settings must work under the user's Android light/dark preference, keyboard, and font scale.

## Direction contract

THESIS: Make readiness and everyday dictation choices easy to find; replace the scattered setup form with readable task groups.

OWN-WORLD: Static neutral Material 3 light/dark themes following Android, system sans-serif roles, 16 dp rounded groups, quiet icons and dividers; monospace only for model IDs.

STORY: Check readiness, finish missing permissions, choose recognition behavior, adjust keyboard tools, then test dictation.

FIRST VIEWPORT: Compact brand/history/theme/language header, large Settings heading and supporting line, full-width readiness disclosure, then Dictation and its two-option mode control. Single column, 680 dp maximum, 20 dp gutters; disclosure opens setup actions in place.

FORM: User-constrained modern, minimalist, functional settings. No concept seed or candidate ranking was used.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

## Shipped hierarchy and interactions

- Readiness disclosure: API key, microphone, and Accessibility actions; summary reflects missing steps or connecting/listening/processing state. It starts collapsed and can reopen for setup changes.
- Dictation: Accurate/Live selector, recognition model, languages, trailing punctuation, and dictionary. Selected mode has a check and contrast fill; IDs use monospace.
- Keyboard: transformation toggle and conditional editing model, selected-text dictionary toggle, and app exclusions. Transformation controls disable during active dictation.
- Advanced: response timeout and model refresh, with missing-key, loading, retry, and newly available model feedback.
- Test field, privacy note, and version finish the scroll.

Languages and dictionary use native modal bottom sheets. Languages offer automatic detection and full-row checkbox choices. Dictionary edits save automatically; Done closes its keyboard-aware sheet. Model IDs open native dropdown menus; timeout opens the existing dialog. History, API key, permissions, and exclusions retain their existing destinations.

Groups use 16 dp corners and tonal separation. Action rows are at least 76 dp, toggle rows 80 dp, and touch controls at least 48 dp. Full-row toggles expose one switch target. Native typography, growing rows, scrollable content, heading semantics, and status/navigation/IME padding support font scaling and keyboard use. The test field and dictionary editor bring their full bounds into view.

## Settled outcome

Light canvas is `#F3F4F6`; dark canvas is `#101114`. A top-right theme button opens System/Light/Dark choices with the selected option checked. The local preference survives restarts and applies across settings, history, exclusions and dialogs. System is the default and follows Android automatically; system bars follow the effective appearance. No wallpaper-derived colors are present. `SettingsTheme.kt` and `SettingsScreen.kt` are the implementation sources, with native roles recorded under `extensions.androidSettings` in the sidecar. The final reviewer disposition is **ship**, with no material findings. No new raster assets were introduced and no design decisions remain unresolved.
