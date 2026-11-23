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
    fun `localizes partial success summary and reason`() {
        val result = ActionResult.PartialSuccess(
            summaryEn = "Settings opened.",
            summaryBn = "সেটিংস খোলা হয়েছে।",
            reasonEn = "Complete the change manually.",
            reasonBn = "পরিবর্তনটি নিজে সম্পন্ন করুন।"
        )

        val english = generator.generate(result, AppLanguageMode.ENGLISH, DetectedLanguage.BENGALI)
        val bengali = generator.generate(result, AppLanguageMode.BENGALI, DetectedLanguage.ENGLISH)

        assertEquals("Settings opened. (Complete the change manually.)", english.displayBody)
        assertEquals("সেটিংস খোলা হয়েছে। (পরিবর্তনটি নিজে সম্পন্ন করুন।)", bengali.displayBody)
        assertEquals("SETTINGS_OPENED", bengali.outcomeBadge)
        assertFalse(bengali.isError)
    }

    @Test
    fun `uses a routine specific title and badge for partial automation results`() {
        val result = ActionResult.PartialSuccess(
            summaryEn = "Routine 'Night mode' completed: 1 of 2 steps succeeded.",
            summaryBn = "'Night mode' রুটিনে ২টির মধ্যে ১টি ধাপ সফল হয়েছে।",
            reasonEn = "Steps needing attention: #1 (FAILED).",
            reasonBn = "যেসব ধাপে মনোযোগ প্রয়োজন: #1 (FAILED)।",
            titleEn = "Routine needs attention",
            titleBn = "রুটিনে পদক্ষেপ প্রয়োজন",
            outcomeBadge = "ROUTINE_PARTIAL"
        )

        val english = generator.generate(result, AppLanguageMode.ENGLISH, DetectedLanguage.ENGLISH)
        val bengali = generator.generate(result, AppLanguageMode.BENGALI, DetectedLanguage.BENGALI)

        assertEquals("Routine needs attention", english.displayTitle)
        assertEquals("ROUTINE_PARTIAL", english.outcomeBadge)
        assertEquals("রুটিনে পদক্ষেপ প্রয়োজন", bengali.displayTitle)
        assertEquals("ROUTINE_PARTIAL", bengali.outcomeBadge)
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
