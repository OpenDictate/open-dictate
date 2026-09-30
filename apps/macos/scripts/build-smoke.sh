#!/bin/bash
set -euo pipefail
macos_root="$(cd "$(dirname "$0")/.." && pwd)"
repo_root="$(cd "$macos_root/../.." && pwd)"
swift build --package-path "$macos_root" --build-system native --product OpenDictate
smoke_app="$macos_root/.build/smoke/OpenDictate.app"
mkdir -p "$(dirname "$smoke_app")"
if [[ ! -d "$repo_root/dist/macos/OpenDictate.app" ]]; then
    echo "Run scripts/package.sh first to create bundle resources." >&2; exit 1
fi
rm -rf "$smoke_app"
ditto "$repo_root/dist/macos/OpenDictate.app" "$smoke_app"
cp "$macos_root/.build/arm64-apple-macosx/debug/OpenDictate" "$smoke_app/Contents/MacOS/OpenDictate"
/usr/libexec/PlistBuddy -c 'Add :OpenDictateLocalSmokeTest bool true' "$smoke_app/Contents/Info.plist"
codesign --force --sign - --identifier com.opendictate.mac \
    --requirements '=designated => identifier "com.opendictate.mac"' \
    --entitlements "$macos_root/Resources/OpenDictate.entitlements" "$smoke_app"
echo "Local-only checks: open '$smoke_app'"
