package com.marufallammahim.meemo.domain.validation

import com.marufallammahim.meemo.actions.MeemoAction
import com.marufallammahim.meemo.data.remote.AiStructuredInterpretation
import com.marufallammahim.meemo.root.RootCommandPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CommandSafetyValidatorTest {

    private lateinit var validator: CommandSafetyValidator

    @Before
    fun setUp() {
        validator = CommandSafetyValidator(RootCommandPolicy())
    }

    @Test
    fun `accepts volume values at both supported boundaries`() {
        assertTrue(validator.validateAction(MeemoAction.SetVolume(0)).isSuccess)
        assertTrue(validator.validateAction(MeemoAction.SetVolume(100)).isSuccess)
    }

    @Test
    fun `rejects volume values below zero or above one hundred`() {
        assertFalse(validator.validateAction(MeemoAction.SetVolume(-1)).isSuccess)
        assertFalse(validator.validateAction(MeemoAction.SetVolume(101)).isSuccess)
    }

    @Test
    fun `accepts approved root diagnostic and rejects unknown root command`() {
        assertTrue(
            validator.validateAction(
                MeemoAction.ExecuteApprovedRootDiagnostic("ROOT_BATTERY_STATS_DUMP")
            ).isSuccess
        )
        assertFalse(
            validator.validateAction(
                MeemoAction.ExecuteApprovedRootDiagnostic("ROOT_UNLISTED_COMMAND")
            ).isSuccess
        )
    }

    @Test
    fun `rejects non finite and out of range AI confidence`() {
        val invalidScores = listOf(
            Float.NaN,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            1.01f,
            -0.01f
        )

        invalidScores.forEach { score ->
            assertFalse(
                "Expected confidence $score to be rejected",
                validator.validateAiInterpretation(
                    AiStructuredInterpretation(
                        intentName = "GET_BATTERY_STATUS",
                        confidence = score
                    )
                ).isSuccess
            )
        }
    }

    @Test
    fun `accepts AI confidence at supported threshold and maximum`() {
        assertTrue(
            validator.validateAiInterpretation(
                AiStructuredInterpretation(
                    intentName = "GET_BATTERY_STATUS",
                    confidence = CommandSafetyValidator.MIN_AI_CONFIDENCE
                )
            ).isSuccess
        )
        assertTrue(
            validator.validateAiInterpretation(
                AiStructuredInterpretation(
                    intentName = "GET_BATTERY_STATUS",
                    confidence = CommandSafetyValidator.MAX_AI_CONFIDENCE
                )
            ).isSuccess
        )
    }

}
