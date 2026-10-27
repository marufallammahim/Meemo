package com.marufallammahim.meemo.root

import com.marufallammahim.meemo.core.result.ResultWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootManager @Inject constructor(
    private val policy: RootCommandPolicy,
    private val shellExecutor: ShellExecutor
) {
    fun isRootBinaryPresent(): Boolean = ShellExecutor.hasSuBinaryOnFilesystem()

    fun getCurrentRootStatus(userEnabledRoot: Boolean): RootStatus {
        if (!isRootBinaryPresent()) return RootStatus.UNAVAILABLE_NO_BINARY
        return if (userEnabledRoot) RootStatus.AUTHORIZED_READY else RootStatus.BINARY_FOUND_UNAUTHORIZED
    }

    fun getApprovedCommandSpecs(): List<RootCommandSpec> = policy.listApprovedCommands()

    fun runApprovedDiagnostic(
        commandId: String,
        parameter: String?,
        userEnabledRoot: Boolean
    ): ResultWrapper<RootExecutionReport> {
        if (!userEnabledRoot) {
            return ResultWrapper.Failure(
                com.marufallammahim.meemo.core.error.MeemoError.RootUnavailableOrRejected(
                    "Root features are disabled in Meemo settings"
                )
            )
        }
        if (!isRootBinaryPresent()) {
            return ResultWrapper.Failure(
                com.marufallammahim.meemo.core.error.MeemoError.RootUnavailableOrRejected(
                    "No su binary found on this device"
                )
            )
        }
        return shellExecutor.executeApprovedCommand(commandId, parameter)
    }
}
