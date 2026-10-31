# Maths Dash

A simple, offline Android maths challenge built for children and families. One puzzle at a time, big number tiles and a dark theme.

## Modes

- **Addition**: Find the only number that is the sum of two others in the grid.
- **Multiplication**: Find the only number that is the product of two others in the grid.
- **Subtraction / Division**: Read the equation and tap its answer. These modes use an equation because a grid of three related numbers can otherwise have more than one valid answer.
- **Times tables**: Select one or more of the 1×–12× tables and a maximum multiplier 1–12. Multiplication facts involving ×1 show an equation so the answer is never ambiguous.
- **Practice**: No timer, no leaderboard entry.
- **Timed**: 30/60/90/120 seconds or custom (10–600 seconds). +1 per correct answer; a wrong answer highlights the correct one and moves on.
- **Leaderboard**: All players appear together, with an optional filter for identical settings. Results are saved locally; the app remembers the previous name.

## Install the APK

Go to **Actions → Build Android APK → latest successful run → Artifacts → Maths-Dash-APK**. Download and extract the ZIP, then open `app-debug.apk` on your Android phone. You may need to allow installation from the app used to open the APK.

The APK is a locally installable **debug build** intended for personal use, not a Play Store release.

## Build locally

Open this repository in Android Studio (JDK 17, Android SDK 35), or install Gradle 8.11.1 and run:

```bash
gradle testDebugUnitTest assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`. The GitHub workflow installs Gradle automatically, so a local Gradle installation is not required just to download an APK.

## Privacy

No account, analytics, network permission or backend. Names, settings and scores are kept in on-device preferences. Removing app data or uninstalling the app can remove the leaderboard.
