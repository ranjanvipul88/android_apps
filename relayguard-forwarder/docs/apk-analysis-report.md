# APK Analysis Report

## Clean-Room Boundary

The APK was inspected only to understand high-level behavior. Decompiled code, resources, layouts, icons, filenames, unique strings, API secrets, and visual assets were not copied into RelayGuard.

## Source Artifact

- Local artifact: user-provided APK bundle on `D:\`
- Version observed: 10.05.16
- Analysis method: manifest/resource decode, high-level screen/resource inventory, partial source decompile for architecture and data categories
- Temporary analysis output: `_analysis_tmp/`, ignored by Git

## Observed Feature Categories

- Incoming SMS detection and forwarding.
- MMS-related setup and test flows.
- RCS/notification-assisted forwarding through notification access.
- Outgoing SMS monitoring through a foreground/background processing flow.
- Rule/filter management with enable, disable, ordering, conditions, and history.
- Recipient routing to phone number, email, HTTPS URL, Telegram-style bot endpoint, Slack-style webhook, and push/account-based destinations.
- Primary and secondary recipients for fallback delivery.
- Message template tokens, text replacement, regex-like matching, and duplicate prevention.
- Sender/body/title/package/SIM based conditions.
- Working-time or schedule based rule activation.
- Remote reply and remote activation via trusted command messages.
- App lock, PIN reset, account login, backup/restore, support/contact, update notice, premium/paywall, rewards/ads, widgets, PC sync, and advanced settings.

## Observed Data Model Categories

- Filter/rule records.
- Incoming and outgoing message history.
- Notification history.
- Chat-style remote reply message records.
- Pending or attempted delivery records.
- Worker/background job payload records.

RelayGuard maps these categories to original Room tables: `filters`, `message_events`, and `relay_attempts`.

## Observed External Integration Categories

- HTTPS webhook delivery.
- Gmail/SMTP-style email delivery.
- Telegram-style bot delivery.
- Slack-style webhook delivery.
- Firebase/Auth/Functions/Messaging style account and push workflows.
- Google Drive style backup.
- Billing/subscription and ad/reward SDKs.
- Local browser/PC sync server.

Exact endpoint URLs, API keys, ad identifiers, OAuth client values, and vendor strings were intentionally not reused. RelayGuard uses new placeholder endpoints, user-provided credentials, and original configuration.

## Third-Party/Licensed Components Observed

- AndroidX, Compose, Room, WorkManager, Material components.
- Firebase and Google API libraries.
- Microsoft identity libraries.
- RevenueCat and Google Billing.
- Multiple ad mediation SDKs.
- YubiKey/YubiKit components.
- OkHttp/Retrofit-like networking libraries.

Replacement approach: RelayGuard uses permissively licensed AndroidX, Hilt, Room, Retrofit, OkHttp, and Android Security Crypto. Ads, billing, Firebase account sync, and vendor-specific support endpoints are excluded from the first production version unless separately configured with proper licenses and app-owned credentials.

## Assumptions

- Minimum SDK defaults to API 26 as requested.
- The initial production target is Android 16/API 36.
- RelayGuard should not include ads, reward timers, or paywalls in the initial release.
- Email, chat, and cloud backup credentials must be supplied by the app owner or end user; no credentials are embedded.
- RCS forwarding can only be approximated through notification access because Android does not expose full carrier RCS APIs to ordinary apps.
