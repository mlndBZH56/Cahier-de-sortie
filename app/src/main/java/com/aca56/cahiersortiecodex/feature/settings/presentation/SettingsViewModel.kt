package com.aca56.cahiersortiecodex.feature.settings.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aca56.cahiersortiecodex.CahierSortieApplication
import com.aca56.cahiersortiecodex.data.backup.DatabaseBackupManager
import com.aca56.cahiersortiecodex.data.export.DatabaseCsvExporter
import com.aca56.cahiersortiecodex.data.export.SessionCsvExporter
import com.aca56.cahiersortiecodex.data.importing.BoatImportParser
import com.aca56.cahiersortiecodex.data.importing.RowerImportParser
import com.aca56.cahiersortiecodex.data.local.entity.BoatEntity
import com.aca56.cahiersortiecodex.data.local.entity.BoatPhotoEntity
import com.aca56.cahiersortiecodex.data.local.entity.DestinationEntity
import com.aca56.cahiersortiecodex.data.local.entity.decodeRemarkPhotoPaths
import com.aca56.cahiersortiecodex.data.local.entity.RemarkEntity
import com.aca56.cahiersortiecodex.data.local.entity.RemarkStatus
import com.aca56.cahiersortiecodex.data.local.entity.RepairUpdateEntity
import com.aca56.cahiersortiecodex.data.local.entity.RowerEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionStatus
import com.aca56.cahiersortiecodex.data.local.relation.SessionWithDetails
import com.aca56.cahiersortiecodex.data.repository.BoatRepository
import com.aca56.cahiersortiecodex.data.repository.DestinationRepository
import com.aca56.cahiersortiecodex.data.repository.RowerRepository
import com.aca56.cahiersortiecodex.data.repository.SessionRepository
import com.aca56.cahiersortiecodex.data.security.PinCodeStore
import com.aca56.cahiersortiecodex.data.settings.AppPreferencesStore
import com.aca56.cahiersortiecodex.data.settings.DefaultErrorPopupDurationMillis
import com.aca56.cahiersortiecodex.data.settings.DefaultInactivityTimeoutMillis
import com.aca56.cahiersortiecodex.data.settings.DefaultPrimaryColorHex
import com.aca56.cahiersortiecodex.data.settings.DefaultSecondaryColorHex
import com.aca56.cahiersortiecodex.data.settings.DefaultSuccessPopupDurationMillis
import com.aca56.cahiersortiecodex.data.settings.DefaultTertiaryColorHex
import com.aca56.cahiersortiecodex.data.settings.ThemeMode
import com.aca56.cahiersortiecodex.feature.history.presentation.HistoryParticipantUi
import com.aca56.cahiersortiecodex.feature.history.presentation.HistorySessionUi
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialogType
import com.aca56.cahiersortiecodex.ui.components.SearchableSelectableOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date

data class DestinationManagementUi(
    val destinations: List<DestinationEntity> = emptyList(),
    val editingDestinationId: Long? = null,
    val destinationNameInput: String = "",
) {
    val isEditing: Boolean
        get() = editingDestinationId != null
}

data class RowerManagementUi(
    val rowers: List<RowerEntity> = emptyList(),
    val editingRowerId: Long? = null,
    val firstNameInput: String = "",
    val lastNameInput: String = "",
) {
    val isEditing: Boolean
        get() = editingRowerId != null
}

data class BoatManagementUi(
    val boats: List<BoatEntity> = emptyList(),
    val editingBoatId: Long? = null,
    val boatNameInput: String = "",
    val seatCountInput: String = "",
) {
    val isEditing: Boolean
        get() = editingBoatId != null
}

enum class DataCleanupPeriod(val label: String, val months: Int?) {
    ONE_MONTH("1 mois", 1),
    THREE_MONTHS("3 mois", 3),
    SIX_MONTHS("6 mois", 6),
    TWELVE_MONTHS("12 mois", 12),
    CUSTOM("Personnalisé", null),
}

data class DataCleanupPreview(
    val sessionsCount: Int,
    val remarksCount: Int,
    val cutoffDate: String,
)

data class SettingsUiState(
    val hasPin: Boolean = false,
    val isUnlocked: Boolean = false,
    val isSuperAdmin: Boolean = false,
    val pinInput: String = "",
    val currentPinInput: String = "",
    val newPinInput: String = "",
    val confirmPinInput: String = "",
    val superAdminCurrentPinInput: String = "",
    val superAdminNewPinInput: String = "",
    val superAdminConfirmPinInput: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val primaryColorInput: String = DefaultPrimaryColorHex,
    val secondaryColorInput: String = DefaultSecondaryColorHex,
    val tertiaryColorInput: String = DefaultTertiaryColorHex,
    val inactivityTimeoutMinutesInput: String = formatMinutes(DefaultInactivityTimeoutMillis),
    val successPopupDurationSecondsInput: String = formatSeconds(DefaultSuccessPopupDurationMillis),
    val errorPopupDurationSecondsInput: String = formatSeconds(DefaultErrorPopupDurationMillis),
    val animationsEnabled: Boolean = true,
    val crewsEnabled: Boolean = false,
    val isWorking: Boolean = false,
    val message: String? = null,
    val messageType: FeedbackDialogType? = null,
    val rowerManagement: RowerManagementUi = RowerManagementUi(),
    val boatManagement: BoatManagementUi = BoatManagementUi(),
    val destinationManagement: DestinationManagementUi = DestinationManagementUi(),
    val exportableSessions: List<HistorySessionUi> = emptyList(),
    val selectedExportRowers: Set<String> = emptySet(),
    val exportRowerSearchQuery: String = "",
    val selectedExportBoatIds: Set<Long> = emptySet(),
    val exportBoatSearchQuery: String = "",
    val selectedExportDestinations: Set<String> = emptySet(),
    val exportDestinationSearchQuery: String = "",
    val exportDateFrom: String = "",
    val exportDateTo: String = "",
    val cleanupSessionsEnabled: Boolean = false,
    val cleanupRemarksEnabled: Boolean = false,
    val cleanupPeriod: DataCleanupPeriod = DataCleanupPeriod.ONE_MONTH,
    val cleanupCustomCutoffDate: String = "",
    val cleanupPreview: DataCleanupPreview? = null,
    val showRestartAfterRestore: Boolean = false,
) {
    val selectedExportBoatLabel: String?
        get() = selectedExportBoatIds.singleOrNull()?.let { selectedBoatId ->
            exportableSessions.firstOrNull { it.boatId == selectedBoatId }?.boatName
        }

    val availableExportRowerOptions: List<SearchableSelectableOption>
        get() = exportableSessions
            .flatMap { session ->
                session.rowerNames.map { rowerName ->
                    SearchableSelectableOption(
                        key = rowerName,
                        label = rowerName,
                        usageCount = 1,
                    )
                }
            }
            .groupBy { it.key }
            .values
            .map { entries ->
                SearchableSelectableOption(
                    key = entries.first().key,
                    label = entries.first().label,
                    usageCount = entries.sumOf { it.usageCount },
                )
            }
            .sortedWith(compareByDescending<SearchableSelectableOption> { it.usageCount }.thenBy { it.label })

    val filteredExportRowerOptions: List<SearchableSelectableOption>
        get() = availableExportRowerOptions.filter { option ->
            exportRowerSearchQuery.isBlank() || option.label.contains(exportRowerSearchQuery.trim(), ignoreCase = true)
        }

    val availableExportBoatOptions: List<SearchableSelectableOption>
        get() = exportableSessions
            .map { session ->
                SearchableSelectableOption(
                    key = session.boatId.toString(),
                    label = session.boatName,
                    usageCount = 1,
                )
            }
            .groupBy { it.key }
            .values
            .map { entries ->
                SearchableSelectableOption(
                    key = entries.first().key,
                    label = entries.first().label,
                    usageCount = entries.sumOf { it.usageCount },
                )
            }
            .sortedWith(compareByDescending<SearchableSelectableOption> { it.usageCount }.thenBy { it.label })

    val filteredExportBoatOptions: List<SearchableSelectableOption>
        get() = availableExportBoatOptions.filter { option ->
            exportBoatSearchQuery.isBlank() || option.label.contains(exportBoatSearchQuery.trim(), ignoreCase = true)
        }

    val availableExportDestinationOptions: List<SearchableSelectableOption>
        get() = exportableSessions
            .mapNotNull { session ->
                session.destination.takeIf { it.isNotBlank() }?.let { destination ->
                    SearchableSelectableOption(
                        key = destination,
                        label = destination,
                        usageCount = 1,
                    )
                }
            }
            .groupBy { it.key }
            .values
            .map { entries ->
                SearchableSelectableOption(
                    key = entries.first().key,
                    label = entries.first().label,
                    usageCount = entries.sumOf { it.usageCount },
                )
            }
            .sortedWith(compareByDescending<SearchableSelectableOption> { it.usageCount }.thenBy { it.label })

    val filteredExportDestinationOptions: List<SearchableSelectableOption>
        get() = availableExportDestinationOptions.filter { option ->
            exportDestinationSearchQuery.isBlank() || option.label.contains(exportDestinationSearchQuery.trim(), ignoreCase = true)
        }

    val filteredExportableSessions: List<HistorySessionUi>
        get() = exportableSessions.filter { session ->
            val rowerMatches = selectedExportRowers.isEmpty() || session.rowerNames.any { it in selectedExportRowers }
            val boatMatches = selectedExportBoatIds.isEmpty() || selectedExportBoatIds.contains(session.boatId)
            val fromDateMatches = exportDateFrom.isBlank() || session.date >= exportDateFrom.trim()
            val toDateMatches = exportDateTo.isBlank() || session.date <= exportDateTo.trim()
            val destinationMatches = selectedExportDestinations.isEmpty() || selectedExportDestinations.contains(session.destination)

            rowerMatches && boatMatches && fromDateMatches && toDateMatches && destinationMatches
        }

    val hasActiveExportFilters: Boolean
        get() = selectedExportRowers.isNotEmpty() ||
            selectedExportBoatIds.isNotEmpty() ||
            selectedExportDestinations.isNotEmpty() ||
            exportDateFrom.isNotBlank() ||
            exportDateTo.isNotBlank()

    val hasCleanupSelection: Boolean
        get() = cleanupSessionsEnabled || cleanupRemarksEnabled
}

class SettingsViewModel(
    private val application: CahierSortieApplication,
    private val backupManager: DatabaseBackupManager,
    private val pinCodeStore: PinCodeStore,
    private val appPreferencesStore: AppPreferencesStore,
    private val rowerRepository: RowerRepository,
    private val boatRepository: BoatRepository,
    private val destinationRepository: DestinationRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private var requirePinOnNextSettingsEntry = true
    private var isInsideSettingsArea = false

    private val uiStateMutable = MutableStateFlow(
        SettingsUiState(hasPin = pinCodeStore.hasPin()),
    )
    val uiState: StateFlow<SettingsUiState> = uiStateMutable.asStateFlow()

    init {
        loadAppPreferencesIntoState()
        observeRowers()
        observeBoats()
        observeDestinations()
        observeSessions()
    }

    fun onNavigationDestinationChanged(route: String?) {
        val isNowInsideSettingsArea = route?.startsWith("settings") == true

        when {
            isNowInsideSettingsArea && !isInsideSettingsArea -> {
                onSettingsAreaEntered()
            }
            !isNowInsideSettingsArea && isInsideSettingsArea -> {
                isInsideSettingsArea = false
                requirePinOnNextSettingsEntry = true
            }
        }
    }

    private fun onSettingsAreaEntered() {
        val hasPin = pinCodeStore.hasPin()
        val preferences = appPreferencesStore.currentPreferences()
        val shouldRequirePin = requirePinOnNextSettingsEntry

        uiStateMutable.update {
            it.copy(
                hasPin = hasPin,
                isUnlocked = if (shouldRequirePin) false else it.isUnlocked,
                isSuperAdmin = if (shouldRequirePin) false else it.isSuperAdmin,
                pinInput = if (shouldRequirePin) "" else it.pinInput,
                currentPinInput = if (shouldRequirePin) "" else it.currentPinInput,
                newPinInput = if (shouldRequirePin) "" else it.newPinInput,
                confirmPinInput = if (shouldRequirePin) "" else it.confirmPinInput,
                superAdminCurrentPinInput = if (shouldRequirePin) "" else it.superAdminCurrentPinInput,
                superAdminNewPinInput = if (shouldRequirePin) "" else it.superAdminNewPinInput,
                superAdminConfirmPinInput = if (shouldRequirePin) "" else it.superAdminConfirmPinInput,
                themeMode = preferences.themeMode,
                primaryColorInput = preferences.primaryColorHex,
                secondaryColorInput = preferences.secondaryColorHex,
                tertiaryColorInput = preferences.tertiaryColorHex,
                inactivityTimeoutMinutesInput = formatMinutes(preferences.inactivityTimeoutMillis),
                successPopupDurationSecondsInput = formatSeconds(preferences.successPopupDurationMillis),
                errorPopupDurationSecondsInput = formatSeconds(preferences.errorPopupDurationMillis),
                animationsEnabled = preferences.animationsEnabled,
                crewsEnabled = preferences.crewsEnabled,
                selectedExportRowers = if (shouldRequirePin) emptySet() else it.selectedExportRowers,
                exportRowerSearchQuery = if (shouldRequirePin) "" else it.exportRowerSearchQuery,
                selectedExportBoatIds = if (shouldRequirePin) emptySet() else it.selectedExportBoatIds,
                exportBoatSearchQuery = if (shouldRequirePin) "" else it.exportBoatSearchQuery,
                selectedExportDestinations = if (shouldRequirePin) emptySet() else it.selectedExportDestinations,
                exportDestinationSearchQuery = if (shouldRequirePin) "" else it.exportDestinationSearchQuery,
                exportDateFrom = if (shouldRequirePin) "" else it.exportDateFrom,
                exportDateTo = if (shouldRequirePin) "" else it.exportDateTo,
                message = if (shouldRequirePin) null else it.message,
                messageType = if (shouldRequirePin) null else it.messageType,
            )
        }

        isInsideSettingsArea = true
        requirePinOnNextSettingsEntry = false
    }

    fun onPinInputChanged(value: String) {
        uiStateMutable.update { it.copy(pinInput = value.filter(Char::isDigit), message = null, messageType = null) }
    }

    fun onCurrentPinChanged(value: String) {
        uiStateMutable.update { it.copy(currentPinInput = value.filter(Char::isDigit), message = null, messageType = null) }
    }

    fun onNewPinChanged(value: String) {
        uiStateMutable.update { it.copy(newPinInput = value.filter(Char::isDigit), message = null, messageType = null) }
    }

    fun onConfirmPinChanged(value: String) {
        uiStateMutable.update { it.copy(confirmPinInput = value.filter(Char::isDigit), message = null, messageType = null) }
    }

    fun onSuperAdminCurrentPinChanged(value: String) {
        uiStateMutable.update { it.copy(superAdminCurrentPinInput = value.filter(Char::isDigit), message = null, messageType = null) }
    }

    fun onSuperAdminNewPinChanged(value: String) {
        uiStateMutable.update { it.copy(superAdminNewPinInput = value.filter(Char::isDigit), message = null, messageType = null) }
    }

    fun onSuperAdminConfirmPinChanged(value: String) {
        uiStateMutable.update { it.copy(superAdminConfirmPinInput = value.filter(Char::isDigit), message = null, messageType = null) }
    }

    fun onThemeModeChanged(value: ThemeMode) {
        uiStateMutable.update { it.copy(themeMode = value, message = null, messageType = null) }
    }

    fun onInactivityTimeoutMinutesChanged(value: String) {
        uiStateMutable.update {
            it.copy(
                inactivityTimeoutMinutesInput = sanitizeDecimalInput(value),
                message = null,
                messageType = null,
            )
        }
    }

    fun onSuccessPopupDurationSecondsChanged(value: String) {
        uiStateMutable.update {
            it.copy(
                successPopupDurationSecondsInput = sanitizeDecimalInput(value),
                message = null,
                messageType = null,
            )
        }
    }

    fun onErrorPopupDurationSecondsChanged(value: String) {
        uiStateMutable.update {
            it.copy(
                errorPopupDurationSecondsInput = sanitizeDecimalInput(value),
                message = null,
                messageType = null,
            )
        }
    }

    fun onAnimationsEnabledChanged(value: Boolean) {
        uiStateMutable.update { it.copy(animationsEnabled = value, message = null, messageType = null) }
    }

    fun onCrewsEnabledChanged(value: Boolean) {
        uiStateMutable.update { it.copy(crewsEnabled = value, message = null, messageType = null) }
    }

    fun onPrimaryColorChanged(value: String) {
        uiStateMutable.update { it.copy(primaryColorInput = value.uppercase(), message = null, messageType = null) }
    }

    fun onSecondaryColorChanged(value: String) {
        uiStateMutable.update { it.copy(secondaryColorInput = value.uppercase(), message = null, messageType = null) }
    }

    fun onTertiaryColorChanged(value: String) {
        uiStateMutable.update { it.copy(tertiaryColorInput = value.uppercase(), message = null, messageType = null) }
    }

    fun saveFirstPin() {
        val pin = uiState.value.newPinInput.trim()
        val confirmPin = uiState.value.confirmPinInput.trim()

        when {
            pin.length < 4 -> {
                uiStateMutable.update { it.copy(message = "Veuillez saisir un code PIN d'au moins 4 chiffres.", messageType = FeedbackDialogType.ERROR) }
                return
            }
            pin != confirmPin -> {
                uiStateMutable.update { it.copy(message = "Les deux codes PIN ne correspondent pas. Veuillez saisir le même code deux fois.", messageType = FeedbackDialogType.ERROR) }
                return
            }
        }

        pinCodeStore.saveNormalPin(pin)
        uiStateMutable.update {
            it.copy(
                hasPin = true,
                isUnlocked = true,
                isSuperAdmin = false,
                pinInput = "",
                currentPinInput = "",
                newPinInput = "",
                confirmPinInput = "",
                message = "Code PIN enregistré. Les paramètres sont désormais protégés.",
                messageType = FeedbackDialogType.SUCCESS,
            )
        }
    }

    fun unlockSettings() {
        val enteredPin = uiState.value.pinInput.trim()
        val isSuperAdmin = pinCodeStore.verifySuperAdminPin(enteredPin)
        val isNormalUser = pinCodeStore.verifyNormalPin(enteredPin)

        if (!isSuperAdmin && !isNormalUser) {
            uiStateMutable.update { it.copy(message = "Le code PIN est incorrect. Veuillez réessayer.", messageType = FeedbackDialogType.ERROR) }
            return
        }

        uiStateMutable.update {
            it.copy(
                isUnlocked = true,
                isSuperAdmin = isSuperAdmin,
                pinInput = "",
                message = null,
                messageType = null,
            )
        }
    }

    fun changeNormalPin() {
        val state = uiState.value
        val currentPin = state.currentPinInput.trim()
        val newPin = state.newPinInput.trim()
        val confirmPin = state.confirmPinInput.trim()

        when {
            !state.isUnlocked -> {
                uiStateMutable.update { it.copy(message = "Veuillez déverrouiller les paramètres avant de modifier le PIN.", messageType = FeedbackDialogType.ERROR) }
                return
            }
            !state.isSuperAdmin && !pinCodeStore.verifyNormalPin(currentPin) -> {
                uiStateMutable.update { it.copy(message = "Le code PIN actuel est incorrect.", messageType = FeedbackDialogType.ERROR) }
                return
            }
            newPin.length < 4 -> {
                uiStateMutable.update { it.copy(message = "Veuillez saisir un nouveau code PIN d'au moins 4 chiffres.", messageType = FeedbackDialogType.ERROR) }
                return
            }
            newPin != confirmPin -> {
                uiStateMutable.update { it.copy(message = "Le nouveau code PIN et sa confirmation ne correspondent pas.", messageType = FeedbackDialogType.ERROR) }
                return
            }
        }

        pinCodeStore.saveNormalPin(newPin)
        uiStateMutable.update {
            it.copy(
                currentPinInput = "",
                newPinInput = "",
                confirmPinInput = "",
                hasPin = true,
                message = if (state.isSuperAdmin) {
                    "Le code PIN des paramètres a été mis à jour avec succès."
                } else {
                    "Le code PIN a été modifié avec succès."
                },
                messageType = FeedbackDialogType.SUCCESS,
            )
        }
    }

    fun changeSuperAdminPin() {
        val currentPin = uiState.value.superAdminCurrentPinInput.trim()
        val newPin = uiState.value.superAdminNewPinInput.trim()
        val confirmPin = uiState.value.superAdminConfirmPinInput.trim()

        when {
            !uiState.value.isSuperAdmin -> {
                uiStateMutable.update { it.copy(message = "Cette option n'est pas disponible.", messageType = FeedbackDialogType.ERROR) }
                return
            }
            !pinCodeStore.verifySuperAdminPin(currentPin) -> {
                uiStateMutable.update { it.copy(message = "Le code PIN actuel est incorrect.", messageType = FeedbackDialogType.ERROR) }
                return
            }
            newPin.length < 6 -> {
                uiStateMutable.update { it.copy(message = "Veuillez saisir un nouveau code PIN d'au moins 6 chiffres.", messageType = FeedbackDialogType.ERROR) }
                return
            }
            newPin != confirmPin -> {
                uiStateMutable.update { it.copy(message = "Le nouveau code PIN et sa confirmation ne correspondent pas.", messageType = FeedbackDialogType.ERROR) }
                return
            }
        }

        pinCodeStore.saveSuperAdminPin(newPin)
        uiStateMutable.update {
            it.copy(
                superAdminCurrentPinInput = "",
                superAdminNewPinInput = "",
                superAdminConfirmPinInput = "",
                message = "Le code PIN super administrateur a été modifié avec succès.",
                messageType = FeedbackDialogType.SUCCESS,
            )
        }
    }

    fun restoreBackup(uri: Uri) {
        launchWork(
            onErrorMessage = "Impossible de restaurer la base de données depuis le fichier ZIP.",
            block = {
                withContext(Dispatchers.IO) {
                    backupManager.restoreFromZip(
                        contentResolver = application.contentResolver,
                        uri = uri,
                    )
                }
                application.reloadAppContainer()
            },
            onSuccess = {
                it.copy(
                    showRestartAfterRestore = true,
                    message = null,
                    messageType = null,
                )
            },
        )
    }

    fun onExportRowerSearchQueryChanged(value: String) {
        uiStateMutable.update {
            it.copy(exportRowerSearchQuery = value, message = null, messageType = null)
        }
    }

    fun onExportRowerSelected(value: String, selected: Boolean) {
        uiStateMutable.update {
            it.copy(
                selectedExportRowers = if (selected) {
                    it.selectedExportRowers + value
                } else {
                    it.selectedExportRowers - value
                },
                message = null,
                messageType = null,
            )
        }
    }

    fun onExportBoatSearchQueryChanged(value: String) {
        uiStateMutable.update {
            it.copy(exportBoatSearchQuery = value, message = null, messageType = null)
        }
    }

    fun onExportBoatSelected(value: Long, selected: Boolean) {
        uiStateMutable.update {
            it.copy(
                selectedExportBoatIds = if (selected) {
                    it.selectedExportBoatIds + value
                } else {
                    it.selectedExportBoatIds - value
                },
                message = null,
                messageType = null,
            )
        }
    }

    fun onExportDestinationSearchQueryChanged(value: String) {
        uiStateMutable.update {
            it.copy(exportDestinationSearchQuery = value, message = null, messageType = null)
        }
    }

    fun onExportDestinationSelected(value: String, selected: Boolean) {
        uiStateMutable.update {
            it.copy(
                selectedExportDestinations = if (selected) {
                    it.selectedExportDestinations + value
                } else {
                    it.selectedExportDestinations - value
                },
                message = null,
                messageType = null,
            )
        }
    }

    fun onExportDateFromChanged(value: String) {
        uiStateMutable.update {
            it.copy(exportDateFrom = value, message = null, messageType = null)
        }
    }

    fun onExportDateToChanged(value: String) {
        uiStateMutable.update {
            it.copy(exportDateTo = value, message = null, messageType = null)
        }
    }

    fun onCleanupSessionsEnabledChanged(value: Boolean) {
        uiStateMutable.update {
            it.copy(
                cleanupSessionsEnabled = value,
                cleanupPreview = null,
                message = null,
                messageType = null,
            )
        }
    }

    fun onCleanupRemarksEnabledChanged(value: Boolean) {
        uiStateMutable.update {
            it.copy(
                cleanupRemarksEnabled = value,
                cleanupPreview = null,
                message = null,
                messageType = null,
            )
        }
    }

    fun onCleanupPeriodChanged(value: DataCleanupPeriod) {
        uiStateMutable.update {
            it.copy(
                cleanupPeriod = value,
                cleanupPreview = null,
                message = null,
                messageType = null,
            )
        }
    }

    fun onCleanupCustomCutoffDateChanged(value: String) {
        uiStateMutable.update {
            it.copy(
                cleanupCustomCutoffDate = value,
                cleanupPreview = null,
                message = null,
                messageType = null,
            )
        }
    }

    fun dismissCleanupPreview() {
        uiStateMutable.update {
            it.copy(
                cleanupPreview = null,
                message = null,
                messageType = null,
            )
        }
    }

    fun clearExportFilters() {
        uiStateMutable.update {
            it.copy(
                selectedExportRowers = emptySet(),
                exportRowerSearchQuery = "",
                selectedExportBoatIds = emptySet(),
                exportBoatSearchQuery = "",
                selectedExportDestinations = emptySet(),
                exportDestinationSearchQuery = "",
                exportDateFrom = "",
                exportDateTo = "",
                message = null,
                messageType = null,
            )
        }
    }

    fun exportFullDatabase(uri: Uri) {
        launchWork(
            onErrorMessage = "Impossible d'exporter toute la base de données.",
            block = {
                withContext(Dispatchers.IO) {
                    backupManager.exportToZip(
                        contentResolver = application.contentResolver,
                        uri = uri,
                        remarks = application.appContainer.remarkRepository.observeRemarks().first(),
                        repairUpdates = application.appContainer.repairUpdateRepository.observeUpdates().first(),
                        boatPhotos = application.appContainer.boatPhotoRepository.observePhotos().first(),
                    )
                }
            },
            onSuccessMessage = "La sauvegarde complète de l'application a été exportée avec succès.",
        )
    }

    fun exportSessions(uri: Uri) {
        launchWork(
            onErrorMessage = "Impossible d'exporter les sorties.",
            block = {
                val sessionsToExport = uiState.value.filteredExportableSessions
                withContext(Dispatchers.IO) {
                    SessionCsvExporter.exportHistorySessions(
                        contentResolver = application.contentResolver,
                        uri = uri,
                        sessions = sessionsToExport,
                    )
                }
            },
            onSuccessMessage = "Les sorties ont été exportées avec succès.",
        )
    }

    fun exportAllBoats(uri: Uri) {
        launchWork(
            onErrorMessage = "Impossible d'exporter les bateaux.",
            block = {
                withContext(Dispatchers.IO) {
                    val remarks = application.appContainer.remarkRepository.observeRemarks().first()
                    val repairUpdates = application.appContainer.repairUpdateRepository.observeUpdates().first()
                    val sessions = sessionRepository.observeSessionsWithDetails().first()
                    val boatPhotos = application.appContainer.boatPhotoRepository.observePhotos().first()

                    DatabaseCsvExporter.exportBoatSheets(
                        contentResolver = application.contentResolver,
                        uri = uri,
                        boats = uiState.value.boatManagement.boats,
                        remarks = remarks,
                        repairUpdates = repairUpdates,
                        sessions = sessions,
                        boatPhotos = boatPhotos,
                    )
                }
            },
            onSuccessMessage = "Les fiches bateaux ont été exportées avec succès.",
        )
    }

    fun exportSingleBoat(uri: Uri, boatId: Long) {
        val boat = uiState.value.boatManagement.boats.firstOrNull { it.id == boatId }
        if (boat == null) {
            uiStateMutable.update {
                it.copy(
                    message = "Le bateau sélectionné est introuvable.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return
        }

        launchWork(
            onErrorMessage = "Impossible d'exporter cette fiche bateau.",
            block = {
                withContext(Dispatchers.IO) {
                    val remarks = application.appContainer.remarkRepository.observeRemarks().first()
                        .filter { it.boatId == boatId }
                    val repairUpdates = application.appContainer.repairUpdateRepository.observeUpdates().first()
                    val sessions = sessionRepository.observeSessionsWithDetails().first()
                    val boatPhotoPaths = application.appContainer.boatPhotoRepository.observePhotos().first()
                        .filter { it.boatId == boatId }
                        .map(BoatPhotoEntity::filePath)

                    DatabaseCsvExporter.exportSingleBoatSheet(
                        contentResolver = application.contentResolver,
                        uri = uri,
                        boat = boat,
                        remarks = remarks,
                        repairUpdates = repairUpdates,
                        boatPhotos = boatPhotoPaths,
                        sessions = sessions,
                    )
                }
            },
            onSuccessMessage = "La fiche bateau a été exportée avec succès.",
        )
    }

    fun exportRemarks(uri: Uri, repairsOnly: Boolean, boatId: Long?) {
        launchWork(
            onErrorMessage = "Impossible d'exporter les remarques.",
            block = {
                withContext(Dispatchers.IO) {
                    DatabaseCsvExporter.exportRemarks(
                        contentResolver = application.contentResolver,
                        uri = uri,
                        remarks = application.appContainer.remarkRepository.observeRemarks().first(),
                        boats = uiState.value.boatManagement.boats,
                        repairUpdates = application.appContainer.repairUpdateRepository.observeUpdates().first(),
                        repairsOnly = repairsOnly,
                        boatId = boatId,
                    )
                }
            },
            onSuccessMessage = "Les remarques ont été exportées avec succès.",
        )
    }

    fun exportRowers(uri: Uri) {
        launchWork(
            onErrorMessage = "Impossible d'exporter les rameurs.",
            block = {
                withContext(Dispatchers.IO) {
                    DatabaseCsvExporter.exportRowers(
                        contentResolver = application.contentResolver,
                        uri = uri,
                        rowers = uiState.value.rowerManagement.rowers,
                    )
                }
            },
            onSuccessMessage = "Les rameurs ont été exportés avec succès.",
        )
    }

    fun exportCrews(uri: Uri) {
        if (!uiState.value.crewsEnabled) {
            uiStateMutable.update {
                it.copy(
                    message = "Les équipages sont désactivés.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return
        }

        launchWork(
            onErrorMessage = "Impossible d'exporter les équipages.",
            block = {
                withContext(Dispatchers.IO) {
                    DatabaseCsvExporter.exportCrews(
                        contentResolver = application.contentResolver,
                        uri = uri,
                        crews = application.appContainer.crewStore.currentCrews(),
                        rowers = uiState.value.rowerManagement.rowers,
                    )
                }
            },
            onSuccessMessage = "Les équipages ont été exportés avec succès.",
        )
    }

    fun previewDataCleanup() {
        if (!uiState.value.isSuperAdmin) {
            uiStateMutable.update {
                it.copy(
                    message = "Cette option n'est pas disponible.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return
        }

        val state = uiState.value
        if (!state.hasCleanupSelection) {
            uiStateMutable.update {
                it.copy(
                    message = "Sélectionnez au moins un type de données à nettoyer.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return
        }

        val cutoffDate = resolveCleanupCutoffDate(state) ?: run {
            uiStateMutable.update {
                it.copy(
                    message = "Veuillez choisir une période ou une date personnalisée valide pour le nettoyage.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return
        }

        launchWork(
            onErrorMessage = "Impossible de préparer l'aperçu du nettoyage.",
            block = {
                val sessions = sessionRepository.observeSessionsWithDetails().first()
                val remarks = application.appContainer.remarkRepository.observeRemarks().first()
                val preview = buildCleanupPreview(
                    state = state,
                    cutoffDate = cutoffDate,
                    sessions = sessions,
                    remarks = remarks,
                )
                uiStateMutable.update {
                    it.copy(
                        cleanupPreview = preview,
                        message = null,
                        messageType = null,
                    )
                }
            },
            onSuccess = { it.copy(message = null, messageType = null) },
        )
    }

    fun performDataCleanup(superAdminPin: String): Boolean {
        val state = uiState.value
        if (!state.isSuperAdmin) {
            uiStateMutable.update {
                it.copy(
                    message = "Cette option n'est pas disponible.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return false
        }

        val preview = state.cleanupPreview
        if (preview == null) {
            uiStateMutable.update {
                it.copy(
                    message = "Préparez d'abord un aperçu du nettoyage.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return false
        }

        val normalizedPin = superAdminPin.trim()
        if (normalizedPin.isBlank()) {
            uiStateMutable.update {
                it.copy(
                    message = "Veuillez saisir le code PIN super administrateur pour confirmer.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return false
        }

        if (!pinCodeStore.verifySuperAdminPin(normalizedPin)) {
            uiStateMutable.update {
                it.copy(
                    message = "Le code PIN super administrateur est incorrect.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return false
        }

        launchWork(
            onErrorMessage = "Le nettoyage des données a échoué.",
            block = {
                withContext(Dispatchers.IO) {
                    executeDataCleanup(preview.cutoffDate)
                }
            },
            onSuccess = {
                it.copy(
                    cleanupPreview = null,
                    message = buildString {
                        append("Nettoyage terminé : ")
                        append("${preview.sessionsCount} sortie(s) et ${preview.remarksCount} remarque(s) supprimées.")
                    },
                    messageType = FeedbackDialogType.SUCCESS,
                )
            },
        )
        return true
    }

    fun importRowers(uri: Uri) {
        launchWorkWithResult(
            onErrorMessage = "Impossible d'importer les rameurs.",
            block = {
                withContext(Dispatchers.IO) {
                    val existingKeys = uiState.value.rowerManagement.rowers.map {
                        normalizePersonKey(it.firstName, it.lastName)
                    }.toMutableSet()
                    var importedCount = 0

                    RowerImportParser.parse(
                        contentResolver = application.contentResolver,
                        uri = uri,
                    ).forEach { rower ->
                        val key = normalizePersonKey(rower.firstName, rower.lastName)
                        if (key.isBlank() || existingKeys.contains(key)) return@forEach
                        rowerRepository.saveRower(rower)
                        existingKeys.add(key)
                        importedCount += 1
                    }

                    importedCount
                }
            },
        ) { importedCount ->
            if (importedCount == 0) {
                "Aucun nouveau rameur n'a été importé."
            } else {
                "$importedCount rameur(s) importé(s)."
            }
        }
    }

    fun importBoats(uri: Uri) {
        launchWorkWithResult(
            onErrorMessage = "Impossible d'importer les bateaux.",
            block = {
                withContext(Dispatchers.IO) {
                    val existingNames = uiState.value.boatManagement.boats.map {
                        it.name.trim().lowercase()
                    }.toMutableSet()
                    var importedCount = 0

                    BoatImportParser.parse(
                        contentResolver = application.contentResolver,
                        uri = uri,
                    ).forEach { boat ->
                        val normalizedName = boat.name.trim().lowercase()
                        if (normalizedName.isBlank() || existingNames.contains(normalizedName)) return@forEach
                        if (boat.seatCount <= 0) return@forEach

                        boatRepository.saveBoat(
                            boat.copy(
                                name = boat.name.trim(),
                            ),
                        )
                        existingNames.add(normalizedName)
                        importedCount += 1
                    }

                    importedCount
                }
            },
        ) { importedCount ->
            if (importedCount == 0) {
                "Aucun nouveau bateau n'a été importé."
            } else {
                "$importedCount bateau(x) importé(s)."
            }
        }
    }

    fun saveAppSettings() {
        if (!uiState.value.isSuperAdmin) {
            uiStateMutable.update { it.copy(message = "Cette option n'est pas disponible.", messageType = FeedbackDialogType.ERROR) }
            return
        }

        val inactivityTimeoutMillis = uiState.value.inactivityTimeoutMinutesInput.toPositiveMillis(
            multiplier = 60_000.0,
        )
        when {
            inactivityTimeoutMillis == null -> {
                uiStateMutable.update {
                    it.copy(
                        message = "Veuillez saisir un délai d'inactivité valide en minutes.",
                        messageType = FeedbackDialogType.ERROR,
                    )
                }
                return
            }
            inactivityTimeoutMillis < 30_000L -> {
                uiStateMutable.update {
                    it.copy(
                        message = "Le délai d'inactivité doit être d'au moins 0,5 minute.",
                        messageType = FeedbackDialogType.ERROR,
                    )
                }
                return
            }
        }

        appPreferencesStore.saveThemeMode(uiState.value.themeMode)
        appPreferencesStore.saveAdvancedBehavior(
            inactivityTimeoutMillis = inactivityTimeoutMillis,
            successPopupDurationMillis = appPreferencesStore.currentPreferences().successPopupDurationMillis,
            errorPopupDurationMillis = appPreferencesStore.currentPreferences().errorPopupDurationMillis,
            animationsEnabled = uiState.value.animationsEnabled,
            crewsEnabled = uiState.value.crewsEnabled,
        )
        loadAppPreferencesIntoState(message = "Le comportement de l'application a été enregistré.")
    }

    fun saveThemeColors() {
        if (!uiState.value.isSuperAdmin) {
            uiStateMutable.update { it.copy(message = "Cette option n'est pas disponible.", messageType = FeedbackDialogType.ERROR) }
            return
        }
        
        val primary = normalizeHex(uiState.value.primaryColorInput)
        val secondary = normalizeHex(uiState.value.secondaryColorInput)
        val tertiary = normalizeHex(uiState.value.tertiaryColorInput)

        if (!isValidColorHex(primary) || !isValidColorHex(secondary) || !isValidColorHex(tertiary)) {
            uiStateMutable.update {
                it.copy(message = "Veuillez saisir des couleurs valides au format #RRGGBB.", messageType = FeedbackDialogType.ERROR)
            }
            return
        }

        appPreferencesStore.saveThemeColors(
            primaryColorHex = primary,
            secondaryColorHex = secondary,
            tertiaryColorHex = tertiary,
        )
        loadAppPreferencesIntoState(message = "Les couleurs du thème ont été enregistrées.")
    }

    fun saveNotificationSettings() {
        if (!uiState.value.isSuperAdmin) {
            uiStateMutable.update { it.copy(message = "Cette option n'est pas disponible.", messageType = FeedbackDialogType.ERROR) }
            return
        }

        val successDurationMillis = uiState.value.successPopupDurationSecondsInput.toPositiveMillis(
            multiplier = 1_000.0,
        )
        val errorDurationMillis = uiState.value.errorPopupDurationSecondsInput.toPositiveMillis(
            multiplier = 1_000.0,
        )

        when {
            successDurationMillis == null -> {
                uiStateMutable.update {
                    it.copy(
                        message = "Veuillez saisir une durée valide pour les confirmations en secondes.",
                        messageType = FeedbackDialogType.ERROR,
                    )
                }
                return
            }
            errorDurationMillis == null -> {
                uiStateMutable.update {
                    it.copy(
                        message = "Veuillez saisir une durée valide pour les erreurs en secondes.",
                        messageType = FeedbackDialogType.ERROR,
                    )
                }
                return
            }
            successDurationMillis < 1_000L -> {
                uiStateMutable.update {
                    it.copy(
                        message = "Les confirmations doivent rester visibles au moins 1 seconde.",
                        messageType = FeedbackDialogType.ERROR,
                    )
                }
                return
            }
            errorDurationMillis < 1_000L -> {
                uiStateMutable.update {
                    it.copy(
                        message = "Les messages d'erreur doivent rester visibles au moins 1 seconde.",
                        messageType = FeedbackDialogType.ERROR,
                    )
                }
                return
            }
        }

        val preferences = appPreferencesStore.currentPreferences()
        appPreferencesStore.saveAdvancedBehavior(
            inactivityTimeoutMillis = preferences.inactivityTimeoutMillis,
            successPopupDurationMillis = successDurationMillis,
            errorPopupDurationMillis = errorDurationMillis,
            animationsEnabled = preferences.animationsEnabled,
            crewsEnabled = preferences.crewsEnabled,
        )
        loadAppPreferencesIntoState(message = "Les paramètres de notification ont été enregistrés.")
    }

    fun resetAllAppData(superAdminPin: String): Boolean {
        if (!uiState.value.isSuperAdmin) {
            uiStateMutable.update {
                it.copy(
                    message = "Cette option n'est pas disponible.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return false
        }

        val normalizedPin = superAdminPin.trim()
        if (normalizedPin.isBlank()) {
            uiStateMutable.update {
                it.copy(
                    message = "Veuillez saisir le code PIN super administrateur pour confirmer.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return false
        }

        if (!pinCodeStore.verifySuperAdminPin(normalizedPin)) {
            uiStateMutable.update {
                it.copy(
                    message = "Le code PIN super administrateur est incorrect.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return false
        }

        launchWork(
            onErrorMessage = "La réinitialisation a échoué. Les données de l'application n'ont pas pu être supprimées.",
            block = {
                withContext(Dispatchers.IO) {
                    application.appContainer.resetAllAppData()
                }
                pinCodeStore.resetAllPins()
                appPreferencesStore.resetToDefaults()
                application.reloadAppContainer()
            },
            onSuccess = {
                isInsideSettingsArea = false
                requirePinOnNextSettingsEntry = true
                SettingsUiState(
                    hasPin = false,
                    isUnlocked = false,
                    isSuperAdmin = false,
                    message = "Réinitialisation terminée. L'application est revenue à son état initial.",
                    messageType = FeedbackDialogType.SUCCESS,
                )
            },
        )
        return true
    }

    fun clearMessage() {
        uiStateMutable.update { it.copy(message = null, messageType = null) }
    }

    fun dismissRestartAfterRestore() {
        uiStateMutable.update { it.copy(showRestartAfterRestore = false) }
    }

    fun onRowerFirstNameChanged(value: String) {
        uiStateMutable.update {
            it.copy(
                rowerManagement = it.rowerManagement.copy(firstNameInput = value),
                message = null,
                messageType = null,
            )
        }
    }

    fun onRowerLastNameChanged(value: String) {
        uiStateMutable.update {
            it.copy(
                rowerManagement = it.rowerManagement.copy(lastNameInput = value),
                message = null,
                messageType = null,
            )
        }
    }

    fun startEditingRower(rower: RowerEntity) {
        uiStateMutable.update {
            it.copy(
                rowerManagement = it.rowerManagement.copy(
                    editingRowerId = rower.id,
                    firstNameInput = rower.firstName,
                    lastNameInput = rower.lastName,
                ),
                message = null,
                messageType = null,
            )
        }
    }

    fun cancelRowerEditing() {
        uiStateMutable.update {
            it.copy(
                rowerManagement = it.rowerManagement.copy(
                    editingRowerId = null,
                    firstNameInput = "",
                    lastNameInput = "",
                ),
                message = null,
                messageType = null,
            )
        }
    }

    fun saveRower() {
        val state = uiState.value
        val firstName = state.rowerManagement.firstNameInput.trim()
        val lastName = state.rowerManagement.lastNameInput.trim()

        if (firstName.isBlank() && lastName.isBlank()) {
            uiStateMutable.update {
                it.copy(
                    message = "Veuillez saisir un prénom, un nom, ou les deux avant d'enregistrer le rameur.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return
        }

        val editingId = state.rowerManagement.editingRowerId
        val duplicate = state.rowerManagement.rowers.any { rower ->
            rower.id != editingId &&
                normalizePersonKey(rower.firstName, rower.lastName) == normalizePersonKey(firstName, lastName)
        }
        if (duplicate) {
            uiStateMutable.update {
                it.copy(
                    message = "Ce rameur existe déjà dans la liste ; il n'a pas été enregistré une seconde fois.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return
        }

        val entity = RowerEntity(
            id = editingId ?: 0,
            firstName = firstName,
            lastName = lastName,
        )

        launchWork(
            onErrorMessage = "Impossible d'enregistrer le rameur.",
            block = {
                withContext(Dispatchers.IO) {
                    if (editingId == null) {
                        rowerRepository.saveRower(entity)
                    } else {
                        rowerRepository.updateRower(entity)
                    }
                }
            },
            onSuccess = {
                it.copy(
                    message = "Rameur enregistré.",
                    rowerManagement = it.rowerManagement.copy(
                        editingRowerId = null,
                        firstNameInput = "",
                        lastNameInput = "",
                    ),
                )
            },
        )
    }

    fun deleteRower(rower: RowerEntity) {
        launchWork(
            onErrorMessage = "Impossible de supprimer le rameur.",
            block = {
                withContext(Dispatchers.IO) {
                    rowerRepository.deleteRower(rower)
                }
            },
            onSuccess = { state ->
                state.copy(
                    message = "Rameur supprimé.",
                    rowerManagement = if (state.rowerManagement.editingRowerId == rower.id) {
                        state.rowerManagement.copy(
                            editingRowerId = null,
                            firstNameInput = "",
                            lastNameInput = "",
                        )
                    } else {
                        state.rowerManagement
                    },
                )
            },
        )
    }

    fun onBoatNameChanged(value: String) {
        uiStateMutable.update {
            it.copy(
                boatManagement = it.boatManagement.copy(boatNameInput = value),
                message = null,
                messageType = null,
            )
        }
    }

    fun onBoatSeatCountChanged(value: String) {
        uiStateMutable.update {
            it.copy(
                boatManagement = it.boatManagement.copy(
                    seatCountInput = value.filter { char -> char.isDigit() },
                ),
                message = null,
                messageType = null,
            )
        }
    }

    fun startEditingBoat(boat: BoatEntity) {
        uiStateMutable.update {
            it.copy(
                boatManagement = it.boatManagement.copy(
                    editingBoatId = boat.id,
                    boatNameInput = boat.name,
                    seatCountInput = boat.seatCount.toString(),
                ),
                message = null,
                messageType = null,
            )
        }
    }

    fun cancelBoatEditing() {
        uiStateMutable.update {
            it.copy(
                boatManagement = it.boatManagement.copy(
                    editingBoatId = null,
                    boatNameInput = "",
                    seatCountInput = "",
                ),
                message = null,
                messageType = null,
            )
        }
    }

    fun saveBoat() {
        val state = uiState.value
        val boatName = state.boatManagement.boatNameInput.trim()
        val seatCount = state.boatManagement.seatCountInput.toIntOrNull()

        when {
            boatName.isBlank() -> {
                uiStateMutable.update {
                    it.copy(
                        message = "Veuillez saisir un nom de bateau avant d'enregistrer.",
                        messageType = FeedbackDialogType.ERROR,
                    )
                }
                return
            }
            seatCount == null || seatCount <= 0 -> {
                uiStateMutable.update {
                    it.copy(
                        message = if (state.boatManagement.seatCountInput.isBlank()) {
                            "Veuillez saisir le nombre de places."
                        } else {
                            "Veuillez saisir un nombre de places valide supérieur à 0."
                        },
                        messageType = FeedbackDialogType.ERROR,
                    )
                }
                return
            }
        }

        val editingId = state.boatManagement.editingBoatId
        val duplicate = state.boatManagement.boats.any { boat ->
            boat.id != editingId && boat.name.equals(boatName, ignoreCase = true)
        }
        if (duplicate) {
            uiStateMutable.update {
                it.copy(
                    message = "Un bateau avec ce nom existe déjà ; il n'a pas été enregistré une seconde fois.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return
        }

        val entity = BoatEntity(
            id = editingId ?: 0,
            name = boatName,
            seatCount = seatCount,
        )

        launchWork(
            onErrorMessage = "Impossible d'enregistrer le bateau.",
            block = {
                withContext(Dispatchers.IO) {
                    if (editingId == null) {
                        boatRepository.saveBoat(entity)
                    } else {
                        boatRepository.updateBoat(entity)
                    }
                }
            },
            onSuccess = {
                it.copy(
                    message = "Bateau enregistré.",
                    boatManagement = it.boatManagement.copy(
                        editingBoatId = null,
                        boatNameInput = "",
                        seatCountInput = "",
                    ),
                )
            },
        )
    }

    fun deleteBoat(boat: BoatEntity) {
        launchWork(
            onErrorMessage = "Impossible de supprimer le bateau.",
            block = {
                withContext(Dispatchers.IO) {
                    boatRepository.deleteBoat(boat)
                }
            },
            onSuccess = { state ->
                state.copy(
                    message = "Bateau supprimé.",
                    boatManagement = if (state.boatManagement.editingBoatId == boat.id) {
                        state.boatManagement.copy(
                            editingBoatId = null,
                            boatNameInput = "",
                            seatCountInput = "",
                        )
                    } else {
                        state.boatManagement
                    },
                )
            },
        )
    }

    fun onDestinationNameChanged(value: String) {
        uiStateMutable.update {
            it.copy(
                destinationManagement = it.destinationManagement.copy(destinationNameInput = value),
                message = null,
                messageType = null,
            )
        }
    }

    fun startEditingDestination(destination: DestinationEntity) {
        uiStateMutable.update {
            it.copy(
                destinationManagement = it.destinationManagement.copy(
                    editingDestinationId = destination.id,
                    destinationNameInput = destination.name,
                ),
                message = null,
                messageType = null,
            )
        }
    }

    fun cancelDestinationEditing() {
        uiStateMutable.update {
            it.copy(
                destinationManagement = it.destinationManagement.copy(
                    editingDestinationId = null,
                    destinationNameInput = "",
                ),
                message = null,
                messageType = null,
            )
        }
    }

    fun saveDestination() {
        val state = uiState.value
        val destinationName = state.destinationManagement.destinationNameInput.trim()
        val editingId = state.destinationManagement.editingDestinationId

        if (destinationName.isBlank()) {
            uiStateMutable.update {
                it.copy(
                    message = "Veuillez saisir un nom de destination avant d'enregistrer.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return
        }

        val duplicate = state.destinationManagement.destinations.any { destination ->
            destination.id != editingId && destination.name.equals(destinationName, ignoreCase = true)
        }
        if (duplicate) {
            uiStateMutable.update {
                it.copy(
                    message = "Cette destination existe déjà dans la liste ; elle n'a pas été enregistrée une seconde fois.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return
        }

        val entity = DestinationEntity(
            id = editingId ?: 0,
            name = destinationName,
        )

        launchWork(
            onErrorMessage = "Impossible d'enregistrer la destination.",
            block = {
                withContext(Dispatchers.IO) {
                    if (editingId == null) {
                        destinationRepository.saveDestination(entity)
                    } else {
                        destinationRepository.updateDestination(entity)
                    }
                }
            },
            onSuccess = {
                it.copy(
                    message = "Destination enregistrée.",
                    destinationManagement = it.destinationManagement.copy(
                        editingDestinationId = null,
                        destinationNameInput = "",
                    ),
                )
            },
        )
    }

    fun deleteDestination(destination: DestinationEntity) {
        launchWork(
            onErrorMessage = "Impossible de supprimer la destination.",
            block = {
                withContext(Dispatchers.IO) {
                    destinationRepository.deleteDestination(destination)
                }
            },
            onSuccess = { state ->
                state.copy(
                    message = "Destination supprimée.",
                    destinationManagement = if (state.destinationManagement.editingDestinationId == destination.id) {
                        state.destinationManagement.copy(
                            editingDestinationId = null,
                            destinationNameInput = "",
                        )
                    } else {
                        state.destinationManagement
                    },
                )
            },
        )
    }

    private fun observeRowers() {
        viewModelScope.launch {
            rowerRepository.observeRowers().collect { rowers ->
                uiStateMutable.update {
                    it.copy(
                        rowerManagement = it.rowerManagement.copy(rowers = rowers),
                    )
                }
            }
        }
    }

    private fun observeBoats() {
        viewModelScope.launch {
            boatRepository.observeBoats().collect { boats ->
                uiStateMutable.update {
                    it.copy(
                        boatManagement = it.boatManagement.copy(boats = boats),
                    )
                }
            }
        }
    }

    private fun observeDestinations() {
        viewModelScope.launch {
            destinationRepository.observeDestinations().collect { destinations ->
                uiStateMutable.update {
                    it.copy(
                        destinationManagement = it.destinationManagement.copy(destinations = destinations),
                    )
                }
            }
        }
    }

    private fun observeSessions() {
        viewModelScope.launch {
            sessionRepository.observeSessionsWithDetails().collect { sessions ->
                uiStateMutable.update {
                    it.copy(exportableSessions = sessions.map { session -> session.toHistoryUi() })
                }
            }
        }
    }

    private fun SessionWithDetails.toHistoryUi(): HistorySessionUi {
        return HistorySessionUi(
            id = session.id,
            date = session.date,
            boatId = boat.id,
            boatName = boat.name,
            participants = sessionRowers.map { participant ->
                HistoryParticipantUi(
                    name = participant.displayName,
                    isGuest = participant.sessionRower.guestName != null,
                )
            },
            destination = destinationName,
            startTime = session.startTime,
            endTime = session.endTime.orEmpty(),
            km = if (session.km == 0.0) "" else session.km.toString(),
            remarks = session.remarks.orEmpty(),
            status = when (session.status) {
                com.aca56.cahiersortiecodex.data.local.entity.SessionStatus.ONGOING -> "EN COURS"
                com.aca56.cahiersortiecodex.data.local.entity.SessionStatus.COMPLETED -> "TERMINÉE"
                com.aca56.cahiersortiecodex.data.local.entity.SessionStatus.NOT_COMPLETED -> "NON TERMINÉE"
            },
        )
    }

    private fun resolveCleanupCutoffDate(state: SettingsUiState): String? {
        return when (state.cleanupPeriod) {
            DataCleanupPeriod.CUSTOM -> state.cleanupCustomCutoffDate.takeIf { it.isNotBlank() }
            else -> {
                val months = state.cleanupPeriod.months ?: return null
                val calendar = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -months)
                }
                cleanupDateFormatter.format(calendar.time)
            }
        }
    }

    private fun buildCleanupPreview(
        state: SettingsUiState,
        cutoffDate: String,
        sessions: List<SessionWithDetails>,
        remarks: List<RemarkEntity>,
    ): DataCleanupPreview {
        val sessionIdsToDelete = if (state.cleanupSessionsEnabled) {
            sessions
                .filter { it.session.status != SessionStatus.ONGOING }
                .filter { it.session.date < cutoffDate }
                .map { it.session.id }
                .toSet()
        } else {
            emptySet()
        }

        val remarksToDelete = if (state.cleanupRemarksEnabled) {
            remarks.filter { remark ->
                remark.date < cutoffDate &&
                    remark.status != RemarkStatus.REPAIR_NEEDED
            }
        } else {
            emptyList()
        }

        return DataCleanupPreview(
            sessionsCount = sessionIdsToDelete.size,
            remarksCount = remarksToDelete.size,
            cutoffDate = cutoffDate,
        )
    }

    private suspend fun executeDataCleanup(cutoffDate: String) {
        val state = uiState.value
        val remarksRepository = application.appContainer.remarkRepository
        val repairUpdateRepository = application.appContainer.repairUpdateRepository
        val boatPhotoStorage = application.appContainer.boatPhotoStorage

        val sessions = sessionRepository.observeSessionsWithDetails().first()
        val remarks = remarksRepository.observeRemarks().first()
        val repairUpdates = repairUpdateRepository.observeUpdates().first()

        val sessionIdsToDelete = if (state.cleanupSessionsEnabled) {
            sessions
                .filter { it.session.status != SessionStatus.ONGOING }
                .filter { it.session.date < cutoffDate }
                .map { it.session.id }
                .toSet()
        } else {
            emptySet()
        }

        val remarksToDelete = if (state.cleanupRemarksEnabled) {
            remarks.filter { remark ->
                remark.date < cutoffDate &&
                    remark.status != RemarkStatus.REPAIR_NEEDED
            }
        } else {
            emptyList()
        }
        val remarkIdsToDelete = remarksToDelete.map { it.id }.toSet()

        remarks
            .filter { it.sessionId in sessionIdsToDelete && it.id !in remarkIdsToDelete }
            .forEach { linkedRemark ->
                remarksRepository.updateRemark(
                    linkedRemark.copy(sessionId = null),
                )
            }

        repairUpdates
            .filter { it.remarkId in remarkIdsToDelete }
            .forEach { update ->
                decodeRemarkPhotoPaths(update.photoPath).forEach(boatPhotoStorage::deletePhoto)
                repairUpdateRepository.deleteUpdate(update)
            }

        remarksToDelete.forEach { remark ->
            decodeRemarkPhotoPaths(remark.photoPath).forEach(boatPhotoStorage::deletePhoto)
            remarksRepository.deleteRemark(remark)
        }

        sessions
            .filter { it.session.id in sessionIdsToDelete }
            .forEach { session ->
                sessionRepository.clearSessionRowers(session.session.id)
                sessionRepository.deleteSession(session.session)
            }
    }

    private fun normalizePersonKey(firstName: String, lastName: String): String {
        return "${firstName.trim().lowercase()}|${lastName.trim().lowercase()}"
    }

    private fun normalizeHex(value: String): String {
        return "#${value.trim().removePrefix("#").uppercase()}"
    }

    private fun isValidColorHex(value: String): Boolean {
        return Regex("^#[0-9A-F]{6}$").matches(value)
    }

    private fun loadAppPreferencesIntoState(message: String? = null) {
        val preferences = appPreferencesStore.currentPreferences()
        uiStateMutable.update {
            it.copy(
                themeMode = preferences.themeMode,
                primaryColorInput = preferences.primaryColorHex,
                secondaryColorInput = preferences.secondaryColorHex,
                tertiaryColorInput = preferences.tertiaryColorHex,
                inactivityTimeoutMinutesInput = formatMinutes(preferences.inactivityTimeoutMillis),
                successPopupDurationSecondsInput = formatSeconds(preferences.successPopupDurationMillis),
                errorPopupDurationSecondsInput = formatSeconds(preferences.errorPopupDurationMillis),
                animationsEnabled = preferences.animationsEnabled,
                crewsEnabled = preferences.crewsEnabled,
                message = message ?: it.message,
                messageType = if (message == null) it.messageType else FeedbackDialogType.SUCCESS,
            )
        }
    }

    private fun sanitizeDecimalInput(value: String): String {
        val filtered = value.filter { char -> char.isDigit() || char == '.' || char == ',' }
            .replace(',', '.')
        val firstDotIndex = filtered.indexOf('.')
        return if (firstDotIndex == -1) {
            filtered
        } else {
            filtered.substring(0, firstDotIndex + 1) +
                filtered.substring(firstDotIndex + 1).replace(".", "")
        }
    }

    private fun String.toPositiveMillis(multiplier: Double): Long? {
        val numericValue = trim().replace(',', '.').toDoubleOrNull() ?: return null
        if (numericValue <= 0.0) return null
        return (numericValue * multiplier).toLong()
    }

    private fun launchWork(
        onErrorMessage: String,
        block: suspend () -> Unit,
        onSuccessMessage: String? = null,
        onSuccess: ((SettingsUiState) -> SettingsUiState)? = null,
    ) {
        viewModelScope.launch {
            uiStateMutable.update { it.copy(isWorking = true, message = null, messageType = null) }

            runCatching { block() }
                .onSuccess {
                    uiStateMutable.update { state ->
                        val updatedState = onSuccess?.invoke(state) ?: state
                        updatedState.copy(
                            isWorking = false,
                            message = updatedState.message ?: onSuccessMessage,
                            messageType = if ((updatedState.message ?: onSuccessMessage) == null) null else FeedbackDialogType.SUCCESS,
                        )
                    }
                }
                .onFailure {
                    uiStateMutable.update {
                        it.copy(
                            isWorking = false,
                            message = onErrorMessage,
                            messageType = FeedbackDialogType.ERROR,
                        )
                    }
                }
        }
    }

    private fun <T> launchWorkWithResult(
        onErrorMessage: String,
        block: suspend () -> T,
        successMessage: (T) -> String,
    ) {
        viewModelScope.launch {
            uiStateMutable.update { it.copy(isWorking = true, message = null, messageType = null) }

            runCatching { block() }
                .onSuccess { result ->
                    uiStateMutable.update {
                        it.copy(
                            isWorking = false,
                            message = successMessage(result),
                            messageType = FeedbackDialogType.SUCCESS,
                        )
                    }
                }
                .onFailure {
                    uiStateMutable.update {
                        it.copy(
                            isWorking = false,
                            message = onErrorMessage,
                            messageType = FeedbackDialogType.ERROR,
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(
            application: CahierSortieApplication,
            backupManager: DatabaseBackupManager,
            pinCodeStore: PinCodeStore,
            appPreferencesStore: AppPreferencesStore,
            rowerRepository: RowerRepository,
            boatRepository: BoatRepository,
            destinationRepository: DestinationRepository,
            sessionRepository: SessionRepository,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(
                        application = application,
                        backupManager = backupManager,
                        pinCodeStore = pinCodeStore,
                        appPreferencesStore = appPreferencesStore,
                        rowerRepository = rowerRepository,
                        boatRepository = boatRepository,
                        destinationRepository = destinationRepository,
                        sessionRepository = sessionRepository,
                    ) as T
                }
            }
        }
    }
}

private val cleanupDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

private fun formatSeconds(durationMillis: Long): String {
    val seconds = durationMillis / 1000.0
    return if (seconds % 1.0 == 0.0) {
        seconds.toInt().toString()
    } else {
        seconds.toString().removeSuffix("0").removeSuffix(".")
    }
}

private fun formatMinutes(durationMillis: Long): String {
    val minutes = durationMillis / 60_000.0
    return if (minutes % 1.0 == 0.0) {
        minutes.toInt().toString()
    } else {
        minutes.toString().removeSuffix("0").removeSuffix(".")
    }
}
