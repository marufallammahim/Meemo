package com.marufallammahim.meemo.apps

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.marufallammahim.meemo.core.logging.SafeLogger
import com.marufallammahim.meemo.domain.model.InstalledAppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageResolver: PackageResolver,
    private val browserResolver: BrowserResolver,
    private val safeLogger: SafeLogger
) {
    fun listLaunchableApps(): List<InstalledAppInfo> = packageResolver.getLaunchableApps()

    fun listInstalledBrowsers(): List<InstalledAppInfo> = browserResolver.getInstalledBrowsers()

    fun launchPackage(packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?: return false
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            safeLogger.info("AppManager", "Launched package=$packageName")
            true
        } catch (e: Exception) {
            safeLogger.error("AppManager", "Failed launching package=$packageName", e)
            false
        }
    }

    fun launchBrowser(packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                    setPackage(packageName)
                }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            safeLogger.info("AppManager", "Launched browser package=$packageName")
            true
        } catch (e: Exception) {
            safeLogger.error("AppManager", "Failed launching browser package=$packageName", e)
            false
        }
    }
}
