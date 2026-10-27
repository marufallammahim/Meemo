package com.marufallammahim.meemo.root

import com.marufallammahim.meemo.core.error.MeemoError
import com.marufallammahim.meemo.core.logging.SafeLogger
import com.marufallammahim.meemo.core.result.ResultWrapper
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Isolated root shell executor.
 * Never accepts raw user or AI strings; executes only validated RootCommandSpec templates
 * from RootCommandPolicy with strict timeouts and output size caps.
 */
@Singleton
class ShellExecutor @Inject constructor(
    private val policy: RootCommandPolicy,
    private val safeLogger: SafeLogger
) {
    fun executeApprovedCommand(
        commandId: String,
        parameter: String? = null
    ): ResultWrapper<RootExecutionReport> {
        val spec = policy.getApprovedCommand(commandId)
            ?: return ResultWrapper.Failure(
                MeemoError.RootUnavailableOrRejected("Command ID '$commandId' is not in RootCommandPolicy")
            )

        val safeCmd = policy.buildSafeCommandString(commandId, parameter)
            ?: return ResultWrapper.Failure(
                MeemoError.RootUnavailableOrRejected("Rejected unsafe parameter for '$commandId'")
            )

        val startMs = System.currentTimeMillis()
        return try {
            val process = ProcessBuilder("su", "-c", safeCmd)
                .redirectErrorStream(false)
                .start()

            val finished = process.waitFor(spec.timeoutMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                return ResultWrapper.Failure(
                    MeemoError.RootUnavailableOrRejected("Root command timed out after ${spec.timeoutMs}ms")
                )
            }

            val stdout = process.inputStream.bufferedReader().use { it.readText().take(MAX_OUTPUT_CHARS) }
            val stderr = process.errorStream.bufferedReader().use { it.readText().take(MAX_OUTPUT_CHARS) }
            val exitCode = process.exitValue()
            val elapsed = System.currentTimeMillis() - startMs

            if (exitCode != 0) {
                safeLogger.warn("ShellExecutor", "Root command $commandId exited with code=$exitCode")
                return ResultWrapper.Failure(
                    MeemoError.RootUnavailableOrRejected("Exit code $exitCode: ${stderr.take(120)}")
                )
            }

            ResultWrapper.Success(
                RootExecutionReport(
                    commandId = commandId,
                    exitCode = exitCode,
                    stdout = stdout.trim(),
                    stderr = stderr.trim(),
                    durationMs = elapsed
                )
            )
        } catch (e: Exception) {
            safeLogger.error("ShellExecutor", "Failed running approved root command $commandId", e)
            ResultWrapper.Failure(
                MeemoError.RootUnavailableOrRejected(e.message ?: e.javaClass.simpleName)
            )
        }
    }

    companion object {
        private const val MAX_OUTPUT_CHARS = 16_384
        val STANDARD_SU_PATHS = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/vendor/bin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su"
        )

        fun hasSuBinaryOnFilesystem(paths: List<String> = STANDARD_SU_PATHS): Boolean {
            return paths.any { path ->
                runCatching { File(path).exists() }.getOrDefault(false)
            }
        }
    }
}
