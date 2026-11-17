package com.meemo.feature.assistant

import com.meemo.core.model.ActionResult
import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantUiStateTest {
    @Test fun successMapsToSuccessStatus() {
        assertEquals("Success", ActionResult.Success("Done").toAssistantText().second)
    }

    @Test fun unsupportedMapsToUnsupportedStatus() {
        assertEquals("Unsupported", ActionResult.Unsupported("No").toAssistantText().second)
    }
}
