# Arctic Fish Trail

A fully offline Android game about an ice-fishing trail — a fishing **Quiz** (3 difficulty levels) and a **Matching Pairs** memory game (9 levels). Kotlin + Jetpack Compose, no network, no tracking.

- **applicationId / namespace:** `com.arcticfishtrail.game`
- **minSdk 24 · compileSdk 36 · targetSdk 36 · JDK 17**

---

## Features

| Mode | What it does |
|---|---|
| **Quiz** | 3 difficulty levels (Easy / Medium / Hard) × 10 fishing questions, 4 options, exactly one correct. One question at a time with a progress bar and `n/10`. Tapping an answer marks the correct option green with ✓ and a wrong pick red with ✗, then moves on after ~0.75 s. The best score per difficulty is saved. Pause overlay: Continue / Restart / Menu. Results panel: `Score: X/10` with Continue / Restart / Menu. |
| **Matching Pairs** | 9 levels (pairs / columns / seconds): 1: 2/2/60 · 2: 3/2/70 · 3: 4/2/70 · 4: 6/3/80 · 5: 6/3/80 · 6: 8/4/100 · 7: 8/4/100 · 8: 10/4/110 · 9: 12/4/120. Each deck draws `pairs` random icons from **18** fishing items, duplicates them and shuffles. Flip two cards: matches stay open, mismatches flip back. A countdown runs per level and stops while paused. Clearing every pair wins (**Well Done**); running out of time loses (**Out of Time**). The best number of matched pairs per level is saved. Level 1 is always open, and each later level unlocks once the one before it is cleared. The grid **always fits the screen without scrolling**, because the card size is calculated from the space available. |

**Screens:** Home · Menu (Base Camp) · Quiz difficulty · Quiz · Quiz results · Levels (9 bubbles, locked/unlocked, Game Rules) · Pairs game · Pairs results · Trail Log (best per difficulty + levels cleared / 9) · Settings (sound, privacy, rate, reset with confirmation) · Game Rules.

## Offline only, privacy

- The manifest declares **no permissions at all**: no `INTERNET` and no runtime permissions. There are no services, WorkManager, alarms or notifications.
- The app has no analytics, ads, accounts, Firebase, attribution or tracking SDKs, and no networking libraries.
- "Rate the App" and "Full policy" hand an `ACTION_VIEW` intent to *another* app (Play Store or a browser). The game itself never opens a network connection.
- Replace `privacy_policy_url` in `res/values/strings.xml` with your real policy page, because Google Play requires a URL.

**Not integrated, on purpose:** ads, analytics, crash reporting, in-app purchases, Play Games, cloud save, push notifications, attribution/MMP (AppsFlyer and similar), OneSignal, remote config.

## Local storage (DataStore)

- `GameRepository` keeps **one** DataStore Preferences key, `game_data_json`, which holds a serialized `GameData`:
  ```kotlin
  @Serializable data class GameData(quizBest: Map<String,Int> = emptyMap(), pairsBest: Map<String,Int> = emptyMap(), settings: Settings = Settings())
  @Serializable data class Settings(soundEnabled: Boolean = true)
  ```
- `GameSerializer` is context-free and unit-tested (`ignoreUnknownKeys`, `coerceInputValues`). Empty storage, a missing key, or empty/corrupted/wrong-typed JSON all decode to defaults, so a read never crashes. Stored values are clamped.
- Cloud backup and device transfer are disabled (`allowBackup=false`, `data_extraction_rules.xml`).
- **Data reset:** Settings → Reset Progress, then confirm. You can also clear the app's storage or uninstall.

## Visual concept: "Aurora Ice Trail"

A night-time ice-fishing trail under the northern lights. The art is Gleb's asset set (background, logo, three heroes, ice plates, 18 items). Panels are drawn in Compose to match the art.

| Color | HEX | Usage |
|---|---|---|
| Polar Night | `#0B1E3A` | app/splash background, panel gradient bottom |
| Glacier Blue | `#1C4E80` | panels, card backs, progress track |
| Ice Teal | `#2EC4B6` | accent borders, progress fill, timer chip |
| Frost White | `#EAF6FF` | body text, card faces |
| Aurora Green | `#7CFFB2` | success accents (✓, cleared) |
| Salmon Coral | `#FF6B5B` | low-time timer, lose panel border |
| Sun Gold | `#FFC857` | titles, stars, gold borders, pairs chip |
| Slate Ice | `#8FB3D1` | muted and locked text |
| Correct Fill | `#17804F` | correct-answer fill (white text ≥ 4.5:1) |
| Wrong Fill | `#C7372F` | wrong-answer fill (white text ≥ 4.5:1) |

**Unique home layout.** Home is not a mascot → title → stats → button-stack dashboard. The logo hangs tilted top-left while the red salmon leaps top-right. A dotted trail winds down the ice past a few fishing items (decoration only). Home has a single action, START; Results, Rules and Settings live only in the Menu, so nothing is duplicated. Aya the angler stands beside the trail, and the ornate **START** plate sits at the trail's end.

**Buttons.** Every button is an image plate with a white bold label drawn on top in code.
- `btn_plate` is drawn as a horizontal **3-slice**: the ice corners stay intact and only the middle stretches.
- `btn_menu` (the ornate plate) keeps its exact aspect ratio at about 70% of the screen width.
- **Modal panels** (pause, results, rules, fallback) are drawn in Compose as a rounded rect with a gradient and an accent border. A bitmap frame is never stretched.

**Quiz layout.** Any empty space under the answers is filled with the cast (Aya plus the two fish, gently bobbing). The strip hides itself when space is tight.

**Typography.** Material 3 type scale on the system font (no bundled fonts), all in `sp`, with a soft text shadow so text stays readable over the art.

**Accessibility.**
- White-on-navy and gold-on-navy contrast.
- Touch targets ≥ 48 dp.
- Content descriptions on every image button and card.
- Card state is announced ("hidden", "Bobber", "Bobber, matched").
- ✓ / ✗, "Locked", "★ Cleared" and the ✓ badge carry state as text, so color is never the only signal.
- Live regions for feedback and score.
- Every screen can scroll at large font scales except the Pairs grid, which resizes instead.

**App icon.** An adaptive icon with the blurred aurora sky from the background as the background layer and the logo inside the 66 dp safe zone as the foreground. It includes a monochrome layer for themed icons (Android 13+) and legacy square/round PNGs for mdpi–xxxhdpi. `store/play_icon_512.png` is the Play Console icon.

**Splash.** `core-splashscreen` with the Polar Night background and the logo as the icon. No heavy assets are involved, and the result is consistent on API 24–36.

### Assets (fixed names, `res/drawable-nodpi/`)

`bg_main.jpg`, `logo`, `mascot_a` (Aya), `mascot_b` (red salmon), `mascot_c` (blue fish), `btn_menu`, `btn_plate`, `plate_round`, `icon_back`, `icon_pause`, `item_01`…`item_18`.

All of them are referenced only through `ui/Assets.kt`. To swap art, overwrite a file with the same name. If its proportions change, update the matching `*_ASPECT` constant. The images are trimmed, resized (items 400 px, heroes ≤ 900 px) and palette-compressed, about 2.8 MB in total.

### Sounds

`res/raw/sfx_click|correct|wrong|match|win|lose.wav` are short synthesized tones played through `SoundPool` (`SoundManager`, created in the `Application`). The Settings toggle is saved in DataStore and applied at startup and whenever it changes. To use real sounds, replace the files and keep the names.

### Quiz content

`domain/QuizContent.kt` holds 3 difficulty levels × 10 questions on a fishing theme. It is marked **REPLACEABLE**: edit the texts freely, but keep `correctIndex` correct and the ids (`easy`, `medium`, `hard`) stable, because best scores are keyed by id.

## Tech stack

Kotlin 2.0.21 · Jetpack Compose (BOM 2024.12.01) · Material 3 · Navigation Compose 2.8.5 (type-safe `@Serializable` routes) · Lifecycle/ViewModel 2.8.7 · Coroutines 1.9.0 + Flow · DataStore Preferences 1.1.1 · Kotlinx Serialization 1.7.3 · core-splashscreen · AGP 8.7.3 · Gradle 8.9 (Kotlin DSL + `gradle/libs.versions.toml`) · JDK 17. It has no DI framework, no Room, no WebView and no networking.

## Architecture (simple MVVM)

```
ArcticFishTrailApp ── AppContainer(GameRepository, SoundManager)   ← created once, no DI framework
MainActivity ── Theme ── CompositionLocals ── AppNavHost (type-safe routes)
  screens ── viewModel(factory = XViewModel.factory(repo, …))
     ProgressViewModel  → ProgressUiState   (menus, levels, trail log)
     QuizViewModel      → QuizUiState       (SavedStateHandle keeps index/score across process death)
     PairsViewModel     → PairsUiState      (timer, board, pause, result)
     SettingsViewModel  → SettingsUiState
domain/GameLogic   pure: level table, deck building, flip rules, unlock/completion/count
domain/QuizContent pure: questions
data/GameSerializer pure: JSON ↔ GameData;  data/GameRepository: DataStore
```

- ViewModels expose **immutable** `StateFlow` UI state, which screens collect with `collectAsStateWithLifecycle`.
- **Safe navigation.** Every route argument has a sentinel default and is decoded with `runCatching { toRoute() }`. Invalid or missing arguments, an unknown category, a locked or out-of-range level, or an impossible result all show a friendly fallback with **Back** instead of crashing.
  - Navigation happens only from a RESUMED entry, which ignores double taps.
  - Game → result uses a replace, so the back stack stays clean.
  - A result is written with `NonCancellable` *before* the app navigates away.
- **Back and pause.**
  - In a game, system Back toggles the pause overlay, and the top-left arrow leaves the game.
  - Games pause automatically on `ON_PAUSE`, and the Pairs timer stops while paused.
- **Layout:** portrait only (`screenOrientation="portrait"`), edge-to-edge with `safeDrawing` insets, and no keep-awake.

## Build and run

1. Open the project folder in **Android Studio** (Ladybug or newer). It picks up the wrapper (`gradle/wrapper`, Gradle 8.9).
2. Set **Gradle JDK = 17** (Settings → Build Tools → Gradle).
3. Install **SDK Platform 36** in the SDK Manager.
4. **Debug build:** `./gradlew assembleDebug`, then `adb install -r app/build/outputs/apk/debug/app-debug.apk`. The debug build uses the suffix `.debug`, so it installs next to the release build.
5. **Unit tests:** `./gradlew testDebugUnitTest`. They cover deck building, flip rules, unlock/completion/count, the level table, safe JSON handling (null, empty, corrupted, unknown keys, missing fields, nulls) and quiz content integrity.

**API 36 note.** AGP 8.7.x does not officially list compileSdk 36, so `gradle.properties` contains `android.suppressUnsupportedCompileSdk=36`. If resource linking or lint ever fails because of it, bump `agp` to `8.9.x` and the Gradle wrapper to `8.11.1`. Nothing else needs to change.

**16 KB page size.** The app is pure Kotlin/Compose with **no native `.so` libraries**, so it is 16 KB page-size compatible. To confirm, look for a `lib/` folder in the AAB (Android Studio → Analyze APK, or `unzip -l app-release.aab | grep '\.so'`). CI also runs `zipalign -c -P 16`.

## Release signing (PKCS12, never the debug key)

Signing comes only from these environment variables:

| Variable | Meaning |
|---|---|
| `ANDROID_KEYSTORE_PATH` | absolute path to the `.p12` |
| `ANDROID_KEYSTORE_PASSWORD` | store password |
| `ANDROID_KEY_ALIAS` | key alias |
| `ANDROID_KEY_PASSWORD` | key password (same as the store password for PKCS12) |

If any of them is missing, or the file doesn't exist, `packageRelease` / `signReleaseBundle` **fail with a clear message**. There is no fallback to debug signing. Debug builds and unit tests don't need the variables.

**Generate a PKCS12 keystore** (a ready one has been generated for you separately; keep it outside git):
```bash
keytool -genkeypair -v -storetype PKCS12 -keystore arcticfishtrail-release.p12 \
  -alias arcticfishtrail -keyalg RSA -keysize 4096 -validity 10000
```

**Sign locally:**
```bash
export ANDROID_KEYSTORE_PATH="$HOME/keys/arcticfishtrail-release.p12"
export ANDROID_KEYSTORE_PASSWORD='…'
export ANDROID_KEY_ALIAS='arcticfishtrail'
export ANDROID_KEY_PASSWORD='…'
./gradlew assembleRelease bundleRelease
```

**Verify:**
```bash
$ANDROID_HOME/build-tools/36.0.0/apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
# must NOT contain "CN=Android Debug"
jarsigner -verify -certs app/build/outputs/bundle/release/app-release.aab
```

## GitHub Actions (`.github/workflows/android-build.yml`)

- **Trigger:** push to `main`, or run it manually (`workflow_dispatch`).
- **Setup:** JDK 17, SDK Platform 36 and Build Tools 36.0.0, then Gradle **8.9** through `gradle/actions/setup-gradle`, so a committed wrapper is not required.
- **Build:** decodes the keystore from secrets, runs unit tests, then runs `assembleRelease bundleRelease`.
- **Signature checks:** `apksigner verify --print-certs` runs on the APK and `jarsigner` on the AAB. **Either one fails the job** if verification fails or the certificate is `CN=Android Debug`. There is also a 16 KB alignment check.
- **Uploads:** the **APK** (for testing only) and the **AAB** (the Google Play artifact) are uploaded as build artifacts.
- **Secrets:** they are never printed, and the decoded keystore is deleted at the end.

**Secrets to add** (Repository → Settings → Secrets and variables → Actions): `ANDROID_KEYSTORE_BASE64` (the `.p12` as a single base64 line: `base64 -i arcticfishtrail-release.p12 | tr -d '\n'`), `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.

**APK vs AAB.** The APK is for sideload testing (`adb install -r`). **Only the `.aab` goes to Google Play.** Enroll in Play App Signing so this key acts as the upload key.

> ⚠️ A green CI build is **not** proof that the app launches. Always verify on a real device (below).

## Local release verification checklist

```bash
adb install -r app-release.apk
adb logcat -c && adb logcat *:E AndroidRuntime:E | grep -i arcticfishtrail
```
- [ ] The splash shows the logo on Polar Night, with no white flash, and Home appears.
- [ ] Home: START, Back from Menu, and Back on Home exits.
- [ ] Quiz Easy/Medium/Hard: green ✓ / red ✗, auto-advance, pause/resume/restart, the results panel, and the best score shown in the Trail Log.
- [ ] Pairs level 1 → win → level 2 unlocks. Let the timer run out → Out of Time. Pause stops the timer.
- [ ] Level 9 (24 cards) fits on a small phone **without scrolling**.
- [ ] Settings: the sound toggle persists after a restart, Reset Progress empties the Trail Log and locks levels 2–9, and Privacy/Rate don't crash without a browser or Store.
- [ ] Rotation stays portrait. Background the app mid-game, return, and the game is paused. "Don't keep activities" in developer options causes no crash.
- [ ] No `FATAL EXCEPTION` appears in logcat.

## Staged R8

The first release ships **with `isMinifyEnabled = false` and `isShrinkResources = false`**. After that release passes the checklist above:
1. Set both flags to `true` in `app/build.gradle.kts`. `proguard-rules.pro` already keeps kotlinx-serialization classes, serializers and the `@Serializable` navigation routes.
2. Rebuild the signed APK/AAB and **run the whole checklist again**. Pay particular attention to navigation (type-safe routes) and saved progress (JSON).
3. Keep `app/build/outputs/mapping/release/mapping.txt` and upload it to Play for deobfuscated crash reports.

## Known limitations

- The quiz questions are a replaceable starter set, written in English only (no localization yet).
- The sound effects are synthesized placeholder tones.
- On Android 16 large screens (≥ 600 dp), the system may ignore the portrait lock, as the platform intends. Layouts are width-capped, so they stay usable.
- A quiz in progress survives process death, but a Pairs round restarts, because the timer isn't persisted.
- The system Back gesture inside a game pauses it rather than leaving it; the top-bar arrow leaves.
- No Gradle build was run while generating this project. Verify locally as described above.
