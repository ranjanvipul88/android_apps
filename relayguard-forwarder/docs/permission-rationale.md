# Permission Rationale

## Requested In Manifest

| Permission | User-facing justification | Developer note |
| --- | --- | --- |
| `INTERNET` | Sends selected message events to destinations you configure. | Used by OkHttp/Retrofit for HTTPS delivery. No default production endpoint is embedded. |
| `RECEIVE_SMS` | Detects new SMS messages for enabled forwarding rules. | Incoming content is processed locally, stored in Room history, and sent only if a rule matches. |
| `SEND_SMS` | Sends SMS forwards and approved replies when configured. | Used only for SMS recipients and remote reply flows; delivery attempts are logged locally. |
| `POST_NOTIFICATIONS` | Shows foreground and delivery status notifications. | Notifications should avoid message content unless the user enables previews. |
| `RECEIVE_BOOT_COMPLETED` | Restarts enabled background processing after reboot. | Schedules an audit worker only; it does not read messages at boot. |
| `FOREGROUND_SERVICE` | Keeps long-running relay work visible to the user. | Reserved for production foreground processing on modern Android. |
| `FOREGROUND_SERVICE_REMOTE_MESSAGING` | Identifies message relay work as remote messaging. | Used for Android 14+ foreground service policy compliance. |

## Optional, Feature-Gated Permissions

| Permission | User-facing justification | Developer note |
| --- | --- | --- |
| `READ_SMS` | Imports recent SMS history when you request it. | Not required for live forwarding; import is user-initiated. |
| `READ_CONTACTS` | Displays contact names in templates and rule pickers. | Contact data stays local unless the user includes a contact token in a template. |
| Notification access | Forwards notifications from selected apps. | User must enable the listener in system settings; only allowlisted packages are processed. |
| Battery optimization exemption | Improves reliability for time-sensitive forwarding. | Should be requested only after explaining tradeoffs and never as a first-run requirement. |
