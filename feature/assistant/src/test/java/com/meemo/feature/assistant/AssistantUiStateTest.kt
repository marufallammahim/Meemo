package com.meemo.feature.assistant

import androidx.lifecycle.ViewModelStore
import com.meemo.core.model.ActionResult
import com.meemo.core.model.Command
import com.meemo.core.model.InputSource
import com.meemo.core.model.MeemoIntent
import com.meemo.domain.command.CommandActionExecutor
import com.meemo.domain.command.CommandParserPort
import com.meemo.domain.usecase.ProcessCommandUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantUiStateTest {
    @Test
    fun successMapsToSuccessStatus() {
        assertEquals("Success", ActionResult.Success("Done").toAssistantText().second)
    }

    @Test
    fun unsupportedMapsToUnsupportedStatus() {
        assertEquals("Unsupported", ActionResult.Unsupported("No").toAssistantText().second)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun viewModelDoesNotTurnCancellationIntoAnErrorMessage() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = AssistantViewModel(suspendingProcessCommand())
            val store = ViewModelStore().apply { put("assistant", viewModel) }

            viewModel.onInputChanged("battery")
            viewModel.sendCommand()
            runCurrent()

            assertTrue(viewModel.uiState.value.isProcessing)
            store.clear()
            runCurrent()

            assertEquals(1, viewModel.uiState.value.messages.size)
            assertTrue(viewModel.uiState.value.messages.single().isUser)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun suspendingProcessCommand(): ProcessCommandUseCase {
        val parser = object : CommandParserPort {
            override fun parse(rawText: String, source: InputSource): Command =
                Command(rawText, rawText, MeemoIntent.GetBattery, 1f, source)
        }
        val executor = object : CommandActionExecutor {
            override suspend fun execute(command: Command): ActionResult = awaitCancellation()
        }
        return ProcessCommandUseCase(parser, executor)
    }
}
