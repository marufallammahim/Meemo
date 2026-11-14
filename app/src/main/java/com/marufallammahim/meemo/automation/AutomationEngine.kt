package com.marufallammahim.meemo.automation

import com.marufallammahim.meemo.domain.model.AutomationRuleModel
import com.marufallammahim.meemo.domain.model.AutomationStepSpec
import com.marufallammahim.meemo.domain.repository.MeemoRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomationEngine @Inject constructor(
    private val repository: MeemoRepository
) {
    fun observeRules(): Flow<List<AutomationRuleModel>> = repository.observeAutomationRules()

    suspend fun createDefaultRoutine(name: String, triggerPhrase: String) {
        val rule = AutomationRuleModel(
            ruleId = UUID.randomUUID().toString(),
            ruleName = name.trim(),
            triggerPhrase = triggerPhrase.trim(),
            steps = listOf(
                AutomationStepSpec(intentName = "TORCH_OFF"),
                AutomationStepSpec(intentName = "MEDIA_PAUSE"),
                AutomationStepSpec(intentName = "GET_BATTERY_STATUS")
            ),
            isEnabled = true,
            requireConfirmation = false
        )
        repository.saveAutomationRule(rule)
    }

    suspend fun deleteRule(ruleId: String) {
        repository.deleteAutomationRule(ruleId)
    }
}
