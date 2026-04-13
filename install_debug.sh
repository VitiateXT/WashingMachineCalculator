#!/bin/bash
set -euo pipefail

APK="app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="de.moritz.waschzeitrechner"
ACTIVITY=".MainActivity"

# Require adb
if ! command -v adb >/dev/null 2>&1; then
    echo "ERROR: adb not found in PATH"
    exit 1
fi

# Require a connected device
if ! adb devices | awk 'NR > 1 && $2 == "device" { found = 1 } END { exit(found ? 0 : 1) }'; then
    echo "ERROR: No connected Android device found"
    exit 1
fi

# Build debug APK
echo "Building debug APK..."
./gradlew assembleDebug

# Uninstall first to avoid signature mismatch (e.g. release → debug)
echo "Installing..."
adb uninstall "$PACKAGE" 2>/dev/null || true
adb install "$APK"

# Relaunch the app
echo "Relaunching..."
adb shell am force-stop "$PACKAGE"
adb shell am start -n "$PACKAGE/$ACTIVITY"
echo "Done."
