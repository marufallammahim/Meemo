package com.marufallammahim.meemo.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marufallammahim.meemo.R
import com.marufallammahim.meemo.assistant.engine.AssistantExecutionState
import com.marufallammahim.meemo.presentation.components.AmbiguousAppSelectorCard
import com.marufallammahim.meemo.presentation.components.ConfirmationPromptCard
import com.marufallammahim.meemo.presentation.components.ConversationBubbleCard
import com.marufallammahim.meemo.presentation.components.ErrorRecoveryBanner
import com.marufallammahim.meemo.presentation.components.MascotVisualState
import com.marufallammahim.meemo.presentation.components.MeemoMascot
import com.marufallammahim.meemo.presentation.components.QuickSuggestionChipsRow
import com.marufallammahim.meemo.presentation.components.StatusPillBadge
import com.marufallammahim.meemo.presentation.theme.MeemoDimens

@Composable
fun AssistantHomeScreen(
    viewModel: AssistantViewModel,
    onRequestMicPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val mascotState = MascotVisualState.fromExecutionState(state.executionState, state.isTtsSpeaking)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MeemoDimens.SpaceMD, vertical = MeemoDimens.SpaceSM)
    ) {
        // 1. Header with Mascot, Execution State Pill, and Stop Speaking control
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MeemoMascot(visualState = mascotState, size = 64.dp)
                Spacer(modifier = Modifier.width(MeemoDimens.SpaceSM))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    StatusPillBadge(
                        label = state.executionState.name,
                        isError = state.executionState == AssistantExecutionState.FAILED
                    )
                }
            }

            if (state.isTtsSpeaking) {
                OutlinedButton(onClick = { viewModel.stopSpeaking() }) {
                    Icon(
                        imageVector = Icons.Default.VolumeOff,
                        contentDescription = stringResource(R.string.action_stop_speaking)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop Speech")
                }
            }
        }

        Spacer(modifier = Modifier.height(MeemoDimens.SpaceXS))

        // 2. Quick Command Suggestion Chips
        QuickSuggestionChipsRow(
            suggestions = state.quickSuggestions,
            onSuggestionClick = { phrase -> viewModel.submitCommand(phrase) }
        )

        Spacer(modifier = Modifier.height(MeemoDimens.SpaceXS))

        // 3. Active Voice Partial Transcript / Error / Ambiguity / Confirmation Cards
        if (state.livePartialTranscript.isNotBlank()) {
            Text(
                text = "Heard: \"${state.livePartialTranscript}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        state.activeError?.let { err ->
            ErrorRecoveryBanner(
                error = err,
                onRecoveryActionClick = { targetErr ->
                    if (targetErr is com.marufallammahim.meemo.core.error.MeemoError.PermissionDenied &&
                        targetErr.permissionName == android.Manifest.permission.RECORD_AUDIO
                    ) {
                        onRequestMicPermission()
                    } else {
                        viewModel.executeRecoveryForError(targetErr)
                    }
                },
                onDismiss = { viewModel.dismissError() }
            )
            Spacer(modifier = Modifier.height(MeemoDimens.SpaceXS))
        }

        if (state.ambiguousCandidates.isNotEmpty()) {
            AmbiguousAppSelectorCard(
                query = state.ambiguousQuery,
                candidates = state.ambiguousCandidates,
                onSelectApp = { app -> viewModel.onSelectAmbiguousCandidate(app) },
                onCancel = { viewModel.cancelActiveOperation() }
            )
            Spacer(modifier = Modifier.height(MeemoDimens.SpaceXS))
        }

        state.pendingConfirmationAction?.let { pending ->
            ConfirmationPromptCard(
                promptText = state.pendingConfirmationPrompt,
                pendingAction = pending,
                onConfirm = { confirmed -> viewModel.onConfirmPendingAction(confirmed) },
                onCancel = { viewModel.cancelActiveOperation() }
            )
            Spacer(modifier = Modifier.height(MeemoDimens.SpaceXS))
        }

        // 4. Conversation History Feed
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceSM)
        ) {
            if (state.recentMessages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = MeemoDimens.SpaceXL),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Say \"Hello Meemo, turn on the flashlight\" or tap a quick command above.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(state.recentMessages, key = { it.messageId }) { msg ->
                    ConversationBubbleCard(entry = msg)
                }
            }
        }

        Spacer(modifier = Modifier.height(MeemoDimens.SpaceXS))

        // 5. Bottom Command Input Bar + Voice Microphone FAB
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.inputDraft,
                onValueChange = { viewModel.onInputDraftChanged(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.command_input_hint)) },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = { viewModel.submitDraftCommand() },
                        enabled = state.inputDraft.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.action_send_command)
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.width(MeemoDimens.SpaceXS))
            FloatingActionButton(
                onClick = {
                    onRequestMicPermission()
                    viewModel.toggleVoiceListening()
                },
                containerColor = if (state.executionState == AssistantExecutionState.LISTENING) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            ) {
                Icon(
                    imageVector = if (state.executionState == AssistantExecutionState.LISTENING) {
                        Icons.Default.Stop
                    } else {
                        Icons.Default.Mic
                    },
                    contentDescription = if (state.executionState == AssistantExecutionState.LISTENING) {
                        stringResource(R.string.action_stop_listening)
                    } else {
                        stringResource(R.string.action_start_listening)
                    }
                )
            }
        }
    }
}
