# 05_Schema.md — Data Model, Relational Database Schema, and Access Control
**Project Name:** Meemo — Android Digital Assistant  
**Database Engine:** Room (SQLite) (`MeemoDatabase`, Schema Version `2` with explicit migration `MIGRATION_1_2`)  
**Package Namespace:** `com.marufallammahim.meemo.data.database`  
**Document Version:** 1.0.0  

---

## A. Local Relational Database Schema (10 Tables)

Meemo is a **local-first** Android application. All relational data is stored on-device in `meemo_assistant.db` using Room entities with explicit foreign keys, indices, and non-destructive migrations.

### 1. `assistant_preferences`
Stores structured relational preference profiles and audit timestamps complementing fast DataStore toggles.
| Column | SQLite Type | Kotlin Type | Constraints & Default | Privacy Classification |
| :--- | :--- | :--- | :--- | :--- |
| `pref_key` | `TEXT` | `String` | `PRIMARY KEY NOT NULL` | Internal Config |
| `pref_value` | `TEXT` | `String` | `NOT NULL` | Internal Config |
| `category` | `TEXT` | `String` | `NOT NULL DEFAULT 'GENERAL'` (Indexed) | Internal Config |
| `updated_at_ms` | `INTEGER` | `Long` | `NOT NULL` | Metadata |

### 2. `conversation_sessions`
Groups user-assistant interactions into logical sessions.
| Column | SQLite Type | Kotlin Type | Constraints & Default | Privacy Classification |
| :--- | :--- | :--- | :--- | :--- |
| `session_id` | `TEXT` | `String` | `PRIMARY KEY NOT NULL` (UUID) | User History |
| `title` | `TEXT` | `String` | `NOT NULL` | User History |
| `language_code` | `TEXT` | `String` | `NOT NULL DEFAULT 'en'` | Metadata |
| `started_at_ms` | `INTEGER` | `Long` | `NOT NULL` (Indexed) | Metadata |
| `last_active_at_ms` | `INTEGER` | `Long` | `NOT NULL` | Metadata |
| `is_pinned` | `INTEGER` | `Boolean` | `NOT NULL DEFAULT 0` | Metadata |

### 3. `conversation_messages`
Stores individual user prompts and assistant responses within a session when history persistence is enabled.
| Column | SQLite Type | Kotlin Type | Constraints & Default | Privacy Classification |
| :--- | :--- | :--- | :--- | :--- |
| `message_id` | `TEXT` | `String` | `PRIMARY KEY NOT NULL` (UUID) | User History |
| `session_id` | `TEXT` | `String` | `NOT NULL`, `FOREIGN KEY(session_id) REFERENCES conversation_sessions(session_id) ON DELETE CASCADE` (Indexed) | User History |
| `sender_role` | `TEXT` | `String` | `NOT NULL` (`USER` or `ASSISTANT`) | User History |
| `content_text` | `TEXT` | `String` | `NOT NULL` | Sensitive User Input |
| `detected_intent` | `TEXT` | `String?` | `NULL` | Metadata |
| `execution_status` | `TEXT` | `String` | `NOT NULL` (`SUCCESS`, `FAILED`, etc.) | Metadata |
| `created_at_ms` | `INTEGER` | `Long` | `NOT NULL` (Indexed) | Metadata |

### 4. `command_history`
Structured execution audit trail of normalized commands and capability outcomes.
| Column | SQLite Type | Kotlin Type | Constraints & Default | Privacy Classification |
| :--- | :--- | :--- | :--- | :--- |
| `command_id` | `TEXT` | `String` | `PRIMARY KEY NOT NULL` (UUID) | User History |
| `session_id` | `TEXT` | `String?` | `NULL`, `FOREIGN KEY(session_id) REFERENCES conversation_sessions(session_id) ON DELETE SET NULL` (Indexed) | User History |
| `raw_input` | `TEXT` | `String` | `NOT NULL` | Sensitive User Input |
| `normalized_input` | `TEXT` | `String` | `NOT NULL` | User History |
| `matched_intent` | `TEXT` | `String` | `NOT NULL` (Indexed) | Metadata |
| `extracted_entity` | `TEXT` | `String?` | `NULL` | Metadata |
| `confidence_score` | `REAL` | `Float` | `NOT NULL` | Metadata |
| `outcome_code` | `TEXT` | `String` | `NOT NULL` | Metadata |
| `latency_ms` | `INTEGER` | `Long` | `NOT NULL DEFAULT 0` | Diagnostics |
| `executed_at_ms` | `INTEGER` | `Long` | `NOT NULL` (Indexed) | Metadata |

### 5. `custom_command_aliases`
User-defined trigger phrases mapped to validated target intents and entities (e.g., `"work browser"` -> `OPEN_APPLICATION` + `"org.mozilla.firefox"`).
| Column | SQLite Type | Kotlin Type | Constraints & Default | Privacy Classification |
| :--- | :--- | :--- | :--- | :--- |
| `alias_id` | `TEXT` | `String` | `PRIMARY KEY NOT NULL` (UUID) | User Customization |
| `trigger_phrase` | `TEXT` | `String` | `NOT NULL` (`UNIQUE` Index `idx_alias_trigger`) | User Customization |
| `target_intent` | `TEXT` | `String` | `NOT NULL` | User Customization |
| `target_entity` | `TEXT` | `String` | `NOT NULL` | User Customization |
| `requires_confirmation` | `INTEGER` | `Boolean` | `NOT NULL DEFAULT 0` | Security Policy |
| `is_enabled` | `INTEGER` | `Boolean` | `NOT NULL DEFAULT 1` | Metadata |
| `usage_count` | `INTEGER` | `Int` | `NOT NULL DEFAULT 0` | Metadata |
| `created_at_ms` | `INTEGER` | `Long` | `NOT NULL` | Metadata |

### 6. `automation_rules`
Inspectable user-created routines (e.g., `"Night Routine"` executing multiple sequential validated actions).
| Column | SQLite Type | Kotlin Type | Constraints & Default | Privacy Classification |
| :--- | :--- | :--- | :--- | :--- |
| `rule_id` | `TEXT` | `String` | `PRIMARY KEY NOT NULL` (UUID) | User Customization |
| `rule_name` | `TEXT` | `String` | `NOT NULL` (`UNIQUE` Index `idx_rule_name`) | User Customization |
| `trigger_phrase` | `TEXT` | `String` | `NOT NULL` (Indexed) | User Customization |
| `actions_json` | `TEXT` | `String` | `NOT NULL` (Serialized `List<AutomationStepSpec>`) | User Customization |
| `is_enabled` | `INTEGER` | `Boolean` | `NOT NULL DEFAULT 1` | Metadata |
| `require_confirmation` | `INTEGER` | `Boolean` | `NOT NULL DEFAULT 0` | Security Policy |
| `created_at_ms` | `INTEGER` | `Long` | `NOT NULL` | Metadata |

### 7. `automation_executions`
Execution log for automation rules with foreign key cascade.
| Column | SQLite Type | Kotlin Type | Constraints & Default | Privacy Classification |
| :--- | :--- | :--- | :--- | :--- |
| `execution_id` | `TEXT` | `String` | `PRIMARY KEY NOT NULL` (UUID) | Diagnostics |
| `rule_id` | `TEXT` | `String` | `NOT NULL`, `FOREIGN KEY(rule_id) REFERENCES automation_rules(rule_id) ON DELETE CASCADE` (Indexed) | Diagnostics |
| `status` | `TEXT` | `String` | `NOT NULL` (`SUCCESS`, `PARTIAL_SUCCESS`, `FAILED`) | Diagnostics |
| `summary_text` | `TEXT` | `String` | `NOT NULL` | Diagnostics |
| `executed_at_ms` | `INTEGER` | `Long` | `NOT NULL` (Indexed) | Metadata |

### 8. `capability_snapshots`
Stores point-in-time diagnostic snapshots of device capabilities (never treated as permanent authorization—permissions are always rechecked live before execution).
| Column | SQLite Type | Kotlin Type | Constraints & Default | Privacy Classification |
| :--- | :--- | :--- | :--- | :--- |
| `capability_id` | `TEXT` | `String` | `PRIMARY KEY NOT NULL` | Diagnostics |
| `display_name` | `TEXT` | `String` | `NOT NULL` | Diagnostics |
| `is_hardware_present` | `INTEGER` | `Boolean` | `NOT NULL` | Diagnostics |
| `is_permission_granted` | `INTEGER` | `Boolean` | `NOT NULL` | Diagnostics |
| `status_summary` | `TEXT` | `String` | `NOT NULL` | Diagnostics |
| `last_checked_at_ms` | `INTEGER` | `Long` | `NOT NULL` | Metadata |

### 9. `plugin_registry`
Tracks registered internal capability plugins and their supported intents and permission contracts.
| Column | SQLite Type | Kotlin Type | Constraints & Default | Privacy Classification |
| :--- | :--- | :--- | :--- | :--- |
| `plugin_id` | `TEXT` | `String` | `PRIMARY KEY NOT NULL` | System Metadata |
| `display_name` | `TEXT` | `String` | `NOT NULL` | System Metadata |
| `version_name` | `TEXT` | `String` | `NOT NULL` | System Metadata |
| `supported_intents_csv` | `TEXT` | `String` | `NOT NULL` | System Metadata |
| `required_permissions_csv` | `TEXT` | `String` | `NOT NULL` | Security Policy |
| `is_enabled` | `INTEGER` | `Boolean` | `NOT NULL DEFAULT 1` | System Metadata |
| `registered_at_ms` | `INTEGER` | `Long` | `NOT NULL` | Metadata |

### 10. `consent_records`
Explicit user consent ledger for sensitive capabilities (`NOTIFICATION_READ_ALOUD`, `WAKE_WORD_FOREGROUND`, `AI_CLOUD_FALLBACK`, `ROOT_EXECUTION`, `ACCESSIBILITY_ASSIST`).
| Column | SQLite Type | Kotlin Type | Constraints & Default | Privacy Classification |
| :--- | :--- | :--- | :--- | :--- |
| `consent_scope` | `TEXT` | `String` | `PRIMARY KEY NOT NULL` | Security & Compliance |
| `is_granted` | `INTEGER` | `Boolean` | `NOT NULL DEFAULT 0` | Security & Compliance |
| `disclosure_version` | `TEXT` | `String` | `NOT NULL` | Security & Compliance |
| `updated_at_ms` | `INTEGER` | `Long` | `NOT NULL` | Security & Compliance |
| `notes` | `TEXT` | `String` | `NOT NULL DEFAULT ''` | Security & Compliance |

---

## B. Notification Privacy & Retention Policy
1. **Zero Default Persistent Notification Storage:** Incoming notifications processed by `MeemoNotificationListener` are held **solely in transient RAM** (`ArrayDeque` bounded to 15 items in `NotificationReadAloud`, plus a 60-second SHA-256 hash cache for duplicate suppression). Raw notification bodies are **never** inserted into Room tables or written to logcat.
2. **History Retention & Purge:**
   - Users can disable `saveConversationHistory` at any time in Privacy Settings.
   - Automatic retention cleanup (`MeemoRepository.pruneExpiredHistory(retentionDays = 30)`) deletes `conversation_sessions`, `conversation_messages`, and `command_history` older than the configured retention window.
   - Tapping **"Delete All Local Data"** executes a single `@Transaction` purging sessions, messages, command history, custom aliases, automation executions, and resetting consents.

---

## C. Explicit Database Migration Strategy
Destructive migrations (`fallbackToDestructiveMigration()`) are **not** used in production configuration. `MeemoMigrations.MIGRATION_1_2` explicitly demonstrates non-destructive schema evolution (adding `notes` column to `consent_records` and an index on `command_history(executed_at_ms)`).

---

## D. Optional Remote Schema (Future Cloud Sync Reference — Not Required for MVP)
Meemo operates 100% locally without a remote account. If cross-device alias synchronization via Supabase is enabled in a future phase:
- Tables (`remote_custom_aliases`, `remote_automation_rules`) must include `user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE`.
- PostgreSQL Row-Level Security (`ALTER TABLE ... ENABLE ROW LEVEL SECURITY`) must enforce `auth.uid() = user_id` on `SELECT`, `INSERT`, `UPDATE`, and `DELETE`.
- Service-role keys must never be embedded in the Android client.
