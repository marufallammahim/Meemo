package com.marufallammahim.meemo.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.marufallammahim.meemo.domain.model.InstalledAppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface BrowserResolutionResult {
    data class ResolvedBrowser(val browser: InstalledAppInfo) : BrowserResolutionResult
    data class MultipleBrowsers(val candidates: List<InstalledAppInfo>) : BrowserResolutionResult
    data object NoBrowserFound : BrowserResolutionResult
}

/**
 * Dynamically resolves installed web browsers supporting ACTION_VIEW with https:// and CATEGORY_BROWSABLE.
 */
@Singleton
class BrowserResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getInstalledBrowsers(): List<InstalledAppInfo> {
        val pm = context.packageManager
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com")).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        val flags = PackageManager.MATCH_ALL
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(browserIntent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(browserIntent, flags)
        }

        return resolveInfos
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == "android" || pkg.contains("resolver")) return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifEmpty { pkg }
                InstalledAppInfo(
                    packageName = pkg,
                    appLabel = label,
                    isBrowser = true,
                    isLaunchable = true
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appLabel.lowercase() }
    }

    fun resolveDefaultOrCandidateBrowsers(
        userPreferredPackage: String?,
        installedBrowsers: List<InstalledAppInfo> = getInstalledBrowsers()
    ): BrowserResolutionResult {
        if (installedBrowsers.isEmpty()) return BrowserResolutionResult.NoBrowserFound

        if (!userPreferredPackage.isNullOrBlank()) {
            val preferred = installedBrowsers.firstOrNull { it.packageName == userPreferredPackage }
            if (preferred != null) {
                return BrowserResolutionResult.ResolvedBrowser(preferred)
            }
        }

        // Check Android system default browser if configured
        val pm = context.packageManager
        val probeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com")).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val defaultResolve = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(probeIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.resolveActivity(probeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        val defaultPkg = defaultResolve?.activityInfo?.packageName
        if (!defaultPkg.isNullOrBlank() && defaultPkg != "android") {
            val match = installedBrowsers.firstOrNull { it.packageName == defaultPkg }
            if (match != null) {
                return BrowserResolutionResult.ResolvedBrowser(match)
            }
        }

        return if (installedBrowsers.size == 1) {
            BrowserResolutionResult.ResolvedBrowser(installedBrowsers.first())
        } else {
            BrowserResolutionResult.MultipleBrowsers(installedBrowsers)
        }
    }
}
