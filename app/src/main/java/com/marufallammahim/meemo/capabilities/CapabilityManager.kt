package com.marufallammahim.meemo.capabilities

import com.marufallammahim.meemo.capabilities.adapters.TorchCapabilityAdapter
import com.marufallammahim.meemo.core.error.MeemoError
import com.marufallammahim.meemo.core.error.RecoveryActionType
import com.marufallammahim.meemo.core.permissions.PermissionChecker
import com.marufallammahim.meemo.core.utilities.TimeProvider
import com.marufallammahim.meemo.data.database.entity.CapabilitySnapshotEntity
import com.marufallammahim.meemo.root.RootManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Evaluates whether a capability is supported, hardware-present, and authorized
 * before any action execution proceeds.
 */
@Singleton
class CapabilityManager @Inject constructor(
    private val registry: CapabilityRegistry,
    private val permissionChecker: PermissionChecker,
    private val torchAdapter: TorchCapabilityAdapter,
    private val rootManager: RootManager,
    private val timeProvider: TimeProvider
) {
    fun checkCapability(capabilityId: String?): CapabilityCheckResult {
        if (capabilityId == null) return CapabilityCheckResult.Ready
        val descriptor = registry.getDescriptor(capabilityId) ?: return CapabilityCheckResult.Ready

        // 1. Hardware check
        if (capabilityId == "CAP_TORCH" && !torchAdapter.isFlashHardwareAvailable()) {
            return CapabilityCheckResult.HardwareAbsent(
                MeemoError.HardwareUnavailable("Camera Flash / Torch")
            )
        }

        // 2. Runtime permission check
        if (descriptor.requiresRuntimePermission != null) {
            if (!permissionChecker.hasRecordAudioPermission()) {
                return CapabilityCheckResult.PermissionMissing(
                    MeemoError.PermissionDenied(
                        permissionName = descriptor.requiresRuntimePermission,
                        rationaleEn = "Microphone permission is required for voice commands.",
                        rationaleBn = "ভয়েস কমান্ড ব্যবহারের জন্য মাইক্রোফোন অনুমতি প্রয়োজন।",
                        targetRecovery = RecoveryActionType.REQUEST_RUNTIME_PERMISSION
                    )
                )
            }
        }

        // 3. Special access check
        if (descriptor.requiresSpecialAccess == "BIND_NOTIFICATION_LISTENER_SERVICE") {
            if (!permissionChecker.isNotificationListenerEnabled()) {
                return CapabilityCheckResult.PermissionMissing(
                    MeemoError.PermissionDenied(
                        permissionName = "BIND_NOTIFICATION_LISTENER_SERVICE",
                        rationaleEn = "Notification Access is not enabled. Enable 'Meemo Notification Read-Aloud' in Android Settings to read incoming messages.",
                        rationaleBn = "নোটিফিকেশন অ্যাক্সেস চালু নেই। মেসেজ শোনার জন্য অ্যান্ড্রয়েড সেটিংস থেকে মিমোকে অনুমতি দিন।",
                        targetRecovery = RecoveryActionType.OPEN_NOTIFICATION_LISTENER_SETTINGS
                    )
                )
            }
        }

        if (descriptor.requiresSpecialAccess == "BIND_ACCESSIBILITY_SERVICE") {
            if (!permissionChecker.isAccessibilityServiceEnabled()) {
                return CapabilityCheckResult.PermissionMissing(
                    MeemoError.PermissionDenied(
                        permissionName = "BIND_ACCESSIBILITY_SERVICE",
                        rationaleEn = "Accessibility Helper is disabled. Enable it in Android Accessibility Settings if needed.",
                        rationaleBn = "অ্যাক্সেসিবিলিটি সার্ভিস বন্ধ আছে। প্রয়োজনে সেটিংস থেকে চালু করুন।",
                        targetRecovery = RecoveryActionType.OPEN_ACCESSIBILITY_SETTINGS
                    )
                )
            }
        }

        // 4. Root check
        if (descriptor.requiresRoot && !rootManager.isRootBinaryPresent()) {
            return CapabilityCheckResult.RootUnavailable(
                MeemoError.RootUnavailableOrRejected("No su binary found on this device")
            )
        }

        return CapabilityCheckResult.Ready
    }

    fun generateDiagnosticSnapshots(): List<CapabilitySnapshotEntity> {
        val now = timeProvider.nowMillis()
        return registry.getAllDescriptors().map { descriptor ->
            val check = checkCapability(descriptor.capabilityId)
            val hardwareOk = check !is CapabilityCheckResult.HardwareAbsent
            val permOk = check is CapabilityCheckResult.Ready
            val summary = when (check) {
                is CapabilityCheckResult.Ready -> "Available & Ready"
                is CapabilityCheckResult.PermissionMissing -> "Requires User Authorization"
                is CapabilityCheckResult.HardwareAbsent -> "Hardware Not Present"
                is CapabilityCheckResult.RestrictedByPlatform -> "OS Restricted (Uses Settings Fallback)"
                is CapabilityCheckResult.RootUnavailable -> "Non-Root Device (Isolated Root Inactive)"
            }
            CapabilitySnapshotEntity(
                capabilityId = descriptor.capabilityId,
                displayName = descriptor.displayNameEn,
                isHardwarePresent = hardwareOk,
                isPermissionGranted = permOk,
                statusSummary = summary,
                lastCheckedAtMs = now
            )
        }
    }
}
