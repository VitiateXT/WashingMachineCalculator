# Waschzeitrechner (Washing Machine Calculator)

A small Android app that works out **when to start** a washing cycle so it finishes at exactly the time you want it to.

Tell the app when you'd like the laundry to be done and how long your chosen program takes — it calculates the delay-start time for you. Ideal for timing a load to finish right before you get home, or as you wake up.

## Features

- **Finish-time-first planning** — pick the finish time, the app tells you when to press start.
- **Built-in wash programs** — Standard, Quick, Eco, Cotton, and Delicates, each with an editable default duration.
- **Smart recommendations** (opt-in) — suggests a better program/duration based on how much time is available and your routine.
- **Persistent preferences** — default program, default finish time, and per-program durations are remembered.
- **Light / dark / system themes.**
- **18 languages** including English, German, French, Spanish, Japanese, Arabic, and more.
- **Fully offline** — no network permission, no tracking, no accounts.

## Screenshots

Screenshots to be added.

## Installation

Download the latest `app-release.apk` from the [Releases page](../../releases) and install it on your Android device. You may need to enable installation from unknown sources.

**Requires:** Android 8.0 (API 26) or higher.

## Tech Stack

- **Kotlin** with **Jetpack Compose** for the UI
- **Material 3** design system
- **AndroidX AppCompat** for per-app locale switching
- **SharedPreferences** for local persistence
- Pure-Kotlin, framework-free scheduling logic unit-tested with `kotlin.test`

## License

Released under the [MIT License](LICENSE).
