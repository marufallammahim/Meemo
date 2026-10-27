package com.marufallammahim.meemo.root

enum class RootStatus {
    NOT_CHECKED,
    UNAVAILABLE_NO_BINARY,
    BINARY_FOUND_UNAUTHORIZED,
    AUTHORIZED_READY
}

data class RootCommandSpec(
    val commandId: String,
    val displayName: String,
    val description: String,
    val executablePath: String,
    val staticArgs: List<String>,
    val acceptsSingleSafeParameter: Boolean = false,
    val timeoutMs: Long = 5000L
)

data class RootExecutionReport(
    val commandId: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long
)
