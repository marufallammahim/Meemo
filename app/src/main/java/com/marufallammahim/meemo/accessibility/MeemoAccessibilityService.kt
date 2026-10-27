package com.marufallammahim.meemo.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.marufallammahim.meemo.core.logging.SafeLogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Strictly bounded, opt-in AccessibilityService.
 * Does NOT scrape screen contents or password fields (`canRetrieveWindowContent="false"` in XML).
 * Provides system global action assistance (e.g., opening notifications shade or quick settings)
 * only when explicitly requested by the user.
 */
@AndroidEntryPoint
class MeemoAccessibilityService : AccessibilityService() {

    @Inject lateinit var safeLogger: SafeLogger

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        safeLogger.info("AccessibilityService", "MeemoAccessibilityService connected by user")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally passive: Meemo never scrapes or logs UI window text
    }

    override fun onInterrupt() {
        safeLogger.info("AccessibilityService", "MeemoAccessibilityService interrupted")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    fun openQuickSettingsPanel(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    fun openNotificationShade(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    companion object {
        @Volatile
        var instance: MeemoAccessibilityService? = null
            private set
    }
}
