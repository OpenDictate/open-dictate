# Application icon

Both applications use the original Android outlined microphone from
`apps/android/app/src/main/res/drawable/ic_launcher_foreground.xml`. Android uses
that vector directly for both adaptive and themed launcher icons, with the
existing `#080808` background. Its original paths and group transform are unchanged.

macOS packaging reads the same vector paths, viewport and group transform and
renders them directly into all ten iconset sizes on a black rounded-square tile.
It adds no shadows, gradients, highlights or bevels. System launchers may apply
their own presentation or adaptive mask. There is no generated bitmap source.

To export the macOS icon on macOS:

```bash
swift apps/macos/scripts/generate-icon.swift apps/android/app/src/main/res/drawable/ic_launcher_foreground.xml /tmp/OpenDictate.iconset
iconutil -c icns /tmp/OpenDictate.iconset
```

The path reader supports the absolute `M`, `L`, `C`, `H`, `V` and `Z` commands
used by this asset. Unsupported commands fail export rather than silently
changing the shape. The original microphone uses OpenAI Apps SDK UI's MicLgDictate
icon under the MIT license; notices are included in each application's resources.

`docs/mark.svg` contains the same original paths for the website and repository mark.
