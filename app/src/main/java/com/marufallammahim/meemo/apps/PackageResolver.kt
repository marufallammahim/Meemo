package com.marufallammahim.meemo.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.marufallammahim.meemo.domain.model.InstalledAppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AppResolutionResult {
    data class ExactOrSingleMatch(val app: InstalledAppInfo) : AppResolutionResult
    data class AmbiguousMatches(val candidates: List<InstalledAppInfo>) : AppResolutionResult
    data object NotFound : AppResolutionResult
}

/**
 * Dynamically discovers installed launchable applications via PackageManager
 * and ranks candidates using exact, prefix, and token matching.
 * Never hardcodes Chrome, YouTube, or WhatsApp as the only choices.
 */
@Singleton
class PackageResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getLaunchableApps(): List<InstalledAppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        return resolveInfos
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
                if (label.isEmpty()) return@mapNotNull null
                InstalledAppInfo(
                    packageName = pkg,
                    appLabel = label,
                    isBrowser = false,
                    isLaunchable = true
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appLabel.lowercase() }
    }

    fun resolveAppByName(
        query: String,
        installedApps: List<InstalledAppInfo> = getLaunchableApps()
    ): AppResolutionResult {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty() || installedApps.isEmpty()) {
            return AppResolutionResult.NotFound
        }

        // 1. Exact label or package match
        val exactMatches = installedApps.filter {
            it.appLabel.lowercase() == cleanQuery || it.packageName.lowercase() == cleanQuery
        }
        if (exactMatches.size == 1) {
            return AppResolutionResult.ExactOrSingleMatch(exactMatches.first())
        }
        if (exactMatches.size > 1) {
            return AppResolutionResult.AmbiguousMatches(exactMatches)
        }

        // 2. Prefix match on app label or word token
        val prefixMatches = installedApps.filter { app ->
            val labelLower = app.appLabel.lowercase()
            labelLower.startsWith(cleanQuery) ||
                labelLower.split(" ", "-", "_").any { token -> token == cleanQuery }
        }
        if (prefixMatches.size == 1) {
            return AppResolutionResult.ExactOrSingleMatch(prefixMatches.first())
        }
        if (prefixMatches.size > 1) {
            return AppResolutionResult.AmbiguousMatches(prefixMatches.take(6))
        }

        // 3. Substring match (minimum 3 characters to avoid noisy single-letter matches)
        if (cleanQuery.length >= 3) {
            val substringMatches = installedApps.filter { app ->
                app.appLabel.lowercase().contains(cleanQuery)
            }
            if (substringMatches.size == 1) {
                return AppResolutionResult.ExactOrSingleMatch(substringMatches.first())
            }
            if (substringMatches.size > 1) {
                return AppResolutionResult.AmbiguousMatches(substringMatches.take(6))
            }
        }

        return AppResolutionResult.NotFound
    }
}
