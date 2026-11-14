package com.marufallammahim.meemo.permissions

import com.marufallammahim.meemo.core.permissions.PermissionChecker
import javax.inject.Inject
import javax.inject.Singleton

data class PermissionStatusSummary(
    val recordAudioGranted: Boolean,
    val postNotificationsGranted: Boolean,
    val notificationListenerEnabled: Boolean,
    val accessibilityServiceEnabled: Boolean
)

@Singleton
class PermissionCoordinator @Inject constructor(
    private val permissionChecker: PermissionChecker
) {
    fun currentStatus(): PermissionStatusSummary = PermissionStatusSummary(
        recordAudioGranted = permissionChecker.hasRecordAudioPermission(),
        postNotificationsGranted = permissionChecker.hasPostNotificationsPermission(),
        notificationListenerEnabled = permissionChecker.isNotificationListenerEnabled(),
        accessibilityServiceEnabled = permissionChecker.isAccessibilityServiceEnabled()
    )
}
