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
```

Each app is kept in its own top-level folder. Work inside one app should not affect the others as long as each app keeps its own Gradle project files and Android app module.

## Working On One App Only

Use Git sparse checkout when you only want one app folder locally.

```bash
git clone --filter=blob:none --sparse https://github.com/ranjanvipul88/android_apps.git
cd android_apps
git sparse-checkout set chatdock-web
```

To switch to another app later:

```bash
git sparse-checkout set another-app-folder
```

## Adding Future Apps

Create each new app as a top-level folder:

```text
android_apps/
  chatdock-web/
  my-next-app/
  another-android-app/
```

Recommended rules:

- Keep each app self-contained.
- Include a project-level `README.md` in every app folder.
- Include a Gradle wrapper per app if the app is independently buildable.
- Do not commit generated folders such as `.gradle/`, `build/`, APK outputs, or IDE caches.
