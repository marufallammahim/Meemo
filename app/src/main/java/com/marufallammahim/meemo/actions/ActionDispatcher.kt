package com.marufallammahim.meemo.actions

import com.marufallammahim.meemo.actions.app.OpenAppActionExecutor
import com.marufallammahim.meemo.actions.app.OpenBrowserActionExecutor
import com.marufallammahim.meemo.actions.device.DeviceInfoActionExecutor
import com.marufallammahim.meemo.actions.device.TorchActionExecutor
import com.marufallammahim.meemo.actions.media.VolumeMediaActionExecutor
import com.marufallammahim.meemo.actions.settings.SettingsActionExecutor
import com.marufallammahim.meemo.core.result.ResultWrapper
import com.marufallammahim.meemo.domain.repository.MeemoRepository
import com.marufallammahim.meemo.notifications.NotificationReadAloud
import com.marufallammahim.meemo.root.RootManager
import com.marufallammahim.meemo.tts.TtsController
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central dispatcher routing validated MeemoAction instances to their platform executors.
 */
@Singleton
class ActionDispatcher @Inject constructor(
    private val openAppExecutor: OpenAppActionExecutor,
    private val openBrowserExecutor: OpenBrowserActionExecutor,
    private val torchExecutor: TorchActionExecutor,
    private val deviceInfoExecutor: DeviceInfoActionExecutor,
    private val volumeMediaExecutor: VolumeMediaActionExecutor,
    private val settingsExecutor: SettingsActionExecutor,
    private val notificationReadAloud: NotificationReadAloud,
    private val ttsController: TtsController,
    private val rootManager: RootManager,
    private val repository: MeemoRepository
) {
    suspend fun dispatch(action: MeemoAction): ActionResult {
        return when (action) {
            is MeemoAction.OpenApplication -> openAppExecutor.execute(action)
            is MeemoAction.OpenBrowser -> openBrowserExecutor.execute(action)
            MeemoAction.TurnTorchOn -> torchExecutor.executeTorch(turnOn = true)
            MeemoAction.TurnTorchOff -> torchExecutor.executeTorch(turnOn = false)
            MeemoAction.GetBatteryStatus -> deviceInfoExecutor.readBatteryStatus()
            MeemoAction.GetDeviceInformation -> deviceInfoExecutor.readDeviceInformation()
            is MeemoAction.SetVolume -> volumeMediaExecutor.executeSetVolume(action)
            is MeemoAction.AdjustVolume -> volumeMediaExecutor.executeAdjustVolume(action)
            is MeemoAction.MediaControl -> volumeMediaExecutor.executeMediaControl(action)
            is MeemoAction.NavigateToSettings -> settingsExecutor.executeNavigateSettings(action)
            MeemoAction.ReadRecentNotifications -> notificationReadAloud.readQueuedNotificationsSummary()
            MeemoAction.StopSpeaking -> {
                ttsController.stopSpeaking()
                ActionResult.Success(
                    summaryEn = "Stopped speaking.",
                    summaryBn = "বলা বন্ধ করা হয়েছে।",
                    verifiedBySystem = true
                )
            }
            MeemoAction.CancelActiveCommand -> ActionResult.Cancelled()
            is MeemoAction.SaveCustomAlias -> {
                repository.saveAlias(
                    triggerPhrase = action.triggerPhrase,
                    targetIntent = "OPEN_APPLICATION",
                    targetEntity = action.resolvedPackageName,
                    requiresConfirmation = false
                )
                ActionResult.Success(
                    summaryEn = "Saved alias '${action.triggerPhrase}' -> ${action.targetAppLabel}.",
                    summaryBn = "'${action.triggerPhrase}' অ্যালিয়াসটি ${action.targetAppLabel}-এর জন্য সংরক্ষণ করা হয়েছে।",
                    verifiedBySystem = true
                )
            }
            is MeemoAction.ExecuteAutomationRoutine -> {
                val results = action.steps.map { step -> dispatch(step) }
                val successCount = results.count { it is ActionResult.Success }
                if (successCount == results.size) {
                    ActionResult.Success(
                        summaryEn = "Executed routine '${action.routineName}' (${results.size} steps).",
                        summaryBn = "'${action.routineName}' রুটিন সম্পন্ন হয়েছে (${results.size}টি ধাপ)।",
                        verifiedBySystem = true
                    )
                } else {
                    val stepsNeedingAttention = results.withIndex()
                        .filter { (_, result) -> result !is ActionResult.Success }
                        .joinToString(", ") { (index, result) ->
                            "#${index + 1} (${result.outcomeCode})"
                        }
                    ActionResult.PartialSuccess(
                        summaryEn = "Routine '${action.routineName}' completed: $successCount of ${results.size} steps succeeded.",
                        summaryBn = "'${action.routineName}' রুটিনে ${results.size}টির মধ্যে ${successCount}টি ধাপ সফল হয়েছে।",
                        reasonEn = "Steps needing attention: $stepsNeedingAttention.",
                        reasonBn = "যেসব ধাপে মনোযোগ প্রয়োজন: $stepsNeedingAttention।",
                        titleEn = "Routine needs attention",
                        titleBn = "রুটিনে পদক্ষেপ প্রয়োজন",
                        outcomeBadge = "ROUTINE_PARTIAL"
                    )
                }
            }
            is MeemoAction.ExecuteApprovedRootDiagnostic -> {
                val prefs = repository.userPreferencesFlow.first()
                when (val res = rootManager.runApprovedDiagnostic(
                    commandId = action.policyCommandId,
                    parameter = action.parameter,
                    userEnabledRoot = prefs.rootFeaturesEnabled
                )) {
                    is ResultWrapper.Success -> ActionResult.Success(
                        summaryEn = "Root diagnostic (${res.data.commandId}) succeeded in ${res.data.durationMs}ms: ${res.data.stdout.take(180)}",
                        summaryBn = "রুট ডায়াগনস্টিক (${res.data.commandId}) সম্পন্ন হয়েছে: ${res.data.stdout.take(180)}",
                        verifiedBySystem = true
                    )
                    is ResultWrapper.Failure -> ActionResult.Failed(res.error)
                }
            }
        }
    }
}
