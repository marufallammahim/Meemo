package com.marufallammahim.meemo.data.repository

import com.marufallammahim.meemo.core.common.DispatcherProvider
import com.marufallammahim.meemo.core.utilities.TimeProvider
import com.marufallammahim.meemo.data.database.dao.MeemoDao
import com.marufallammahim.meemo.data.database.entity.AutomationRuleEntity
import com.marufallammahim.meemo.data.database.entity.CapabilitySnapshotEntity
import com.marufallammahim.meemo.data.database.entity.CommandHistoryEntity
import com.marufallammahim.meemo.data.database.entity.ConsentRecordEntity
import com.marufallammahim.meemo.data.database.entity.ConversationMessageEntity
import com.marufallammahim.meemo.data.database.entity.ConversationSessionEntity
import com.marufallammahim.meemo.data.database.entity.CustomCommandAliasEntity
import com.marufallammahim.meemo.data.database.entity.PluginRegistryEntity
import com.marufallammahim.meemo.data.datastore.MeemoPreferencesDataStore
import com.marufallammahim.meemo.data.local.LocalDataSeeder
import com.marufallammahim.meemo.domain.model.AppLanguageMode
import com.marufallammahim.meemo.domain.model.AppThemeMode
import com.marufallammahim.meemo.domain.model.AutomationRuleModel
import com.marufallammahim.meemo.domain.model.AutomationStepSpec
import com.marufallammahim.meemo.domain.model.ConversationEntryModel
import com.marufallammahim.meemo.domain.model.CustomAliasModel
import com.marufallammahim.meemo.domain.model.UserPreferences
import com.marufallammahim.meemo.domain.repository.MeemoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeemoRepositoryImpl @Inject constructor(
    private val dao: MeemoDao,
    private val dataStore: MeemoPreferencesDataStore,
    private val seeder: LocalDataSeeder,
    private val dispatchers: DispatcherProvider,
    private val timeProvider: TimeProvider
) : MeemoRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override val userPreferencesFlow: Flow<UserPreferences> = dataStore.userPreferencesFlow

    override fun observeRecentMessages(limit: Int): Flow<List<ConversationEntryModel>> =
        dao.observeRecentMessages(limit).map { list ->
            list.sortedBy { it.createdAtMs }.map { entity ->
                ConversationEntryModel(
                    messageId = entity.messageId,
                    sessionId = entity.sessionId,
                    isUser = entity.senderRole == "USER",
                    text = entity.contentText,
                    intentName = entity.detectedIntent,
                    statusBadge = entity.executionStatus,
                    timestampMs = entity.createdAtMs
                )
            }
        }

    override fun observeCommandHistory(limit: Int): Flow<List<CommandHistoryEntity>> =
        dao.observeCommandHistory(limit)

    override fun observeAliases(): Flow<List<CustomAliasModel>> =
        dao.observeAliases().map { list -> list.map { it.toDomain() } }

    override fun observeAutomationRules(): Flow<List<AutomationRuleModel>> =
        dao.observeAutomationRules().map { list -> list.map { it.toDomain() } }

    override fun observeCapabilitySnapshots(): Flow<List<CapabilitySnapshotEntity>> =
        dao.observeCapabilitySnapshots()

    override fun observePlugins(): Flow<List<PluginRegistryEntity>> =
        dao.observePlugins()

    override fun observeConsents(): Flow<List<ConsentRecordEntity>> =
        dao.observeConsents()

    override suspend fun ensureSeeded() = withContext(dispatchers.io) {
        val now = timeProvider.nowMillis()
        dao.upsertPlugins(seeder.getBuiltInPlugins(now))
    }

    override suspend fun recordInteraction(
        sessionId: String,
        rawInput: String,
        normalizedInput: String,
        matchedIntent: String,
        extractedEntity: String?,
        confidence: Float,
        outcomeCode: String,
        assistantReply: String,
        latencyMs: Long
    ) = withContext(dispatchers.io) {
        val prefs = userPreferencesFlow.first()
        if (!prefs.saveConversationHistory) return@withContext

        val now = timeProvider.nowMillis()
        dao.upsertSession(
            ConversationSessionEntity(
                sessionId = sessionId,
                title = rawInput.take(40).ifBlank { "Meemo Session" },
                languageCode = prefs.languageMode.bcp47Tag,
                startedAtMs = now,
                lastActiveAtMs = now
            )
        )
        dao.insertMessage(
            ConversationMessageEntity(
                messageId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                senderRole = "USER",
                contentText = rawInput,
                detectedIntent = matchedIntent,
                executionStatus = "SUBMITTED",
                createdAtMs = now
            )
        )
        dao.insertMessage(
            ConversationMessageEntity(
                messageId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                senderRole = "ASSISTANT",
                contentText = assistantReply,
                detectedIntent = matchedIntent,
                executionStatus = outcomeCode,
                createdAtMs = now + 1L
            )
        )
        dao.insertCommandHistory(
            CommandHistoryEntity(
                commandId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                rawInput = rawInput,
                normalizedInput = normalizedInput,
                matchedIntent = matchedIntent,
                extractedEntity = extractedEntity,
                confidenceScore = confidence,
                outcomeCode = outcomeCode,
                latencyMs = latencyMs,
                executedAtMs = now
            )
        )
    }

    override suspend fun findAliasByTrigger(triggerPhrase: String): CustomAliasModel? =
        withContext(dispatchers.io) {
            val found = dao.findAliasByTrigger(triggerPhrase.trim())
            if (found != null) {
                dao.incrementAliasUsage(found.aliasId)
            }
            found?.toDomain()
        }

    override suspend fun saveAlias(
        triggerPhrase: String,
        targetIntent: String,
        targetEntity: String,
        requiresConfirmation: Boolean
    ) = withContext(dispatchers.io) {
        val cleanTrigger = triggerPhrase.trim().lowercase()
        val existing = dao.findAliasByTrigger(cleanTrigger)
        dao.upsertAlias(
            CustomCommandAliasEntity(
                aliasId = existing?.aliasId ?: UUID.randomUUID().toString(),
                triggerPhrase = cleanTrigger,
                targetIntent = targetIntent,
                targetEntity = targetEntity.trim(),
                requiresConfirmation = requiresConfirmation,
                isEnabled = true,
                usageCount = existing?.usageCount ?: 0,
                createdAtMs = existing?.createdAtMs ?: timeProvider.nowMillis()
            )
        )
    }

    override suspend fun deleteAlias(aliasId: String) = withContext(dispatchers.io) {
        dao.deleteAlias(aliasId)
    }

    override suspend fun findAutomationRuleByTrigger(triggerPhrase: String): AutomationRuleModel? =
        withContext(dispatchers.io) {
            dao.findAutomationRuleByTrigger(triggerPhrase.trim())?.toDomain()
        }

    override suspend fun saveAutomationRule(rule: AutomationRuleModel) = withContext(dispatchers.io) {
        val stepsJson = json.encodeToString(
            ListSerializer(AutomationStepSpec.serializer()),
            rule.steps
        )
        dao.upsertAutomationRule(
            AutomationRuleEntity(
                ruleId = rule.ruleId.ifBlank { UUID.randomUUID().toString() },
                ruleName = rule.ruleName.trim(),
                triggerPhrase = rule.triggerPhrase.trim().lowercase(),
                actionsJson = stepsJson,
                isEnabled = rule.isEnabled,
                requireConfirmation = rule.requireConfirmation,
                createdAtMs = timeProvider.nowMillis()
            )
        )
    }

    override suspend fun deleteAutomationRule(ruleId: String) = withContext(dispatchers.io) {
        dao.deleteAutomationRule(ruleId)
    }

    override suspend fun saveCapabilitySnapshots(snapshots: List<CapabilitySnapshotEntity>) =
        withContext(dispatchers.io) {
            dao.upsertCapabilitySnapshots(snapshots)
        }

    override suspend fun recordConsent(scope: String, granted: Boolean, notes: String) =
        withContext(dispatchers.io) {
            dao.upsertConsent(
                ConsentRecordEntity(
                    consentScope = scope,
                    isGranted = granted,
                    disclosureVersion = "1.0.0",
                    updatedAtMs = timeProvider.nowMillis(),
                    notes = notes
                )
            )
        }

    override suspend fun hasGrantedConsent(scope: String): Boolean = withContext(dispatchers.io) {
        dao.getConsent(scope)?.isGranted == true
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) =
        dataStore.setOnboardingCompleted(completed)

    override suspend fun setThemeMode(mode: AppThemeMode) =
        dataStore.setThemeMode(mode)

    override suspend fun setLanguageMode(mode: AppLanguageMode) =
        dataStore.setLanguageMode(mode)

    override suspend fun setTtsEnabled(enabled: Boolean) =
        dataStore.setTtsEnabled(enabled)

    override suspend fun setTtsSpeechRate(rate: Float) =
        dataStore.setTtsSpeechRate(rate)

    override suspend fun setSaveConversationHistory(enabled: Boolean) =
        dataStore.setSaveConversationHistory(enabled)

    override suspend fun setWakeWordEnabled(enabled: Boolean) {
        recordConsent("WAKE_WORD_FOREGROUND", enabled, "User toggled wake-word detection")
        dataStore.setWakeWordEnabled(enabled)
    }

    override suspend fun setNotificationReadAloud(enabled: Boolean) {
        recordConsent("NOTIFICATION_READ_ALOUD", enabled, "User toggled notification read-aloud")
        dataStore.setNotificationReadAloud(enabled)
    }

    override suspend fun setNotificationAnnounceSenderOnly(senderOnly: Boolean) =
        dataStore.setNotificationAnnounceSenderOnly(senderOnly)

    override suspend fun setQuietHours(enabled: Boolean, startHour: Int, endHour: Int) =
        dataStore.setQuietHours(enabled, startHour, endHour)

    override suspend fun setAiConfiguration(enabled: Boolean, endpointUrl: String, modelName: String) {
        recordConsent("AI_CLOUD_FALLBACK", enabled, "User configured optional AI fallback")
        dataStore.setAiConfiguration(enabled, endpointUrl, modelName)
    }

    override suspend fun setRootFeaturesEnabled(enabled: Boolean) {
        recordConsent("ROOT_EXECUTION", enabled, "User toggled isolated root capability")
        dataStore.setRootFeaturesEnabled(enabled)
    }

    override suspend fun setPreferredBrowserPackage(packageName: String?) =
        dataStore.setPreferredBrowserPackage(packageName)

    override suspend fun pruneExpiredHistory(retentionDays: Int): Int = withContext(dispatchers.io) {
        val cutoff = timeProvider.nowMillis() - (retentionDays.toLong() * 24L * 3600_000L)
        dao.deleteHistoryOlderThan(cutoff)
    }

    override suspend fun purgeAllUserData() = withContext(dispatchers.io) {
        dao.purgeAllUserData()
    }

    private fun CustomCommandAliasEntity.toDomain() = CustomAliasModel(
        aliasId = aliasId,
        triggerPhrase = triggerPhrase,
        targetIntent = targetIntent,
        targetEntity = targetEntity,
        requiresConfirmation = requiresConfirmation,
        isEnabled = isEnabled,
        usageCount = usageCount
    )

    private fun AutomationRuleEntity.toDomain(): AutomationRuleModel {
        val steps = runCatching {
            json.decodeFromString(ListSerializer(AutomationStepSpec.serializer()), actionsJson)
        }.getOrDefault(emptyList())
        return AutomationRuleModel(
            ruleId = ruleId,
            ruleName = ruleName,
            triggerPhrase = triggerPhrase,
            steps = steps,
            isEnabled = isEnabled,
            requireConfirmation = requireConfirmation
        )
    }
}
