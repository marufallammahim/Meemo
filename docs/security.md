# Security and privacy baseline

- Prefer Android platform APIs and user-mediated system settings over privileged workarounds.
- Root is optional and must remain disabled unless the user explicitly enables it.
- Never pass raw command text to a shell, Runtime.exec, reflection or Intent.parseUri.
- Root operations must be represented by an allow-listed typed command model with validated parameters; no raw-string execution API is permitted.
- Sensitive or system-wide actions require explicit confirmation.
- Redact command text, app labels and credentials from release logs.
- Keep history local and provide a user-visible clear-data action.
- Exclude sensitive local data from backup and device transfer.
- Optional AI must be off by default, use HTTPS, send only the command text after user opt-in, and never receive installed-app lists or command history by default.
- Unsupported actions must return a clear reason instead of silently failing.
