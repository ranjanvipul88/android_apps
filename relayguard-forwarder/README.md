# RelayGuard Forwarder

RelayGuard Forwarder is an original Android app for forwarding SMS, MMS-notification, and selected app-notification events to user-configured destinations. It was designed after a high-level review of the user-provided APK, but no APK code, assets, resource filenames, or distinctive text were reused.

## Project Layout

```text
relayguard-forwarder/
  app/              Android entry point, permissions, receivers, workers
  core/domain/      Pure Kotlin models, policies, and use cases
  core/data/        Room, encrypted preferences, Retrofit/OkHttp transports
  core/ui/          Compose Material 3 screens, theme, reusable components
  docs/             Discovery, privacy, permissions, design, QA, release notes
  tools/            Originality and release verification scripts
```

## Requirements

- JDK 17
- Android Studio Meerkat or newer. The app targets Android 16/API 36; Android Gradle Plugin 9.2.1 is the current official release line, while this repository pins AGP 8.5.2/Gradle 8.7 for compatibility with the existing monorepo wrapper until the repo is upgraded together.
- Android SDK Platform 36 and Build Tools 36.x
- Minimum supported device: Android 8.0, API 26

Android 16 API 36 is the current target SDK per the official Android SDK setup guidance: https://developer.android.com/about/versions/16/setup-sdk.

## Build

```powershell
cd D:\Android_Apps\relayguard-forwarder
.\gradlew.bat assembleDebug
```

## Run On An Emulator Or Device

```powershell
cd D:\Android_Apps\relayguard-forwarder
.\gradlew.bat :app:installDebug
adb shell am start -n com.ranjanvipul.relayguard.debug/com.ranjanvipul.relayguard.MainActivity
```

Grant the SMS permissions when prompted, open Settings, then create one or more enabled rules:

- Forward to another phone: enter the destination number and tap `Enable SMS forwarding`.
- Forward to email: enter SMTP host, port, username, app password, from address, and recipient email. Port 587 uses STARTTLS; check SSL/TLS for port 465.
- Forward to Telegram: enter a Telegram bot token and the contact/chat id. The recipient must have started the bot or be reachable by that bot.

Delivery successes and failures appear in the History tab under `Delivery attempts`.

## Test

```powershell
.\gradlew.bat test
.\gradlew.bat connectedDebugAndroidTest
```

Coverage target: at least 70% line coverage for `core:domain` and `core:data`. UI tests cover the primary navigation, empty states, rule creation, and permission rationale surfaces.

## Release

```powershell
.\gradlew.bat lintRelease
.\gradlew.bat bundleRelease
```

Release builds must use a private keystore outside the repository. Do not commit secrets, signing files, production webhook URLs, SMTP passwords, OAuth client secrets, or certificate pins.

## Documentation

- [Implementation plan](docs/implementation-plan.md)
- [Feature parity checklist](docs/feature-parity-checklist.md)
- [APK analysis report](docs/apk-analysis-report.md)
- [Inspiration log](docs/inspiration-log.md)
- [Permission rationale](docs/permission-rationale.md)
- [Privacy summary](docs/privacy-summary.md)
- [Style guide](docs/style-guide.md)
- [QA test plan](docs/qa-test-plan.md)
- [Security checklist](docs/security-checklist.md)
- [Acceptance criteria](docs/acceptance-criteria.md)
- [Test report](docs/test-report.md)

## Originality Scan

Run this after decoding the reference APK into an analysis directory:

```powershell
.\tools\originality_scan.ps1 -ApkResourceDir D:\Android_Apps\_analysis_tmp\sms_forwarder_base_res
```

The script compares long APK string values and resource filenames against this project while ignoring build outputs and the local analysis directory.
