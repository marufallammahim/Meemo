# Supported Deterministic Commands (`docs/commands/SUPPORTED_COMMANDS.md`)

| Intent | English Examples | Bengali / Banglish Examples | Execution Path |
| :--- | :--- | :--- | :--- |
| `OPEN_APPLICATION` | `"Open YouTube"`, `"Launch Calculator"` | `"ইউটিউব খোলো"`, `"Chrome kholo"` | `PackageResolver` -> `OpenAppActionExecutor` |
| `OPEN_BROWSER` | `"Open my browser"`, `"Open browser"` | `"ব্রাউজার খোলো"`, `"আমার ব্রাউজার চালু করো"` | `BrowserResolver` -> `OpenBrowserActionExecutor` |
| `TORCH_ON` | `"Turn on the flashlight"`, `"Torch on"` | `"ফ্ল্যাশলাইট চালু করো"`, `"টর্চ জ্বালাও"` | `TorchCapabilityAdapter` -> `CameraManager.setTorchMode(true)` |
| `TORCH_OFF` | `"Turn off the flashlight"`, `"Torch off"` | `"ফ্ল্যাশলাইট বন্ধ করো"`, `"টর্চ নেভাও"` | `TorchCapabilityAdapter` -> `CameraManager.setTorchMode(false)` |
| `GET_BATTERY_STATUS` | `"What is my battery status?"`, `"Battery level"` | `"ব্যাটারি কত?"`, `"চার্জ কত"` | `DeviceInfoActionExecutor.readBatteryStatus()` |
| `GET_DEVICE_INFORMATION` | `"Device info"`, `"Android version"` | `"ডিভাইস তথ্য"`, `"ফোনের তথ্য"` | `DeviceInfoActionExecutor.readDeviceInformation()` |
| `SET_VOLUME` | `"Set volume to 50 percent"`, `"Volume 70%"` | `"ভলিউম ৫০ শতাংশে সেট করো"` | `VolumeMediaActionExecutor.executeSetVolume()` |
| `ADJUST_VOLUME` | `"Turn up the volume"`, `"Mute volume"` | `"ভলিউম বাড়াও"`, `"ভলিউম কমাও"` | `VolumeMediaActionExecutor.executeAdjustVolume()` |
| `MEDIA_PLAY` / `PAUSE` / `NEXT` / `PREVIOUS` | `"Play music"`, `"Pause media"`, `"Next song"` | `"গান চালাও"`, `"গান থামাও"`, `"পরের গান"` | `AudioManager.dispatchMediaKeyEvent()` |
| `OPEN_SETTINGS` | `"Open Wi-Fi settings"`, `"Open Bluetooth settings"` | `"ওয়াইফাই সেটিংস খোলো"`, `"ব্লুটুথ খোলো"` | `SettingsActionExecutor` |
| `READ_NOTIFICATIONS` | `"Read my notifications"`, `"Read messages"` | `"নোটিফিকেশন পড়ো"`, `"নতুন মেসেজ শোনাও"` | `NotificationReadAloud.readQueuedNotificationsSummary()` |
| `STOP_SPEAKING` | `"Stop speaking"`, `"Be quiet"` | `"বলা বন্ধ করো"`, `"থামো"` | `TtsController.stopSpeaking()` |
| `CANCEL_COMMAND` | `"Cancel"`, `"Cancel command"` | `"বাতিল করো"`, `"বাদ দাও"` | Cancels active voice or confirmation state |
| `CREATE_ALIAS` | `"Whenever I say work browser open Firefox"` | `"যখন আমি বলব অফিস ব্রাউজার তখন Firefox খোলো"` | Saves alias in `custom_command_aliases` |
| `RUN_AUTOMATION` | `"Run routine bedtime"` | `"রুটিন চালু করো bedtime"` | Executes multi-step `MeemoAction.ExecuteAutomationRoutine` |
| `ROOT_SYSTEM_DIAGNOSTIC` | `"Run root battery dump"`, `"Root thermal status"` | `"রুট ডায়াগনস্টিক"` | Requires confirmation + `RootCommandPolicy` allowlist |
