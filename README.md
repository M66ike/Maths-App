# Maths Dash

A simple, offline Android maths challenge built for children and families. One puzzle at a time, big number tiles and a dark theme.

## Modes

- **Addition**: Find the only number that is the sum of two others in the grid.
- **Multiplication**: Find the only number that is the product of two others in the grid. Distractor result tiles are drawn from the selected times-table products first; only when there are not enough safe, distinct values does the generator use unrelated numbers.
- **Subtraction / Division**: Read the equation and tap its answer. These modes use an equation because a grid of three related numbers can otherwise have more than one valid answer. Division choices stay within 1–12 instead of showing unrelated large numbers.
- **Times tables**: Select one or more of the 2×–12× tables and a maximum multiplier 2–12. ×1 facts are excluded entirely. Multiplication always uses a tile grid, including square facts such as 2 × 2 (which legitimately use two identical source tiles).
- **Practice**: No timer, no leaderboard entry.
- **Timed**: 30/60/90/120 seconds or custom (10–600 seconds). +1 per correct answer. A wrong answer pauses the timer and highlights the correct tile and calculation for about two seconds, then the next question starts automatically. Correct answers advance automatically.
- **Leaderboard**: All players appear together, with an optional filter for identical settings. Results are saved locally; the app remembers the previous name.
- **Themes**: Tap Theme next to Scores to choose separate background and button colours from 28 preset colours each, or use the HSV colour wheel and brightness slider for a custom shade. Readable text switches automatically between white and black. Your selection is saved on the phone.
- **End button**: A smaller visible button asks for confirmation before ending or finishing a round. The Android Back action also confirms during a game.
- **Menu**: The home menu includes an exit button with confirmation. Theme and Scores are spaced evenly beneath the title.
- **App icon**: Four outlined black-and-white tiles showing +, −, ÷ and ×.

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

**Updating an existing debug install:** The first update from v1.0 may require uninstalling the old APK because GitHub's temporary debug signing key has changed. Uninstalling erases locally stored scores. The workflow now caches its debug key for more consistent future updates. For a permanent release, use a privately stored release signing key.

- **Reset leaderboard**: The Scores screen includes a Reset all scores button. A confirmation is required; only saved score entries are erased, not the remembered player name or preferences.

## Landscape orientation

Maths Dash supports portrait and landscape on phones and tablets. Rotate the device (with system auto-rotate enabled) to place the score, timer and End button on the left and a compact puzzle grid on the right. The current game and remaining time stay in place during rotation. Menus, themes, results and scores remain scrollable in either orientation.
