package com.marufallammahim.meemo.domain.repository

import com.marufallammahim.meemo.data.database.entity.CapabilitySnapshotEntity
import com.marufallammahim.meemo.data.database.entity.CommandHistoryEntity
import com.marufallammahim.meemo.data.database.entity.ConsentRecordEntity
import com.marufallammahim.meemo.data.database.entity.PluginRegistryEntity
import com.marufallammahim.meemo.domain.model.AppLanguageMode
import com.marufallammahim.meemo.domain.model.AppThemeMode
import com.marufallammahim.meemo.domain.model.AutomationRuleModel
import com.marufallammahim.meemo.domain.model.ConversationEntryModel
import com.marufallammahim.meemo.domain.model.CustomAliasModel
import com.marufallammahim.meemo.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface MeemoRepository {
    val userPreferencesFlow: Flow<UserPreferences>

    fun observeRecentMessages(limit: Int = 50): Flow<List<ConversationEntryModel>>
    fun observeCommandHistory(limit: Int = 100): Flow<List<CommandHistoryEntity>>
    fun observeAliases(): Flow<List<CustomAliasModel>>
    fun observeAutomationRules(): Flow<List<AutomationRuleModel>>
    fun observeCapabilitySnapshots(): Flow<List<CapabilitySnapshotEntity>>
    fun observePlugins(): Flow<List<PluginRegistryEntity>>
    fun observeConsents(): Flow<List<ConsentRecordEntity>>

    suspend fun ensureSeeded()
    suspend fun recordInteraction(
        sessionId: String,
        rawInput: String,
        normalizedInput: String,
        matchedIntent: String,
        extractedEntity: String?,
        confidence: Float,
        outcomeCode: String,
        assistantReply: String,
        latencyMs: Long
    )

    suspend fun findAliasByTrigger(triggerPhrase: String): CustomAliasModel?
    suspend fun saveAlias(triggerPhrase: String, targetIntent: String, targetEntity: String, requiresConfirmation: Boolean)
    suspend fun deleteAlias(aliasId: String)

    suspend fun findAutomationRuleByTrigger(triggerPhrase: String): AutomationRuleModel?
    suspend fun saveAutomationRule(rule: AutomationRuleModel)
    suspend fun deleteAutomationRule(ruleId: String)

    suspend fun saveCapabilitySnapshots(snapshots: List<CapabilitySnapshotEntity>)
    suspend fun recordConsent(scope: String, granted: Boolean, notes: String = "")
    suspend fun hasGrantedConsent(scope: String): Boolean

    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setThemeMode(mode: AppThemeMode)
    suspend fun setLanguageMode(mode: AppLanguageMode)
    suspend fun setTtsEnabled(enabled: Boolean)
    suspend fun setTtsSpeechRate(rate: Float)
    suspend fun setSaveConversationHistory(enabled: Boolean)
    suspend fun setWakeWordEnabled(enabled: Boolean)
    suspend fun setNotificationReadAloud(enabled: Boolean)
    suspend fun setNotificationAnnounceSenderOnly(senderOnly: Boolean)
    suspend fun setQuietHours(enabled: Boolean, startHour: Int, endHour: Int)
    suspend fun setAiConfiguration(enabled: Boolean, endpointUrl: String, modelName: String)
    suspend fun setRootFeaturesEnabled(enabled: Boolean)
    suspend fun setPreferredBrowserPackage(packageName: String?)

    suspend fun pruneExpiredHistory(retentionDays: Int = 30): Int
    suspend fun purgeAllUserData()
}
