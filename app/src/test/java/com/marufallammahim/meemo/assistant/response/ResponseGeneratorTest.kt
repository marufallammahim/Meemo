package com.marufallammahim.meemo.assistant.response

import com.marufallammahim.meemo.actions.ActionResult
import com.marufallammahim.meemo.assistant.command.DetectedLanguage
import com.marufallammahim.meemo.domain.model.AppLanguageMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseGeneratorTest {

    private val generator = ResponseGenerator()

    @Test
    fun `uses Bengali response when language mode is explicitly Bengali`() {
        val response = generator.generate(
            result = ActionResult.Success(summaryEn = "Torch is on.", summaryBn = "টর্চ চালু হয়েছে।"),
            languageMode = AppLanguageMode.BENGALI,
            detectedLanguage = DetectedLanguage.ENGLISH
        )

        assertEquals("bn-BD", response.languageTag)
        assertEquals("সফলভাবে সম্পন্ন", response.displayTitle)
        assertEquals("টর্চ চালু হয়েছে।", response.displayBody)
        assertEquals(response.displayBody, response.spokenText)
        assertFalse(response.isError)
    }

    @Test
    fun `uses detected language in bilingual automatic mode`() {
        val bengali = generator.generate(
            ActionResult.Success("Done.", "হয়ে গেছে।"),
            AppLanguageMode.BILINGUAL_AUTO,
            DetectedLanguage.BENGALI
        )
        val english = generator.generate(
            ActionResult.Success("Done.", "হয়ে গেছে।"),
            AppLanguageMode.BILINGUAL_AUTO,
            DetectedLanguage.ENGLISH
        )

        assertEquals("bn-BD", bengali.languageTag)
        assertEquals("হয়ে গেছে।", bengali.spokenText)
        assertEquals("en-US", english.languageTag)
        assertEquals("Done.", english.spokenText)
    }

    @Test
    fun `marks unverified success as completed rather than verified`() {
        val response = generator.generate(
            ActionResult.Success("Opened settings.", "সেটিংস খোলা হয়েছে।", verifiedBySystem = false),
            AppLanguageMode.ENGLISH,
            DetectedLanguage.ENGLISH
        )

        assertEquals("COMPLETED", response.outcomeBadge)
        assertFalse(response.isError)
    }

    @Test
    fun `cancelled response does not produce spoken output`() {
        val response = generator.generate(
            ActionResult.Cancelled(messageEn = "Cancelled.", messageBn = "বাতিল হয়েছে।"),
            AppLanguageMode.ENGLISH,
            DetectedLanguage.ENGLISH
        )

        assertEquals("Cancelled", response.displayTitle)
        assertEquals("", response.spokenText)
        assertEquals("CANCELLED", response.outcomeBadge)
        assertFalse(response.isError)
    }
}
