package com.marufallammahim.meemo.domain.validation

import com.marufallammahim.meemo.assistant.intent.MeemoIntent
import com.marufallammahim.meemo.data.remote.AiStructuredInterpretation
import com.marufallammahim.meemo.root.RootCommandPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CommandSafetyAndAiFallbackTest {

    private lateinit var validator: CommandSafetyValidator

    @Before
    fun setUp() {
        validator = CommandSafetyValidator(RootCommandPolicy())
    }

    @Test
    fun `accepts valid high-confidence AI interpretation for normal intent`() {
        val aiOutput = AiStructuredInterpretation(
            intentName = "TORCH_ON",
            entityValue = null,
            confidence = 0.88f,
            explanation = "User asked to illuminate the room"
        )
        val result = validator.validateAiInterpretation(aiOutput)
        assertTrue(result.isSuccess)
        assertEquals(MeemoIntent.TORCH_ON, result.getOrNull())
    }

    @Test
    fun `rejects AI interpretation attempting to trigger root or shell metacharacters`() {
        val rootAttempt = AiStructuredInterpretation(
            intentName = "ROOT_SYSTEM_DIAGNOSTIC",
            entityValue = "ROOT_BATTERY_STATS_DUMP",
            confidence = 0.99f
        )
        assertFalse(validator.validateAiInterpretation(rootAttempt).isSuccess)

        val injectionAttempt = AiStructuredInterpretation(
            intentName = "OPEN_APPLICATION",
            entityValue = "Calculator; rm -rf /",
            confidence = 0.95f
        )
        assertFalse(validator.validateAiInterpretation(injectionAttempt).isSuccess)
    }

    @Test
    fun `rejects low-confidence AI guesses`() {
        val lowConfidence = AiStructuredInterpretation(
            intentName = "TORCH_OFF",
            entityValue = null,
            confidence = 0.42f
        )
        assertFalse(validator.validateAiInterpretation(lowConfidence).isSuccess)
    }
}
