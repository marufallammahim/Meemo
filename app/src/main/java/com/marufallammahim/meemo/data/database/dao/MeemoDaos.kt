package com.marufallammahim.meemo.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.marufallammahim.meemo.data.database.entity.AssistantPreferenceEntity
import com.marufallammahim.meemo.data.database.entity.AutomationExecutionEntity
import com.marufallammahim.meemo.data.database.entity.AutomationRuleEntity
import com.marufallammahim.meemo.data.database.entity.CapabilitySnapshotEntity
import com.marufallammahim.meemo.data.database.entity.CommandHistoryEntity
import com.marufallammahim.meemo.data.database.entity.ConsentRecordEntity
import com.marufallammahim.meemo.data.database.entity.ConversationMessageEntity
import com.marufallammahim.meemo.data.database.entity.ConversationSessionEntity
import com.marufallammahim.meemo.data.database.entity.CustomCommandAliasEntity
import com.marufallammahim.meemo.data.database.entity.PluginRegistryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeemoDao {

    // 1. Assistant Preferences
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreference(pref: AssistantPreferenceEntity)

    @Query("SELECT * FROM assistant_preferences WHERE pref_key = :key LIMIT 1")
    suspend fun getPreference(key: String): AssistantPreferenceEntity?

    // 2 & 3. Conversation Sessions & Messages
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: ConversationSessionEntity)

    @Query("SELECT * FROM conversation_sessions ORDER BY last_active_at_ms DESC")
    fun observeSessions(): Flow<List<ConversationSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ConversationMessageEntity)

    @Query("SELECT * FROM conversation_messages WHERE session_id = :sessionId ORDER BY created_at_ms ASC")
    fun observeMessagesForSession(sessionId: String): Flow<List<ConversationMessageEntity>>

    @Query("SELECT * FROM conversation_messages ORDER BY created_at_ms DESC LIMIT :limit")
    fun observeRecentMessages(limit: Int = 50): Flow<List<ConversationMessageEntity>>

    @Query("DELETE FROM conversation_sessions WHERE session_id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    // 4. Command History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommandHistory(entry: CommandHistoryEntity)

    @Query("SELECT * FROM command_history ORDER BY executed_at_ms DESC LIMIT :limit")
    fun observeCommandHistory(limit: Int = 100): Flow<List<CommandHistoryEntity>>

    @Query("DELETE FROM command_history WHERE executed_at_ms < :cutoffMs")
    suspend fun deleteHistoryOlderThan(cutoffMs: Long): Int

    // 5. Custom Command Aliases
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlias(alias: CustomCommandAliasEntity)

    @Query("SELECT * FROM custom_command_aliases ORDER BY trigger_phrase ASC")
    fun observeAliases(): Flow<List<CustomCommandAliasEntity>>

    @Query("SELECT * FROM custom_command_aliases WHERE is_enabled = 1")
    suspend fun getEnabledAliases(): List<CustomCommandAliasEntity>

    @Query("SELECT * FROM custom_command_aliases WHERE LOWER(trigger_phrase) = LOWER(:trigger) AND is_enabled = 1 LIMIT 1")
    suspend fun findAliasByTrigger(trigger: String): CustomCommandAliasEntity?

    @Query("UPDATE custom_command_aliases SET usage_count = usage_count + 1 WHERE alias_id = :aliasId")
    suspend fun incrementAliasUsage(aliasId: String)

    @Query("DELETE FROM custom_command_aliases WHERE alias_id = :aliasId")
    suspend fun deleteAlias(aliasId: String)

    // 6 & 7. Automation Rules & Executions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAutomationRule(rule: AutomationRuleEntity)

    @Query("SELECT * FROM automation_rules ORDER BY created_at_ms DESC")
    fun observeAutomationRules(): Flow<List<AutomationRuleEntity>>

    @Query("SELECT * FROM automation_rules WHERE LOWER(trigger_phrase) = LOWER(:trigger) AND is_enabled = 1 LIMIT 1")
    suspend fun findAutomationRuleByTrigger(trigger: String): AutomationRuleEntity?

    @Query("DELETE FROM automation_rules WHERE rule_id = :ruleId")
    suspend fun deleteAutomationRule(ruleId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomationExecution(execution: AutomationExecutionEntity)

    @Query("SELECT * FROM automation_executions ORDER BY executed_at_ms DESC LIMIT 50")
    fun observeAutomationExecutions(): Flow<List<AutomationExecutionEntity>>

    // 8. Capability Snapshots
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCapabilitySnapshots(snapshots: List<CapabilitySnapshotEntity>)

    @Query("SELECT * FROM capability_snapshots ORDER BY display_name ASC")
    fun observeCapabilitySnapshots(): Flow<List<CapabilitySnapshotEntity>>

    // 9. Plugin Registry
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlugins(plugins: List<PluginRegistryEntity>)

    @Query("SELECT * FROM plugin_registry ORDER BY display_name ASC")
    fun observePlugins(): Flow<List<PluginRegistryEntity>>

    // 10. Consent Records
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConsent(record: ConsentRecordEntity)

    @Query("SELECT * FROM consent_records")
    fun observeConsents(): Flow<List<ConsentRecordEntity>>

    @Query("SELECT * FROM consent_records WHERE consent_scope = :scope LIMIT 1")
    suspend fun getConsent(scope: String): ConsentRecordEntity?

    // Full Privacy Purge Transaction
    @Query("DELETE FROM conversation_sessions")
    suspend fun clearAllSessions()

    @Query("DELETE FROM conversation_messages")
    suspend fun clearAllMessages()

    @Query("DELETE FROM command_history")
    suspend fun clearAllCommandHistory()

    @Query("DELETE FROM custom_command_aliases")
    suspend fun clearAllAliases()

    @Query("DELETE FROM automation_rules")
    suspend fun clearAllAutomationRules()

    @Query("DELETE FROM consent_records")
    suspend fun clearAllConsents()

    @Transaction
    suspend fun purgeAllUserData() {
        clearAllMessages()
        clearAllCommandHistory()
        clearAllSessions()
        clearAllAliases()
        clearAllAutomationRules()
        clearAllConsents()
    }
}
