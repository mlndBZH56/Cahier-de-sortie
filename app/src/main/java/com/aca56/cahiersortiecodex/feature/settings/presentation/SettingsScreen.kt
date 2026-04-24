package com.aca56.cahiersortiecodex.feature.settings.presentation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aca56.cahiersortiecodex.data.local.entity.BoatEntity
import com.aca56.cahiersortiecodex.data.local.entity.DestinationEntity
import com.aca56.cahiersortiecodex.data.local.entity.RowerEntity
import com.aca56.cahiersortiecodex.data.settings.ThemeMode
import com.aca56.cahiersortiecodex.feature.settings.presentation.DataCleanupPeriod
import com.aca56.cahiersortiecodex.ui.components.AppTextField
import com.aca56.cahiersortiecodex.ui.components.AppTextFieldType
import com.aca56.cahiersortiecodex.ui.components.AppSelectorFieldButton
import com.aca56.cahiersortiecodex.ui.components.AppDatePickerDialog
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialog
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialogType
import com.aca56.cahiersortiecodex.ui.components.SearchableSelectableList
import com.aca56.cahiersortiecodex.ui.components.SearchableSelectableOption
import com.aca56.cahiersortiecodex.ui.components.SearchableSingleSelectList
import com.aca56.cahiersortiecodex.ui.components.formatDateForDisplay
import com.aca56.cahiersortiecodex.ui.components.rememberInteractionAwareValueChange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsRoute(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel,
    onOpenRowers: () -> Unit,
    onOpenBoats: () -> Unit,
    onOpenDestinations: () -> Unit,
    onOpenCrews: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingSingleBoatExportId by remember { mutableStateOf<Long?>(null) }
    var pendingRemarksRepairsOnly by remember { mutableStateOf(false) }
    var pendingRemarksBoatId by remember { mutableStateOf<Long?>(null) }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = OpenDocument(),
    ) { uri ->
        uri?.let(viewModel::restoreBackup) ?: viewModel.clearMessage()
    }
    val fullDatabaseExportLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument("application/zip"),
    ) { uri ->
        uri?.let(viewModel::exportFullDatabase) ?: viewModel.clearMessage()
    }
    val sessionsExportLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument("text/csv"),
    ) { uri ->
        uri?.let(viewModel::exportSessions) ?: viewModel.clearMessage()
    }
    val allBoatsExportLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument("application/zip"),
    ) { uri ->
        uri?.let(viewModel::exportAllBoats) ?: viewModel.clearMessage()
    }
    val singleBoatExportLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument("application/zip"),
    ) { uri ->
        val boatId = pendingSingleBoatExportId
        pendingSingleBoatExportId = null
        if (uri != null && boatId != null) {
            viewModel.exportSingleBoat(uri, boatId)
        } else {
            viewModel.clearMessage()
        }
    }
    val remarksExportLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument("text/csv"),
    ) { uri ->
        val repairsOnly = pendingRemarksRepairsOnly
        val boatId = pendingRemarksBoatId
        pendingRemarksRepairsOnly = false
        pendingRemarksBoatId = null
        if (uri != null) {
            viewModel.exportRemarks(uri, repairsOnly, boatId)
        } else {
            viewModel.clearMessage()
        }
    }
    val rowersExportLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument("text/csv"),
    ) { uri ->
        uri?.let(viewModel::exportRowers) ?: viewModel.clearMessage()
    }
    val crewsExportLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument("text/csv"),
    ) { uri ->
        uri?.let(viewModel::exportCrews) ?: viewModel.clearMessage()
    }

    SettingsScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        onPinInputChanged = viewModel::onPinInputChanged,
        onCurrentPinChanged = viewModel::onCurrentPinChanged,
        onNewPinChanged = viewModel::onNewPinChanged,
        onConfirmPinChanged = viewModel::onConfirmPinChanged,
        onSuperAdminCurrentPinChanged = viewModel::onSuperAdminCurrentPinChanged,
        onSuperAdminNewPinChanged = viewModel::onSuperAdminNewPinChanged,
        onSuperAdminConfirmPinChanged = viewModel::onSuperAdminConfirmPinChanged,
        onThemeModeChanged = viewModel::onThemeModeChanged,
        onInactivityTimeoutMinutesChanged = viewModel::onInactivityTimeoutMinutesChanged,
        onSuccessPopupDurationSecondsChanged = viewModel::onSuccessPopupDurationSecondsChanged,
        onErrorPopupDurationSecondsChanged = viewModel::onErrorPopupDurationSecondsChanged,
        onAnimationsEnabledChanged = viewModel::onAnimationsEnabledChanged,
        onCrewsEnabledChanged = viewModel::onCrewsEnabledChanged,
        onPrimaryColorChanged = viewModel::onPrimaryColorChanged,
        onSecondaryColorChanged = viewModel::onSecondaryColorChanged,
        onTertiaryColorChanged = viewModel::onTertiaryColorChanged,
        onUnlockSettings = viewModel::unlockSettings,
        onSaveFirstPin = viewModel::saveFirstPin,
        onChangeNormalPin = viewModel::changeNormalPin,
        onChangeSuperAdminPin = viewModel::changeSuperAdminPin,
        onSaveAppSettings = viewModel::saveAppSettings,
        onSaveNotificationSettings = viewModel::saveNotificationSettings,
        onSaveThemeColors = viewModel::saveThemeColors,
        onResetAllAppData = viewModel::resetAllAppData,
        onRowerFirstNameChanged = viewModel::onRowerFirstNameChanged,
        onRowerLastNameChanged = viewModel::onRowerLastNameChanged,
        onSaveRower = viewModel::saveRower,
        onEditRower = viewModel::startEditingRower,
        onCancelRowerEditing = viewModel::cancelRowerEditing,
        onDeleteRower = viewModel::deleteRower,
        onBoatNameChanged = viewModel::onBoatNameChanged,
        onBoatSeatCountChanged = viewModel::onBoatSeatCountChanged,
        onSaveBoat = viewModel::saveBoat,
        onEditBoat = viewModel::startEditingBoat,
        onCancelBoatEditing = viewModel::cancelBoatEditing,
        onDeleteBoat = viewModel::deleteBoat,
        onDestinationNameChanged = viewModel::onDestinationNameChanged,
        onSaveDestination = viewModel::saveDestination,
        onEditDestination = viewModel::startEditingDestination,
        onCancelDestinationEditing = viewModel::cancelDestinationEditing,
        onDeleteDestination = viewModel::deleteDestination,
        onCleanupSessionsEnabledChanged = viewModel::onCleanupSessionsEnabledChanged,
        onCleanupRemarksEnabledChanged = viewModel::onCleanupRemarksEnabledChanged,
        onCleanupPeriodChanged = viewModel::onCleanupPeriodChanged,
        onCleanupCustomCutoffDateChanged = viewModel::onCleanupCustomCutoffDateChanged,
        onPreviewDataCleanup = viewModel::previewDataCleanup,
        onDismissCleanupPreview = viewModel::dismissCleanupPreview,
        onPerformDataCleanup = viewModel::performDataCleanup,
        onExportRowerSearchQueryChanged = viewModel::onExportRowerSearchQueryChanged,
        onExportRowerSelected = viewModel::onExportRowerSelected,
        onExportBoatSearchQueryChanged = viewModel::onExportBoatSearchQueryChanged,
        onExportBoatSelected = viewModel::onExportBoatSelected,
        onExportDateFromChanged = viewModel::onExportDateFromChanged,
        onExportDateToChanged = viewModel::onExportDateToChanged,
        onClearExportFilters = viewModel::clearExportFilters,
        onExportFullDatabase = {
            viewModel.clearMessage()
            fullDatabaseExportLauncher.launch(defaultFullDatabaseExportFileName())
        },
        onExportSessions = {
            viewModel.clearMessage()
            sessionsExportLauncher.launch(defaultSessionsExportFileName())
        },
        onExportAllBoats = {
            viewModel.clearMessage()
            allBoatsExportLauncher.launch(defaultBoatSheetsExportFileName())
        },
        onExportSingleBoat = { boatId ->
            pendingSingleBoatExportId = boatId
            viewModel.clearMessage()
            singleBoatExportLauncher.launch(defaultSingleBoatExportFileName())
        },
        onExportRemarks = { repairsOnly, boatId ->
            pendingRemarksRepairsOnly = repairsOnly
            pendingRemarksBoatId = boatId
            viewModel.clearMessage()
            remarksExportLauncher.launch(defaultRemarksExportFileName())
        },
        onExportRowers = {
            viewModel.clearMessage()
            rowersExportLauncher.launch(defaultRowersExportFileName())
        },
        onExportCrews = {
            viewModel.clearMessage()
            crewsExportLauncher.launch(defaultCrewsExportFileName())
        },
        onRestoreBackup = {
            viewModel.clearMessage()
            restoreLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed"))
        },
        onOpenRowers = onOpenRowers,
        onOpenBoats = onOpenBoats,
        onOpenDestinations = onOpenDestinations,
        onOpenCrews = onOpenCrews,
        onDismissRestartAfterRestore = viewModel::dismissRestartAfterRestore,
        onClearMessage = viewModel::clearMessage,
    )
}

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    uiState: SettingsUiState,
    onPinInputChanged: (String) -> Unit,
    onCurrentPinChanged: (String) -> Unit,
    onNewPinChanged: (String) -> Unit,
    onConfirmPinChanged: (String) -> Unit,
    onSuperAdminCurrentPinChanged: (String) -> Unit,
    onSuperAdminNewPinChanged: (String) -> Unit,
    onSuperAdminConfirmPinChanged: (String) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onInactivityTimeoutMinutesChanged: (String) -> Unit,
    onSuccessPopupDurationSecondsChanged: (String) -> Unit,
    onErrorPopupDurationSecondsChanged: (String) -> Unit,
    onAnimationsEnabledChanged: (Boolean) -> Unit,
    onCrewsEnabledChanged: (Boolean) -> Unit,
    onPrimaryColorChanged: (String) -> Unit,
    onSecondaryColorChanged: (String) -> Unit,
    onTertiaryColorChanged: (String) -> Unit,
    onUnlockSettings: () -> Unit,
    onSaveFirstPin: () -> Unit,
    onChangeNormalPin: () -> Unit,
    onChangeSuperAdminPin: () -> Unit,
    onSaveAppSettings: () -> Unit,
    onSaveNotificationSettings: () -> Unit,
    onSaveThemeColors: () -> Unit,
    onResetAllAppData: (String) -> Boolean,
    onRowerFirstNameChanged: (String) -> Unit,
    onRowerLastNameChanged: (String) -> Unit,
    onSaveRower: () -> Unit,
    onEditRower: (RowerEntity) -> Unit,
    onCancelRowerEditing: () -> Unit,
    onDeleteRower: (RowerEntity) -> Unit,
    onBoatNameChanged: (String) -> Unit,
    onBoatSeatCountChanged: (String) -> Unit,
    onSaveBoat: () -> Unit,
    onEditBoat: (BoatEntity) -> Unit,
    onCancelBoatEditing: () -> Unit,
    onDeleteBoat: (BoatEntity) -> Unit,
    onDestinationNameChanged: (String) -> Unit,
    onSaveDestination: () -> Unit,
    onEditDestination: (DestinationEntity) -> Unit,
    onCancelDestinationEditing: () -> Unit,
    onDeleteDestination: (DestinationEntity) -> Unit,
    onCleanupSessionsEnabledChanged: (Boolean) -> Unit,
    onCleanupRemarksEnabledChanged: (Boolean) -> Unit,
    onCleanupPeriodChanged: (DataCleanupPeriod) -> Unit,
    onCleanupCustomCutoffDateChanged: (String) -> Unit,
    onPreviewDataCleanup: () -> Unit,
    onDismissCleanupPreview: () -> Unit,
    onPerformDataCleanup: (String) -> Boolean,
    onExportRowerSearchQueryChanged: (String) -> Unit,
    onExportRowerSelected: (String, Boolean) -> Unit,
    onExportBoatSearchQueryChanged: (String) -> Unit,
    onExportBoatSelected: (Long, Boolean) -> Unit,
    onExportDateFromChanged: (String) -> Unit,
    onExportDateToChanged: (String) -> Unit,
    onClearExportFilters: () -> Unit,
    onExportFullDatabase: () -> Unit,
    onExportSessions: () -> Unit,
    onExportAllBoats: () -> Unit,
    onExportSingleBoat: (Long) -> Unit,
    onExportRemarks: (Boolean, Long?) -> Unit,
    onExportRowers: () -> Unit,
    onExportCrews: () -> Unit,
    onRestoreBackup: () -> Unit,
    onOpenRowers: () -> Unit,
    onOpenBoats: () -> Unit,
    onOpenDestinations: () -> Unit,
    onOpenCrews: () -> Unit,
    onDismissRestartAfterRestore: () -> Unit,
    onClearMessage: () -> Unit,
) {
    val context = LocalContext.current

    when {
        !uiState.hasPin -> CreatePinScreen(
            contentPadding = contentPadding,
            uiState = uiState,
            onNewPinChanged = onNewPinChanged,
            onConfirmPinChanged = onConfirmPinChanged,
            onSaveFirstPin = onSaveFirstPin,
        )
        !uiState.isUnlocked -> UnlockSettingsScreen(
            contentPadding = contentPadding,
            uiState = uiState,
            onPinInputChanged = onPinInputChanged,
            onUnlockSettings = onUnlockSettings,
        )
        else -> SettingsContent(
            contentPadding = contentPadding,
            uiState = uiState,
            onCurrentPinChanged = onCurrentPinChanged,
            onNewPinChanged = onNewPinChanged,
            onConfirmPinChanged = onConfirmPinChanged,
            onSuperAdminCurrentPinChanged = onSuperAdminCurrentPinChanged,
            onSuperAdminNewPinChanged = onSuperAdminNewPinChanged,
            onSuperAdminConfirmPinChanged = onSuperAdminConfirmPinChanged,
            onThemeModeChanged = onThemeModeChanged,
            onInactivityTimeoutMinutesChanged = onInactivityTimeoutMinutesChanged,
            onSuccessPopupDurationSecondsChanged = onSuccessPopupDurationSecondsChanged,
            onErrorPopupDurationSecondsChanged = onErrorPopupDurationSecondsChanged,
            onAnimationsEnabledChanged = onAnimationsEnabledChanged,
            onCrewsEnabledChanged = onCrewsEnabledChanged,
            onPrimaryColorChanged = onPrimaryColorChanged,
            onSecondaryColorChanged = onSecondaryColorChanged,
            onTertiaryColorChanged = onTertiaryColorChanged,
            onChangeNormalPin = onChangeNormalPin,
            onChangeSuperAdminPin = onChangeSuperAdminPin,
            onSaveAppSettings = onSaveAppSettings,
            onSaveNotificationSettings = onSaveNotificationSettings,
            onSaveThemeColors = onSaveThemeColors,
            onResetAllAppData = onResetAllAppData,
            onRowerFirstNameChanged = onRowerFirstNameChanged,
            onRowerLastNameChanged = onRowerLastNameChanged,
            onSaveRower = onSaveRower,
            onEditRower = onEditRower,
            onCancelRowerEditing = onCancelRowerEditing,
            onDeleteRower = onDeleteRower,
            onBoatNameChanged = onBoatNameChanged,
            onBoatSeatCountChanged = onBoatSeatCountChanged,
            onSaveBoat = onSaveBoat,
            onEditBoat = onEditBoat,
            onCancelBoatEditing = onCancelBoatEditing,
            onDeleteBoat = onDeleteBoat,
            onDestinationNameChanged = onDestinationNameChanged,
            onSaveDestination = onSaveDestination,
            onEditDestination = onEditDestination,
            onCancelDestinationEditing = onCancelDestinationEditing,
            onDeleteDestination = onDeleteDestination,
            onCleanupSessionsEnabledChanged = onCleanupSessionsEnabledChanged,
            onCleanupRemarksEnabledChanged = onCleanupRemarksEnabledChanged,
            onCleanupPeriodChanged = onCleanupPeriodChanged,
            onCleanupCustomCutoffDateChanged = onCleanupCustomCutoffDateChanged,
            onPreviewDataCleanup = onPreviewDataCleanup,
            onDismissCleanupPreview = onDismissCleanupPreview,
            onPerformDataCleanup = onPerformDataCleanup,
            onExportRowerSearchQueryChanged = onExportRowerSearchQueryChanged,
            onExportRowerSelected = onExportRowerSelected,
            onExportBoatSearchQueryChanged = onExportBoatSearchQueryChanged,
            onExportBoatSelected = onExportBoatSelected,
            onExportDateFromChanged = onExportDateFromChanged,
            onExportDateToChanged = onExportDateToChanged,
            onClearExportFilters = onClearExportFilters,
            onExportFullDatabase = onExportFullDatabase,
            onExportSessions = onExportSessions,
            onExportAllBoats = onExportAllBoats,
            onExportSingleBoat = onExportSingleBoat,
            onExportRemarks = onExportRemarks,
            onExportRowers = onExportRowers,
            onExportCrews = onExportCrews,
            onRestoreBackup = onRestoreBackup,
            onOpenRowers = onOpenRowers,
            onOpenBoats = onOpenBoats,
            onOpenDestinations = onOpenDestinations,
            onOpenCrews = onOpenCrews,
            onDismissRestartAfterRestore = onDismissRestartAfterRestore,
        )
    }

    if (uiState.showRestartAfterRestore) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = MaterialTheme.colorScheme.errorContainer,
            titleContentColor = MaterialTheme.colorScheme.onErrorContainer,
            textContentColor = MaterialTheme.colorScheme.onErrorContainer,
            title = { Text("Redémarrage requis") },
            text = { Text("Redémarrer l'application pour appliquer les modifications") },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissRestartAfterRestore()
                        (context as? Activity)?.finishAffinity()
                    },
                ) {
                    Text("Fermer l'application")
                }
            },
        )
    }

    uiState.message?.let { message ->
        FeedbackDialog(
            message = message,
            type = uiState.messageType ?: FeedbackDialogType.ERROR,
            onDismiss = onClearMessage,
        )
    }
}

@Composable
private fun CreatePinScreen(
    contentPadding: PaddingValues,
    uiState: SettingsUiState,
    onNewPinChanged: (String) -> Unit,
    onConfirmPinChanged: (String) -> Unit,
    onSaveFirstPin: () -> Unit,
) {
    val trackedOnNewPinChanged = rememberInteractionAwareValueChange(onNewPinChanged)
    val trackedOnConfirmPinChanged = rememberInteractionAwareValueChange(onConfirmPinChanged)

    SettingsGateScreen(
        contentPadding = contentPadding,
        title = "Créer le code PIN des paramètres",
        description = "Définissez un code PIN lors de la première utilisation pour protéger l'écran des paramètres.",
    ) {
        AppTextField(
            value = uiState.newPinInput,
            onValueChange = trackedOnNewPinChanged,
            label = "Nouveau code PIN",
            modifier = Modifier.fillMaxWidth(),
            type = AppTextFieldType.PIN,
        )
        AppTextField(
            value = uiState.confirmPinInput,
            onValueChange = trackedOnConfirmPinChanged,
            label = "Confirmer le code PIN",
            modifier = Modifier.fillMaxWidth(),
            type = AppTextFieldType.PIN,
        )
        Button(
            onClick = {
                onSaveFirstPin()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Enregistrer le code PIN")
        }
    }
}

@Composable
private fun UnlockSettingsScreen(
    contentPadding: PaddingValues,
    uiState: SettingsUiState,
    onPinInputChanged: (String) -> Unit,
    onUnlockSettings: () -> Unit,
) {
    val trackedOnPinInputChanged = rememberInteractionAwareValueChange(onPinInputChanged)

    SettingsGateScreen(
        contentPadding = contentPadding,
        title = "Paramètres",
        description = "Saisir le code PIN",
    ) {
        AppTextField(
            value = uiState.pinInput,
            onValueChange = trackedOnPinInputChanged,
            label = "Saisir le code PIN",
            modifier = Modifier.fillMaxWidth(),
            type = AppTextFieldType.PIN,
        )
        Button(
            onClick = {
                onUnlockSettings()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Déverrouiller")
        }
    }
}

@Composable
private fun SettingsGateScreen(
    contentPadding: PaddingValues,
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 3.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                content()
            }
        }
    }
}

@Composable
private fun SettingsContent(
    contentPadding: PaddingValues,
    uiState: SettingsUiState,
    onCurrentPinChanged: (String) -> Unit,
    onNewPinChanged: (String) -> Unit,
    onConfirmPinChanged: (String) -> Unit,
    onSuperAdminCurrentPinChanged: (String) -> Unit,
    onSuperAdminNewPinChanged: (String) -> Unit,
    onSuperAdminConfirmPinChanged: (String) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onInactivityTimeoutMinutesChanged: (String) -> Unit,
    onSuccessPopupDurationSecondsChanged: (String) -> Unit,
    onErrorPopupDurationSecondsChanged: (String) -> Unit,
    onAnimationsEnabledChanged: (Boolean) -> Unit,
    onCrewsEnabledChanged: (Boolean) -> Unit,
    onPrimaryColorChanged: (String) -> Unit,
    onSecondaryColorChanged: (String) -> Unit,
    onTertiaryColorChanged: (String) -> Unit,
    onChangeNormalPin: () -> Unit,
    onChangeSuperAdminPin: () -> Unit,
    onSaveAppSettings: () -> Unit,
    onSaveNotificationSettings: () -> Unit,
    onSaveThemeColors: () -> Unit,
    onResetAllAppData: (String) -> Boolean,
    onRowerFirstNameChanged: (String) -> Unit,
    onRowerLastNameChanged: (String) -> Unit,
    onSaveRower: () -> Unit,
    onEditRower: (RowerEntity) -> Unit,
    onCancelRowerEditing: () -> Unit,
    onDeleteRower: (RowerEntity) -> Unit,
    onBoatNameChanged: (String) -> Unit,
    onBoatSeatCountChanged: (String) -> Unit,
    onSaveBoat: () -> Unit,
    onEditBoat: (BoatEntity) -> Unit,
    onCancelBoatEditing: () -> Unit,
    onDeleteBoat: (BoatEntity) -> Unit,
    onDestinationNameChanged: (String) -> Unit,
    onSaveDestination: () -> Unit,
    onEditDestination: (DestinationEntity) -> Unit,
    onCancelDestinationEditing: () -> Unit,
    onDeleteDestination: (DestinationEntity) -> Unit,
    onCleanupSessionsEnabledChanged: (Boolean) -> Unit,
    onCleanupRemarksEnabledChanged: (Boolean) -> Unit,
    onCleanupPeriodChanged: (DataCleanupPeriod) -> Unit,
    onCleanupCustomCutoffDateChanged: (String) -> Unit,
    onPreviewDataCleanup: () -> Unit,
    onDismissCleanupPreview: () -> Unit,
    onPerformDataCleanup: (String) -> Boolean,
    onExportRowerSearchQueryChanged: (String) -> Unit,
    onExportRowerSelected: (String, Boolean) -> Unit,
    onExportBoatSearchQueryChanged: (String) -> Unit,
    onExportBoatSelected: (Long, Boolean) -> Unit,
    onExportDateFromChanged: (String) -> Unit,
    onExportDateToChanged: (String) -> Unit,
    onClearExportFilters: () -> Unit,
    onExportFullDatabase: () -> Unit,
    onExportSessions: () -> Unit,
    onExportAllBoats: () -> Unit,
    onExportSingleBoat: (Long) -> Unit,
    onExportRemarks: (Boolean, Long?) -> Unit,
    onExportRowers: () -> Unit,
    onExportCrews: () -> Unit,
    onRestoreBackup: () -> Unit,
    onOpenRowers: () -> Unit,
    onOpenBoats: () -> Unit,
    onOpenDestinations: () -> Unit,
    onOpenCrews: () -> Unit,
    onDismissRestartAfterRestore: () -> Unit,
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showResetPinDialog by remember { mutableStateOf(false) }
    var resetPinInput by remember { mutableStateOf("") }
    var cleanupPinInput by remember { mutableStateOf("") }
    var showSessionsExportDialog by remember { mutableStateOf(false) }
    var showRemarksExportDialog by remember { mutableStateOf(false) }
    var showSingleBoatExportDialog by remember { mutableStateOf(false) }
    var showExportDateFromPicker by remember { mutableStateOf(false) }
    var showExportDateToPicker by remember { mutableStateOf(false) }
    var remarksRepairsOnly by remember { mutableStateOf(false) }
    var remarksBoatSearchQuery by remember { mutableStateOf("") }
    var selectedRemarksBoatId by remember { mutableStateOf<Long?>(null) }
    var singleBoatSearchQuery by remember { mutableStateOf("") }
    var selectedSingleBoatId by remember { mutableStateOf<Long?>(null) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = MaterialTheme.colorScheme.errorContainer,
            titleContentColor = MaterialTheme.colorScheme.onErrorContainer,
            textContentColor = MaterialTheme.colorScheme.onErrorContainer,
            title = { Text("Réinitialiser les données") },
            text = { Text("Voulez-vous vraiment réinitialiser toutes les données de l'application ?") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        resetPinInput = ""
                        showResetPinDialog = true
                    },
                ) {
                    Text("Continuer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Annuler")
                }
            },
        )
    }

    if (showResetPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showResetPinDialog = false
                resetPinInput = ""
            },
            containerColor = MaterialTheme.colorScheme.errorContainer,
            titleContentColor = MaterialTheme.colorScheme.onErrorContainer,
            textContentColor = MaterialTheme.colorScheme.onErrorContainer,
            title = { Text("Confirmation finale") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Saisissez le code PIN super administrateur pour confirmer définitivement la réinitialisation.")
                    AppTextField(
                        value = resetPinInput,
                        onValueChange = { resetPinInput = it.filter(Char::isDigit) },
                        label = "Code PIN super administrateur",
                        modifier = Modifier.fillMaxWidth(),
                        type = AppTextFieldType.PIN,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (onResetAllAppData(resetPinInput)) {
                            showResetPinDialog = false
                            resetPinInput = ""
                        }
                    },
                    enabled = !uiState.isWorking,
                ) {
                    Text("Réinitialiser")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showResetPinDialog = false
                        resetPinInput = ""
                    },
                ) {
                    Text("Annuler")
                }
            },
        )
    }

    uiState.cleanupPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = {
                cleanupPinInput = ""
                onDismissCleanupPreview()
            },
            title = { Text("Confirmer le nettoyage") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Éléments à supprimer avant le ${formatDateForDisplay(preview.cutoffDate)} : ${preview.sessionsCount} sortie(s), ${preview.remarksCount} remarque(s).",
                    )
                    AppTextField(
                        value = cleanupPinInput,
                        onValueChange = { cleanupPinInput = it.filter(Char::isDigit) },
                        label = "Code PIN super administrateur",
                        modifier = Modifier.fillMaxWidth(),
                        type = AppTextFieldType.PIN,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (onPerformDataCleanup(cleanupPinInput)) {
                            cleanupPinInput = ""
                        }
                    },
                    enabled = !uiState.isWorking,
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        cleanupPinInput = ""
                        onDismissCleanupPreview()
                    },
                ) {
                    Text("Annuler")
                }
            },
        )
    }

    if (showSessionsExportDialog) {
        AlertDialog(
            onDismissRequest = { showSessionsExportDialog = false },
            title = { Text("Exporter les sorties") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Appliquez un ou plusieurs filtres avant l'export CSV. Sans filtre, toutes les sorties seront exportées.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SearchableSelectableList(
                        searchQuery = uiState.exportBoatSearchQuery,
                        onSearchQueryChanged = onExportBoatSearchQueryChanged,
                        searchLabel = "Filtrer par bateau",
                        selectedKeys = uiState.selectedExportBoatIds.mapTo(mutableSetOf()) { it.toString() },
                        options = uiState.filteredExportBoatOptions,
                        emptyLabel = "Aucun bateau disponible.",
                        noResultsLabel = "Aucun bateau trouvé.",
                        onOptionToggled = { key ->
                            key.toLongOrNull()?.let { onExportBoatSelected(it, it !in uiState.selectedExportBoatIds) }
                        },
                    )
                    SearchableSelectableList(
                        searchQuery = uiState.exportRowerSearchQuery,
                        onSearchQueryChanged = onExportRowerSearchQueryChanged,
                        searchLabel = "Filtrer par rameur",
                        selectedKeys = uiState.selectedExportRowers,
                        options = uiState.filteredExportRowerOptions,
                        emptyLabel = "Aucun rameur disponible.",
                        noResultsLabel = "Aucun rameur trouvé.",
                        onOptionToggled = { key ->
                            onExportRowerSelected(key, key !in uiState.selectedExportRowers)
                        },
                    )
                    AppSelectorFieldButton(
                        onClick = { showExportDateFromPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Date de début : ${uiState.exportDateFrom.takeIf { it.isNotBlank() }?.let(::formatDateForDisplay) ?: "Toutes"}")
                    }
                    AppSelectorFieldButton(
                        onClick = { showExportDateToPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Date de fin : ${uiState.exportDateTo.takeIf { it.isNotBlank() }?.let(::formatDateForDisplay) ?: "Toutes"}")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onClearExportFilters,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Réinitialiser")
                        }
                        Button(
                            onClick = {
                                showSessionsExportDialog = false
                                onExportSessions()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Exporter")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSessionsExportDialog = false }) {
                    Text("Fermer")
                }
            },
        )
    }

    if (showRemarksExportDialog) {
        val boatOptions = uiState.boatManagement.boats
            .sortedBy { it.name.lowercase() }
            .map { SearchableSelectableOption(key = it.id.toString(), label = it.name) }
        AlertDialog(
            onDismissRequest = { showRemarksExportDialog = false },
            title = { Text("Exporter les remarques") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Réparations uniquement")
                            Text(
                                "Activez pour n’exporter que les remarques de maintenance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = remarksRepairsOnly,
                            onCheckedChange = { remarksRepairsOnly = it },
                        )
                    }
                    SearchableSingleSelectList(
                        searchQuery = remarksBoatSearchQuery,
                        onSearchQueryChanged = { remarksBoatSearchQuery = it },
                        searchLabel = "Filtrer par bateau",
                        selectedKey = selectedRemarksBoatId?.toString(),
                        options = boatOptions,
                        emptyLabel = "Aucun bateau disponible.",
                        noResultsLabel = "Aucun bateau trouvé.",
                        onOptionSelected = { key ->
                            selectedRemarksBoatId = key.toLongOrNull()
                        },
                    )
                    OutlinedButton(
                        onClick = { selectedRemarksBoatId = null },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Tous les bateaux")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRemarksExportDialog = false
                        onExportRemarks(remarksRepairsOnly, selectedRemarksBoatId)
                    },
                ) {
                    Text("Exporter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemarksExportDialog = false }) {
                    Text("Annuler")
                }
            },
        )
    }

    if (showSingleBoatExportDialog) {
        val boatOptions = uiState.boatManagement.boats
            .sortedBy { it.name.lowercase() }
            .map { SearchableSelectableOption(key = it.id.toString(), label = it.name) }
        AlertDialog(
            onDismissRequest = { showSingleBoatExportDialog = false },
            title = { Text("Exporter un bateau") },
            text = {
                SearchableSingleSelectList(
                    searchQuery = singleBoatSearchQuery,
                    onSearchQueryChanged = { singleBoatSearchQuery = it },
                    searchLabel = "Sélectionner un bateau",
                    selectedKey = selectedSingleBoatId?.toString(),
                    options = boatOptions,
                    emptyLabel = "Aucun bateau disponible.",
                    noResultsLabel = "Aucun bateau trouvé.",
                    onOptionSelected = { key ->
                        selectedSingleBoatId = key.toLongOrNull()
                    },
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedSingleBoatId?.let {
                            showSingleBoatExportDialog = false
                            onExportSingleBoat(it)
                        }
                    },
                    enabled = selectedSingleBoatId != null,
                ) {
                    Text("Exporter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSingleBoatExportDialog = false }) {
                    Text("Annuler")
                }
            },
        )
    }

    if (showExportDateFromPicker) {
        AppDatePickerDialog(
            storageDate = uiState.exportDateFrom.ifBlank { defaultCleanupCutoffDate() },
            onDismissRequest = { showExportDateFromPicker = false },
            onDateSelected = { selectedDate ->
                onExportDateFromChanged(selectedDate)
                showExportDateFromPicker = false
            },
        )
    }

    if (showExportDateToPicker) {
        AppDatePickerDialog(
            storageDate = uiState.exportDateTo.ifBlank { defaultCleanupCutoffDate() },
            onDismissRequest = { showExportDateToPicker = false },
            onDateSelected = { selectedDate ->
                onExportDateToChanged(selectedDate)
                showExportDateToPicker = false
            },
        )
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
    ) {
        val useTwoColumns = maxWidth >= 900.dp

        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Paramètres",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                if (useTwoColumns) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            SectionHeader(
                                title = "Général",
                                description = "Actions principales de l'application et outils de maintenance.",
                            )
                            BackupSection(
                                uiState = uiState,
                                onExportFullDatabase = onExportFullDatabase,
                                onRestoreBackup = onRestoreBackup,
                            )
                            DataExportSection(
                                uiState = uiState,
                                onOpenSessionsExport = { showSessionsExportDialog = true },
                                onOpenSingleBoatExport = {
                                    singleBoatSearchQuery = ""
                                    selectedSingleBoatId = null
                                    showSingleBoatExportDialog = true
                                },
                                onOpenRemarksExport = {
                                    remarksBoatSearchQuery = ""
                                    selectedRemarksBoatId = null
                                    remarksRepairsOnly = false
                                    showRemarksExportDialog = true
                                },
                                onExportAllBoats = onExportAllBoats,
                                onExportRowers = onExportRowers,
                                onExportCrews = onExportCrews,
                            )

                            SectionHeader(
                                title = "Gestion des données",
                                description = "Ouvrir et gérer les listes principales de l'application.",
                            )
                            ManagementLinksSection(
                                onOpenRowers = onOpenRowers,
                                onOpenBoats = onOpenBoats,
                                onOpenDestinations = onOpenDestinations,
                            )

                            SectionHeader(
                                title = "Sécurité",
                                description = "Gérer le code PIN standard des paramètres.",
                            )
                            SecuritySection(
                                uiState = uiState,
                                onCurrentPinChanged = onCurrentPinChanged,
                                onNewPinChanged = onNewPinChanged,
                                onConfirmPinChanged = onConfirmPinChanged,
                                onChangeNormalPin = onChangeNormalPin,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                if (uiState.isSuperAdmin) {
                    SectionHeader(
                        title = "Super administrateur",
                        description = "Configuration avancée.",
                    )
                                AppBehaviorSection(
                                    uiState = uiState,
                                    onThemeModeChanged = onThemeModeChanged,
                                    onInactivityTimeoutMinutesChanged = onInactivityTimeoutMinutesChanged,
                                    onAnimationsEnabledChanged = onAnimationsEnabledChanged,
                                    onCrewsEnabledChanged = onCrewsEnabledChanged,
                                    onOpenCrews = onOpenCrews,
                                    onSaveAppSettings = onSaveAppSettings,
                                )
                                NotificationSettingsSection(
                                    uiState = uiState,
                                    onSuccessPopupDurationSecondsChanged = onSuccessPopupDurationSecondsChanged,
                                    onErrorPopupDurationSecondsChanged = onErrorPopupDurationSecondsChanged,
                                    onSaveNotificationSettings = onSaveNotificationSettings,
                                )
                                ThemeColorsSection(
                                    uiState = uiState,
                                    onPrimaryColorChanged = onPrimaryColorChanged,
                                    onSecondaryColorChanged = onSecondaryColorChanged,
                                    onTertiaryColorChanged = onTertiaryColorChanged,
                                    onSaveThemeColors = onSaveThemeColors,
                                )
                                SystemToolsSection(
                                    uiState = uiState,
                                    onResetAllAppData = { showResetDialog = true },
                                )
                                DataCleanupSection(
                                    uiState = uiState,
                                    onCleanupSessionsEnabledChanged = onCleanupSessionsEnabledChanged,
                                    onCleanupRemarksEnabledChanged = onCleanupRemarksEnabledChanged,
                                    onCleanupPeriodChanged = onCleanupPeriodChanged,
                                    onCleanupCustomCutoffDateChanged = onCleanupCustomCutoffDateChanged,
                                    onPreviewDataCleanup = onPreviewDataCleanup,
                                )
                                AdvancedAccessSection(
                                    uiState = uiState,
                                    onSuperAdminCurrentPinChanged = onSuperAdminCurrentPinChanged,
                                    onSuperAdminNewPinChanged = onSuperAdminNewPinChanged,
                                    onSuperAdminConfirmPinChanged = onSuperAdminConfirmPinChanged,
                                    onChangeSuperAdminPin = onChangeSuperAdminPin,
                                )
                            }
                        }
                    }
                } else {
                    SectionHeader(
                        title = "Général",
                        description = "Actions principales de l'application et outils de maintenance.",
                    )
                    BackupSection(
                        uiState = uiState,
                        onExportFullDatabase = onExportFullDatabase,
                        onRestoreBackup = onRestoreBackup,
                    )
                    DataExportSection(
                        uiState = uiState,
                        onOpenSessionsExport = { showSessionsExportDialog = true },
                        onOpenSingleBoatExport = {
                            singleBoatSearchQuery = ""
                            selectedSingleBoatId = null
                            showSingleBoatExportDialog = true
                        },
                        onOpenRemarksExport = {
                            remarksBoatSearchQuery = ""
                            selectedRemarksBoatId = null
                            remarksRepairsOnly = false
                            showRemarksExportDialog = true
                        },
                        onExportAllBoats = onExportAllBoats,
                        onExportRowers = onExportRowers,
                        onExportCrews = onExportCrews,
                    )

                    SectionHeader(
                        title = "Gestion des données",
                        description = "Ouvrir et gérer les listes principales de l'application.",
                    )
                    ManagementLinksSection(
                        onOpenRowers = onOpenRowers,
                        onOpenBoats = onOpenBoats,
                        onOpenDestinations = onOpenDestinations,
                    )

                    SectionHeader(
                        title = "Sécurité",
                        description = "Gérer le code PIN standard des paramètres.",
                    )
                    SecuritySection(
                        uiState = uiState,
                        onCurrentPinChanged = onCurrentPinChanged,
                        onNewPinChanged = onNewPinChanged,
                        onConfirmPinChanged = onConfirmPinChanged,
                        onChangeNormalPin = onChangeNormalPin,
                    )
                    if (uiState.isSuperAdmin) {
                        SectionHeader(
                            title = "Avancé",
                            description = "Configuration supplémentaire.",
                        )
                        AppBehaviorSection(
                            uiState = uiState,
                            onThemeModeChanged = onThemeModeChanged,
                            onInactivityTimeoutMinutesChanged = onInactivityTimeoutMinutesChanged,
                            onAnimationsEnabledChanged = onAnimationsEnabledChanged,
                            onCrewsEnabledChanged = onCrewsEnabledChanged,
                            onOpenCrews = onOpenCrews,
                            onSaveAppSettings = onSaveAppSettings,
                        )
                        NotificationSettingsSection(
                            uiState = uiState,
                            onSuccessPopupDurationSecondsChanged = onSuccessPopupDurationSecondsChanged,
                            onErrorPopupDurationSecondsChanged = onErrorPopupDurationSecondsChanged,
                            onSaveNotificationSettings = onSaveNotificationSettings,
                        )
                        ThemeColorsSection(
                            uiState = uiState,
                            onPrimaryColorChanged = onPrimaryColorChanged,
                            onSecondaryColorChanged = onSecondaryColorChanged,
                            onTertiaryColorChanged = onTertiaryColorChanged,
                            onSaveThemeColors = onSaveThemeColors,
                        )
                        SystemToolsSection(
                            uiState = uiState,
                            onResetAllAppData = { showResetDialog = true },
                        )
                        DataCleanupSection(
                            uiState = uiState,
                            onCleanupSessionsEnabledChanged = onCleanupSessionsEnabledChanged,
                            onCleanupRemarksEnabledChanged = onCleanupRemarksEnabledChanged,
                            onCleanupPeriodChanged = onCleanupPeriodChanged,
                            onCleanupCustomCutoffDateChanged = onCleanupCustomCutoffDateChanged,
                            onPreviewDataCleanup = onPreviewDataCleanup,
                        )
                        AdvancedAccessSection(
                            uiState = uiState,
                            onSuperAdminCurrentPinChanged = onSuperAdminCurrentPinChanged,
                            onSuperAdminNewPinChanged = onSuperAdminNewPinChanged,
                            onSuperAdminConfirmPinChanged = onSuperAdminConfirmPinChanged,
                            onChangeSuperAdminPin = onChangeSuperAdminPin,
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    description: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ManagementLinksSection(
    onOpenRowers: () -> Unit,
    onOpenBoats: () -> Unit,
    onOpenDestinations: () -> Unit,
) {
    SettingsSection(
        title = "Gérer les données",
        description = "Ouvrir les écrans de gestion des rameurs, bateaux et destinations.",
    ) {
        Button(
            onClick = onOpenRowers,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Ouvrir l'écran des rameurs")
        }
        Button(
            onClick = onOpenBoats,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Ouvrir l'écran des bateaux")
        }
        Button(
            onClick = onOpenDestinations,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Ouvrir l'écran des destinations")
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 3.dp,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

@Composable
private fun RowerManagementSection(
    uiState: SettingsUiState,
    onFirstNameChanged: (String) -> Unit,
    onLastNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    onEdit: (RowerEntity) -> Unit,
    onCancelEditing: () -> Unit,
    onDelete: (RowerEntity) -> Unit,
    onImportRowers: () -> Unit,
) {
    val trackedOnFirstNameChanged = rememberInteractionAwareValueChange(onFirstNameChanged)
    val trackedOnLastNameChanged = rememberInteractionAwareValueChange(onLastNameChanged)

    SettingsSection(
        title = "Gérer les rameurs",
        description = "Ajouter, modifier, supprimer ou importer des rameurs.",
    ) {
        OutlinedButton(
            onClick = onImportRowers,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isWorking) "Traitement..." else "Importer des rameurs")
        }
        AppTextField(
            value = uiState.rowerManagement.firstNameInput,
            onValueChange = trackedOnFirstNameChanged,
            label = "Prénom",
            modifier = Modifier.fillMaxWidth(),
            type = AppTextFieldType.SIMPLE,
        )
        AppTextField(
            value = uiState.rowerManagement.lastNameInput,
            onValueChange = trackedOnLastNameChanged,
            label = "Nom",
            modifier = Modifier.fillMaxWidth(),
            type = AppTextFieldType.SIMPLE,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onSave,
                enabled = !uiState.isWorking,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (uiState.rowerManagement.isEditing) "Mettre à jour" else "Ajouter")
            }
            if (uiState.rowerManagement.isEditing) {
                OutlinedButton(
                    onClick = onCancelEditing,
                    enabled = !uiState.isWorking,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Annuler")
                }
            }
        }
        if (uiState.rowerManagement.rowers.isEmpty()) {
            Text(
                text = "Aucun rameur enregistré pour le moment.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            uiState.rowerManagement.rowers.forEach { rower ->
                EditableListRow(
                    label = listOf(rower.firstName, rower.lastName).filter { it.isNotBlank() }.joinToString(" "),
                    secondaryLabel = null,
                    enabled = !uiState.isWorking,
                    onEdit = { onEdit(rower) },
                    onDelete = { onDelete(rower) },
                )
            }
        }
    }
}

@Composable
private fun BoatManagementSection(
    uiState: SettingsUiState,
    onBoatNameChanged: (String) -> Unit,
    onBoatSeatCountChanged: (String) -> Unit,
    onSaveBoat: () -> Unit,
    onEditBoat: (BoatEntity) -> Unit,
    onCancelBoatEditing: () -> Unit,
    onDeleteBoat: (BoatEntity) -> Unit,
    onImportBoats: () -> Unit,
) {
    val trackedOnBoatNameChanged = rememberInteractionAwareValueChange(onBoatNameChanged)
    val trackedOnBoatSeatCountChanged = rememberInteractionAwareValueChange(onBoatSeatCountChanged)

    SettingsSection(
        title = "Gérer les bateaux",
        description = "Ajouter, modifier ou supprimer des bateaux et leur nombre de places.",
    ) {
        OutlinedButton(
            onClick = onImportBoats,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isWorking) "Traitement..." else "Importer des bateaux (CSV)")
        }
        AppTextField(
            value = uiState.boatManagement.boatNameInput,
            onValueChange = trackedOnBoatNameChanged,
            label = "Nom du bateau",
            modifier = Modifier.fillMaxWidth(),
            type = AppTextFieldType.SIMPLE,
        )
        AppTextField(
            value = uiState.boatManagement.seatCountInput,
            onValueChange = trackedOnBoatSeatCountChanged,
            label = "Nombre de places",
            modifier = Modifier.fillMaxWidth(),
            type = AppTextFieldType.NUMERIC,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onSaveBoat,
                enabled = !uiState.isWorking,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (uiState.boatManagement.isEditing) "Mettre à jour" else "Ajouter")
            }
            if (uiState.boatManagement.isEditing) {
                OutlinedButton(
                    onClick = onCancelBoatEditing,
                    enabled = !uiState.isWorking,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Annuler")
                }
            }
        }
        if (uiState.boatManagement.boats.isEmpty()) {
            Text(
                text = "Aucun bateau enregistré pour le moment.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            uiState.boatManagement.boats.forEach { boat ->
                EditableListRow(
                    label = boat.name,
                    secondaryLabel = "Places : ${boat.seatCount}",
                    enabled = !uiState.isWorking,
                    onEdit = { onEditBoat(boat) },
                    onDelete = { onDeleteBoat(boat) },
                )
            }
        }
    }
}

@Composable
private fun DestinationManagementSection(
    uiState: SettingsUiState,
    onDestinationNameChanged: (String) -> Unit,
    onSaveDestination: () -> Unit,
    onEditDestination: (DestinationEntity) -> Unit,
    onCancelDestinationEditing: () -> Unit,
    onDeleteDestination: (DestinationEntity) -> Unit,
) {
    val trackedOnDestinationNameChanged = rememberInteractionAwareValueChange(onDestinationNameChanged)

    SettingsSection(
        title = "Gérer les destinations",
        description = "Gérer la liste des destinations utilisées par les sessions.",
    ) {
        AppTextField(
            value = uiState.destinationManagement.destinationNameInput,
            onValueChange = trackedOnDestinationNameChanged,
            label = if (uiState.destinationManagement.isEditing) {
                        "Modifier la destination"
                    } else {
                        "Nouvelle destination"
            },
            modifier = Modifier.fillMaxWidth(),
            type = AppTextFieldType.SIMPLE,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onSaveDestination,
                enabled = !uiState.isWorking,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (uiState.destinationManagement.isEditing) "Mettre à jour" else "Ajouter")
            }
            if (uiState.destinationManagement.isEditing) {
                OutlinedButton(
                    onClick = onCancelDestinationEditing,
                    enabled = !uiState.isWorking,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Annuler")
                }
            }
        }
        if (uiState.destinationManagement.destinations.isEmpty()) {
            Text(
                text = "Aucune destination enregistrée pour le moment.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            uiState.destinationManagement.destinations.forEach { destination ->
                EditableListRow(
                    label = destination.name,
                    secondaryLabel = null,
                    enabled = !uiState.isWorking,
                    onEdit = { onEditDestination(destination) },
                    onDelete = { onDeleteDestination(destination) },
                )
            }
        }
    }
}

@Composable
private fun AppBehaviorSection(
    uiState: SettingsUiState,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onInactivityTimeoutMinutesChanged: (String) -> Unit,
    onAnimationsEnabledChanged: (Boolean) -> Unit,
    onCrewsEnabledChanged: (Boolean) -> Unit,
    onOpenCrews: () -> Unit,
    onSaveAppSettings: () -> Unit,
) {
    val trackedOnInactivityTimeoutMinutesChanged = rememberInteractionAwareValueChange(onInactivityTimeoutMinutesChanged)

    SettingsSection(
        title = "Comportement de l'application",
        description = "Contrôler l'inactivité, les animations et le mode d'affichage.",
    ) {
        AppTextField(
            value = uiState.inactivityTimeoutMinutesInput,
            onValueChange = trackedOnInactivityTimeoutMinutesChanged,
            label = "Délai d'inactivité (minutes)",
            modifier = Modifier.fillMaxWidth(),
            type = AppTextFieldType.NUMERIC,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Activer les animations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "Activer ou désactiver les animations.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = uiState.animationsEnabled,
                onCheckedChange = onAnimationsEnabledChanged,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Activer les équipages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "Afficher la gestion des équipages et leur sélection dans les sorties.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = uiState.crewsEnabled,
                onCheckedChange = onCrewsEnabledChanged,
            )
        }
        if (uiState.crewsEnabled) {
            OutlinedButton(
                onClick = onOpenCrews,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Ouvrir l'écran des équipages")
            }
        }
        Text(
            text = "Mode d'apparence",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        ThemeMode.values().forEach { themeMode ->
            val isSelected = uiState.themeMode == themeMode
            if (isSelected) {
                Button(
                    onClick = { onThemeModeChanged(themeMode) },
                    enabled = !uiState.isWorking,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(themeMode.displayLabel())
                }
            } else {
                OutlinedButton(
                    onClick = { onThemeModeChanged(themeMode) },
                    enabled = !uiState.isWorking,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(themeMode.displayLabel())
                }
            }
        }
        Button(
            onClick = onSaveAppSettings,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isWorking) "Traitement..." else "Enregistrer les paramètres")
        }
    }
}

@Composable
private fun NotificationSettingsSection(
    uiState: SettingsUiState,
    onSuccessPopupDurationSecondsChanged: (String) -> Unit,
    onErrorPopupDurationSecondsChanged: (String) -> Unit,
    onSaveNotificationSettings: () -> Unit,
) {
    val trackedOnSuccessPopupDurationSecondsChanged =
        rememberInteractionAwareValueChange(onSuccessPopupDurationSecondsChanged)
    val trackedOnErrorPopupDurationSecondsChanged =
        rememberInteractionAwareValueChange(onErrorPopupDurationSecondsChanged)

    SettingsSection(
        title = "Notifications",
        description = "Configurer la durée d'affichage des fenêtres de retour.",
    ) {
        AppTextField(
            value = uiState.successPopupDurationSecondsInput,
            onValueChange = trackedOnSuccessPopupDurationSecondsChanged,
            label = "Durée de la confirmation (secondes)",
            modifier = Modifier.fillMaxWidth(),
            type = AppTextFieldType.NUMERIC,
        )
        AppTextField(
            value = uiState.errorPopupDurationSecondsInput,
            onValueChange = trackedOnErrorPopupDurationSecondsChanged,
            label = "Durée de l'erreur (secondes)",
            modifier = Modifier.fillMaxWidth(),
            type = AppTextFieldType.NUMERIC,
        )
        Button(
            onClick = onSaveNotificationSettings,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isWorking) "Traitement..." else "Enregistrer les notifications")
        }
    }
}

@Composable
private fun SystemToolsSection(
    uiState: SettingsUiState,
    onResetAllAppData: () -> Unit,
) {
    SettingsSection(
        title = "Outils système",
        description = "Utiliser les outils de maintenance pour le diagnostic et la récupération.",
    ) {
        Button(
            onClick = onResetAllAppData,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isWorking) "Traitement..." else "Réinitialiser toutes les données de l'application")
        }
    }
}

@Composable
private fun DataCleanupSection(
    uiState: SettingsUiState,
    onCleanupSessionsEnabledChanged: (Boolean) -> Unit,
    onCleanupRemarksEnabledChanged: (Boolean) -> Unit,
    onCleanupPeriodChanged: (DataCleanupPeriod) -> Unit,
    onCleanupCustomCutoffDateChanged: (String) -> Unit,
    onPreviewDataCleanup: () -> Unit,
) {
    var showCustomDatePicker by remember { mutableStateOf(false) }

    if (showCustomDatePicker) {
        AppDatePickerDialog(
            storageDate = uiState.cleanupCustomCutoffDate.ifBlank { defaultCleanupCutoffDate() },
            onDismissRequest = { showCustomDatePicker = false },
            onDateSelected = onCleanupCustomCutoffDateChanged,
        )
    }

    SettingsSection(
        title = "Nettoyage des données",
        description = "Supprimer des sorties historiques et des remarques anciennes sans toucher aux sorties en cours ni aux réparations actives.",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CleanupToggleButton(
                label = "Historique",
                selected = uiState.cleanupSessionsEnabled,
                onClick = { onCleanupSessionsEnabledChanged(!uiState.cleanupSessionsEnabled) },
                modifier = Modifier.weight(1f),
            )
            CleanupToggleButton(
                label = "Remarques",
                selected = uiState.cleanupRemarksEnabled,
                onClick = { onCleanupRemarksEnabledChanged(!uiState.cleanupRemarksEnabled) },
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = "Période",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )

        DataCleanupPeriod.values().forEach { period ->
            val selected = uiState.cleanupPeriod == period
            if (selected) {
                Button(
                    onClick = { onCleanupPeriodChanged(period) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(period.label)
                }
            } else {
                OutlinedButton(
                    onClick = { onCleanupPeriodChanged(period) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(period.label)
                }
            }
        }

        if (uiState.cleanupPeriod == DataCleanupPeriod.CUSTOM) {
            AppSelectorFieldButton(
                onClick = { showCustomDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (uiState.cleanupCustomCutoffDate.isBlank()) {
                        "Choisir une date limite"
                    } else {
                        "Avant le ${formatDateForDisplay(uiState.cleanupCustomCutoffDate)}"
                    },
                )
            }
        }

        Button(
            onClick = onPreviewDataCleanup,
            enabled = !uiState.isWorking && uiState.hasCleanupSelection,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isWorking) "Traitement..." else "Prévisualiser le nettoyage")
        }
    }
}

@Composable
private fun CleanupToggleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    }
}

@Composable
private fun ThemeColorsSection(
    uiState: SettingsUiState,
    onPrimaryColorChanged: (String) -> Unit,
    onSecondaryColorChanged: (String) -> Unit,
    onTertiaryColorChanged: (String) -> Unit,
    onSaveThemeColors: () -> Unit,
) {
    val trackedOnPrimaryColorChanged = rememberInteractionAwareValueChange(onPrimaryColorChanged)
    val trackedOnSecondaryColorChanged = rememberInteractionAwareValueChange(onSecondaryColorChanged)
    val trackedOnTertiaryColorChanged = rememberInteractionAwareValueChange(onTertiaryColorChanged)

    SettingsSection(
        title = "Configuration du thème",
        description = "Ajuster les couleurs primaire, secondaire et tertiaire de l'application.",
    ) {
        AppTextField(
            value = uiState.primaryColorInput,
            onValueChange = trackedOnPrimaryColorChanged,
            label = "Couleur primaire (#RRGGBB)",
            modifier = Modifier.fillMaxWidth(),
            type = AppTextFieldType.SIMPLE,
        )
        AppTextField(
            value = uiState.secondaryColorInput,
            onValueChange = trackedOnSecondaryColorChanged,
            label = "Couleur secondaire (#RRGGBB)",
            modifier = Modifier.fillMaxWidth(),
            type = AppTextFieldType.SIMPLE,
        )
        AppTextField(
            value = uiState.tertiaryColorInput,
            onValueChange = trackedOnTertiaryColorChanged,
            label = "Couleur tertiaire (#RRGGBB)",
            modifier = Modifier.fillMaxWidth(),
            type = AppTextFieldType.SIMPLE,
        )
        Button(
            onClick = onSaveThemeColors,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isWorking) "Traitement..." else "Enregistrer le thème")
        }
    }
}

@Composable
private fun SecuritySection(
    uiState: SettingsUiState,
    onCurrentPinChanged: (String) -> Unit,
    onNewPinChanged: (String) -> Unit,
    onConfirmPinChanged: (String) -> Unit,
    onChangeNormalPin: () -> Unit,
) {
    val trackedOnCurrentPinChanged = rememberInteractionAwareValueChange(onCurrentPinChanged)
    val trackedOnNewPinChanged = rememberInteractionAwareValueChange(onNewPinChanged)
    val trackedOnConfirmPinChanged = rememberInteractionAwareValueChange(onConfirmPinChanged)

    SettingsSection(
        title = "PIN des paramètres",
        description = if (uiState.isSuperAdmin) {
            "Définir le code PIN standard utilisé pour accéder aux paramètres."
        } else {
            "Mettre à jour le code PIN standard utilisé pour accéder aux paramètres."
        },
    ) {
        if (!uiState.isSuperAdmin) {
            AppTextField(
                value = uiState.currentPinInput,
                onValueChange = trackedOnCurrentPinChanged,
                label = "PIN actuel",
                modifier = Modifier.fillMaxWidth(),
                type = AppTextFieldType.PIN,
            )
        }
        AppTextField(
            value = uiState.newPinInput,
            onValueChange = trackedOnNewPinChanged,
            label = "Nouveau PIN",
            modifier = Modifier.fillMaxWidth(),
            type = AppTextFieldType.PIN,
        )
        AppTextField(
            value = uiState.confirmPinInput,
            onValueChange = trackedOnConfirmPinChanged,
            label = "Confirmer le PIN",
            modifier = Modifier.fillMaxWidth(),
            type = AppTextFieldType.PIN,
        )
        Button(
            onClick = onChangeNormalPin,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isWorking) "Traitement..." else "Modifier le PIN")
        }
    }
}

@Composable
private fun AdvancedAccessSection(
    uiState: SettingsUiState,
    onSuperAdminCurrentPinChanged: (String) -> Unit,
    onSuperAdminNewPinChanged: (String) -> Unit,
    onSuperAdminConfirmPinChanged: (String) -> Unit,
    onChangeSuperAdminPin: () -> Unit,
) {
    val trackedOnSuperAdminCurrentPinChanged = rememberInteractionAwareValueChange(onSuperAdminCurrentPinChanged)
    val trackedOnSuperAdminNewPinChanged = rememberInteractionAwareValueChange(onSuperAdminNewPinChanged)
    val trackedOnSuperAdminConfirmPinChanged = rememberInteractionAwareValueChange(onSuperAdminConfirmPinChanged)

    SettingsSection(
        title = "PIN super administrateur",
        description = "Mettre à jour le code PIN utilisé pour accéder à la section super administrateur.",
    ) {
        AppTextField(
            value = uiState.superAdminCurrentPinInput,
            onValueChange = trackedOnSuperAdminCurrentPinChanged,
            label = "PIN actuel",
            modifier = Modifier.fillMaxWidth(),
            type = AppTextFieldType.PIN,
        )
        AppTextField(
            value = uiState.superAdminNewPinInput,
            onValueChange = trackedOnSuperAdminNewPinChanged,
            label = "Nouveau PIN",
            modifier = Modifier.fillMaxWidth(),
            type = AppTextFieldType.PIN,
        )
        AppTextField(
            value = uiState.superAdminConfirmPinInput,
            onValueChange = trackedOnSuperAdminConfirmPinChanged,
            label = "Confirmer le nouveau PIN",
            modifier = Modifier.fillMaxWidth(),
            type = AppTextFieldType.PIN,
        )
        Button(
            onClick = onChangeSuperAdminPin,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isWorking) "Traitement..." else "Modifier le PIN super administrateur")
        }
    }
}

@Composable
private fun DataExportSection(
    uiState: SettingsUiState,
    onOpenSessionsExport: () -> Unit,
    onOpenSingleBoatExport: () -> Unit,
    onOpenRemarksExport: () -> Unit,
    onExportAllBoats: () -> Unit,
    onExportRowers: () -> Unit,
    onExportCrews: () -> Unit,
) {
    SettingsSection(
        title = "Export de données",
        description = "Exporter des données ciblées sans remplacer la sauvegarde complète de l'application.",
    ) {
        Button(
            onClick = onOpenSessionsExport,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Exporter les sorties")
        }
        Button(
            onClick = onExportAllBoats,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Exporter tous les bateaux")
        }
        OutlinedButton(
            onClick = onOpenSingleBoatExport,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Exporter un bateau")
        }
        Button(
            onClick = onOpenRemarksExport,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Exporter les remarques")
        }
        Button(
            onClick = onExportRowers,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Exporter les rameurs")
        }
        if (uiState.crewsEnabled) {
            Button(
                onClick = onExportCrews,
                enabled = !uiState.isWorking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Exporter les équipages")
            }
        }
    }
}

@Composable
private fun BackupSection(
    uiState: SettingsUiState,
    onExportFullDatabase: () -> Unit,
    onRestoreBackup: () -> Unit,
) {
    SettingsSection(
        title = "Sauvegarde & restauration",
        description = "Exporter la base complète ou restaurer une sauvegarde précédente.",
    ) {
        Button(
            onClick = onExportFullDatabase,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isWorking) "Traitement..." else "Exporter la base de données complète")
        }
        Button(
            onClick = onRestoreBackup,
            enabled = !uiState.isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isWorking) "Traitement..." else "Restaurer la base depuis un ZIP")
        }
    }
}

@Composable
fun SettingsRowersRoute(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val rowerImportLauncher = rememberLauncherForActivityResult(
        contract = OpenDocument(),
    ) { uri ->
        uri?.let(viewModel::importRowers) ?: viewModel.clearMessage()
    }

    RowerManagementScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        onFirstNameChanged = viewModel::onRowerFirstNameChanged,
        onLastNameChanged = viewModel::onRowerLastNameChanged,
        onSave = viewModel::saveRower,
        onEdit = viewModel::startEditingRower,
        onCancelEditing = viewModel::cancelRowerEditing,
        onDelete = viewModel::deleteRower,
        onImportRowers = {
            viewModel.clearMessage()
            rowerImportLauncher.launch(
                arrayOf(
                    "text/csv",
                    "text/comma-separated-values",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/octet-stream",
                ),
            )
        },
    )
}

@Composable
fun SettingsBoatsRoute(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val boatImportLauncher = rememberLauncherForActivityResult(
        contract = OpenDocument(),
    ) { uri ->
        uri?.let(viewModel::importBoats) ?: viewModel.clearMessage()
    }

    BoatManagementScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        onBoatNameChanged = viewModel::onBoatNameChanged,
        onBoatSeatCountChanged = viewModel::onBoatSeatCountChanged,
        onSaveBoat = viewModel::saveBoat,
        onEditBoat = viewModel::startEditingBoat,
        onCancelBoatEditing = viewModel::cancelBoatEditing,
        onDeleteBoat = viewModel::deleteBoat,
        onImportBoats = {
            viewModel.clearMessage()
            boatImportLauncher.launch(
                arrayOf(
                    "text/csv",
                    "text/comma-separated-values",
                    "application/octet-stream",
                ),
            )
        },
    )
}

@Composable
fun SettingsDestinationsRoute(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    DestinationManagementScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        onDestinationNameChanged = viewModel::onDestinationNameChanged,
        onSaveDestination = viewModel::saveDestination,
        onEditDestination = viewModel::startEditingDestination,
        onCancelDestinationEditing = viewModel::cancelDestinationEditing,
        onDeleteDestination = viewModel::deleteDestination,
    )
}

@Composable
private fun RowerManagementScreen(
    contentPadding: PaddingValues,
    uiState: SettingsUiState,
    onFirstNameChanged: (String) -> Unit,
    onLastNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    onEdit: (RowerEntity) -> Unit,
    onCancelEditing: () -> Unit,
    onDelete: (RowerEntity) -> Unit,
    onImportRowers: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Gérer les rameurs", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        RowerManagementSection(
            uiState = uiState,
            onFirstNameChanged = onFirstNameChanged,
            onLastNameChanged = onLastNameChanged,
            onSave = onSave,
            onEdit = onEdit,
            onCancelEditing = onCancelEditing,
            onDelete = onDelete,
            onImportRowers = onImportRowers,
        )
    }
}

@Composable
private fun BoatManagementScreen(
    contentPadding: PaddingValues,
    uiState: SettingsUiState,
    onBoatNameChanged: (String) -> Unit,
    onBoatSeatCountChanged: (String) -> Unit,
    onSaveBoat: () -> Unit,
    onEditBoat: (BoatEntity) -> Unit,
    onCancelBoatEditing: () -> Unit,
    onDeleteBoat: (BoatEntity) -> Unit,
    onImportBoats: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Gérer les bateaux", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        BoatManagementSection(
            uiState = uiState,
            onBoatNameChanged = onBoatNameChanged,
            onBoatSeatCountChanged = onBoatSeatCountChanged,
            onSaveBoat = onSaveBoat,
            onEditBoat = onEditBoat,
            onCancelBoatEditing = onCancelBoatEditing,
            onDeleteBoat = onDeleteBoat,
            onImportBoats = onImportBoats,
        )
    }
}

@Composable
private fun DestinationManagementScreen(
    contentPadding: PaddingValues,
    uiState: SettingsUiState,
    onDestinationNameChanged: (String) -> Unit,
    onSaveDestination: () -> Unit,
    onEditDestination: (DestinationEntity) -> Unit,
    onCancelDestinationEditing: () -> Unit,
    onDeleteDestination: (DestinationEntity) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Gérer les destinations", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        DestinationManagementSection(uiState, onDestinationNameChanged, onSaveDestination, onEditDestination, onCancelDestinationEditing, onDeleteDestination)
    }
}

@Composable
private fun EditableListRow(
    label: String,
    secondaryLabel: String?,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                )
                secondaryLabel?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OutlinedButton(
                onClick = onEdit,
                enabled = enabled,
            ) {
                Text("Modifier")
            }
            OutlinedButton(
                onClick = onDelete,
                enabled = enabled,
            ) {
                Text("Supprimer")
            }
        }
    }
}

private fun defaultFullDatabaseExportFileName(): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return "base_complete_export_$timestamp.zip"
}

private fun defaultSessionsExportFileName(): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return "sorties_export_$timestamp.csv"
}

private fun defaultBoatSheetsExportFileName(): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return "bateaux_export_$timestamp.zip"
}

private fun defaultSingleBoatExportFileName(): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return "fiche_bateau_$timestamp.zip"
}

private fun defaultRemarksExportFileName(): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return "remarques_export_$timestamp.csv"
}

private fun defaultRowersExportFileName(): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return "rameurs_export_$timestamp.csv"
}

private fun defaultCrewsExportFileName(): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return "equipages_export_$timestamp.csv"
}

private fun defaultCleanupCutoffDate(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}

private fun ThemeMode.displayLabel(): String {
    return when (this) {
        ThemeMode.SYSTEM -> "Système"
        ThemeMode.LIGHT -> "Clair"
        ThemeMode.DARK -> "Sombre"
    }
}
