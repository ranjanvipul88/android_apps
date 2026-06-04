# QA Test Plan

## Automated

- `:core:domain:test`: rule matching, schedule matching, template rendering.
- `:core:data:test`: entity mapper round trips.
- `:app:connectedDebugAndroidTest`: Compose empty state, rule list, privacy screen.
- `lintRelease`: manifest, permission, and Android API checks.

## Manual Android 11+

1. Install debug build on Android 11 or newer.
2. Grant SMS and notification permissions only when prompted.
3. Create a disabled starter HTTPS rule.
4. Toggle the rule on and off.
5. Send a test SMS from another phone or emulator.
6. Confirm the event appears in History.
7. Configure a mock HTTPS endpoint and confirm delivery attempt logging.
8. Enable notification access and select one test package.
9. Confirm only selected app notifications are processed.
10. Reboot the device and confirm rules remain configured.

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
