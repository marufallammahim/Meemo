package com.marufallammahim.meemo.assistant.planning

import com.marufallammahim.meemo.actions.ActionResult
import com.marufallammahim.meemo.actions.MediaCommandType
import com.marufallammahim.meemo.actions.MeemoAction
import com.marufallammahim.meemo.apps.PackageResolver
import com.marufallammahim.meemo.assistant.entity.ExtractedEntity
import com.marufallammahim.meemo.assistant.entity.VolumeDirection
import com.marufallammahim.meemo.assistant.intent.MeemoIntent
import com.marufallammahim.meemo.domain.model.AutomationRuleModel
import com.marufallammahim.meemo.domain.model.AutomationStepSpec
import com.marufallammahim.meemo.domain.repository.MeemoRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionPlannerAutomationTest {
    private val repository = mockk<MeemoRepository>()
    private val planner = ActionPlanner(mockk<PackageResolver>(), repository)

    @Test
    fun `loads saved routine and preserves confirmation requirement`() = runTest {
        val rule = AutomationRuleModel(
            ruleId = "routine-1",
            ruleName = "Night mode",
            triggerPhrase = "good night",
            steps = listOf(
                AutomationStepSpec("TORCH_OFF"),
                AutomationStepSpec("MEDIA_PAUSE"),
                AutomationStepSpec("GET_BATTERY_STATUS")
            ),
            isEnabled = true,
            requireConfirmation = true
        )
        coEvery {
            repository.findAutomationRuleByNameOrTrigger("Night mode")
        } returns rule

        val outcome = planner.plan(
            MeemoIntent.RUN_AUTOMATION,
            ExtractedEntity.AutomationTrigger("Night mode"),
            preferredBrowserPackage = null
        )

        assertEquals(
            ActionPlanOutcome.Planned(
                MeemoAction.ExecuteAutomationRoutine(
                    routineName = "Night mode",
                    steps = listOf(
                        MeemoAction.TurnTorchOff,
                        MeemoAction.MediaControl(MediaCommandType.PAUSE),
                        MeemoAction.GetBatteryStatus
                    ),
                    requiresConfirmation = true
                )
            ),
            outcome
        )
    }

    @Test
    fun `rejects unknown routines instead of running a hardcoded fallback`() = runTest {
        coEvery {
            repository.findAutomationRuleByNameOrTrigger("missing")
        } returns null

        val outcome = planner.plan(
            MeemoIntent.RUN_AUTOMATION,
            ExtractedEntity.AutomationTrigger("missing"),
            preferredBrowserPackage = null
        )

        assertTrue(outcome is ActionPlanOutcome.ImmediateResult)
        assertTrue((outcome as ActionPlanOutcome.ImmediateResult).result is ActionResult.Failed)
    }

    @Test
    fun `rejects root and nested automation steps`() = runTest {
        val rule = sampleRule(
            steps = listOf(
                AutomationStepSpec("TORCH_OFF"),
                AutomationStepSpec("ROOT_SYSTEM_DIAGNOSTIC")
            )
        )
        coEvery {
            repository.findAutomationRuleByNameOrTrigger("good night")
        } returns rule

        val outcome = planner.plan(
            MeemoIntent.RUN_AUTOMATION,
            ExtractedEntity.AutomationTrigger("good night"),
            preferredBrowserPackage = null
        )

        assertTrue(outcome is ActionPlanOutcome.ImmediateResult)
    }

    @Test
    fun `rejects invalid volume parameters in saved routines`() = runTest {
        val rule = sampleRule(
            steps = listOf(AutomationStepSpec("SET_VOLUME", "101"))
        )
        coEvery {
            repository.findAutomationRuleByNameOrTrigger("good night")
        } returns rule

        val outcome = planner.plan(
            MeemoIntent.RUN_AUTOMATION,
            ExtractedEntity.AutomationTrigger("good night"),
            preferredBrowserPackage = null
        )

        assertTrue(outcome is ActionPlanOutcome.ImmediateResult)
    }

    @Test
    fun `maps allowed volume steps into typed actions`() = runTest {
        val rule = sampleRule(
            steps = listOf(
                AutomationStepSpec("SET_VOLUME", "35%"),
                AutomationStepSpec("ADJUST_VOLUME", VolumeDirection.MUTE.name)
            )
        )
        coEvery {
            repository.findAutomationRuleByNameOrTrigger("good night")
        } returns rule

        val outcome = planner.plan(
            MeemoIntent.RUN_AUTOMATION,
            ExtractedEntity.AutomationTrigger("good night"),
            preferredBrowserPackage = null
        )

        assertEquals(
            ActionPlanOutcome.Planned(
                MeemoAction.ExecuteAutomationRoutine(
                    routineName = "Night mode",
                    steps = listOf(
                        MeemoAction.SetVolume(35),
                        MeemoAction.AdjustVolume(VolumeDirection.MUTE)
                    )
                )
            ),
            outcome
        )
    }

    private fun sampleRule(steps: List<AutomationStepSpec>) = AutomationRuleModel(
        ruleId = "routine-1",
        ruleName = "Night mode",
        triggerPhrase = "good night",
        steps = steps,
        isEnabled = true,
        requireConfirmation = false
    )
}
