package com.meemo.capability.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VolumeTargetPolicyTest {
    @Test
    fun convertsPercentToAvailableStreamSteps() {
        assertEquals(8, calculateVolumeTarget(3, 15, percent = 50, delta = null))
        assertEquals(0, calculateVolumeTarget(3, 15, percent = 0, delta = null))
        assertEquals(15, calculateVolumeTarget(3, 15, percent = 100, delta = null))
    }

    @Test
    fun adjustsByOneStepAndClampsToStreamBounds() {
        assertEquals(8, calculateVolumeTarget(7, 15, percent = null, delta = 1))
        assertEquals(6, calculateVolumeTarget(7, 15, percent = null, delta = -1))
        assertEquals(15, calculateVolumeTarget(15, 15, percent = null, delta = 1))
        assertEquals(0, calculateVolumeTarget(0, 15, percent = null, delta = -1))
    }

    @Test
    fun rejectsInvalidOrAmbiguousRequests() {
        assertNull(calculateVolumeTarget(3, 15, percent = 101, delta = null))
        assertNull(calculateVolumeTarget(3, 15, percent = null, delta = null))
        assertNull(calculateVolumeTarget(3, 15, percent = 50, delta = 1))
        assertNull(calculateVolumeTarget(16, 15, percent = null, delta = 1))
        assertNull(calculateVolumeTarget(0, 0, percent = 50, delta = null))
    }
}
