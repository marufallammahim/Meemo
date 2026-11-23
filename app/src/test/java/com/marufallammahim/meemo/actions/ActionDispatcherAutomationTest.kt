package com.marufallammahim.meemo.actions

import com.marufallammahim.meemo.actions.device.TorchActionExecutor
import com.marufallammahim.meemo.actions.media.VolumeMediaActionExecutor
import com.marufallammahim.meemo.core.error.MeemoError
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionDispatcherAutomationTest {
    private val torchExecutor = mockk<TorchActionExecutor>(relaxed = true)
    private val volumeMediaExecutor = mockk<VolumeMediaActionExecutor>(relaxed = true)

    private val dispatcher = ActionDispatcher(
        openAppExecutor = mockk(relaxed = true),
        openBrowserExecutor = mockk(relaxed = true),
        torchExecutor = torchExecutor,
        deviceInfoExecutor = mockk(relaxed = true),
        volumeMediaExecutor = volumeMediaExecutor,
        settingsExecutor = mockk(relaxed = true),
        notificationReadAloud = mockk(relaxed = true),
        ttsController = mockk(relaxed = true),
        rootManager = mockk(relaxed = true),
        repository = mockk(relaxed = true)
    )

    @Test
    fun `partial routine result identifies successful count and failed step indexes`() = runTest {
        every { torchExecutor.executeTorch(turnOn = false) } returns
            ActionResult.Failed(MeemoError.HardwareUnavailable("Camera Flash"))
        every {
            volumeMediaExecutor.executeMediaControl(MeemoAction.MediaControl(MediaCommandType.PAUSE))
        } returns ActionResult.Success("Media paused.", "মিডিয়া থামানো হয়েছে。")

        val result = dispatcher.dispatch(
            MeemoAction.ExecuteAutomationRoutine(
                routineName = "Night mode",
                steps = listOf(
                    MeemoAction.TurnTorchOff,
                    MeemoAction.MediaControl(MediaCommandType.PAUSE)
                )
            )
        )

        val partial = result as ActionResult.PartialSuccess
        assertEquals("ROUTINE_PARTIAL", partial.outcomeBadge)
        assertEquals("Routine needs attention", partial.titleEn)
        assertTrue(partial.summaryEn.contains("1 of 2 steps succeeded"))
        assertTrue(partial.summaryBn.contains("1টি ধাপ"))
        assertTrue(partial.reasonEn.contains("#1 (FAILED)"))
        assertTrue(partial.reasonBn.contains("#1 (FAILED)"))
    }
}
