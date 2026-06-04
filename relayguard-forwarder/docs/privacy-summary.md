# Privacy Summary

RelayGuard processes message content only to perform user-configured forwarding rules.

## Data Collected

- SMS sender, body, timestamp, and optional SIM slot.
- Selected notification package, title, text, and timestamp.
- User-created filters, recipients, templates, schedules, and delivery settings.
- Delivery attempt status and short local previews for troubleshooting.

## Why It Is Collected

- To match messages against enabled rules.
- To deliver selected content to destinations configured by the user.
- To show local history and help diagnose failed delivery.

## Storage

- Structured records are stored in local Room database tables.
- Secrets such as webhook tokens and SMTP passwords are stored with Android Keystore-backed encrypted preferences.
- No APK-derived credentials or vendor endpoints are included.

## Transmission

- Network delivery uses HTTPS by default.
- SMS delivery uses Android's standard `SmsManager`.
- Data is transmitted only to user-configured recipients.

## Retention

- Message history defaults to local retention until the user clears it.
- A production setting should allow automatic deletion after 7, 30, or 90 days.
- Exported backups should be encrypted before leaving the device.
