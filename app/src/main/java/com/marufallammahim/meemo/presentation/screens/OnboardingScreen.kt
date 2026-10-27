package com.marufallammahim.meemo.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marufallammahim.meemo.domain.model.AppLanguageMode
import com.marufallammahim.meemo.presentation.components.MascotVisualState
import com.marufallammahim.meemo.presentation.components.MeemoMascot
import com.marufallammahim.meemo.presentation.theme.MeemoDimens

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(MeemoDimens.SpaceLG),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceMD)
        ) {
            Spacer(modifier = Modifier.height(MeemoDimens.SpaceSM))
            MeemoMascot(visualState = MascotVisualState.IDLE, size = 108.dp)
            Text(
                text = "Welcome to Meemo",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Your Android. Your Commands. Your Assistant.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(MeemoDimens.SpaceMD)) {
                    Text(
                        text = "1. Choose Language Preference",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(MeemoDimens.SpaceXS))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceXS)
                    ) {
                        AppLanguageMode.entries.forEach { mode ->
                            FilterChip(
                                selected = prefs.languageMode == mode,
                                onClick = { viewModel.selectLanguage(mode) },
                                label = { Text(mode.displayLabel) }
                            )
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(MeemoDimens.SpaceMD)) {
                    Text(
                        text = "2. Honest Capability & Privacy Promise",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(MeemoDimens.SpaceXS))
                    Text(
                        text = "• Works offline for app launching, browser resolution, torch, battery, media, and settings.\n" +
                            "• Never claims an action succeeded unless verified by Android.\n" +
                            "• Microphone, Notification Read-Aloud, Accessibility, and Root are strictly opt-in.\n" +
                            "• Notification message text is processed transiently in RAM and never stored on disk.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(MeemoDimens.SpaceSM))
            Button(
                onClick = {
                    viewModel.completeOnboarding()
                    onFinishOnboarding()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Using Meemo")
            }
        }
    }
}
