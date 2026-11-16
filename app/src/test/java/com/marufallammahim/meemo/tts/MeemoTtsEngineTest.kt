package com.marufallammahim.meemo.tts

import org.junit.Assert.assertEquals
import org.junit.Test

class MeemoTtsEngineTest {

    @Test
    fun \`clamps finite speech rates to supported range\`() {
        assertEquals(0.5f, MeemoTtsEngine.normalizeSpeechRate(0.1f))
        assertEquals(1.5f, MeemoTtsEngine.normalizeSpeechRate(1.5f))
        assertEquals(2.0f, MeemoTtsEngine.normalizeSpeechRate(3.0f))
    }

    @Test
    fun \`falls back to normal rate for non finite speech rates\`() {
        assertEquals(1.0f, MeemoTtsEngine.normalizeSpeechRate(Float.NaN))
        assertEquals(1.0f, MeemoTtsEngine.normalizeSpeechRate(Float.POSITIVE_INFINITY))
        assertEquals(1.0f, MeemoTtsEngine.normalizeSpeechRate(Float.NEGATIVE_INFINITY))
    }
}
