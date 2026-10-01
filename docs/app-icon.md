# Application icon

`app-icon.png` is the approved shared source for the Android and macOS application
icons. Preserve its filled microphone, symmetric cradle, short stem and rounded
foot. Do not add shadows, gradients, highlights or bevels during export. System
launchers may apply their own presentation or adaptive mask.

The PNG was created with the built-in image generation tool and approved on
2026-10-01. Final generation request: preserve the filled microphone's silhouette,
placement and spacing; use flat white on a flat near-black rounded square with a
transparent exterior; remove all shadows, gradients, bevels, highlights, reflective
rims, outlines, glow, textures and three-dimensional effects.

macOS packaging resizes this source into an iconset and assembles `AppIcon.icns`:

```bash
swift apps/macos/scripts/generate-icon.swift docs/app-icon.png /tmp/OpenDictate.iconset
iconutil -c icns /tmp/OpenDictate.iconset
```

Android resources are checked in so Linux builds do not need AppKit. After changing
the source, regenerate them on macOS:

```bash
swift apps/android/scripts/generate-icons.swift docs/app-icon.png apps/android/app/src/main/res
```

The exporter preserves the complete artwork for square legacy launcher icons and
uses a circular background for their round variant. Adaptive and themed icons use
the same microphone alpha mask on the launcher's background.
The glyph is extracted from the PNG, rather than redrawn with different geometry.
The white/dark threshold removes background pixels while retaining edge antialiasing.
