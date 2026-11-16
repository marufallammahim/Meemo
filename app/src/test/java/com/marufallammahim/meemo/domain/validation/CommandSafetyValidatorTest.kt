package com.marufallammahim.meemo.domain.validation

import com.marufallammahim.meemo.actions.MeemoAction
import com.marufallammahim.meemo.root.RootCommandPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CommandSafetyValidatorTest {

    private lateinit var validator: CommandSafetyValidator

    @Before
    fun setUp() {
        validator = CommandSafetyValidator(RootCommandPolicy())
    }

    @Test
    fun `accepts volume values at both supported boundaries`() {
        assertTrue(validator.validateAction(MeemoAction.SetVolume(0)).isSuccess)
        assertTrue(validator.validateAction(MeemoAction.SetVolume(100)).isSuccess)
    }

    @Test
    fun `rejects volume values below zero or above one hundred`() {
        assertFalse(validator.validateAction(MeemoAction.SetVolume(-1)).isSuccess)
        assertFalse(validator.validateAction(MeemoAction.SetVolume(101)).isSuccess)
    }

    @Test
    fun `accepts approved root diagnostic and rejects unknown root command`() {
        assertTrue(
            validator.validateAction(
                MeemoAction.ExecuteApprovedRootDiagnostic("ROOT_BATTERY_STATS_DUMP")
            ).isSuccess
        )
        assertFalse(
            validator.validateAction(
                MeemoAction.ExecuteApprovedRootDiagnostic("ROOT_UNLISTED_COMMAND")
            ).isSuccess
        )
    }
}
