#!/bin/bash
set -euo pipefail

macos_root="$(cd "$(dirname "$0")/.." && pwd)"
repo_root="$(cd "$macos_root/../.." && pwd)"
version="${APP_VERSION:-$(cat "$macos_root/VERSION")}"
output="$repo_root/dist/macos"
staging="$macos_root/.build/dmg-staging"
app="$output/OpenDictate.app"
signing_identity="${MACOS_SIGNING_IDENTITY:--}"

if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "APP_VERSION must be a stable three-part version." >&2
    exit 1
fi
mkdir -p "$output"
for architecture in arm64 x86_64; do
    swift build --package-path "$macos_root" --build-system native --configuration release \
        --triple "${architecture}-apple-macosx14.0" --scratch-path "$macos_root/.build/$architecture" --product OpenDictate
done
rm -rf "$app" "$staging"
mkdir -p "$app/Contents/MacOS" "$app/Contents/Resources" "$staging"
lipo -create \
    "$macos_root/.build/arm64/arm64-apple-macosx/release/OpenDictate" \
    "$macos_root/.build/x86_64/x86_64-apple-macosx/release/OpenDictate" \
    -output "$app/Contents/MacOS/OpenDictate"
cp "$macos_root/Resources/Info.plist" "$app/Contents/Info.plist"
/usr/libexec/PlistBuddy -c "Set :CFBundleShortVersionString $version" "$app/Contents/Info.plist"
/usr/libexec/PlistBuddy -c "Set :CFBundleVersion ${APP_BUILD:-1}" "$app/Contents/Info.plist"
cp -R "$macos_root/Resources/en.lproj" "$macos_root/Resources/ru.lproj" "$app/Contents/Resources/"
cp "$repo_root/LICENSE" "$app/Contents/Resources/LICENSE.txt"
swift "$macos_root/scripts/generate-icon.swift" "$macos_root/.build/AppIcon.iconset"
iconutil -c icns "$macos_root/.build/AppIcon.iconset" -o "$app/Contents/Resources/AppIcon.icns"

if [[ "$signing_identity" == "-" ]]; then
    # Stable identifier requirement keeps local Accessibility grants across ad-hoc updates.
    codesign --force --sign - --identifier com.opendictate.mac \
        --requirements '=designated => identifier "com.opendictate.mac"' \
        --entitlements "$macos_root/Resources/OpenDictate.entitlements" "$app"
else
    codesign --force --sign "$signing_identity" --options runtime --timestamp \
        --entitlements "$macos_root/Resources/OpenDictate.entitlements" "$app"
fi
codesign --verify --deep --strict "$app"
architectures="$(lipo -archs "$app/Contents/MacOS/OpenDictate")"
[[ "$architectures" == "x86_64 arm64" || "$architectures" == "arm64 x86_64" ]]
ditto "$app" "$staging/OpenDictate.app"
ln -s /Applications "$staging/Applications"
cp "$macos_root/INSTALL.txt" "$staging/Install OpenDictate.txt"
dmg="$output/OpenDictate-macOS-$version-universal.dmg"
rm -f "$dmg"
hdiutil create -volname "OpenDictate $version" -srcfolder "$staging" -format UDZO "$dmg"
hdiutil verify "$dmg"

if [[ -n "${MACOS_NOTARY_PROFILE:-}" ]]; then
    if [[ "$signing_identity" == "-" ]]; then echo "Notarization requires Developer ID signing." >&2; exit 1; fi
    xcrun notarytool submit "$dmg" --keychain-profile "$MACOS_NOTARY_PROFILE" --wait
    xcrun stapler staple "$dmg"
    xcrun stapler validate "$dmg"
fi
(cd "$output" && shasum -a 256 "$(basename "$dmg")" > "$(basename "$dmg").sha256")
echo "Packaged: $dmg"
