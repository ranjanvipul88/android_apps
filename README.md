# android_apps

This repository is a monorepo for Android applications.

## Structure

```text
android_apps/
  chatdock-web/
    README.md
    settings.gradle.kts
    build.gradle.kts
    gradle.properties
    gradlew
    gradlew.bat
    gradle/
    app/
  personal_sms/
    README.md                  # Project-specific developer guide
    android/                   # Native Kotlin Android Studio Project
      app/
      build.gradle
      settings.gradle
      ...
    web-portal/                # Persistent Cloud Sync & Web Dashboard
      server.js
      package.json
      public/
```

Each app is kept in its own top-level folder. Work inside one app should not affect the others as long as each app keeps its own Gradle project files and Android app module.

---

## Applications

### 1. Chatdock Web (`chatdock-web`)
An Android application for messaging and chat services integration.

### 2. Personal SMS & MMS Replica (`personal_sms`)
A premium, production-ready replica of Google Messages App with advanced cellular integration.

- **Native Android Client**: Built in Kotlin and Jetpack Compose (Material 3) supporting SDK 34. Operates as the **Default SMS Handler**.
- **Auto-Forwarder**: Integrates a background receiver that intercepts incoming cellular SMS and routes them via `SmsManager` to a configured target number.
- **Web Sync Portal**: Node.js Express & SQLite cloud server with a premium Material 3 dashboard, syncing cellular message logs. Bypasses standard battery saver timeouts by persisting messages in the cloud.
- **Never-Expiring Sessions**: Web portal logins utilize persistent security tokens with a **99-year session life**, ensuring you never sign out even if accessed once a year.

---

## Working On One App Only

Use Git sparse checkout when you only want one app folder locally.

```bash
git clone --filter=blob:none --sparse https://github.com/ranjanvipul88/android_apps.git
cd android_apps
git sparse-checkout set chatdock-web
```

To switch or add another app later:
```bash
git sparse-checkout set personal_sms
# or to checkout both:
git sparse-checkout set chatdock-web personal_sms
```

## Adding Future Apps

Create each new app as a top-level folder:

```text
android_apps/
  chatdock-web/
  personal_sms/
  my-next-app/
```

Recommended rules:

- Keep each app self-contained.
- Include a project-level `README.md` in every app folder.
- Include a Gradle wrapper per app if the app is independently buildable.
- Do not commit generated folders such as `.gradle/`, `build/`, APK outputs, or IDE caches.
