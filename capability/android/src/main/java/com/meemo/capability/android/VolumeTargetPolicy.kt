package com.meemo.capability.android

import kotlin.math.roundToInt

internal fun calculateVolumeTarget(
    currentVolume: Int,
    maxVolume: Int,
    percent: Int?,
    delta: Int?
): Int? {
    if (maxVolume <= 0 || currentVolume !in 0..maxVolume) return null
    if ((percent == null) == (delta == null)) return null

    return when {
        percent != null -> {
            if (percent !in 0..100) {
                null
            } else {
                (maxVolume * percent / 100f).roundToInt().coerceIn(0, maxVolume)
            }
        }
        else -> {
            val adjustment = delta ?: return null
            (currentVolume.toLong() + adjustment.toLong())
                .coerceIn(0L, maxVolume.toLong())
                .toInt()
        }
    }
}
