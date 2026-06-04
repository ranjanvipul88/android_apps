# QA Test Plan

## Automated

- `:core:domain:test`: rule matching, schedule matching, template rendering.
- `:core:data:test`: entity mapper round trips.
- `:app:connectedDebugAndroidTest`: Compose empty state, rule list, privacy screen.
- `lintRelease`: manifest, permission, and Android API checks.

## Manual Android 11+

1. Install debug build on Android 11 or newer.
2. Grant SMS and notification permissions only when prompted.
3. Open Settings and create an SMS forwarding rule with a second phone number controlled by the tester.
4. Send a test SMS from another phone or emulator and confirm the second phone receives the forwarded message.
5. Confirm the original message and the successful SMS delivery attempt appear in History.
6. Create an email rule with a test SMTP account or app password, then send another SMS and confirm the email arrives.
7. Create a WhatsApp Business rule with a test Meta Cloud API phone number, messages endpoint, recipient phone, and bearer token; confirm the API accepts the message.
8. Toggle each rule off and confirm no delivery attempts are created for disabled rules.
9. Enable notification access and select one test package.
10. Confirm only selected app notifications are processed.
11. Reboot the device and confirm rules remain configured.

## Emulator SMS Injection

Use an Android emulator with the app installed and permissions granted:

```powershell
adb emu sms send 15551230000 "RelayGuard smoke test"
```

The message should appear in History. Delivery requires at least one enabled forwarding rule.

## Error States

- No telephony hardware.
- Missing SMS permission.
- Invalid webhook URL.
- TLS/network failure.
- SMS send failure.
- Notification listener disabled.
- Empty message body.
- Duplicate message burst.

## Performance

- Rules screen should open in under 500 ms with 100 rules.
- History should scroll smoothly with 1,000 local rows.
- Background processing should complete a simple single-recipient SMS event in under 2 seconds on a mid-range Android 11 device.

Profiling snapshots should be captured with Android Studio Profiler before release.
