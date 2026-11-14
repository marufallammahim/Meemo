package com.marufallammahim.meemo.capabilities

import android.Manifest
import android.content.pm.PackageManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CapabilityRegistry @Inject constructor() {

    private val descriptors: Map<String, CapabilityDescriptor> = listOf(
        CapabilityDescriptor(
            capabilityId = "CAP_APP_LAUNCH",
            displayNameEn = "Dynamic Application Discovery & Launch",
            displayNameBn = "অ্যাপ অনুসন্ধান ও চালু",
            supportsVerification = true
        ),
        CapabilityDescriptor(
            capabilityId = "CAP_BROWSER_RESOLVE",
            displayNameEn = "Browser Intent Resolution",
            displayNameBn = "ব্রাউজার নির্বাচন ও চালু",
            supportsVerification = true
        ),
        CapabilityDescriptor(
            capabilityId = "CAP_TORCH",
            displayNameEn = "Camera2 Flashlight Control",
            displayNameBn = "ফ্ল্যাশলাইট নিয়ন্ত্রণ",
            requiresHardwareFeature = PackageManager.FEATURE_CAMERA_FLASH,
            supportsVerification = true
        ),
        CapabilityDescriptor(
            capabilityId = "CAP_BATTERY_INFO",
            displayNameEn = "Battery Status & Health Reader",
            displayNameBn = "ব্যাটারি তথ্য পাঠক",
            supportsVerification = true
        ),
        CapabilityDescriptor(
            capabilityId = "CAP_DEVICE_INFO",
            displayNameEn = "Device & Android Version Diagnostics",
            displayNameBn = "ডিভাইস ও সিস্টেম তথ্য",
            supportsVerification = true
        ),
        CapabilityDescriptor(
            capabilityId = "CAP_AUDIO_VOLUME",
            displayNameEn = "Audio Stream Volume Control",
            displayNameBn = "ভলিউম নিয়ন্ত্রণ",
            supportsVerification = true
        ),
        CapabilityDescriptor(
            capabilityId = "CAP_MEDIA_CONTROL",
            displayNameEn = "Media Session Transport Control",
            displayNameBn = "মিডিয়া প্লেব্যাক নিয়ন্ত্রণ",
            supportsVerification = false
        ),
        CapabilityDescriptor(
            capabilityId = "CAP_SETTINGS_NAVIGATION",
            displayNameEn = "System Settings Navigation",
            displayNameBn = "সিস্টেম সেটিংস নেভিগেশন",
            supportsVerification = true
        ),
        CapabilityDescriptor(
            capabilityId = "CAP_SPEECH_RECOGNITION",
            displayNameEn = "Voice Speech-to-Text Input",
            displayNameBn = "ভয়েস রিকগনিশন",
            requiresHardwareFeature = PackageManager.FEATURE_MICROPHONE,
            requiresRuntimePermission = Manifest.permission.RECORD_AUDIO
        ),
        CapabilityDescriptor(
            capabilityId = "CAP_NOTIFICATION_READER",
            displayNameEn = "Opt-In Notification Read-Aloud",
            displayNameBn = "নোটিফিকেশন পড়ে শোনানো",
            requiresSpecialAccess = "BIND_NOTIFICATION_LISTENER_SERVICE"
        ),
        CapabilityDescriptor(
            capabilityId = "CAP_ACCESSIBILITY_HELPER",
            displayNameEn = "Authorized Accessibility Guidance",
            displayNameBn = "অ্যাক্সেসিবিলিটি সহায়তা",
            requiresSpecialAccess = "BIND_ACCESSIBILITY_SERVICE"
        ),
        CapabilityDescriptor(
            capabilityId = "CAP_ROOT_EXECUTION",
            displayNameEn = "Isolated Root Diagnostics",
            displayNameBn = "আইসোলেটেড রুট ডায়াগনস্টিক",
            requiresRoot = true,
            requiresConfirmation = true,
            supportsVerification = true
        )
    ).associateBy { it.capabilityId }

    fun getDescriptor(capabilityId: String): CapabilityDescriptor? = descriptors[capabilityId]
    fun getAllDescriptors(): List<CapabilityDescriptor> = descriptors.values.toList()
}
