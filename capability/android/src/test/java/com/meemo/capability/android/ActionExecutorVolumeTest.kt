package com.meemo.capability.android

import android.content.Context
import android.media.AudioManager
import com.meemo.core.model.ActionResult
import com.meemo.core.model.Command
import com.meemo.core.model.InputSource
import com.meemo.core.model.MeemoIntent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActionExecutorVolumeTest {
    @Test
    fun `sets media stream to the requested percentage`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val context = mockk<Context>()
            val audioManager = mockk<AudioManager>()
            every { context.getSystemService(AudioManager::class.java) } returns audioManager
            every { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) } returns 15
            every { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 8, AudioManager.FLAG_SHOW_UI) } just runs
            every { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) } returns 8

            val command = Command(
                rawText = "set volume to 50 percent",
                normalizedText = "set volume to 50 percent",
                intent = MeemoIntent.SetVolume(percent = 50, delta = null),
                confidence = 0.98f,
                source = InputSource.TEXT
            )
            val result = ActionExecutor(context).execute(command)

            assertTrue(result is ActionResult.Success)
            verify(exactly = 1) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 8, AudioManager.FLAG_SHOW_UI)
            }
        } finally {
            Dispatchers.resetMain()
        }
    }
}
