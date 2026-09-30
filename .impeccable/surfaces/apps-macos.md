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
and gaining an ellipsis while processing. The recording HUD is a nonactivating
AppKit panel without input targets; it ignores mouse events and never takes
keyboard focus. Escape cancels only during a session. Localized Edit commands
use the AppKit responder chain for normal text-field and editor shortcuts.

FORM: Direct native adaptation of the established Android settings surface;
code-led within the established visual world, with no approved visual comp and
no composition tournament. Web fonts, pixel dimensions and hover lift are not
native implementation tokens.

FINISH: Ship verdict after the material review findings were resolved. DESIGN.md
and its sidecar record the built native adaptation. Native surfaces have no
detector pass; the review uses implementation inspection and actual screenshots.

## Implementation evidence

- `apps/macos/Sources/OpenDictate/SettingsView.swift`: four pages, system type,
  semantic colors, native control styles, scrolling, and passive status content.
- `apps/macos/Sources/OpenDictate/OpenDictateApp.swift`: window dimensions,
  menu commands, recording panel ownership and nonactivation.
- `.impeccable/review/macos-dictation-en.png` and
  `.impeccable/review/macos-dictation-ru.png`: default Dictation page in both
  languages.
- `.impeccable/review/macos-dictation-ru-min.png`: Russian Dictation at the compact
  content size, with vertical scrolling.
- `.impeccable/review/macos-settings-en.png`,
  `.impeccable/review/macos-dictionary-en.png`, and
  `.impeccable/review/macos-history-en.png`: secondary pages, including a saved
  dictionary term and empty-history state.

These captures show settings content, not the recording HUD; the HUD's focus and
mouse behavior is recorded from the AppKit panel configuration.

## Asset provenance

`apps/macos/scripts/generate-icon.swift` draws the near-black rounded square and
white microphone as a native code adaptation of `docs/mark.svg` and the existing
OpenDictate identity. Packaging produces the icon raster sizes and `.icns` in
build output. No generated shipping raster assets are committed to the repository;
review screenshots are evidence rather than shipping artwork.
