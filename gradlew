#!/bin/sh
# Compatibility entry point; the Android build lives in apps/android.
set -eu
cd "$(dirname "$0")/apps/android"
exec ./gradlew "$@"
