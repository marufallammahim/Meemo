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
    fun `rejects unregistered command IDs and shell injection attempts`() {
        assertFalse(policy.isCommandAllowed("RM_RF_SYSTEM", null))
        assertFalse(policy.isCommandAllowed("ROOT_PACKAGE_PATH_CHECK", "com.app; rm -rf /"))
        assertFalse(policy.isCommandAllowed("ROOT_PACKAGE_PATH_CHECK", "com.app && reboot"))
        assertFalse(policy.isCommandAllowed("ROOT_PACKAGE_PATH_CHECK", "com.app | cat /data/system/users/0.xml"))
        assertFalse(policy.isCommandAllowed("ROOT_PACKAGE_PATH_CHECK", "\\$(whoami)"))
        assertNull(policy.buildSafeCommandString("ROOT_PACKAGE_PATH_CHECK", "com.app; id"))
    }
}
