# Contributing to ZeroDroid

Thanks for considering a contribution — bug reports, new tool ideas, hardware compatibility
notes, and pull requests are all welcome.

## Before you start

- For anything beyond a small fix, open an issue first (or start a
  [discussion](https://github.com/theabhishekchandra/ZeroDroid/discussions)) so we can align on
  approach before you invest time.
- **Security disclosures should not go through public issues or PRs.** See
  [SECURITY.md](SECURITY.md).
- By contributing, you agree your changes are made in the spirit of the project's
  [Ethical Use](README.md#-ethical-use) and [Disclaimer](README.md#-disclaimer) sections —
  ZeroDroid is for education, authorized security research, and defensive use.

## Development setup

- Android Studio Ladybug (2025.1+) or newer
- JDK 17+, Android SDK 36+
- A **physical Android device** — most tools depend on real radios/sensors that emulators
  can't provide (BLE, NFC, UWB, IR, cellular, barometer, etc.)

```bash
git clone https://github.com/theabhishekchandra/ZeroDroid.git
cd ZeroDroid
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Project conventions

- **Architecture:** MVVM — `domain/` (business logic, no Android UI imports) → `data/`
  (Room repositories, if needed) → `ui/` (Compose screens) → `viewmodel/` (`@HiltViewModel`).
  Each feature under `features/<name>/` follows this same shape.
- **DI:** Hilt. Constructor-inject dependencies into ViewModels; provide new
  services/repositories via the relevant `core/di/*Module.kt` (`SystemServiceModule` for
  Android system services, `FeatureModule` for feature domain/repository classes,
  `DatabaseModule` for Room/DAOs).
- **Style:** Kotlin, StateFlow for UI state, Jetpack Compose + Material 3, JetBrains Mono
  terminal aesthetic (see `core/ui/TerminalCard.kt`, `StatusIndicator.kt` for the existing
  design language — match it rather than introducing a new visual style).
- **No dead abstractions:** don't add a new dependency, wrapper, or config flag unless the
  task actually needs it.

## Adding a new tool

1. Create `features/<name>/{domain,data,ui,viewmodel}/` following an existing feature as a
   template (e.g. `features/rogue_ap_detector/` is a good mid-complexity example).
2. Register the screen in `navigation/ZeroDroidScreen.kt` (route, title, icon, category).
3. Add any new domain/repository classes to the appropriate `core/di/*Module.kt`.
4. Document it in [`docs/TOOLS.md`](docs/TOOLS.md): what it solves, how to use it, what you see.
5. If it can detect something alert-worthy, consider wiring it into the
   [Alert Center](app/src/main/java/com/abhishek/zerodroid/core/alerts/AlertCenterRepository.kt) —
   see how `RogueApViewModel` or `HiddenCameraViewModel` do it, including their dedup logic
   (don't record the same finding on every scan cycle).

## Testing

- Pure logic in `domain/`/`util/` should get JVM unit tests under `app/src/test/` — see
  `app/src/test/java/com/abhishek/zerodroid/features/rogue_ap_detector/domain/RogueApAnalyzerTest.kt`
  for the expected shape (framework-free, no Android dependencies).
- UI/hardware-dependent behavior should be manually verified on a physical device before
  opening a PR — note which device you tested on.

## Branching model

- **`develop`** is the default branch — all active work happens here.
- **`main`** is the stable/release branch. It only moves forward via a PR from `develop`
  and is protected (PR + passing CI required, enforced even for admins, linear history only).
- `develop` is also protected: direct pushes require a PR, and the `build` CI check
  (lint, unit tests, debug assemble) must pass before merging.

## Submitting a PR

1. Fork the repo and branch off `develop`: `git checkout -b feature/my-tool develop`
2. Keep the change focused — unrelated refactors/formatting make review harder.
3. Run `./gradlew test` and `./gradlew assembleDebug` locally.
4. Open a PR **into `develop`** (not `main`) describing what changed and how you verified it
   (the PR template will prompt you).

## Code of Conduct

This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md).
