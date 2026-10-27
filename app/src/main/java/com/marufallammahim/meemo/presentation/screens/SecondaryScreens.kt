package com.marufallammahim.meemo.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marufallammahim.meemo.domain.model.AppLanguageMode
import com.marufallammahim.meemo.domain.model.AppThemeMode
import com.marufallammahim.meemo.presentation.components.StatusPillBadge
import com.marufallammahim.meemo.presentation.theme.MeemoDimens

@Composable
fun InstalledAppsScreen(
    viewModel: AppsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MeemoDimens.SpaceMD),
        verticalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceSM)
    ) {
        Text(
            text = "Dynamic Application & Browser Discovery",
            style = MaterialTheme.typography.headlineMedium
        )
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search installed applications…") },
            singleLine = true
        )

        if (state.browsers.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(MeemoDimens.SpaceMD)) {
                    Text(
                        text = "Detected Browsers (${state.browsers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceXS)
                    ) {
                        state.browsers.forEach { browser ->
                            FilterChip(
                                selected = state.preferredBrowserPackage == browser.packageName,
                                onClick = { viewModel.selectPreferredBrowser(browser.packageName) },
                                label = { Text(browser.appLabel) }
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceXS)
        ) {
            items(state.launchableApps, key = { it.packageName }) { app ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MeemoDimens.SpaceMD),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = app.appLabel, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = { viewModel.launchApp(app.packageName) }) {
                            Text("Launch")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryAndAliasesScreen(
    viewModel: MemoryAliasesViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var triggerDraft by remember { mutableStateOf("") }
    var targetAppDraft by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MeemoDimens.SpaceMD),
        verticalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceSM)
    ) {
        Text(
            text = "Custom Aliases, Routines & History",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(MeemoDimens.SpaceMD),
                verticalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceXS)
            ) {
                Text(
                    text = "Create Custom Command Alias",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = triggerDraft,
                    onValueChange = { triggerDraft = it },
                    placeholder = { Text("Trigger phrase (e.g., work browser)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = targetAppDraft,
                    onValueChange = { targetAppDraft = it },
                    placeholder = { Text("Target app or package (e.g., org.mozilla.firefox)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = {
                        viewModel.saveCustomAlias(triggerDraft, targetAppDraft)
                        triggerDraft = ""
                        targetAppDraft = ""
                    },
                    enabled = triggerDraft.isNotBlank() && targetAppDraft.isNotBlank()
                ) {
                    Text("Save Alias")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceXS)
        ) {
            item {
                Text(
                    text = "Saved Aliases (${state.aliases.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(state.aliases, key = { it.aliasId }) { alias ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MeemoDimens.SpaceMD),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ""${alias.triggerPhrase}" -> ${alias.targetEntity}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Intent: ${alias.targetIntent} • Used ${alias.usageCount} time(s)",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        OutlinedButton(onClick = { viewModel.deleteAlias(alias.aliasId) }) {
                            Text("Delete")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(MeemoDimens.SpaceSM))
                Text(
                    text = "Recent Command Audit Log (${state.historyEntries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(state.historyEntries, key = { it.commandId }) { cmd ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(MeemoDimens.SpaceSM)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = cmd.normalizedInput, style = MaterialTheme.typography.bodyMedium)
                            StatusPillBadge(label = cmd.outcomeCode)
                        }
                        Text(
                            text = "Intent: ${cmd.matchedIntent} • Latency: ${cmd.latencyMs}ms",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CapabilitiesAndDiagnosticsScreen(
    viewModel: CapabilitiesViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(MeemoDimens.SpaceMD),
        verticalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceSM)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Capabilities & Diagnostics",
                    style = MaterialTheme.typography.headlineMedium
                )
                OutlinedButton(onClick = { viewModel.refreshDiagnostics() }) {
                    Text("Refresh")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(MeemoDimens.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceXS)
                ) {
                    Text(
                        text = "Special Access & Service Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Microphone Permission: ${if (state.permissions.recordAudioGranted) "Granted" else "Not Granted"}")
                    Text("Notification Read-Aloud Access: ${if (state.permissions.notificationListenerEnabled) "Enabled" else "Disabled"}")
                    Text("Accessibility Helper: ${if (state.permissions.accessibilityServiceEnabled) "Enabled" else "Disabled"}")
                    Text("Root Status: ${state.rootStatus.name}")

                    Row(horizontalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceXS)) {
                        OutlinedButton(onClick = { viewModel.settingsCoordinator.openNotificationListenerSettings() }) {
                            Text("Notification Access")
                        }
                        OutlinedButton(onClick = { viewModel.settingsCoordinator.openAccessibilitySettings() }) {
                            Text("Accessibility")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Registered Device Capabilities (${state.capabilitySnapshots.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        items(state.capabilitySnapshots, key = { it.capabilityId }) { cap ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MeemoDimens.SpaceMD),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = cap.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(text = cap.statusSummary, style = MaterialTheme.typography.bodyMedium)
                    }
                    StatusPillBadge(
                        label = if (cap.isPermissionGranted && cap.isHardwarePresent) "READY" else "LIMITED"
                    )
                }
            }
        }

        item {
            Text(
                text = "Approved Root Policy Templates (${state.approvedRootSpecs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        items(state.approvedRootSpecs, key = { it.commandId }) { spec ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(MeemoDimens.SpaceSM)) {
                    Text(text = "${spec.displayName} (${spec.commandId})", style = MaterialTheme.typography.bodyLarge)
                    Text(text = spec.description, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Executable: ${spec.executablePath} ${spec.staticArgs.joinToString(" ")}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun PrivacyAndSettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    var aiKeyInput by remember { mutableStateOf("") }
    var aiEndpointInput by remember(prefs.aiEndpointUrl) { mutableStateOf(prefs.aiEndpointUrl) }
    var aiModelInput by remember(prefs.aiModelName) { mutableStateOf(prefs.aiModelName) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(MeemoDimens.SpaceMD),
        verticalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceSM)
    ) {
        item {
            Text(
                text = "Privacy, Voice & Assistant Settings",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        // Theme & Language
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(MeemoDimens.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceXS)
                ) {
                    Text("Theme Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceXS)) {
                        AppThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = prefs.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(mode.name) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(MeemoDimens.SpaceXS))
                    Text("Language Mode (English / বাংলা)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceXS)) {
                        AppLanguageMode.entries.forEach { lang ->
                            FilterChip(
                                selected = prefs.languageMode == lang,
                                onClick = { viewModel.setLanguageMode(lang) },
                                label = { Text(lang.name) }
                            )
                        }
                    }
                }
            }
        }

        // Voice, TTS & Wake-Word
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(MeemoDimens.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceXS)
                ) {
                    Text("Speech & Text-to-Speech", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Spoken TTS Responses")
                        Switch(checked = prefs.ttsEnabled, onCheckedChange = { viewModel.setTtsEnabled(it) })
                    }
                    Text("Speech Rate: ${"%.2f".format(prefs.ttsSpeechRate)}x")
                    Slider(
                        value = prefs.ttsSpeechRate,
                        onValueChange = { viewModel.setTtsRate(it) },
                        valueRange = 0.5f..2.0f
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Foreground Wake-Word ("Hello Meemo")")
                            Text(
                                "Uses visible microphone foreground notification when active.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(checked = prefs.wakeWordEnabled, onCheckedChange = { viewModel.setWakeWordEnabled(it) })
                    }
                }
            }
        }

        // Notification Read-Aloud & Quiet Hours
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(MeemoDimens.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceXS)
                ) {
                    Text("Notification Read-Aloud (Transient RAM Only)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Opt-In Message Read-Aloud")
                        Switch(
                            checked = prefs.notificationReadAloudEnabled,
                            onCheckedChange = { viewModel.setNotificationReadAloud(it) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Announce App/Sender Only (Hide Message Body)")
                        Switch(
                            checked = prefs.notificationAnnounceSenderOnly,
                            onCheckedChange = { viewModel.setNotificationSenderOnly(it) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Quiet Hours (${prefs.quietHoursStartHour}:00 – ${prefs.quietHoursEndHour}:00)")
                        Switch(
                            checked = prefs.quietHoursEnabled,
                            onCheckedChange = { viewModel.setQuietHours(it, prefs.quietHoursStartHour, prefs.quietHoursEndHour) }
                        )
                    }
                }
            }
        }

        // Optional AI & Isolated Root
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(MeemoDimens.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceXS)
                ) {
                    Text("Optional AI Fallback & Isolated Root", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Optional AI Interpretation Fallback")
                        Switch(
                            checked = prefs.aiFallbackEnabled,
                            onCheckedChange = { viewModel.configureAiProvider(it, aiEndpointInput, aiModelInput, aiKeyInput) }
                        )
                    }
                    OutlinedTextField(
                        value = aiEndpointInput,
                        onValueChange = { aiEndpointInput = it },
                        label = { Text("HTTPS Endpoint") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = aiKeyInput,
                        onValueChange = { aiKeyInput = it },
                        label = { Text("API Key (Encrypted in AndroidKeyStore)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            viewModel.configureAiProvider(prefs.aiFallbackEnabled, aiEndpointInput, aiModelInput, aiKeyInput)
                            aiKeyInput = ""
                        }
                    ) {
                        Text("Save Encrypted AI Configuration")
                    }

                    Spacer(modifier = Modifier.height(MeemoDimens.SpaceXS))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Isolated Root Diagnostics")
                            Text(
                                "Strictly limited to allowlisted RootCommandPolicy templates.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = prefs.rootFeaturesEnabled,
                            onCheckedChange = { viewModel.setRootFeaturesEnabled(it) }
                        )
                    }
                }
            }
        }

        // Privacy & One-Tap Data Deletion
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(MeemoDimens.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceXS)
                ) {
                    Text("Data Retention & Deletion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Save Local Conversation History")
                        Switch(
                            checked = prefs.saveConversationHistory,
                            onCheckedChange = { viewModel.setSaveHistory(it) }
                        )
                    }
                    Button(
                        onClick = { viewModel.deleteAllLocalData() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete All Local History, Aliases & Credentials")
                    }
                }
            }
        }
    }
}
