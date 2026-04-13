# WashingMachineApp

Android app for calculating the best start time for a washing program based on a desired finish time and wash duration.

## Project Notes

- UI is built with Kotlin and Jetpack Compose.
- Core scheduling logic is extracted into a testable calculator.
- Local release packaging is handled by `build_release.sh`.
- Project review, validation, and release notes are stored in `.knowledge/`.

## Useful Files

- `app/src/main/java/com/example/washingmachineapp/MainActivity.kt`
- `app/src/main/java/com/example/washingmachineapp/WashScheduleCalculator.kt`
- `app/src/test/java/com/example/washingmachineapp/WashScheduleCalculatorTest.kt`
- `app/build.gradle.kts`
- `build_release.sh`
- `.knowledge/release-guide.md`

## Build

```bash
./gradlew testDebugUnitTest assembleDebug
```

## Signed Release Build

```bash
./build_release.sh
```

## Important Before Play Release

- Replace the placeholder package name `com.example.washingmachineapp` with the final production application ID before the first public release.
- Upload the generated AAB to Google Play, not the APK.