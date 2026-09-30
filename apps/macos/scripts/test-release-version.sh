#!/bin/bash
set -euo pipefail
validator="$(cd "$(dirname "$0")" && pwd)/release-version.sh"
for version in 0.1.0 1.2.3 0.2.0-rc.1 0.2.0-rc.12; do
    [[ "$(bash "$validator" "$version")" == "${version%%-*}" ]]
done
for version in '' 0.2 0.2.0-rc 0.2.0-rc.0 0.2.0-rc.01 01.2.0 0.2.0-beta.1 0.2.0-rc.1+build '0.2.0 '; do
    if bash "$validator" "$version" >/dev/null 2>&1; then
        echo "Incorrectly accepted: $version" >&2; exit 1
    fi
done
echo "Release version checks passed."
