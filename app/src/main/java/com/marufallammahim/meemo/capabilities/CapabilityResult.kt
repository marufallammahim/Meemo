package com.marufallammahim.meemo.capabilities

import com.marufallammahim.meemo.core.error.MeemoError

data class CapabilityDescriptor(
    val capabilityId: String,
    val displayNameEn: String,
    val displayNameBn: String,
    val minSdk: Int = 26,
    val requiresHardwareFeature: String? = null,
    val requiresRuntimePermission: String? = null,
    val requiresSpecialAccess: String? = null,
    val requiresRoot: Boolean = false,
    val requiresConfirmation: Boolean = false,
    val supportsVerification: Boolean = true
)

sealed interface CapabilityCheckResult {
    data object Ready : CapabilityCheckResult
    data class PermissionMissing(val error: MeemoError.PermissionDenied) : CapabilityCheckResult
    data class HardwareAbsent(val error: MeemoError.HardwareUnavailable) : CapabilityCheckResult
    data class RestrictedByPlatform(val error: MeemoError.PlatformRestricted) : CapabilityCheckResult
    data class RootUnavailable(val error: MeemoError.RootUnavailableOrRejected) : CapabilityCheckResult
}
