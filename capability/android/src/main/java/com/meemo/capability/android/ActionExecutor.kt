package com.meemo.capability.android

import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.CancellationException
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
        } catch (cancelled: CancellationException) {
            // Cancellation is a lifecycle signal, not an action failure.
            throw cancelled
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
        val stream = AudioManager.STREAM_MUSIC
        val maxVolume = audioManager.getStreamMaxVolume(stream)
        if (maxVolume <= 0) {
            return ActionResult.Unsupported("Media volume control is not available on this device.")
        }

        val targetVolume = when {
            intent.percent != null -> {
                (intent.percent.coerceIn(0, 100) / 100f * maxVolume).roundToInt()
            }
            intent.delta != null -> {
                val currentVolume = audioManager.getStreamVolume(stream)
                (currentVolume.toLong() + intent.delta.toLong()).coerceIn(0L, maxVolume.toLong()).toInt()
            }
            else -> return ActionResult.Unsupported("A target volume level was not provided.")
        }.coerceIn(0, maxVolume)

        audioManager.setStreamVolume(stream, targetVolume, AudioManager.FLAG_SHOW_UI)
        val verifiedVolume = audioManager.getStreamVolume(stream).coerceIn(0, maxVolume)
        val verifiedPercent = (verifiedVolume.toFloat() / maxVolume * 100).roundToInt()
        return ActionResult.Success("Media volume is now $verifiedPercent%.")
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

    private suspend fun openApp(query: String): ActionResult {
        val packageManager = context.packageManager
        val launchableApps = withContext(Dispatchers.IO) {
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            packageManager.queryIntentActivities(launcherIntent, 0)
                .mapNotNull { resolveInfo ->
                    val appInfo = resolveInfo.activityInfo?.applicationInfo ?: return@mapNotNull null
                    val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                        ?: return@mapNotNull null
                    ResolvedLaunchableApp(
                        app = LaunchableApp(
                            packageName = appInfo.packageName,
                            label = appInfo.loadLabel(packageManager).toString()
                        ),
                        launchIntent = launchIntent
                    )
                }
                .distinctBy { it.app.packageName }
        }

        val matches = matchLaunchableApps(query, launchableApps.map { it.app })
        val selected = matches.singleOrNull()?.let { match ->
            launchableApps.firstOrNull { it.app.packageName == match.packageName }
        }
        if (selected == null) {
            return if (matches.size > 1) {
                ActionResult.NeedsUserAction("More than one app matches “$query”. Please choose the app.")
            } else {
                ActionResult.Unsupported("I could not find an installed app named “$query”.")
            }
        }

        selected.launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(selected.launchIntent)
        return ActionResult.Success("Opening ${selected.app.label}.")
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

    private data class ResolvedLaunchableApp(
        val app: LaunchableApp,
        val launchIntent: Intent
    )
}
