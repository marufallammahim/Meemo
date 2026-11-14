package com.marufallammahim.meemo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marufallammahim.meemo.actions.MeemoAction
import com.marufallammahim.meemo.core.error.MeemoError
import com.marufallammahim.meemo.domain.model.ConversationEntryModel
import com.marufallammahim.meemo.domain.model.InstalledAppInfo
import com.marufallammahim.meemo.presentation.theme.MeemoDimens
import com.marufallammahim.meemo.presentation.theme.MeemoPalette

@Composable
fun StatusPillBadge(
    label: String,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isError -> MeemoPalette.StatusError.copy(alpha = 0.18f)
        label == "VERIFIED" || label == "SUCCESS" -> MeemoPalette.StatusSuccess.copy(alpha = 0.18f)
        label == "SETTINGS_OPENED" || label == "AWAITING_CONFIRMATION" -> MeemoPalette.StatusWarning.copy(alpha = 0.20f)
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    }
    val textColor = when {
        isError -> MeemoPalette.StatusError
        label == "VERIFIED" || label == "SUCCESS" -> MeemoPalette.StatusSuccess
        label == "SETTINGS_OPENED" || label == "AWAITING_CONFIRMATION" -> MeemoPalette.StatusWarning
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun QuickSuggestionChipsRow(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = MeemoDimens.SpaceMD),
        horizontalArrangement = Arrangement.spacedBy(MeemoDimens.SpaceXS)
    ) {
        items(suggestions) { phrase ->
            AssistChip(
                onClick = { onSuggestionClick(phrase) },
                label = {
                    Text(
                        text = phrase,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }
    }
}

@Composable
fun ConversationBubbleCard(
    entry: ConversationEntryModel,
    modifier: Modifier = Modifier
) {
    val containerColor = if (entry.isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (entry.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(MeemoDimens.BubbleCornerRadius),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            Column(modifier = Modifier.padding(MeemoDimens.SpaceMD)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (entry.isUser) "You" else "Meemo",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!entry.isUser) {
                        StatusPillBadge(
                            label = entry.statusBadge,
                            isError = entry.statusBadge.startsWith("ERR_") || entry.statusBadge == "FAILED"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(MeemoDimens.SpaceXS))
                Text(
                    text = entry.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!entry.intentName.isNullOrBlank() && !entry.isUser) {
                    Spacer(modifier = Modifier.height(MeemoDimens.SpaceXXS))
                    Text(
                        text = "Intent: ${entry.intentName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AmbiguousAppSelectorCard(
    query: String,
    candidates: List<InstalledAppInfo>,
    onSelectApp: (InstalledAppInfo) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(MeemoDimens.CardCornerRadius)
    ) {
        Column(modifier = Modifier.padding(MeemoDimens.SpaceMD)) {
            Text(
                text = "Multiple Applications Match '$query'",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(MeemoDimens.SpaceXS))
            candidates.forEach { app ->
                OutlinedButton(
                    onClick = { onSelectApp(app) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Text(text = "${app.appLabel} (${app.packageName})")
                }
            }
            Spacer(modifier = Modifier.height(MeemoDimens.SpaceXS))
            OutlinedButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun ConfirmationPromptCard(
    promptText: String,
    pendingAction: MeemoAction,
    onConfirm: (MeemoAction) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(MeemoDimens.CardCornerRadius)
    ) {
        Column(modifier = Modifier.padding(MeemoDimens.SpaceMD)) {
            StatusPillBadge(label = "CONFIRMATION REQUIRED")
            Spacer(modifier = Modifier.height(MeemoDimens.SpaceXS))
            Text(
                text = promptText,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(MeemoDimens.SpaceSM))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(MeemoDimens.SpaceXS))
                Button(onClick = { onConfirm(pendingAction) }) {
                    Text("Confirm & Execute")
                }
            }
        }
    }
}

@Composable
fun ErrorRecoveryBanner(
    error: MeemoError,
    onRecoveryActionClick: (MeemoError) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MeemoPalette.StatusError.copy(alpha = 0.14f)
        ),
        shape = RoundedCornerShape(MeemoDimens.CardCornerRadius)
    ) {
        Column(modifier = Modifier.padding(MeemoDimens.SpaceMD)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = error.titleEn,
                    style = MaterialTheme.typography.titleMedium,
                    color = MeemoPalette.StatusError
                )
                Text(
                    text = error.code,
                    style = MaterialTheme.typography.labelSmall,
                    color = MeemoPalette.StatusError
                )
            }
            Spacer(modifier = Modifier.height(MeemoDimens.SpaceXXS))
            Text(
                text = error.messageEn,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(MeemoDimens.SpaceSM))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
                Spacer(modifier = Modifier.width(MeemoDimens.SpaceXS))
                Button(onClick = { onRecoveryActionClick(error) }) {
                    Text("Resolve")
                }
            }
        }
    }
}
