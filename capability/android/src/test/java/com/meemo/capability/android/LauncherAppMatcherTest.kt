package com.meemo.capability.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherAppMatcherTest {
    @Test
    fun `exact label takes precedence over partial matches`() {
        val exact = LaunchableApp("com.example.chrome", "Chrome")
        val apps = listOf(
            LaunchableApp("com.example.chrome.beta", "Chrome Beta"),
            exact
        )

        assertEquals(listOf(exact), matchLaunchableApps("Chrome", apps))
    }

    @Test
    fun `duplicate launcher activities from one package are treated as one app`() {
        val app = LaunchableApp("com.example.camera", "Camera")
        val duplicateActivity = app.copy(label = "Camera")

        assertEquals(listOf(app), matchLaunchableApps("Camera", listOf(app, duplicateActivity)))
    }

    @Test
    fun `partial matches remain ambiguous when they refer to different packages`() {
        val apps = listOf(
            LaunchableApp("com.example.photos", "Photos"),
            LaunchableApp("com.example.gallery", "Photo Gallery")
        )

        assertEquals(2, matchLaunchableApps("photo", apps).size)
    }

    @Test
    fun `blank query never matches every installed app`() {
        assertTrue(matchLaunchableApps("  ", listOf(LaunchableApp("com.example.app", "Example"))).isEmpty())
    }
}
