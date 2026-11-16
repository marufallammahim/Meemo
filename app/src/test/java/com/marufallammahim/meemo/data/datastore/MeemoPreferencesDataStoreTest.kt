package com.marufallammahim.meemo.data.datastore

import org.junit.Assert.assertEquals
import org.junit.Test

class MeemoPreferencesDataStoreTest {

    @Test
    fun `clamps finite speech rates to supported bounds`() {
        assertEquals(0.5f, normalizeStoredSpeechRate(0.1f), 0.0f)
        assertEquals(1.25f, normalizeStoredSpeechRate(1.25f), 0.0f)
        assertEquals(2.0f, normalizeStoredSpeechRate(3.0f), 0.0f)
    }

    @Test
    fun `replaces non finite speech rates with normal speed`() {
        assertEquals(1.0f, normalizeStoredSpeechRate(Float.NaN), 0.0f)
        assertEquals(1.0f, normalizeStoredSpeechRate(Float.POSITIVE_INFINITY), 0.0f)
        assertEquals(1.0f, normalizeStoredSpeechRate(Float.NEGATIVE_INFINITY), 0.0f)
    }
}
