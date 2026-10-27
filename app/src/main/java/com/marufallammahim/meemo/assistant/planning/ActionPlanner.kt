package com.marufallammahim.meemo.assistant.planning

import com.marufallammahim.meemo.actions.ActionResult
import com.marufallammahim.meemo.actions.MediaCommandType
import com.marufallammahim.meemo.actions.MeemoAction
import com.marufallammahim.meemo.apps.AppResolutionResult
import com.marufallammahim.meemo.apps.PackageResolver
import com.marufallammahim.meemo.assistant.entity.ExtractedEntity
import com.marufallammahim.meemo.assistant.entity.SettingsTarget
import com.marufallammahim.meemo.assistant.intent.MeemoIntent
import com.marufallammahim.meemo.core.error.MeemoError
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ActionPlanOutcome {
    data class Planned(val action: MeemoAction) : ActionPlanOutcome
    data class ImmediateResult(val result: ActionResult) : ActionPlanOutcome
}

@Singleton
class ActionPlanner @Inject constructor(
    private val packageResolver: PackageResolver
) {
    fun plan(
        intent: MeemoIntent,
        entity: ExtractedEntity,
        preferredBrowserPackage: String?
    ): ActionPlanOutcome {
        return when (intent) {
            MeemoIntent.OPEN_APPLICATION -> {
                val appQuery = (entity as? ExtractedEntity.AppQuery)?.appName.orEmpty()
                if (appQuery.isBlank()) {
                    return ActionPlanOutcome.ImmediateResult(
                        ActionResult.Failed(MeemoError.UnrecognizedCommand("Missing application name"))
                    )
                }
                when (val res = packageResolver.resolveAppByName(appQuery)) {
                    is AppResolutionResult.ExactOrSingleMatch -> ActionPlanOutcome.Planned(
                        MeemoAction.OpenApplication(
                            packageName = res.app.packageName,
                            appLabel = res.app.appLabel
                        )
                    )
                    is AppResolutionResult.AmbiguousMatches -> ActionPlanOutcome.ImmediateResult(
                        ActionResult.Ambiguous(query = appQuery, candidateApps = res.candidates)
                    )
                    AppResolutionResult.NotFound -> ActionPlanOutcome.ImmediateResult(
                        ActionResult.Failed(MeemoError.ApplicationNotFound(appQuery))
                    )
                }
            }
            MeemoIntent.OPEN_BROWSER -> ActionPlanOutcome.Planned(
                MeemoAction.OpenBrowser(preferredPackage = preferredBrowserPackage)
            )
            MeemoIntent.TORCH_ON -> ActionPlanOutcome.Planned(MeemoAction.TurnTorchOn)
            MeemoIntent.TORCH_OFF -> ActionPlanOutcome.Planned(MeemoAction.TurnTorchOff)
            MeemoIntent.GET_BATTERY_STATUS -> ActionPlanOutcome.Planned(MeemoAction.GetBatteryStatus)
            MeemoIntent.GET_DEVICE_INFORMATION -> ActionPlanOutcome.Planned(MeemoAction.GetDeviceInformation)
            MeemoIntent.SET_VOLUME -> {
                val vol = entity as? ExtractedEntity.VolumePercentage
                    ?: ExtractedEntity.VolumePercentage(50)
                ActionPlanOutcome.Planned(
                    MeemoAction.SetVolume(levelPercent = vol.levelPercent, streamName = vol.streamName)
                )
            }
            MeemoIntent.ADJUST_VOLUME -> {
                val adj = entity as? ExtractedEntity.VolumeAdjustment
                    ?: return ActionPlanOutcome.ImmediateResult(
                        ActionResult.Failed(MeemoError.UnrecognizedCommand("Volume direction missing"))
                    )
                ActionPlanOutcome.Planned(MeemoAction.AdjustVolume(adj.direction))
            }
            MeemoIntent.MEDIA_PLAY -> ActionPlanOutcome.Planned(MeemoAction.MediaControl(MediaCommandType.PLAY))
            MeemoIntent.MEDIA_PAUSE -> ActionPlanOutcome.Planned(MeemoAction.MediaControl(MediaCommandType.PAUSE))
            MeemoIntent.MEDIA_NEXT -> ActionPlanOutcome.Planned(MeemoAction.MediaControl(MediaCommandType.NEXT))
            MeemoIntent.MEDIA_PREVIOUS -> ActionPlanOutcome.Planned(MeemoAction.MediaControl(MediaCommandType.PREVIOUS))
            MeemoIntent.OPEN_SETTINGS -> {
                val dest = entity as? ExtractedEntity.SettingsDestination
                    ?: ExtractedEntity.SettingsDestination(SettingsTarget.MAIN_SETTINGS, false)
                ActionPlanOutcome.Planned(
                    MeemoAction.NavigateToSettings(
                        target = dest.target,
                        isFallbackForDirectToggle = dest.isDirectToggleAttempt
                    )
                )
            }
            MeemoIntent.READ_NOTIFICATIONS -> ActionPlanOutcome.Planned(MeemoAction.ReadRecentNotifications)
            MeemoIntent.STOP_SPEAKING -> ActionPlanOutcome.Planned(MeemoAction.StopSpeaking)
            MeemoIntent.CANCEL_COMMAND -> ActionPlanOutcome.Planned(MeemoAction.CancelActiveCommand)
            MeemoIntent.CREATE_ALIAS -> {
                val def = entity as? ExtractedEntity.AliasDefinition
                    ?: return ActionPlanOutcome.ImmediateResult(
                        ActionResult.Failed(MeemoError.UnrecognizedCommand("Invalid alias syntax"))
                    )
                when (val targetRes = packageResolver.resolveAppByName(def.targetAppName)) {
                    is AppResolutionResult.ExactOrSingleMatch -> ActionPlanOutcome.Planned(
                        MeemoAction.SaveCustomAlias(
                            triggerPhrase = def.triggerPhrase,
                            targetAppLabel = targetRes.app.appLabel,
                            resolvedPackageName = targetRes.app.packageName
                        )
                    )
                    is AppResolutionResult.AmbiguousMatches -> ActionPlanOutcome.ImmediateResult(
                        ActionResult.Ambiguous(query = def.targetAppName, candidateApps = targetRes.candidates)
                    )
                    AppResolutionResult.NotFound -> ActionPlanOutcome.ImmediateResult(
                        ActionResult.Failed(MeemoError.ApplicationNotFound(def.targetAppName))
                    )
                }
            }
            MeemoIntent.RUN_AUTOMATION -> {
                val trigger = (entity as? ExtractedEntity.AutomationTrigger)?.routineName.orEmpty()
                ActionPlanOutcome.Planned(
                    MeemoAction.ExecuteAutomationRoutine(
                        routineName = trigger,
                        steps = listOf(MeemoAction.TurnTorchOff, MeemoAction.GetBatteryStatus)
                    )
                )
            }
            MeemoIntent.ROOT_SYSTEM_DIAGNOSTIC -> {
                val rootEntity = entity as? ExtractedEntity.RootOperationTarget
                    ?: ExtractedEntity.RootOperationTarget("ROOT_BATTERY_STATS_DUMP")
                ActionPlanOutcome.Planned(
                    MeemoAction.ExecuteApprovedRootDiagnostic(
                        policyCommandId = rootEntity.policyCommandId,
                        parameter = rootEntity.parameter,
                        requiresConfirmation = true
                    )
                )
            }
            MeemoIntent.UNKNOWN -> ActionPlanOutcome.ImmediateResult(
                ActionResult.Failed(MeemoError.UnrecognizedCommand(""))
            )
        }
    }
}
