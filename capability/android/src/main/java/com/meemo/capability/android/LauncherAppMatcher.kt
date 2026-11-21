package com.meemo.capability.android

internal data class LaunchableApp(
    val packageName: String,
    val label: String
)

/**
 * Prefers exact app-label matches and never treats multiple launcher activities
 * from the same package as separate app choices.
 */
internal fun matchLaunchableApps(query: String, apps: List<LaunchableApp>): List<LaunchableApp> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return emptyList()

    val uniqueApps = apps.distinctBy { it.packageName }
    val exactMatches = uniqueApps.filter { it.label.equals(normalizedQuery, ignoreCase = true) }
    return exactMatches.ifEmpty {
        uniqueApps.filter { it.label.contains(normalizedQuery, ignoreCase = true) }
    }
}
