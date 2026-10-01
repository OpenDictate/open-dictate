# macOS application

Mode: Operate. Native SwiftUI settings and AppKit menu bar utility for macOS 14+,
with no third-party UI dependency. Inherit OpenDictate's black, graphite, white
microphone identity through system typography, SF Symbols, native controls,
keyboard navigation and English/Russian copy. The user's instruction delegates
technology and implementation choices: beautiful, clean, light and fast.

## Direction contract

THESIS: Dictate into the app already in use through a global keyboard shortcut.

OWN-WORLD: Dark graphite settings canvas and sidebar, white microphone, SF Symbols
and native controls; no marketing imagery or decorative motion. System accent
color is retained for selected controls and keyboard focus. The captured system
accent is blue; this is a native control state, not an added brand color.

STORY: The first Dictation page presents the API key, microphone and Accessibility
permissions, Live/Accurate mode, speech language, and start/finish, voice-edit,
cancel shortcuts. Dictionary, History, and Settings are secondary pages. The
rail keeps the current shortcut and version visible.

FIRST VIEWPORT: Default settings content is 790 × 650pt, with a fixed 190pt
sidebar and a scrollable body. The NSWindow minimum is 740 × 620pt including
window chrome; the SwiftUI content minimum is 740 × 590pt. The compact review
uses 740 × 590pt content. Rail padding is 20pt and body padding is 28pt. At the
default size, setup, mode, language and shortcut rows form one continuous page;
at the minimum, the longer Russian supporting copy scrolls vertically. Headings
use 27pt semibold system type; navigation uses 13pt regular/selected-medium type.
The selected row's tonal fill and the native keyboard focus ring are distinct.

A menu bar microphone reports recording state, becoming a waveform while active
and gaining an ellipsis while processing. The recording indicator is a
nonactivating AppKit panel that cannot become key or main. During preparation and recording,
its red finish control invokes `model.stop()` without taking keyboard focus;
only processing ignores mouse events. Escape cancels only during a
session. Localized Edit commands use the AppKit responder chain for normal
text-field and editor shortcuts.

RECORDING INDICATOR: The pinned compact treatment shows only a microphone and
red finish control from preparation through recording. Its dark capsule is
44 × 22pt; the SF `mic.fill` symbol is 9pt medium in a 10pt frame, with 6pt
spacing before the 14pt red circle and explicit 6pt horizontal content insets.
The centered white stop square is 4.5pt with a 1pt corner, inside a 16pt circular
hit area. Processing shows only a centered native circular `ProgressView` at
mini control size on a 22 × 22pt capsule; idle is
hidden. No visible text, timer, waveform, or shortcut hint belongs to this
indicator. `windowBackgroundColor` and the dark scheme supply its background;
the native panel shadow remains. Position it at the current screen's visible
frame center, 28pt above the bottom edge. Localized finish label/help and
processing accessibility label carry the action and busy state.

FORM: Direct native adaptation of the established Android settings surface;
code-led within the established visual world, with no approved visual comp and
no composition tournament. Web fonts, pixel dimensions and hover lift are not
native implementation tokens.

FINISH: Ship verdict after the material review findings were resolved. DESIGN.md
and its sidecar record the built native adaptation. Native surfaces have no
detector pass; the review uses implementation inspection and actual screenshots.
The rc.1 indicator refinement passed a finish review; rc.4 halves both dimensions
and makes its finish control available during preparation. Production-view
raster tests and offscreen native captures verify the new dimensions and spacing.
Retain the red circle, white stop square, dark capsule, localized accessibility,
and nonactivation. This was a code-led refinement in the existing world, with no
comp or direction roll.

## Implementation evidence

- `apps/macos/Sources/OpenDictate/SettingsView.swift`: four pages, system type,
  semantic colors, native control styles, scrolling, and passive status content.
- `apps/macos/Sources/OpenDictate/OpenDictateApp.swift`: window dimensions,
  menu commands, recording panel ownership and nonactivation.
- `apps/macos/Sources/OpenDictate/RecordingIndicator.swift`: indicator states,
  dimensions, capsule background, finish control, accessibility and panel focus
  behavior.
- `.impeccable/review/macos-dictation-en.png` and
  `.impeccable/review/macos-dictation-ru.png`: default Dictation page in both
  languages.
- `.impeccable/review/macos-dictation-ru-min.png`: Russian Dictation at the compact
  content size, with vertical scrolling.
- `.impeccable/review/macos-settings-en.png`,
  `.impeccable/review/macos-dictionary-en.png`, and
  `.impeccable/review/macos-history-en.png`: secondary pages, including a saved
  dictionary term and empty-history state.
- `.impeccable/review/macos-indicator-recording.png` and
  `.impeccable/review/macos-indicator-processing.png`: the reviewed compact
  recording and processing indicators.

The committed indicator captures document the earlier rc.1 geometry. The rc.4
size and spacing are checked by production-view render tests and offscreen
NSHostingView captures, with no change to panel nonactivation. Panel focus,
mouse behavior and localized accessibility are grounded in the SwiftUI/AppKit
implementation.

## Asset provenance

`apps/macos/scripts/generate-icon.swift` draws the near-black rounded square and
white microphone as a native code adaptation of `docs/mark.svg` and the existing
OpenDictate identity. Packaging produces the icon raster sizes and `.icns` in
build output. No generated shipping raster assets are committed to the repository;
review screenshots are evidence rather than shipping artwork.

## Theme choice

A native menu button at the top right offers System, Light and Dark appearance.
The choice is saved in local preferences and applies to all four settings pages.
Dark preserves the prior default; System follows macOS. Semantic AppKit surfaces
and foreground colors keep the navigation selection readable in either theme.
The nonactivating recording indicator retains its dark appearance.

## Waveform indicator choice

Settings saves a choice between Microphone (the existing compact indicator,
retained by default) and Waveform. The waveform follows the user's image:
50 × 22pt black capsule, ten centered white bars, and a subtle 0.5pt white border
at 14% opacity. Bars are 1.5pt wide, separated by 1.5pt; measured input levels
map from 2pt silence marks to 10pt peaks, with square-root scaling. New input
appears at the right, and older values move left. Ten normalized levels stay
in memory and are cleared at the start/end of each session.

The whole waveform capsule is the finish action, including during preparation;
processing replaces its bars with a mini spinner at the same capsule size.
The panel never activates or takes editor focus. The existing visibility switch
hides either style. The native picker and guidance are localized in English and
Russian. Linear 40ms height interpolation follows audio updates and is disabled
by Reduce Motion. Production-view raster tests, offscreen native captures,
actual Settings/preview interaction and a local microphone check ground this
variant. No captured audio or level history is persisted.

## RC7 refinement

Both indicator styles and processing spinners scale uniformly by 1.25, including icons, bars, borders, spacing and hit targets. Nominal compact size is 55 × 27.5pt and waveform size is 62.5 × 27.5pt. Native panel bounds round fractional points as needed. Finish uses an accessibility-labelled native NSView that refuses first responder and dispatches the action after mouse-up. The panel remains nonactivating. Escape in the shortcut recorder clears and persists the binding; disabled shortcuts are skipped by global registration.
