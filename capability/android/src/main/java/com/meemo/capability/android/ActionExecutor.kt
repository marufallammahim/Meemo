package com.meemo.capability.android

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import com.meemo.core.model.ActionResult
import com.meemo.core.model.Command
import com.meemo.core.model.MeemoIntent
import com.meemo.core.model.SettingsPage
import com.meemo.domain.command.CommandActionExecutor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class ActionExecutor(
    @ApplicationContext private val context: Context
) : CommandActionExecutor {
    override suspend fun execute(command: Command): ActionResult = withContext(Dispatchers.Main.immediate) {
        try {
            when (val intent = command.intent) {
                MeemoIntent.GetBattery -> batteryStatus()
                MeemoIntent.OpenBrowser -> openBrowser()
                is MeemoIntent.OpenApp -> openApp(intent.query)
                is MeemoIntent.OpenSettings -> openSettings(intent.page)
                is MeemoIntent.SetVolume -> setVolume(intent)
                is MeemoIntent.Unknown -> ActionResult.Unsupported("I could not understand “${intent.raw}”.")
                else -> ActionResult.Unsupported("This action is not available in the text assistant yet.")
            }
        } catch (exception: Exception) {
            ActionResult.Failed("Meemo could not complete that action.", exception)
        }
    }

    private fun batteryStatus(): ActionResult {
        val manager = context.getSystemService(BatteryManager::class.java)
        val level = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        return if (level in 0..100) {
            ActionResult.Success("Battery level is $level%.")
        } else {
            ActionResult.Unsupported("Battery information is not available on this device.")
        }
    }

    private fun setVolume(intent: MeemoIntent.SetVolume): ActionResult {
        val audioManager = context.getSystemService(AudioManager::class.java)
            ?: return ActionResult.Unsupported("Media volume control is not available on this device.")
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (maxVolume <= 0) {
            return ActionResult.Unsupported("Media volume control is not available on this device.")
        }
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val targetVolume = calculateVolumeTarget(
            currentVolume = currentVolume,
            maxVolume = maxVolume,
            percent = intent.percent,
            delta = intent.delta
        ) ?: return ActionResult.Unsupported("Choose a media volume from 0 to 100 percent.")

        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            targetVolume,
            AudioManager.FLAG_SHOW_UI
        )
        val actualVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (actualVolume != targetVolume) {
            return ActionResult.Failed("Meemo could not verify the requested media volume.")
        }
        val actualPercent = (actualVolume * 100f / maxVolume).roundToInt()
        return ActionResult.Success("Media volume is now $actualPercent%.")
    }

    private fun openBrowser(): ActionResult {
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        context.startActivity(Intent.createChooser(viewIntent, "Open with browser").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        return ActionResult.Success("Opening your browser.")
    }

    private fun openApp(query: String): ActionResult {
        val packageManager = context.packageManager
        val matches = packageManager.getInstalledApplications(PackageManager.GET_META_DATA).mapNotNull { app ->
            val label = app.loadLabel(packageManager).toString()
            if (label.equals(query, true) || label.contains(query, true)) {
                packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                    Triple(label, it, app.packageName)
                }
            } else null
        }
        val selected = matches.firstOrNull { it.first.equals(query, true) } ?: matches.singleOrNull()
        if (selected == null) {
            return if (matches.size > 1) {
                ActionResult.NeedsUserAction("More than one app matches “$query”. Please choose the app.")
            } else {
                ActionResult.Unsupported("I could not find an installed app named “$query”.")
            }
        }
        selected.second.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(selected.second)
        return ActionResult.Success("Opening ${selected.first}.")
    }

    private fun openSettings(page: SettingsPage): ActionResult {
        val action = when (page) {
            SettingsPage.MAIN -> Settings.ACTION_SETTINGS
            SettingsPage.WIFI -> Settings.ACTION_WIFI_SETTINGS
            SettingsPage.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
            SettingsPage.DISPLAY -> Settings.ACTION_DISPLAY_SETTINGS
            SettingsPage.SOUND -> Settings.ACTION_SOUND_SETTINGS
            SettingsPage.BATTERY -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            SettingsPage.ACCESSIBILITY -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            SettingsPage.NOTIFICATIONS -> Settings.ACTION_APP_NOTIFICATION_SETTINGS
        }
        val settingsIntent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (page == SettingsPage.NOTIFICATIONS) putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        if (settingsIntent.resolveActivity(context.packageManager) == null) {
            return ActionResult.Unsupported("That settings page is not available on this device.")
        }
        context.startActivity(settingsIntent)
        return ActionResult.Success("Opening ${page.name.lowercase()} settings.")
    }
}
