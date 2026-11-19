# Permission and special-access inventory

Permissions are requested only when the related feature is invoked and after an in-app explanation.

| Permission or access | Feature | Policy |
| --- | --- | --- |
| RECORD_AUDIO | Voice input | Request at the moment voice input is started |
| POST_NOTIFICATIONS | Listening notification | Request on Android 13+ when needed |
| FOREGROUND_SERVICE | Optional listening service | Declare only with the service implementation |
| FOREGROUND_SERVICE_MICROPHONE | Optional wake-word service | Respect Android foreground-service restrictions |
| BLUETOOTH_CONNECT | Bluetooth capability, if implemented | Request only when the feature uses it on API 31+ |
| Notification listener access | Media/notification integration | User enables it in Android Settings |
| Accessibility service | User-requested navigation actions | Disabled by default; show a prominent disclosure first |
| Root access | Optional advanced capability | Never required; never attempt a root action while root mode is off |

Use package visibility queries for launcher activities and HTTPS VIEW intents. Do not request QUERY_ALL_PACKAGES. Any permission not required by an implemented feature must be removed.
