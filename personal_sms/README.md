# Google Messages SMS/MMS Replica: Production-Grade Suite

This isolated directory houses the complete codebase for a highly secure, modern, and production-ready replica of **Google Messages**. 

It is divided into two distinct, standalone components designed for seamless coordination:
1. **`android/` (Native Kotlin App)**: A modern, native Android app built using **Kotlin, Jetpack Compose, Room DB, WorkManager, and Ktor**. Engineered to serve as the **Default SMS Handler**, authorizing direct system-level access to intercept, read, send, and style messages on the Google Play Store.
2. **`web-portal/` (Cloud Sync Backend & Web Portal)**: A full-stack Node.js Express server backed by an SQLite database. Houses a premium, responsive **Material You Web Dashboard** that synchronizes all phone SMS/MMS messages. Configured with a **99-year session token** (never times out or signs you out, even if accessed only once a year) and allows message reading and control even when the phone is offline.

---

## Technical Features

* **Default SMS Handler Architecture**: Configured with standard manifest intent-filters (`SMS_DELIVER`, `WAP_PUSH_DELIVER`, `RESPOND_VIA_MESSAGE`) to replace the default messaging app.
* **Auto-Forwarding Background Service**: Intercepts cellular SMS in the background using a `BroadcastReceiver`. Automatically routes the text content to a target number using `SmsManager` without needing active app displays.
* **Dynamic Material You Accent Theming**: Supports the Android 12+ wallpaper extraction color engine, adapting the theme colors of the Android Compose layout dynamically.
* **99-Year Longevity Session Portal**: Web Portal sessions utilize robust JWT encryption configured with a 99-year longevity limit (`expiresIn: "99y"`). Sessions persist forever unless manually logged out.
* **Off-Line Web Reading**: Rather than relying on direct on-device connections, the phone syncs messages to the SQLite cloud database in the background whenever message events occur. If you log in once a year, you instantly load the logs without needing the phone online.
* **Double-Sided Mock Simulators**:
  - **Android Developer Mode**: Let's you simulate incoming SMS from Mom or Boss directly in the UI to test receivers, auto-forwarding, and database syncs.
  - **Web SMS Simulator Panel**: Simulates phone-level incoming cellular texts and media directly in the browser dashboard.

---

## Directory Organization

```
d:/Android_Apps/google-messages-replica/
├── android/                   # Full Native Kotlin Android Studio Gradle Project
│   ├── app/
│   │   ├── build.gradle       # App compilation packages
│   │   └── src/main/
│   │       ├── AndroidManifest.xml # Core permissions & Default SMS configs
│   │       ├── java/com/vipul/messages/smsmms/
│   │       │   ├── MainActivity.kt # Compose App UI, triggers, permissions
│   │       │   ├── data/           # Rooms Local DB Caching
│   │       │   ├── receiver/       # SMS & MMS Broadcast Receivers (Auto-forwarding)
│   │       │   ├── service/        # Headless Send Service (mandatory for OS)
│   │       │   └── worker/         # WorkManager API Sync Manager
│   │       └── res/                # XML Layouts, dynamic Vector SMS/MMS icon
│   └── build.gradle           # Root Gradle definition
└── web-portal/                # Persistent Cloud Sync & Web Dashboard
    ├── server.js              # Express API Server (sqlite3 database, pair endpoints)
    ├── package.json           # Node packages
    └── public/                # Web Dashboard Frontend
        ├── index.html         # Premium Material 3 Dashboard View
        ├── css/style.css      # Custom animations, chat bubbles, toggle switch stylings
        └── js/dashboard.js    # Sync orchestrator, simulators, starred managers
```

---

## Deployment & Running Guidelines

### 1. Compiling and Deploying the Android Application
To open, test, and package the Android app:
1. Launch **Android Studio**.
2. Select **Open Project** and navigate to `d:\Android_Apps\google-messages-replica\android`.
3. Allow Gradle to synchronize dependencies (Compose BOM 2023.10.01, Room 2.6.1, Ktor 2.3.7, WorkManager 2.9.0).
4. Run the application on an Android Emulator or a real device connected via USB.
5. **Set as Default SMS App**: Upon launch, the app detects if it is the default SMS application. Click **"Set"** on the warning banner to invoke the standard OS dialog and make it your primary messaging client.
6. **Play Store Compilation**: To bundle a production package ready for Play Store console submission:
   - In Android Studio, go to **Build** > **Generate Signed Bundle / APK**.
   - Select **Android App Bundle (AAB)** and follow standard keystore credentials packaging.

### 2. Setting Up the Web Access Portal
To spin up the web dashboard database backend locally:
1. Ensure Node.js is installed. Open a terminal and navigate to the web-portal:
   ```bash
   cd d:\Android_Apps\google-messages-replica\web-portal
   ```
2. Set PowerShell Execution Policy to bypass if you encounter signing issues on Windows:
   ```powershell
   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
   ```
3. Install dependencies:
   ```bash
   npm install
   ```
4. Start the server:
   ```bash
   npm start
   ```
5. Open your web browser and navigate to **`http://localhost:3000`**.

---

## Step-by-Step Sync & Auto-Forwarding Walkthrough

Let's test all features end-to-end without needing real cellular carrier SIM cards:

### Step A: Log In to the Web Portal
1. Navigate to `http://localhost:3000` on your browser.
2. In the **Web Login** tab, log in with the seeded developer credentials:
   - **Email**: `admin@messages.sms`
   - **Password**: `admin123`
3. Click **Log In**. You are taken to the dashboard. The status bar will show "Waiting for phone..." with a grey battery indicator.

### Step B: Pair the Android Phone
1. In the Web Portal header, click on **Pair Android Phone** or check your pairing key (e.g. `MSG-582910`).
2. Open the paired Android App on your emulator/device. Navigate to the **Settings** navigation tab.
3. Keep the **Cloud Sync Server URL** as `http://localhost:3000` (or your hosted IP).
4. Enter the **Pairing Key** displayed on the Web Screen, and click **Pair Device**.
5. The phone connects! The Web Portal instantly switches to the dashboard interface. The header indicator displays **"Phone Sync Online"** alongside active battery percentages and signal strength!

### Step C: Configure Auto-Forwarding Remotely
1. On the Web Portal Dashboard, click the **Shortcut (Auto-Forward)** icon in the top header. The Auto-Forwarding drawer slides in.
2. Check the box to **"Enable Auto-Forwarding"**.
3. Input a target forwarding number (e.g., `+15550009999`) and click **Save Settings**.
4. Saving this updates the database. The phone automatically fetches this during its sync ticks and updates its local shared preferences. (Alternatively, you can toggle this directly on the phone's Settings screen!).

### Step D: Trigger Simulated SMS and MMS Traffic
1. On the Web Portal Dashboard, click the **Terminal (Simulator)** icon in the top header. The SMS Traffic Simulator drawer slides in.
2. Choose a preset contact (e.g., **Mom ❤️**).
3. Choose **SMS (Text)** and leave the message body as: *"Hi honey! Hope you are having a wonderful day! I baked a cherry pie! 🥧"*
4. Click **Trigger Incoming SMS**.
5. **The Magic Happens**:
   - The simulator inserts the incoming text directly into the database.
   - The backend checks forwarding configurations, sees that forwarding is enabled, and automatically creates an outgoing SMS forward text to your target number (`+15550009999`) saying: *`[FWD from Mom ❤️]: Hi honey! Hope you...`*
   - The simulation logs window instantly updates to display the intercepted signal and the successful auto-forward routing!
   - The chat thread view instantly lists the incoming text and the corresponding auto-forwarded text, complete with delivery checkmarks!
6. Try triggering an **MMS (Photo)** message preset: Mom will send a gorgeous picture bubble containing a cherry pie, rendering on both your mobile frame and desktop synced screen in real-time!
