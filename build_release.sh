#!/bin/bash

# Release build script for Waschzeitrechner.
# Cleans, runs unit tests, and produces a signed release APK
# suitable for distribution via GitHub Releases.

set -euo pipefail

KEYSTORE_PATH="my-release-key.keystore"
KEY_ALIAS="${KEY_ALIAS:-wash-app-key}"

echo "=========================================="
echo "Waschzeitrechner Release Build"
echo "=========================================="

if [ ! -f "$KEYSTORE_PATH" ]; then
    echo "ERROR: $KEYSTORE_PATH not found!"
    echo "Create the keystore first with:"
    echo "  keytool -genkey -v -keystore $KEYSTORE_PATH -keyalg RSA -keysize 2048 -validity 10000 -alias $KEY_ALIAS"
    exit 1
fi

read -s -p "Enter keystore password: " PASSWORD
echo ""
export KEYSTORE_PASSWORD="$PASSWORD"
export KEY_ALIAS

echo ""
echo "Configuration: KEY_ALIAS=$KEY_ALIAS"
echo ""

echo "Step 1: Cleaning build..."
./gradlew clean

echo ""
echo "Step 2: Running unit tests..."
./gradlew testDebugUnitTest

echo ""
echo "Step 3: Assembling release APK..."
./gradlew assembleRelease

echo ""
echo "Step 4: Verifying build artifact..."
APK_PATH="app/build/outputs/apk/release/app-release.apk"

if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo "✓ APK built successfully: $APK_PATH ($APK_SIZE)"
else
    echo "✗ APK not found at $APK_PATH"
    exit 1
fi

echo ""
echo "Step 5: Checking for connected device..."
if command -v adb >/dev/null 2>&1 && adb devices | awk 'NR > 1 && $2 == "device" { found = 1 } END { exit(found ? 0 : 1) }'; then
    echo "Device detected. Installing APK for testing..."
    adb uninstall de.moritz.waschzeitrechner 2>/dev/null || true
    adb install "$APK_PATH"
    echo "✓ Release build installed on device"
else
    echo "⚠ No connected device found. Skipping installation."
    echo "  To test later: adb install $APK_PATH"
fi

echo ""
echo "=========================================="
echo "Release build completed successfully!"
echo "=========================================="
echo ""
echo "Artifact: $APK_PATH"
echo ""
echo "Next steps:"
echo "  1. Test the APK on a device."
echo "  2. Create a GitHub Release and attach the APK."
