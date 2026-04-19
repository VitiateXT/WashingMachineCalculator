#!/bin/bash

# Run all unit tests on the desktop (JVM) — no Android device or emulator required.

set -euo pipefail

cd "$(dirname "$0")/.."

echo "=========================================="
echo "Waschzeitrechner Desktop Unit Tests"
echo "=========================================="
echo ""

./gradlew testDebugUnitTest "$@"

echo ""
echo "All tests passed."
