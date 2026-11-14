package com.marufallammahim.meemo.actions.app

import com.marufallammahim.meemo.actions.ActionExecutor
import com.marufallammahim.meemo.actions.ActionResult
import com.marufallammahim.meemo.actions.MeemoAction
import com.marufallammahim.meemo.apps.AppManager
import com.marufallammahim.meemo.apps.BrowserResolutionResult
import com.marufallammahim.meemo.apps.BrowserResolver
import com.marufallammahim.meemo.core.error.MeemoError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAppActionExecutor @Inject constructor(
    private val appManager: AppManager
) : ActionExecutor<MeemoAction.OpenApplication> {

    override suspend fun execute(action: MeemoAction.OpenApplication): ActionResult {
        val launched = appManager.launchPackage(action.packageName)
        return if (launched) {
            ActionResult.Success(
                summaryEn = "Opened ${action.appLabel}.",
                summaryBn = "${action.appLabel} খোলা হয়েছে।",
                verifiedBySystem = true,
                details = mapOf("packageName" to action.packageName)
            )
        } else {
            ActionResult.Failed(MeemoError.ApplicationNotFound(action.appLabel))
        }
    }
}

@Singleton
class OpenBrowserActionExecutor @Inject constructor(
    private val browserResolver: BrowserResolver,
    private val appManager: AppManager
) : ActionExecutor<MeemoAction.OpenBrowser> {

    override suspend fun execute(action: MeemoAction.OpenBrowser): ActionResult {
        return when (val resolution = browserResolver.resolveDefaultOrCandidateBrowsers(action.preferredPackage)) {
            is BrowserResolutionResult.ResolvedBrowser -> {
                val ok = appManager.launchBrowser(resolution.browser.packageName)
                if (ok) {
                    ActionResult.Success(
                        summaryEn = "Opened browser (${resolution.browser.appLabel}).",
                        summaryBn = "ব্রাউজার (${resolution.browser.appLabel}) খোলা হয়েছে।",
                        verifiedBySystem = true,
                        details = mapOf("packageName" to resolution.browser.packageName)
                    )
                } else {
                    ActionResult.Failed(MeemoError.BrowserNotFound)
                }
            }
            is BrowserResolutionResult.MultipleBrowsers -> {
                ActionResult.Ambiguous(
                    query = "Browser",
                    candidateApps = resolution.candidates
                )
            }
            BrowserResolutionResult.NoBrowserFound -> {
                ActionResult.Unavailable(MeemoError.BrowserNotFound)
            }
        }
    }
}
