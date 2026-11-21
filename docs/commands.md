# Meemo command and device verification checklist

This document is the manual test ledger for capability work. Each command must be tested on a supported emulator or device and must report the actual result.

## Phase 2 supported text commands

| User text | Parsed intent | Expected behavior |
| --- | --- | --- |
| battery, check battery, battery status | GetBattery | Report the current battery percentage |
| open browser, browse internet | OpenBrowser | Show Android's browser chooser for an HTTPS link |
| open Chrome, open Calculator | OpenApp | Launch a uniquely matched visible launcher app |
| open Wi-Fi settings | OpenSettings(WIFI) | Open Android Wi-Fi settings |
| open Bluetooth settings | OpenSettings(BLUETOOTH) | Open Android Bluetooth settings |
| open display settings | OpenSettings(DISPLAY) | Open Android display settings |
| open sound settings | OpenSettings(SOUND) | Open Android sound settings |
| open battery settings | OpenSettings(BATTERY) | Open Android battery saver settings |
| open accessibility settings | OpenSettings(ACCESSIBILITY) | Open Android accessibility settings |
| open notification settings | OpenSettings(NOTIFICATIONS) | Open this app's notification settings |
| whenever I say work open Chrome | DefineAlias | Save a reusable app-launch alias locally |

Wake phrases such as “Hey Meemo” and polite filler such as “please” are removed before parsing. Common verb and device synonyms are canonicalized. Fuzzy intent matches carry lower confidence and are routed to confirmation when confidence is below 0.8.

## Saved automation routines

The Memory & Aliases screen can create a sample routine. A routine is stored locally with a display name, trigger phrase, ordered steps, enabled state, and confirmation preference.

- Say `run routine <name>` or `run routine <trigger>` to execute an enabled saved routine.
- Routine lookup accepts either the saved display name or its trigger phrase, ignoring English letter case.
- Supported step IDs are `TORCH_ON`, `TORCH_OFF`, `GET_BATTERY_STATUS`, `GET_DEVICE_INFORMATION`, `SET_VOLUME`, `ADJUST_VOLUME`, and `MEDIA_PLAY`, `MEDIA_PAUSE`, `MEDIA_NEXT`, `MEDIA_PREVIOUS`.
- `SET_VOLUME` requires an integer percentage from 0 through 100. `ADJUST_VOLUME` requires `UP`, `DOWN`, `MUTE`, or `UNMUTE`.
- Unsupported, invalid, root, alias-writing, and nested automation steps are rejected before any routine step is dispatched.
- The saved `requireConfirmation` setting is honored by the normal assistant confirmation flow.

## Phase 2 manual emulator verification

- [ ] Type open browser; verify Android displays a browser chooser or opens the configured browser.
- [ ] Type battery; verify Meemo reports a percentage between 0 and 100.
- [ ] Type open Wi-Fi settings; verify Android's Wi-Fi settings page opens.
- [ ] Type open Bluetooth settings; verify Android's Bluetooth settings page opens.
- [ ] Type open an-app-that-is-not-installed; verify Meemo returns a clear unsupported message.
- [ ] Type an unclear phrase; verify Meemo does not execute an arbitrary action.
- [ ] Type Hey Meemo, please open browser; verify wake phrase and filler removal.

## Automation manual verification

- [ ] Create a sample routine with a unique name and trigger phrase in Memory & Aliases.
- [ ] Run it by its display name and then by its trigger phrase; verify the stored steps run in order.
- [ ] Create a routine with confirmation enabled; verify execution waits for the assistant confirmation flow.
- [ ] Verify an unknown routine and a routine with an unsupported step fail without executing any step.

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

## Safety and platform limits

- App discovery is restricted by Android package visibility and the manifest's launcher query; the app does not request QUERY_ALL_PACKAGES.
- Browser launch uses an Android ACTION_VIEW intent and does not inspect browsing history.
- Battery status is read through BatteryManager.
- OEMs may omit or customize individual settings destinations.
- Root diagnostics remain a separate explicitly confirmed capability and cannot be embedded in a routine.
- Emulator/device interaction must be verified manually; CI unit tests cannot prove OEM-specific intent resolution.
