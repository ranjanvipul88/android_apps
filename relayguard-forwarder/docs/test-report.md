# Test Report

Last run: 2026-06-04.

| Command | Result | Notes |
| --- | --- | --- |
| `./gradlew.bat test` | Passed | Domain rule tests and data mapper tests passed for debug and release variants. |
| `./gradlew.bat --no-daemon lintDebug` | Passed | Remaining warnings are dependency freshness and the adaptive icon folder note caused by minSdk 26. |
| `./gradlew.bat --no-daemon :app:assembleDebug :app:bundleRelease` | Passed | Debug APK and release AAB artifacts were generated. Release signing uses the default unsigned/debug-local setup until a private keystore is configured. |
| `./gradlew.bat --no-daemon test lintDebug :app:assembleDebug :app:bundleRelease` | Passed | Final verification after Telegram/email/ad-free parity changes and WorkManager receiver fix. |
| `./gradlew.bat --no-daemon :app:assembleDebugAndroidTest` | Passed | Instrumentation test APK compiles after the production forwarding UI changes. |
| `./tools/originality_scan.ps1` | Passed | No long APK strings or APK resource filenames were found in the RelayGuard project. |
| `./gradlew.bat --no-daemon clean :app:assembleDebug :app:bundleRelease` | Blocked by local file lock | Windows kept `app/build` locked after earlier Gradle/Kotlin daemon work. Stop Android Studio/Gradle/Java processes or reboot, then rerun the command. |
| `./gradlew.bat --no-daemon :app:connectedDebugAndroidTest` | Gradle task failed, test result passed | The emulator result file reports `test_status: PASSED` and `RelayGuardAppTest.emptyStateIsVisible` passed; Gradle/UTP returned `Failed to receive the UTP test results`. |
| Emulator receive smoke test | Passed | Installed debug APK on `emulator-5554`, granted runtime permissions, delivered an RCS-compatible test broadcast, and confirmed a persisted `IncomingRcs` event in Room. |

Artifacts:
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release bundle: `app/build/outputs/bundle/release/app-release.aab`
- Lint report: `app/build/reports/lint-results-debug.html`
- Android test result: `app/build/outputs/androidTest-results/connected/debug/Pixel_10_Pro(AVD) - 17/test-result.textproto`
