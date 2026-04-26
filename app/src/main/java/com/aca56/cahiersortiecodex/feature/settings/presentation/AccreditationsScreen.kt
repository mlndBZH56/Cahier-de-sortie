package com.aca56.cahiersortiecodex.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aca56.cahiersortiecodex.data.local.entity.RowerLevel
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialog
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialogType

@Composable
fun AccreditationsRoute(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    AccreditationsScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        onLevelChanged = viewModel::updateRowerLevel,
        onDismissFeedback = viewModel::clearMessage
    )
}

@Composable
fun AccreditationsScreen(
    contentPadding: PaddingValues,
    uiState: SettingsUiState,
    onLevelChanged: (Long, RowerLevel) -> Unit,
    onDismissFeedback: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Accréditations des rameurs",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Définissez le niveau de compétence de chaque rameur pour restreindre l'accès à certains bateaux.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (uiState.rowerManagement.rowers.isEmpty()) {
            Text(
                text = "Aucun rameur enregistré.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp)
            )
        } else {
            uiState.rowerManagement.rowers.forEach { rower ->
                RowerLevelCard(
                    name = rower.displayName,
                    currentLevel = rower.level,
                    onLevelSelected = { newLevel -> onLevelChanged(rower.id, newLevel) }
                )
            }
        }
    }

    uiState.message?.let { message ->
        FeedbackDialog(
            message = message,
            type = uiState.messageType ?: FeedbackDialogType.SUCCESS,
            onDismiss = onDismissFeedback
        )
    }
}

@Composable
private fun RowerLevelCard(
    name: String,
    currentLevel: RowerLevel,
    onLevelSelected: (RowerLevel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Box {
                Surface(
                    modifier = Modifier
                        .clickable { expanded = true }
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = currentLevel.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    RowerLevel.entries.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level.label) },
                            onClick = {
                                onLevelSelected(level)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
