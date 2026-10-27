package com.marufallammahim.meemo.notifications

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import com.marufallammahim.meemo.core.utilities.TimeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider
) {
    fun extractTransientItem(sbn: StatusBarNotification): TransientNotificationItem? {
        if (sbn.isOngoing) return null
        val pkg = sbn.packageName ?: return null
        if (pkg == context.packageName) return null

        val notification = sbn.notification ?: return null
        val isRedacted = notification.visibility == Notification.VISIBILITY_SECRET

        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()

        if (title.isEmpty() && text.isEmpty() && !isRedacted) return null

        val appName = runCatching {
            val appInfo = context.packageManager.getApplicationInfo(pkg, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(pkg)

        return TransientNotificationItem(
            packageName = pkg,
            appDisplayName = appName,
            senderOrTitle = title.ifEmpty { appName },
            messageBody = if (isRedacted) "" else text.take(240),
            isContentRedactedBySystem = isRedacted,
            receivedAtMs = timeProvider.nowMillis()
        )
    }
}
