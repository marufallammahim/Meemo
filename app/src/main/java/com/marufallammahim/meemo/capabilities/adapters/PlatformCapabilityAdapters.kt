package com.marufallammahim.meemo.capabilities.adapters

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import com.marufallammahim.meemo.core.logging.SafeLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hardware adapter for Camera2 Torch with live TorchCallback state verification.
 */
@Singleton
class TorchCapabilityAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safeLogger: SafeLogger
) {
    private val cameraManager: CameraManager? by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }

    private val currentTorchState = AtomicBoolean(false)
    private var callbackRegistered = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            currentTorchState.set(enabled)
        }
    }

    fun isFlashHardwareAvailable(): Boolean {
        val hasFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        if (!hasFeature) return false
        return findFlashCameraId() != null
    }

    fun findFlashCameraId(): String? {
        val manager = cameraManager ?: return null
        return runCatching {
            manager.cameraIdList.firstOrNull { id ->
                val characteristics = manager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    fun setTorchEnabled(enabled: Boolean): Boolean {
        val manager = cameraManager ?: return false
        val cameraId = findFlashCameraId() ?: return false
        ensureCallbackRegistered(manager)
        return try {
            manager.setTorchMode(cameraId, enabled)
            currentTorchState.set(enabled)
            safeLogger.info("TorchAdapter", "Torch state set to $enabled on cameraId=$cameraId")
            true
        } catch (e: Exception) {
            safeLogger.error("TorchAdapter", "Failed setting torch to $enabled", e)
            false
        }
    }

    fun isTorchCurrentlyOn(): Boolean = currentTorchState.get()

    private fun ensureCallbackRegistered(manager: CameraManager) {
        if (!callbackRegistered) {
            runCatching {
                manager.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
                callbackRegistered = true
            }
        }
    }
}
