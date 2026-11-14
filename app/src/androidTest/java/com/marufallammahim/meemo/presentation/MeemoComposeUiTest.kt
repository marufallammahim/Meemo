package com.marufallammahim.meemo.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.marufallammahim.meemo.presentation.components.MascotVisualState
import com.marufallammahim.meemo.presentation.components.MeemoMascot
import com.marufallammahim.meemo.presentation.components.StatusPillBadge
import com.marufallammahim.meemo.presentation.theme.MeemoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeemoComposeUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mascotAndStatusBadgeRenderWithAccessibilitySemantics() {
        composeTestRule.setContent {
            MeemoTheme {
                MeemoMascot(visualState = MascotVisualState.LISTENING)
                StatusPillBadge(label = "VERIFIED")
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Meemo mascot is actively listening to microphone")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("VERIFIED")
            .assertIsDisplayed()
    }
}
