package com.meemo.capability.android

import android.content.Context
import android.os.BatteryManager
import com.meemo.core.model.Command
import com.meemo.core.model.InputSource
import com.meemo.core.model.MeemoIntent
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertThrows
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActionExecutorCancellationTest {
    @Test
    fun `execute rethrows cancellation instead of converting it to a failure`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val context = mockk<Context>()
            every { context.getSystemService(BatteryManager::class.java) } throws CancellationException("cancelled")
            val command = Command(
                rawText = "battery",
                normalizedText = "battery",
                intent = MeemoIntent.GetBattery,
                confidence = 1f,
                source = InputSource.TEXT
            )

            assertThrows(CancellationException::class.java) {
                kotlinx.coroutines.runBlocking { ActionExecutor(context).execute(command) }
            }
        } finally {
            Dispatchers.resetMain()
        }
    }
}
