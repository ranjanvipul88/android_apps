# Full Parity Audit

This audit is based on the local APK bundle `D:\SMS Forwarder 10.05.16.apk+`, its manifest, decoded resources, and decompiled high-level structure. Decompiled code was used only to understand behavior categories and architecture. No source code, resource files, unique strings, or binary assets were copied into RelayGuard.

## Implemented In RelayGuard

| APK-observed area | RelayGuard status |
| --- | --- |
| Incoming SMS broadcast forwarding | Implemented with `SmsEventReceiver`, WorkManager, rule evaluation, delivery history. |
| MMS receive surface | Implemented as MMS notice forwarding from WAP push broadcasts. Binary MMS extraction/attachment forwarding still needs carrier/device validation. |
| RCS receive surface | Implemented as best-effort handling for RCS-style broadcast payloads exposed by compatible vendor services. |
| Outgoing SMS monitoring | Implemented with `READ_SMS`-gated sent-folder scanning and periodic WorkManager scheduling. |
| Notification forwarding | Implemented with `NotificationListenerService` and shared relay dispatch. |
| Rule enable/disable/delete | Implemented in Compose rule list. |
| Event-type selection per rule | Implemented for incoming SMS, MMS notice, RCS, outgoing SMS, and app notifications. |
| Phone-number recipient | Implemented with Android `SmsManager`. |
| SMTP email recipient | Implemented with direct SMTP STARTTLS/SSL support and encrypted credential storage. |
| HTTPS webhook recipient | Implemented with Retrofit/OkHttp. |
| Telegram recipient | Implemented with Telegram Bot API `sendMessage`. |
| Slack recipient | Implemented with Slack incoming webhook JSON payload. |
| Secondary recipient/fallback | Implemented in shared dispatcher: fallback recipients are attempted when primary delivery fails. |
| Duplicate prevention | Implemented with a short-lived fingerprint guard per rule and message content. |
| Local history | Implemented for received events and delivery attempts. |
| Local backup/restore | Implemented for rules and destination secrets in app-private storage. |
| Reboot/package-update recovery | Implemented for startup audit and outgoing scan scheduling. |

## External Items That Require Owner Configuration

These cannot be made "working like the APK" without owner-provided project/account configuration. RelayGuard provides user-configurable implementations rather than copying credentials from the APK.

| Area | Required from owner |
| --- | --- |
| Telegram forwarding | Bot token and chat id. |
| Slack forwarding | Incoming webhook URL. |
| SMTP/Gmail-style email | SMTP host, port, account/app password, sender, and recipient. |
| Firebase push/account sync | A new Firebase project, google-services config, server/API contracts, and auth policy. |
| Microsoft sign-in/Graph-style flows | New Azure app registration, redirect URI, scopes, and tenant policy. |
| Google Drive/cloud backup | New Google Cloud OAuth client and Drive scopes. |
| Play Billing/RevenueCat/ads | Intentionally not implemented; RelayGuard is free and ad-free by product decision. |

## Still Not Claimed As Exact

The APK includes cloud account flows, PC sync, app lock, widgets, support/contact screens, and richer MMS/attachment UI. Those require additional implementation and, for cloud features, owner-provided credentials and policies. Premium, billing, rewards, and ads are intentionally excluded so RelayGuard remains free and ad-free.
