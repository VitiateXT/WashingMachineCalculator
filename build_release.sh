#!/bin/bash

# Release Build Script for Waschzeitrechner
# This script builds, signs, and tests the release version

set -euo pipefail

KEYSTORE_PATH="my-release-key.keystore"
KEY_ALIAS="${KEY_ALIAS:-wash-app-key}"

echo "=========================================="
echo "Waschzeitrechner Release Build Script"
echo "=========================================="

# Check if keystore exists
if [ ! -f "$KEYSTORE_PATH" ]; then
    echo "ERROR: $KEYSTORE_PATH not found!"
    echo "Please create the keystore first using:"
    echo "keytool -genkey -v -keystore $KEYSTORE_PATH -keyalg RSA -keysize 2048 -validity 10000 -alias $KEY_ALIAS"
    exit 1
fi

# Prompt for password
read -s -p "Enter password: " PASSWORD
echo ""
export KEYSTORE_PASSWORD="$PASSWORD"
export KEY_ALIAS

echo ""
echo "Configuration: KEY_ALIAS=$KEY_ALIAS"
echo ""

# Step 1: Clean
echo "Step 1: Cleaning build..."
./gradlew clean

# Step 2: Run tests
echo ""
echo "Step 2: Running unit tests..."
./gradlew testDebugUnitTest

# Step 3: Assemble Release APK
echo ""
echo "Step 3: Assembling release APK..."
./gradlew assembleRelease

# Step 4: Assemble Release AAB (for Google Play)
echo ""
echo "Step 4: Assembling release AAB (Android App Bundle)..."
./gradlew bundleRelease

# Step 5: Verify artifacts
echo ""
echo "Step 5: Verifying build artifacts..."
APK_PATH="app/build/outputs/apk/release/app-release.apk"
AAB_PATH="app/build/outputs/bundle/release/app-release.aab"

if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo "✓ APK built successfully: $APK_PATH ($APK_SIZE)"
else
    echo "✗ APK not found at $APK_PATH"
    exit 1
fi

if [ -f "$AAB_PATH" ]; then
    AAB_SIZE=$(du -h "$AAB_PATH" | cut -f1)
    echo "✓ AAB built successfully: $AAB_PATH ($AAB_SIZE)"
else
    echo "✗ AAB not found at $AAB_PATH"
    exit 1
fi

# Step 6: Test on device
echo ""
echo "Step 6: Checking for connected device..."
if command -v adb >/dev/null 2>&1 && adb devices | awk 'NR > 1 && $2 == "device" { found = 1 } END { exit(found ? 0 : 1) }'; then
    echo "Device detected. Installing APK for testing..."
    adb uninstall de.moritz.waschzeitrechner 2>/dev/null || true
    adb install "$APK_PATH"
    echo "✓ Release build installed on device"
    echo ""
    echo "Launch the app to test the release build."
else
    echo "⚠ No connected device found or adb is unavailable. Skipping installation test."
    echo "To test later, run: adb install $APK_PATH"
fi

echo ""
echo "=========================================="
echo "Release build completed successfully!"
echo "=========================================="
echo ""
echo "Build artifacts:"
echo "  APK: $APK_PATH"
echo "  AAB: $AAB_PATH"
echo ""
echo "Next steps:"
echo "  1. Test the app on your device"
echo "  2. Upload the AAB to Google Play Console"
echo "  3. Review and submit for release"
