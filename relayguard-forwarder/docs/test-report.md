# Test Report

Last run: 2026-06-04.

| Command | Result | Notes |
| --- | --- | --- |
| `./gradlew.bat test` | Passed | Domain rule tests and data mapper tests passed for debug and release variants. |
| `./gradlew.bat --no-daemon lintDebug` | Passed | Remaining warnings are dependency freshness and the adaptive icon folder note caused by minSdk 26. |
| `./gradlew.bat --no-daemon :app:assembleDebug :app:bundleRelease` | Passed | Debug APK and release AAB artifacts were generated. Release signing uses the default unsigned/debug-local setup until a private keystore is configured. |
| `./gradlew.bat --no-daemon :app:assembleDebugAndroidTest` | Passed | Instrumentation test APK compiles after the production forwarding UI changes. |
| `./tools/originality_scan.ps1` | Passed | No long APK strings or APK resource filenames were found in the RelayGuard project. |
| `./gradlew.bat --no-daemon clean :app:assembleDebug :app:bundleRelease` | Blocked by local file lock | Windows kept `app/build` locked after earlier Gradle/Kotlin daemon work. Stop Android Studio/Gradle/Java processes or reboot, then rerun the command. |
| `./gradlew.bat :app:connectedDebugAndroidTest` | Gradle task failed, test result passed | The emulator result file reports `test_status: PASSED`; Gradle/UTP returned `Failed to receive the UTP test results`. This should be rerun after upgrading the project toolchain or on a stable physical device. |

Artifacts:
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release bundle: `app/build/outputs/bundle/release/app-release.aab`
- Lint report: `app/build/reports/lint-results-debug.html`
- Android test result: `app/build/outputs/androidTest-results/connected/debug/Pixel_10_Pro(AVD) - 17/test-result.textproto`
