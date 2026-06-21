# SMS Backup & Safe Forwarding App

A privacy-first, minimal-permission Android application built with modern Kotlin and Jetpack Compose. This application operates as a secure, local, encrypted SMS vault with the ability to sync historical messages, backup to the cloud, and safely forward incoming alerts based on custom rules. 

The app is fully compliant with Google Play Store SMS Policies, integrating strict, non-bypassable security checks to guarantee that banking notices, transactions, and OTPs are **never** forwarded out of the secure environment.

---

## 📋 Table of Contents
1. [Core Features](#-core-features)
2. [Security Review Checklist](#%EF%B8%8F-security-review-checklist-banking-safety)
3. [Play Store Listing Specification](#-play-store-listing-specification)
4. [On-Device Privacy & Disclosures Policy](#-on-device-privacy--disclosures-policy)
5. [Technical Architecture & Specifications](#-technical-architecture--specifications)
6. [Compilation & Publishing Instructions](#-compilation--publishing-instructions)

---

## ✨ Core Features

*   **🔒 The Cipher Vault (AES-256-GCM)**: Every incoming or historically synced text message is immediately encrypted on the device using Android's hardware Keystore before writing to the local Room database. 
*   **📡 Safe Authorized Forwarding Matrix**: Forwarding rules execute strictly on explicit triggers (e.g., "From specific contact/mobile" forwarding to a dedicated number). Toggles allow immediate revokes and include an instant "Undo" recovery feature.
*   **🛡️ Dynamic Banking & OTP Guardrails**: Features a non-bypassable checking engine with regex heuristics and keyword databases that filters out transaction PINs, verification OTPs, two-factor authentication formats, bank account debits, and credit updates.
*   **☁️ Dual Backup Infrastructure**: Export your entire Cipher Vault and rules matrix directly to an offline JSON file via Android SAF, or authenticate seamlessly via Google Sign-In to sync the encrypted archive to your private Google Drive.

---

## 🛡️ Security Review Checklist (Banking Safety)

To prevent security vulnerabilities, malicious usage, or the inadvertent collection of sensitive credentials, the application implements several non-bypassable defense mechanisms:

| Defense Component | Implementation Mechanism | Play Store Policy Alignment |
| :--- | :--- | :--- |
| **Monetary Alert Exclusions** | Scans for monetary signs ($ RM USD EUR) accompanied by numeral amounts. Restricts automatic routing of credit/debit records. | High-risk financial data prevention |
| **Multi-digit Pin Quarantines** | Scans for standard 4-to-8 digit numbering arrangements situated in proximity of secure verification identifiers (PIN, Code, OTP, Confirm). | Prevent credential extraction |
| **Log Leakage Protection** | No raw plain-text message contents are recorded to audit trace logs. Senders are masked (e.g. `+155******76`) and blocking reasons are generalized. | Privacy at-rest compliance |
| **No Background Server syncs** | Transmitting matching alerts depends on native, cellular, on-device `SmsManager` utilities. Runs no web sync background sockets. | Minimal-permissions posture |
| **Biometric Master Lock** | The app's lifecycle is guarded by native Android Biometric prompts, requiring fingerprint/face authentication upon returning to the app from the background. | Physical unauthorized access prevention |

---

## 📢 Play Store Listing Specification

### App Title
`SMS Backup & Safe Forward`

### Short Description
`Locally encrypted SMS back-up with user-authorized, OTP-safe forwarding filters.`

### Full Description
```text
SMS Backup & Safe Forward is a privacy-first utility designed to help families, professionals, and developers manage backups and authorized routing securely.

🔒 THE CIPHER VAULT
All backing up messages undergo on-device AES-256-GCM encryption before committing to local database storage. Encryption keys remain insulated inside the Android hardware Keystore. Sync thousands of historical SMS messages directly into the vault.

☁️ DUAL BACKUPS
We believe you own your data. Backup your entire encrypted vault seamlessly to Google Drive, or export it totally offline to your device's Downloads folder as a portable file. 

📡 TRANSPARENT DYNAMIC FORWARDING
Establish intentional routing rules (for example, forwarding texts from a specific emergency relative to an alternative active mobile line). No automatic covert transmissions can trigger. 

🛡️ PATENT BANKING & OTP SHIELD
Built with automated, non-bypassable banking safety filters, our security parser checks incoming texts for patterns: Credit/Debit transactional notices, 2FA code layouts, verification OTPs, passwords, and security PINs are caught and filtered out by default.

📂 LOG ACCOUNTABILITY
A chronological on-device security audit trace displays every operation context. Instantly analyze which message was securely backed up, which filter was mapped, or which OTP was proactively quarantined.
```

---

## 📜 On-Device Privacy & Disclosures Policy

### 1. Scope of Collection
*   **SMS Messages**: Read access is restricted entirely to analyzing the message sender and body locally. Encryption runs at rest.
*   **Third-Party Transfers**: Zero network metrics or plain text bodies are sent to third parties. We do not use any external web hosting analytics or metric libraries.
*   **Revocation**: You retain absolute control over critical permissions. Wiping app state via the "Purge Workspace" deletes all database rows instantly.

---

## 🛠️ Technical Architecture & Specifications

The codebase utilizes standard modern Android architecture:
*   **Jetpack Compose**: Pure declarative Material 3 interface layer.
*   **Room SQL Databases**: Sandboxed SQLite storage using Kotlin coroutines `Flow` models.
*   **Android KeyStore**: Generates Symmetric AES keys safely within hardware security modules.

```
/app/src/main/java/com/example/
├── MainActivity.kt                # App initialization
├── data/
│   ├── Database.kt                # SQL Entities, DAOs, and database configurations  
│   ├── EncryptionHelper.kt        # AES-256-GCM Cryptographic system
│   └── Repository.kt              # Combines Room streams and encryption routines
├── receiver/
│   └── SmsReceiver.kt             # Handles SMS_RECEIVED broadcast notifications
├── security/
│   └── SmsSecurityAnalyzer.kt     # Financial/OTP pattern detection parser
└── ui/
    ├── SmsDashboardMain.kt        # Entire Material 3 dashboard, Vault, & Dual Backup logic
    └── SmsViewModel.kt            # LiveState flows and Background Sync Engine
```

---

## 📦 Compilation & Publishing Instructions

### 1. Setting Up Gradle Keys for Play Store
To produce signed Release-ready APKs/AABs for Play Store submission:
1.  Use the `safeforward_key` keystore file located in the root directory.
2.  Google Cloud Console: Configure your Android Client ID using the SHA-1 footprint of this keystore to activate Google Drive sync capabilities.

### 2. Building via command line
To compile an APK for local testing:
```bash
gradle assembleDebug
```
The output debug APK will be generated at `/app/build/outputs/apk/debug/app-debug.apk`. 

To compile a signed release bundle (AAB):
```bash
gradle bundleRelease
```
The optimized bundle will be generated at `/app/build/outputs/bundle/release/app-release.aab`.
