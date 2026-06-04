# Inspiration Log

| Learned from APK | RelayGuard decision | Difference |
| --- | --- | --- |
| Rule-based forwarding is the core workflow. | Built an original `ForwardFilter` model and evaluator. | New names, data structure, UI, and matching implementation. |
| Filters can target several message sources. | Supports SMS, MMS notice, outgoing SMS, and app notifications as enum categories. | Uses new enum names and a smaller launch scope. |
| Messages can be sent to phone numbers, email, URLs, and chat webhooks. | Added recipient types and a transport dispatcher. | Uses app/user-provided endpoints and no copied vendor endpoints. |
| Users need history when rules fail. | Added local event and relay attempt tables. | New schema and concise history UI. |
| Users may need replacement/template tokens. | Added `{{sender}}`, `{{message}}`, `{{time}}`, and related RelayGuard tokens. | Token syntax and renderer are original. |
| Remote reply and remote activation are useful. | Documented secure command/parser plan. | Not copied; implementation will require explicit allowlists and audit logs. |
| PC sync can improve desktop workflows. | Planned a local-only HTTPS web console. | No bundled web assets or copied UI. |
| Premium/ads were prominent. | Excluded ads/paywall from initial app. | Privacy-first, no ad SDKs, no reward timers. |
| Backup/restore exists. | Planned encrypted local export/import before cloud integrations. | No copied Google Drive implementation or credentials. |
| Many permissions were declared. | Reduced manifest to launch-critical permissions only. | Optional permissions are documented and requested only when a feature is enabled. |
