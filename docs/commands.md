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
| whenever I say work open Chrome | DefineAlias | Parse the alias definition; persistence is a later phase |

Wake phrases such as “Hey Meemo” and polite filler such as “please” are removed before parsing. Common verb and device synonyms are canonicalized. Fuzzy intent matches carry lower confidence and are routed to confirmation when confidence is below 0.8.

## Phase 2 manual emulator verification

- [ ] Type open browser; verify Android displays a browser chooser or opens the configured browser.
- [ ] Type battery; verify Meemo reports a percentage between 0 and 100.
- [ ] Type open Wi-Fi settings; verify Android's Wi-Fi settings page opens.
- [ ] Type open Bluetooth settings; verify Android's Bluetooth settings page opens.
- [ ] Type open an-app-that-is-not-installed; verify Meemo returns a clear unsupported message.
- [ ] Type an unclear phrase; verify Meemo does not execute an arbitrary action.
- [ ] Type Hey Meemo, please open browser; verify wake phrase and filler removal.

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
- This phase does not implement alias storage, voice input, root commands, shell execution, Wi-Fi/Bluetooth toggles, or arbitrary system control.
- Emulator/device interaction must be verified manually; CI unit tests cannot prove OEM-specific intent resolution.
