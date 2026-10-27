package com.marufallammahim.meemo.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.marufallammahim.meemo.core.logging.SafeLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Opt-in Android NotificationListenerService.
 * Processes incoming notifications transiently in memory only when explicitly authorized by the user.
 */
@AndroidEntryPoint
class MeemoNotificationListener : NotificationListenerService() {

    @Inject lateinit var processor: NotificationProcessor
    @Inject lateinit var readAloud: NotificationReadAloud
    @Inject lateinit var safeLogger: SafeLogger

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val statusBarNotification = sbn ?: return
        val item = processor.extractTransientItem(statusBarNotification) ?: return
        // Log only package metadata; never log notification_body
        safeLogger.info("NotificationListener", "Processed transient notification from pkg=${item.packageName}")
        serviceScope.launch {
            readAloud.handleIncomingNotification(item)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Transient queue automatically ages out
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
