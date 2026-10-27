package com.marufallammahim.meemo.actions.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.marufallammahim.meemo.actions.ActionResult
import com.marufallammahim.meemo.capabilities.adapters.TorchCapabilityAdapter
import com.marufallammahim.meemo.core.error.MeemoError
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorchActionExecutor @Inject constructor(
    private val torchAdapter: TorchCapabilityAdapter
) {
    fun executeTorch(turnOn: Boolean): ActionResult {
        if (!torchAdapter.isFlashHardwareAvailable()) {
            return ActionResult.Unavailable(MeemoError.HardwareUnavailable("Camera Flash"))
        }
        val succeeded = torchAdapter.setTorchEnabled(turnOn)
        val verifiedState = torchAdapter.isTorchCurrentlyOn()
        return if (succeeded && verifiedState == turnOn) {
            if (turnOn) {
                ActionResult.Success(
                    summaryEn = "Flashlight turned on and verified.",
                    summaryBn = "ফ্ল্যাশলাইট চালু করা হয়েছে।",
                    verifiedBySystem = true,
                    details = mapOf("torchState" to "ON")
                )
            } else {
                ActionResult.Success(
                    summaryEn = "Flashlight turned off and verified.",
                    summaryBn = "ফ্ল্যাশলাইট বন্ধ করা হয়েছে।",
                    verifiedBySystem = true,
                    details = mapOf("torchState" to "OFF")
                )
            }
        } else {
            ActionResult.Failed(MeemoError.HardwareUnavailable("Camera Torch (in use or unavailable)"))
        }
    }
}

@Singleton
class DeviceInfoActionExecutor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun readBatteryStatus(): ActionResult {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) {
            (level * 100) / scale
        } else {
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        }

        if (pct < 0) {
            return ActionResult.Unavailable(MeemoError.HardwareUnavailable("Battery Status Sensor"))
        }

        val statusInt = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = statusInt == BatteryManager.BATTERY_STATUS_CHARGING ||
            statusInt == BatteryManager.BATTERY_STATUS_FULL
        val chargingLabelEn = if (isCharging) "Charging" else "Discharging"
        val chargingLabelBn = if (isCharging) "চার্জ হচ্ছে" else "চার্জ হচ্ছে না"

        val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val voltageText = if (voltageMv > 0) "${voltageMv}mV" else "Unavailable"

        return ActionResult.Success(
            summaryEn = "Battery is at $pct% ($chargingLabelEn, Voltage: $voltageText).",
            summaryBn = "ব্যাটারি এখন $pct% ($chargingLabelBn, ভোল্টেজ: $voltageText)।",
            verifiedBySystem = true,
            details = mapOf(
                "batteryPercent" to "$pct%",
                "chargingState" to chargingLabelEn,
                "voltage" to voltageText
            )
        )
    }

    fun readDeviceInformation(): ActionResult {
        val manufacturer = Build.MANUFACTURER.ifBlank { "Unknown" }
        val model = Build.MODEL.ifBlank { "Unknown" }
        val release = Build.VERSION.RELEASE ?: "Unknown"
        val sdkInt = Build.VERSION.SDK_INT

        return ActionResult.Success(
            summaryEn = "Device: $manufacturer $model running Android $release (API $sdkInt).",
            summaryBn = "ডিভাইস: $manufacturer $model, অ্যান্ড্রয়েড ভার্সন $release (API $sdkInt)।",
            verifiedBySystem = true,
            details = mapOf(
                "manufacturer" to manufacturer,
                "model" to model,
                "androidRelease" to release,
                "sdkInt" to sdkInt.toString()
            )
        )
    }
}
