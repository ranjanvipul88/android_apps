# Security Checklist

- [x] No hardcoded production secrets.
- [x] Cleartext network traffic disabled by default.
- [x] Debug/local cleartext exception limited to localhost.
- [x] Secrets stored with Android Keystore-backed encrypted preferences.
- [x] Release minification enabled.
- [x] Manifest avoids ad ID, package-query, account, storage, and contact permissions by default.
- [ ] Add certificate pinning for first-party production APIs if a stable backend is introduced.
- [ ] Add export encryption for backup files.
- [ ] Add remote command allowlist and replay protection.
- [ ] Add duplicate-send throttling persistence.
- [ ] Add static analysis gate before release.
