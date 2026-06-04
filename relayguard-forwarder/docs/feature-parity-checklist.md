# Feature Parity Checklist

| Area | Observed target behavior | RelayGuard implementation | Status |
| --- | --- | --- | --- |
| Incoming SMS forwarding | Detect new SMS and route through rules | `SmsEventReceiver`, `InboundMessageWorker`, rule evaluator, real relay attempts | Implemented |
| MMS handling | Settings/test surface for multimedia messages | WAP push MMS notices feed the relay engine; binary attachment extraction still requires carrier/device validation | Implemented for notices |
| Notification/RCS forwarding | Notification listener for selected apps/RCS-like content | Notification listener and RCS-style broadcast intake feed the shared relay engine | Implemented |
| Outgoing SMS forwarding | Monitor outgoing messages | `READ_SMS`-gated sent-folder scanner with WorkManager scheduling | Implemented |
| Rule CRUD | Add/edit/delete/toggle filters | Room repository and Compose create/toggle/delete surfaces | Implemented except deep edit screen |
| Conditions | Sender/body/title/package/SIM matching | Domain condition evaluator | Implemented |
| Regex/replacements | Transform forwarded text | Domain template renderer with regex replacement | Implemented |
| Recipients | SMS, email, URL, chat webhooks, fallback | SMS, SMTP, HTTPS webhook, Telegram Bot API, and Slack incoming webhook transports | Implemented for configured destinations |
| Secondary fallback | Try alternate recipient on failure | Shared dispatcher sends fallback recipients after primary failure | Implemented |
| Duplicate prevention | Avoid repeated sends | Shared dispatcher uses short-lived per-rule message fingerprints | Implemented |
| Working-time schedule | Restrict rule by time/day | Domain schedule model | Implemented |
| Remote reply | Reply from forwarded destination | Data model and UX plan; secure command parser planned | Planned |
| Remote activation | Enable/disable filters remotely | UX and security plan; command receiver planned | Planned |
| Backup/restore | Cloud or file backup | Local app-private backup/restore for rules and destination secrets | Implemented locally |
| PC sync | Local browser access | HTTPS-first local web plan; not included in skeleton | Planned |
| App lock | PIN/biometric lock | Security checklist and planned UI | Planned |
| Widget | Toggle a rule from launcher | Planned with Glance widget | Planned |
| Premium/ads | Paywall/reward flows | Intentionally excluded for legal/privacy simplicity | Different by design |

## Acceptance Tests By Milestone

- Milestone 1: Analysis report contains features, flows, data categories, endpoint families, assumptions, and legal exclusions.
- Milestone 2: Style guide and mockups cover rules, history, privacy, and settings.
- Milestone 3: Unit tests pass for rule matching and template rendering; app opens to Compose shell.
- Milestone 4: SMS events persist locally and dispatch to real SMS, SMTP, HTTPS, Telegram, and Slack destinations when configured.
- Milestone 5: Release build, privacy docs, originality scan, manual QA checklist, and signed AAB process are complete.
