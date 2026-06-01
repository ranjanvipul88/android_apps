# ChatDock Web

ChatDock Web is a native Android wrapper around the official WhatsApp Web session at `https://web.whatsapp.com`.

It uses standard browser/WebView technology and does not use private WhatsApp APIs or bypass end-to-end encryption.

## Package

```text
applicationId: com.chatdock.web
minSdk: 24
targetSdk: 34
```

## Main Features

- Official WhatsApp Web session embedded in Android WebView.
- Desktop Chrome/Windows user agent for WhatsApp Web compatibility.
- Five isolated account slots on Android 9 and newer.
- Rename all five account slots.
- Persistent WebView cookies, DOM storage, database storage, and cache.
- Camera and microphone WebRTC bridge for web voice/video features.
- File picker bridge for images, videos, audio, PDFs, documents, and zip files.
- Downloads for shared media/docs into `Downloads/ChatDock Web`.
- Native notification bridge for web notifications.
- Message recovery log for future notification/message previews received through this app.
- Status Saver for user-selected accessible image/video files.
- Repost saved status media through Android share sheet.
- Screen Lock with PIN.
- App menu actions:
  - Open official WhatsApp chat menu
  - Open current page in browser
  - Print
  - Translate
  - Reload
  - Share
  - Lock now

## Important Limitations

- Existing WhatsApp features such as messaging, reactions, calls, groups, channels, disappearing messages, and E2EE are provided by official WhatsApp Web inside the WebView.
- The app does not and should not reimplement private WhatsApp chat actions.
- Message recovery can only store future notification previews that pass through this app. It cannot restore old deleted WhatsApp database messages or bypass encryption.
- Status Saver uses user-selected files through Android's document picker. It does not scrape private WhatsApp folders.

## Build Requirements

- JDK 17
- Android SDK with platform 34 and build tools 34
- Internet access for Gradle dependency resolution on first build

## Build Debug APK

From this folder:

```powershell
.\gradlew.bat assembleDebug
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Build Release APK

```powershell
.\gradlew.bat assembleRelease
```

Output:

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

The release APK must be signed with a release key before distribution.

## Build Google Play App Bundle

```powershell
.\gradlew.bat bundleRelease
```

Output:

```text
app/build/outputs/bundle/release/app-release.aab
```

For Google Play, upload a signed release app bundle through Play Console.

## Install Debug APK As Update

Do not uninstall the existing app if you want to preserve WhatsApp Web sessions and settings.

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The `-r` flag replaces the APK while preserving app data.

## Sparse Checkout This App Only

From a fresh clone:

```bash
git clone --filter=blob:none --sparse https://github.com/ranjanvipul88/android_apps.git
cd android_apps
git sparse-checkout set chatdock-web
```
