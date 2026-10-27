# Privacy & Isolated Root Security Model (`docs/security/PRIVACY_AND_ROOT_MODEL.md`)

1. **Transient Notification Memory:** `MeemoNotificationListener` holds up to 15 recent notifications in an in-memory `ArrayDeque` and a 60-second SHA-256 deduplication map. Zero notification message bodies are written to SQLite or logcat.
2. **Allowlisted Root Execution:** `RootCommandPolicy` permits only three static diagnostic commands (`ROOT_BATTERY_STATS_DUMP`, `ROOT_THERMAL_STATUS`, `ROOT_PACKAGE_PATH_CHECK`) and validates optional package parameters against `^[a-zA-Z0-9._\-]{1,64}$`.
3. **Log Redaction:** `SafeLogger` masks bearer tokens, `sk-...` API keys, email addresses, and `notification_body=[...]` tags before storing diagnostic entries in memory.
