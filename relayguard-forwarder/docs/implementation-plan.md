# Implementation Plan

## Milestone 1: Discovery And Spec

Estimate: 1-2 engineering days.

Delivered here:
- High-level APK analysis report.
- Feature parity checklist.
- Data model summary.
- Legal safety assumptions.

Acceptance tests:
- No decompiled source or resources are committed.
- Analysis notes avoid unique strings, credentials, and copied assets.
- Third-party SDKs are flagged with replacement decisions.

## Milestone 2: Design

Estimate: 2-3 engineering days.

Delivered here:
- Compose Material 3 style guide.
- Exported SVG mockups for primary screens.
- Accessibility requirements for contrast, labels, and navigation.

Acceptance tests:
- Main screens have empty/loading/error states.
- Text meets contrast targets.
- Touch targets are at least 48dp.

## Milestone 3: Core Implementation

Estimate: 5-8 engineering days.

Delivered here:
- Kotlin/Compose project with clean module boundaries.
- Hilt DI, repository interfaces, Room storage, Retrofit/OkHttp transports.
- Domain unit tests and UI smoke test.

Acceptance tests:
- Rule evaluator matches message type, conditions, schedule, and enabled state.
- Template renderer substitutes original RelayGuard tokens.
- Room mappers round-trip filter records.

## Milestone 4: Integration And Polish

Estimate: 6-10 engineering days.

Remaining work:
- Complete filter editor.
- Add retry/fallback orchestration.
- Add SMTP/Gmail account setup using app-owned OAuth credentials.
- Add notification package allowlist.
- Add remote reply and activation command parser.
- Add backup/export/import.
- Add tablet layout and animation polish.

Acceptance tests:
- SMS-to-webhook and SMS-to-SMS flows pass on Android 11+.
- Notification forwarding is opt-in per package.
- Secrets are never logged or exported in plaintext.

## Milestone 5: Testing And Release

Estimate: 3-5 engineering days.

Remaining work:
- Instrumented tests on Android 11, 13, 15, and 16.
- Lint and static analysis gates.
- Release signing outside the repository.
- Performance profiling screenshots for rules and history screens.

Acceptance tests:
- `test`, `connectedDebugAndroidTest`, `lintRelease`, and `bundleRelease` pass.
- Domain/data coverage is at least 70%.
- Originality scan is clean.
- Permission rationale and privacy docs match the manifest.
