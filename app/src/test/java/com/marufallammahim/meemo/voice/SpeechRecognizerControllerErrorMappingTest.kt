package com.marufallammahim.meemo.voice

import android.speech.SpeechRecognizer
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechRecognizerControllerErrorMappingTest {

    @Test
    fun `maps modern recognizer errors to actionable messages`() {
        assertEquals(
            "Too many recognition requests; wait and try again",
            SpeechRecognizerController.mapRecognizerErrorCode(SpeechRecognizer.ERROR_TOO_MANY_REQUESTS)
        )
        assertEquals(
            "Speech recognition service disconnected",
            SpeechRecognizerController.mapRecognizerErrorCode(SpeechRecognizer.ERROR_SERVER_DISCONNECTED)
        )
        assertEquals(
            "Selected recognition language is not supported",
            SpeechRecognizerController.mapRecognizerErrorCode(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED)
        )
        assertEquals(
            "Selected recognition language is not installed",
            SpeechRecognizerController.mapRecognizerErrorCode(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
        )
        assertEquals(
            "Recognition language support could not be checked",
            SpeechRecognizerController.mapRecognizerErrorCode(SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT)
        )
        assertEquals(
            "Recognition language download status is unavailable",
            SpeechRecognizerController.mapRecognizerErrorCode(
                SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS
            )
        )
    }

    @Test
    fun `keeps unknown platform errors diagnosable`() {
        assertEquals(
            "Speech error (999)",
            SpeechRecognizerController.mapRecognizerErrorCode(999)
        )
    }
}
