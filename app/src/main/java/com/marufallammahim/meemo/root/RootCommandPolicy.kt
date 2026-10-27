package com.marufallammahim.meemo.root

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Strict compile-time allowlist for isolated root operations.
 * Rejects unknown command IDs, shell metacharacters, and unvalidated user/AI input.
 */
@Singleton
class RootCommandPolicy @Inject constructor() {

    private val approvedCommands: Map<String, RootCommandSpec> = listOf(
        RootCommandSpec(
            commandId = "ROOT_BATTERY_STATS_DUMP",
            displayName = "System Battery Service Dump",
            description = "Reads extended battery status via 'dumpsys battery'",
            executablePath = "/system/bin/dumpsys",
            staticArgs = listOf("battery"),
            acceptsSingleSafeParameter = false
        ),
        RootCommandSpec(
            commandId = "ROOT_THERMAL_STATUS",
            displayName = "Thermal Service Status",
            description = "Reads thermal status via 'dumpsys thermalservice'",
            executablePath = "/system/bin/dumpsys",
            staticArgs = listOf("thermalservice"),
            acceptsSingleSafeParameter = false
        ),
        RootCommandSpec(
            commandId = "ROOT_PACKAGE_PATH_CHECK",
            displayName = "Package APK Path Lookup",
            description = "Reads APK path for a validated package identifier via 'pm path <package>'",
            executablePath = "/system/bin/pm",
            staticArgs = listOf("path"),
            acceptsSingleSafeParameter = true
        )
    ).associateBy { it.commandId }

    fun getApprovedCommand(commandId: String): RootCommandSpec? = approvedCommands[commandId]

    fun listApprovedCommands(): List<RootCommandSpec> = approvedCommands.values.toList()

    fun isCommandAllowed(commandId: String, optionalParam: String?): Boolean {
        val spec = approvedCommands[commandId] ?: return false
        if (optionalParam != null) {
            if (!spec.acceptsSingleSafeParameter) return false
            if (!SAFE_PARAM_REGEX.matches(optionalParam)) return false
            if (FORBIDDEN_METACHARS.containsMatchIn(optionalParam)) return false
        }
        return true
    }

    fun buildSafeCommandString(commandId: String, optionalParam: String?): String? {
        if (!isCommandAllowed(commandId, optionalParam)) return null
        val spec = approvedCommands[commandId] ?: return null
        val base = (listOf(spec.executablePath) + spec.staticArgs).joinToString(" ")
        return if (spec.acceptsSingleSafeParameter && !optionalParam.isNullOrBlank()) {
            "$base ${optionalParam.trim()}"
        } else {
            base
        }
    }

    companion object {
        private val SAFE_PARAM_REGEX = Regex("^[a-zA-Z0-9._\\-]{1,64}$")
        private val FORBIDDEN_METACHARS = Regex("[;|&`\\$><\\n\\r\\s\"\'\\\\]")
    }
}
