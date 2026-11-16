package com.marufallammahim.meemo.root

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RootCommandPolicyTest {

    private lateinit var policy: RootCommandPolicy

    @Before
    fun setUp() {
        policy = RootCommandPolicy()
    }

    @Test
    fun `allows registered static diagnostic commands without parameters`() {
        assertTrue(policy.isCommandAllowed("ROOT_BATTERY_STATS_DUMP", null))
        assertEquals(
            "/system/bin/dumpsys battery",
            policy.buildSafeCommandString("ROOT_BATTERY_STATS_DUMP", null)
        )
    }

    @Test
    fun `allows valid package name for ROOT_PACKAGE_PATH_CHECK`() {
        assertTrue(policy.isCommandAllowed("ROOT_PACKAGE_PATH_CHECK", "org.mozilla.firefox"))
        assertNotNull(policy.buildSafeCommandString("ROOT_PACKAGE_PATH_CHECK", "org.mozilla.firefox"))
    }

    @Test
    fun `rejects malformed package identifiers for root package lookup`() {
        listOf(".", "..", "com..app", "com.app.", "com.-app", "com.").forEach { invalidPackage ->
            assertFalse(policy.isCommandAllowed("ROOT_PACKAGE_PATH_CHECK", invalidPackage))
        }
    }

    @Test
    fun `rejects unregistered command IDs and shell injection attempts`() {
        assertFalse(policy.isCommandAllowed("RM_RF_SYSTEM", null))
        assertFalse(policy.isCommandAllowed("ROOT_PACKAGE_PATH_CHECK", "com.app; rm -rf /"))
        assertFalse(policy.isCommandAllowed("ROOT_PACKAGE_PATH_CHECK", "com.app && reboot"))
        assertFalse(policy.isCommandAllowed("ROOT_PACKAGE_PATH_CHECK", "com.app | cat /data/system/users/0.xml"))
        assertFalse(policy.isCommandAllowed("ROOT_PACKAGE_PATH_CHECK", "\\$(whoami)"))
        assertNull(policy.buildSafeCommandString("ROOT_PACKAGE_PATH_CHECK", "com.app; id"))
    }

    @Test
    fun `allows the thermal diagnostic command without arguments`() {
        assertTrue(policy.isCommandAllowed("ROOT_THERMAL_STATUS", null))
        assertEquals(
            "/system/bin/dumpsys thermalservice",
            policy.buildSafeCommandString("ROOT_THERMAL_STATUS", null)
        )
    }

    @Test
    fun `rejects parameters for static diagnostic commands`() {
        assertFalse(policy.isCommandAllowed("ROOT_BATTERY_STATS_DUMP", "battery"))
        assertFalse(policy.isCommandAllowed("ROOT_THERMAL_STATUS", "thermalservice"))
        assertNull(policy.buildSafeCommandString("ROOT_THERMAL_STATUS", "unexpected"))
    }

    @Test
    fun `does not append an empty package parameter to root command`() {
        assertTrue(policy.isCommandAllowed("ROOT_PACKAGE_PATH_CHECK", ""))
        assertEquals(
            "/system/bin/pm path",
            policy.buildSafeCommandString("ROOT_PACKAGE_PATH_CHECK", "")
        )
    }

    @Test
    fun `enforces the maximum package parameter length`() {
        val maximumLengthPackage = "a." + "b".repeat(60)
        val overLimitPackage = "a." + "b".repeat(61)

        assertEquals(62, maximumLengthPackage.length)
        assertTrue(policy.isCommandAllowed("ROOT_PACKAGE_PATH_CHECK", maximumLengthPackage))
        assertFalse(policy.isCommandAllowed("ROOT_PACKAGE_PATH_CHECK", overLimitPackage))
    }

}
