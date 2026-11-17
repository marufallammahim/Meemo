# Command and device verification checklist

This document is the manual test ledger for capability work. Each command must be tested on a supported emulator or device and must report the actual result.

## Foundation
- [ ] Install and launch the debug build.
- [ ] Confirm the Assistant destination renders without a crash.
- [ ] Rotate the device and verify the screen remains usable.
- [ ] Enable TalkBack and verify navigation labels.

## Device capability checks
- [ ] Battery query reports the platform battery state.
- [ ] App launch resolves only visible launchable applications.
- [ ] Browser launch resolves an HTTPS VIEW handler.
- [ ] Settings commands open the matching Android settings page.
- [ ] Torch reports unsupported when the device has no flash hardware.

## Privacy and permissions
- [ ] Voice input requests microphone permission only when invoked.
- [ ] Denied microphone permission produces a recoverable explanation.
- [ ] Accessibility remains disabled until user opt-in and disclosure.
- [ ] Root mode off results in no root process invocation.
- [ ] Clear-data action removes local history and aliases.
