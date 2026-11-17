## Summary

Describe the change and the phase it belongs to.

## Architecture and security
- [ ] Dependency direction is preserved.
- [ ] No new permission lacks a documented feature.
- [ ] User data and credentials are not logged.
- [ ] Root operations, if any, use approved typed commands only.

## Verification
- [ ] `./gradlew assembleDebug`
- [ ] `./gradlew lint`
- [ ] `./gradlew detekt`
- [ ] `./gradlew ktlintCheck`
- [ ] Relevant unit and UI tests
