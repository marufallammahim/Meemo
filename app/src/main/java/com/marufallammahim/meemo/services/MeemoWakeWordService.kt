package com.marufallammahim.meemo.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.marufallammahim.meemo.MainActivity
import com.marufallammahim.meemo.R
import com.marufallammahim.meemo.core.logging.SafeLogger
import com.marufallammahim.meemo.core.permissions.PermissionChecker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Opt-in Foreground Service for active "Hello Meemo" wake-word session.
 * Always displays a visible persistent microphone notification and stops immediately
 * if permission is revoked or the user taps Stop.
 */
@AndroidEntryPoint
class MeemoWakeWordService : Service() {

    @Inject lateinit var permissionChecker: PermissionChecker
    @Inject lateinit var safeLogger: SafeLogger

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_WAKE_WORD || !permissionChecker.hasRecordAudioPermission()) {
            safeLogger.info("WakeWordService", "Stopping foreground wake-word service")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            isRunning = false
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        isRunning = true
        safeLogger.info("WakeWordService", "Started foreground wake-word session with visible disclosure")
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Meemo Foreground Wake-Word",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Displays whenever Meemo wake-word detection is active"
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildForegroundNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, MeemoWakeWordService::class.java).apply {
            action = ACTION_STOP_WAKE_WORD
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_meemo_mascot)
            .setContentTitle(getString(R.string.wake_word_notification_title))
            .setContentText(getString(R.string.wake_word_notification_body))
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .addAction(0, "Stop", pendingStop)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "meemo_wake_word_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_WAKE_WORD = "com.marufallammahim.meemo.action.STOP_WAKE_WORD"

        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
