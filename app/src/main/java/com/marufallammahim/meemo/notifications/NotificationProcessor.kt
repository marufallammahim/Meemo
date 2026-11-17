package com.marufallammahim.meemo.notifications

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import com.marufallammahim.meemo.core.utilities.TimeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

internal fun boundedNotificationText(value: CharSequence?, maxLength: Int): String {
    require(maxLength >= 0)
    if (value == null || maxLength == 0) return ""

    val boundedLength = value.length.coerceAtMost(maxLength)
    return value.subSequence(0, boundedLength).toString().trim()
}

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
        val title = if (isRedacted) "" else boundedNotificationText(
            extras?.getCharSequence(Notification.EXTRA_TITLE),
            MAX_NOTIFICATION_TITLE_LENGTH
        )
        val text = if (isRedacted) "" else boundedNotificationText(
            extras?.getCharSequence(Notification.EXTRA_TEXT),
            MAX_NOTIFICATION_BODY_LENGTH
        )

        if (title.isEmpty() && text.isEmpty() && !isRedacted) return null

        val appName = runCatching {
            val appInfo = context.packageManager.getApplicationInfo(pkg, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(pkg)

        return TransientNotificationItem(
            packageName = pkg,
            appDisplayName = appName,
            senderOrTitle = title.ifEmpty { appName },
            messageBody = text,
            isContentRedactedBySystem = isRedacted,
            receivedAtMs = timeProvider.nowMillis()
        )
    }

    companion object {
        private const val MAX_NOTIFICATION_TITLE_LENGTH = 120
        private const val MAX_NOTIFICATION_BODY_LENGTH = 240
    }
}
