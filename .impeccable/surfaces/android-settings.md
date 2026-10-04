---
version: 1
slug: "android-settings"
primary_target: "apps/android/app/src/main/java/com/opendictate/app/ui/SettingsScreen.kt"
related_targets: ["apps/android/app/src/main/java/com/opendictate/app/ui/SettingsTheme.kt", "apps/android/app/src/main/java/com/opendictate/app/ui/ReplacementSheet.kt"]
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
- Dictation: Accurate/Live selector, recognition model, languages, trailing punctuation, word replacements, and dictionary. Selected mode has a check and contrast fill; IDs use monospace.
- Keyboard: transformation toggle and conditional editing model, selected-text dictionary toggle, and app exclusions. Transformation controls disable during active dictation.
- Advanced: response timeout and model refresh, with missing-key, loading, retry, and newly available model feedback.
- Test field, privacy note, and version finish the scroll.

Languages, word replacements, and dictionary use native modal bottom sheets. Languages offer automatic detection and full-row checkbox choices. Dictionary edits save automatically; Done closes its keyboard-aware sheet. Model IDs open native dropdown menus; timeout opens the existing dialog. History, API key, permissions, and exclusions retain their existing destinations.

Groups use 16 dp corners and tonal separation. Action rows are at least 76 dp, toggle rows 80 dp, and touch controls at least 48 dp. Full-row toggles expose one switch target. Native typography, growing rows, scrollable content, heading semantics, and status/navigation/IME padding support font scaling and keyboard use. The test field and dictionary editor bring their full bounds into view.

## Settled outcome

Light canvas is `#F3F4F6`; dark canvas is `#101114`. A top-right theme button opens System/Light/Dark choices with the selected option checked. The local preference survives restarts and applies across settings, history, exclusions and dialogs. System is the default and follows Android automatically; system bars follow the effective appearance. No wallpaper-derived colors are present. `SettingsTheme.kt` and `SettingsScreen.kt` are the implementation sources, with native roles recorded under `extensions.androidSettings` in the sidecar. The final reviewer disposition is **ship**, with no material findings. No new raster assets were introduced and no design decisions remain unresolved.

## Local word replacements extension

Mode remains Operate; the existing neutral Material 3 identity and settings hierarchy are preserved. Word replacements opens its own native bottom sheet from Dictation, separate from recognition hints in Dictionary. The keyboard-aware lazy list uses 24 dp horizontal padding and 12 dp vertical spacing. English/Russian source and replacement fields are outlined, full width, and keep their complete bounds in view. A global switch sits above the editor; saved rows pair a checkbox with source/replacement text and accessible edit/delete actions. Editing scrolls to the fields and changes Add to Save with Cancel; Done closes the sheet.

Blank inputs disable Add/Save. Validation guidance covers unique source phrases, field limits, and the rule limit; storage-error copy explains that saved data was kept and editing is unavailable. The empty state provides a source → replacement example. Native theme colors carry supporting and error copy, with wrapping and scrolling for long rules, large text, and keyboard insets. Changes take effect on the next dictation in either mode. Rules are local, are not sent to OpenAI, and leave voice-edit commands unchanged. Synchronization is deferred until Google Drive support exists; this extension adds no sync control.

Finish reviewer disposition: **ship**, with no material findings. Implementation sources are `ReplacementSheet.kt` and its entry in `SettingsScreen.kt`. Native captures `.impeccable/review/android-phone.png`, `.impeccable/review/android-phone-dark-large-text.png`, and `.impeccable/review/android-tablet.png` were reviewed without clipping. Native implementation and captures ground the review; an HTML detector does not apply. No shipping raster assets or new visual tokens were introduced.

## Synchronization provider list

Synchronization now has its own localized section heading and a single Google Drive provider row. The existing rounded group contains only the color vector logo, provider name and trailing native switch. The whole 80 dp minimum row is one accessible switch target. Enabling starts Google authorization; cancellation keeps the switch off and available. Disabling disconnects immediately, including during background synchronization. Descriptions, persistent status copy and manual action buttons are removed; errors appear as transient toasts, and existing automatic synchronization remains active.

Native emulator captures `sync-phone.png`, `sync-phone-dark-large-text.png` (1.3 font scale), and `sync-tablet-dark.png` in `.impeccable/review/` show the row without clipping. Google authorization launch and cancellation were exercised without signing in. Review disposition: **ship**. No raster assets or new theme tokens were added.
