package com.marufallammahim.meemo.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 1. assistant_preferences table
 */
@Entity(
    tableName = "assistant_preferences",
    indices = [Index(value = ["category"], name = "idx_pref_category")]
)
data class AssistantPreferenceEntity(
    @PrimaryKey
    @ColumnInfo(name = "pref_key") val prefKey: String,
    @ColumnInfo(name = "pref_value") val prefValue: String,
    @ColumnInfo(name = "category", defaultValue = "GENERAL") val category: String = "GENERAL",
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long
)

/**
 * 2. conversation_sessions table
 */
@Entity(
    tableName = "conversation_sessions",
    indices = [Index(value = ["started_at_ms"], name = "idx_session_started")]
)
data class ConversationSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "language_code", defaultValue = "en") val languageCode: String = "en",
    @ColumnInfo(name = "started_at_ms") val startedAtMs: Long,
    @ColumnInfo(name = "last_active_at_ms") val lastActiveAtMs: Long,
    @ColumnInfo(name = "is_pinned", defaultValue = "0") val isPinned: Boolean = false
)

/**
 * 3. conversation_messages table
 */
@Entity(
    tableName = "conversation_messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationSessionEntity::class,
            parentColumns = ["session_id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["session_id"], name = "idx_msg_session"),
        Index(value = ["created_at_ms"], name = "idx_msg_created")
    ]
)
data class ConversationMessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "message_id") val messageId: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "sender_role") val senderRole: String,
    @ColumnInfo(name = "content_text") val contentText: String,
    @ColumnInfo(name = "detected_intent") val detectedIntent: String?,
    @ColumnInfo(name = "execution_status") val executionStatus: String,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long
)

/**
 * 4. command_history table
 */
@Entity(
    tableName = "command_history",
    foreignKeys = [
        ForeignKey(
            entity = ConversationSessionEntity::class,
            parentColumns = ["session_id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["session_id"], name = "idx_cmd_session"),
        Index(value = ["matched_intent"], name = "idx_cmd_intent"),
        Index(value = ["executed_at_ms"], name = "idx_cmd_executed")
    ]
)
data class CommandHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "command_id") val commandId: String,
    @ColumnInfo(name = "session_id") val sessionId: String?,
    @ColumnInfo(name = "raw_input") val rawInput: String,
    @ColumnInfo(name = "normalized_input") val normalizedInput: String,
    @ColumnInfo(name = "matched_intent") val matchedIntent: String,
    @ColumnInfo(name = "extracted_entity") val extractedEntity: String?,
    @ColumnInfo(name = "confidence_score") val confidenceScore: Float,
    @ColumnInfo(name = "outcome_code") val outcomeCode: String,
    @ColumnInfo(name = "latency_ms", defaultValue = "0") val latencyMs: Long = 0L,
    @ColumnInfo(name = "executed_at_ms") val executedAtMs: Long
)

/**
 * 5. custom_command_aliases table
 */
@Entity(
    tableName = "custom_command_aliases",
    indices = [Index(value = ["trigger_phrase"], unique = true, name = "idx_alias_trigger")]
)
data class CustomCommandAliasEntity(
    @PrimaryKey
    @ColumnInfo(name = "alias_id") val aliasId: String,
    @ColumnInfo(name = "trigger_phrase") val triggerPhrase: String,
    @ColumnInfo(name = "target_intent") val targetIntent: String,
    @ColumnInfo(name = "target_entity") val targetEntity: String,
    @ColumnInfo(name = "requires_confirmation", defaultValue = "0") val requiresConfirmation: Boolean = false,
    @ColumnInfo(name = "is_enabled", defaultValue = "1") val isEnabled: Boolean = true,
    @ColumnInfo(name = "usage_count", defaultValue = "0") val usageCount: Int = 0,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long
)

/**
 * 6. automation_rules table
 */
@Entity(
    tableName = "automation_rules",
    indices = [
        Index(value = ["rule_name"], unique = true, name = "idx_rule_name"),
        Index(value = ["trigger_phrase"], name = "idx_rule_trigger")
    ]
)
data class AutomationRuleEntity(
    @PrimaryKey
    @ColumnInfo(name = "rule_id") val ruleId: String,
    @ColumnInfo(name = "rule_name") val ruleName: String,
    @ColumnInfo(name = "trigger_phrase") val triggerPhrase: String,
    @ColumnInfo(name = "actions_json") val actionsJson: String,
    @ColumnInfo(name = "is_enabled", defaultValue = "1") val isEnabled: Boolean = true,
    @ColumnInfo(name = "require_confirmation", defaultValue = "0") val requireConfirmation: Boolean = false,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long
)

/**
 * 7. automation_executions table
 */
@Entity(
    tableName = "automation_executions",
    foreignKeys = [
        ForeignKey(
            entity = AutomationRuleEntity::class,
            parentColumns = ["rule_id"],
            childColumns = ["rule_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["rule_id"], name = "idx_exec_rule"),
        Index(value = ["executed_at_ms"], name = "idx_exec_timestamp")
    ]
)
data class AutomationExecutionEntity(
    @PrimaryKey
    @ColumnInfo(name = "execution_id") val executionId: String,
    @ColumnInfo(name = "rule_id") val ruleId: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "summary_text") val summaryText: String,
    @ColumnInfo(name = "executed_at_ms") val executedAtMs: Long
)

/**
 * 8. capability_snapshots table
 */
@Entity(tableName = "capability_snapshots")
data class CapabilitySnapshotEntity(
    @PrimaryKey
    @ColumnInfo(name = "capability_id") val capabilityId: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "is_hardware_present") val isHardwarePresent: Boolean,
    @ColumnInfo(name = "is_permission_granted") val isPermissionGranted: Boolean,
    @ColumnInfo(name = "status_summary") val statusSummary: String,
    @ColumnInfo(name = "last_checked_at_ms") val lastCheckedAtMs: Long
)

/**
 * 9. plugin_registry table
 */
@Entity(tableName = "plugin_registry")
data class PluginRegistryEntity(
    @PrimaryKey
    @ColumnInfo(name = "plugin_id") val pluginId: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "version_name") val versionName: String,
    @ColumnInfo(name = "supported_intents_csv") val supportedIntentsCsv: String,
    @ColumnInfo(name = "required_permissions_csv") val requiredPermissionsCsv: String,
    @ColumnInfo(name = "is_enabled", defaultValue = "1") val isEnabled: Boolean = true,
    @ColumnInfo(name = "registered_at_ms") val registeredAtMs: Long
)

/**
 * 10. consent_records table
 */
@Entity(tableName = "consent_records")
data class ConsentRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "consent_scope") val consentScope: String,
    @ColumnInfo(name = "is_granted", defaultValue = "0") val isGranted: Boolean = false,
    @ColumnInfo(name = "disclosure_version") val disclosureVersion: String,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long,
    @ColumnInfo(name = "notes", defaultValue = "") val notes: String = ""
)
