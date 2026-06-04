# Test Report

Last run: 2026-06-04.

| Command | Result | Notes |
| --- | --- | --- |
| `./gradlew.bat test` | Passed | Domain rule tests and data mapper tests passed for debug and release variants. |
| `./gradlew.bat lintDebug` | Passed | Remaining warnings are dependency freshness and the adaptive icon folder note caused by minSdk 26. |
| `./gradlew.bat assembleDebug bundleRelease` | Passed | Debug APK and release AAB artifacts were generated. Release signing uses the default unsigned/debug-local setup until a private keystore is configured. |
| `./tools/originality_scan.ps1` | Passed | No long APK strings or APK resource filenames were found in the RelayGuard project. |
| `./gradlew.bat :app:connectedDebugAndroidTest` | Gradle task failed, test result passed | The emulator result file reports `test_status: PASSED`; Gradle/UTP returned `Failed to receive the UTP test results`. This should be rerun after upgrading the project toolchain or on a stable physical device. |

Artifacts:
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release bundle: `app/build/outputs/bundle/release/app-release.aab`
- Lint report: `app/build/reports/lint-results-debug.html`
- Android test result: `app/build/outputs/androidTest-results/connected/debug/Pixel_10_Pro(AVD) - 17/test-result.textproto`
