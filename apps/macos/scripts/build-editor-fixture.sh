#!/bin/bash
set -euo pipefail
macos_root="$(cd "$(dirname "$0")/.." && pwd)"
app="$macos_root/.build/editor-fixture/EditorFixture.app"
mkdir -p "$app/Contents/MacOS"
swiftc -parse-as-library "$macos_root/Tests/Fixtures/EditorFixture.swift" -o "$app/Contents/MacOS/EditorFixture"
cat > "$app/Contents/Info.plist" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<plist version="1.0"><dict>
<key>CFBundleName</key><string>EditorFixture</string>
<key>CFBundleIdentifier</key><string>com.opendictate.editor-fixture</string>
<key>CFBundleExecutable</key><string>EditorFixture</string>
<key>CFBundlePackageType</key><string>APPL</string>
</dict></plist>
PLIST
codesign --force --sign - "$app"
echo "$app"
