package com.aca56.cahiersortiecodex.feature.newsession.presentation

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aca56.cahiersortiecodex.data.crew.CrewStore
import com.aca56.cahiersortiecodex.data.logging.AppLogStore
import com.aca56.cahiersortiecodex.data.local.entity.BoatEntity
import com.aca56.cahiersortiecodex.data.local.entity.DestinationEntity
import com.aca56.cahiersortiecodex.data.local.entity.RemarkEntity
import com.aca56.cahiersortiecodex.data.local.entity.decodeRemarkPhotoPaths
import com.aca56.cahiersortiecodex.data.local.entity.encodeRemarkPhotoPaths
import com.aca56.cahiersortiecodex.data.local.entity.RemarkStatus
import com.aca56.cahiersortiecodex.data.local.entity.RowerEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionRowerEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionStatus
import com.aca56.cahiersortiecodex.data.local.relation.SessionWithDetails
import com.aca56.cahiersortiecodex.data.media.BoatPhotoStorage
import com.aca56.cahiersortiecodex.data.repository.BoatRepository
import com.aca56.cahiersortiecodex.data.repository.DestinationRepository
import com.aca56.cahiersortiecodex.data.repository.RemarkRepository
import com.aca56.cahiersortiecodex.data.repository.RowerRepository
import com.aca56.cahiersortiecodex.data.repository.SessionRepository
import com.aca56.cahiersortiecodex.data.settings.AppPreferencesStore
import com.aca56.cahiersortiecodex.ui.components.SearchableSelectableOption
import com.aca56.cahiersortiecodex.ui.components.currentStorageDate
import com.aca56.cahiersortiecodex.ui.components.currentStorageTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private fun defaultSessionDate(): String = currentStorageDate()

private fun defaultSessionTime(): String = currentStorageTime()

data class GuestRowerUi(
    val localId: Long,
    val fullName: String,
)

data class CrewSummaryUi(
    val id: Long,
    val name: String,
    val rowerNames: List<String>,
    val rowerIds: Set<Long>,
)

data class BoatConflictUi(
    val boatId: Long,
    val boatName: String,
    val activeSessionId: Long,
    val title: String,
    val description: String,
)

private fun normalizeGuestName(value: String): String {
    return value.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ")
}

data class NewSessionUiState(
    val editingSessionId: Long? = null,
    val isQuickMode: Boolean = false,
    val date: String = defaultSessionDate(),
    val selectedBoatId: Long? = null,
    val availableBoats: List<BoatEntity> = emptyList(),
    val availableDestinations: List<DestinationEntity> = emptyList(),
    val selectedDestinationId: Long? = null,
    val isCustomDestination: Boolean = false,
    val availableRowers: List<RowerEntity> = emptyList(),
    val rowerUsageCounts: Map<Long, Int> = emptyMap(),
    val boatRowerCounts: Map<Long, Int> = emptyMap(),
    val boatUsageCounts: Map<Long, Int> = emptyMap(),
    val boatStatuses: Map<Long, BoatSelectionStatus> = emptyMap(),
    val selectedRowerIds: Set<Long> = emptySet(),
    val rowerSearchQuery: String = "",
    val guestRowerName: String = "",
    val guestRowers: List<GuestRowerUi> = emptyList(),
    val crewsEnabled: Boolean = false,
    val availableCrews: List<CrewSummaryUi> = emptyList(),
    val boatConflict: BoatConflictUi? = null,
    val startTime: String = defaultSessionTime(),
    val endTime: String = "",
    val km: String = "",
    val remarks: String = "",
    val sessionRemarkStatus: RemarkStatus = RemarkStatus.NORMAL,
    val sessionRemarkPhotoPaths: List<String> = emptyList(),
    val destination: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val savedSessionStatus: SessionStatus? = null,
) {
    val isEditMode: Boolean
        get() = editingSessionId != null

    val selectedBoat: BoatEntity?
        get() = availableBoats.firstOrNull { it.id == selectedBoatId }

    val totalSelectedRowers: Int
        get() = selectedRowerIds.size + guestRowers.size

    val isSeatCountValid: Boolean
        get() = selectedBoat?.let { totalSelectedRowers <= it.seatCount } ?: true

    val canSave: Boolean
        get() = !isSaving &&
            selectedBoatId != null &&
            totalSelectedRowers > 0 &&
            date.isNotBlank() &&
            startTime.isNotBlank() &&
            isSeatCountValid

    val filteredRowers: List<RowerEntity>
        get() {
            val defaultIndexes = availableRowers.withIndex().associate { it.value.id to it.index }
            val query = rowerSearchQuery.trim()
            val selectedRowers = availableRowers
                .filter { it.id in selectedRowerIds }
                .sortedBy { defaultIndexes[it.id] ?: Int.MAX_VALUE }
            val unselectedRowers = availableRowers.filterNot { it.id in selectedRowerIds }
            val searchMatches = if (query.isBlank()) {
                emptyList()
            } else {
                unselectedRowers.filter { rower ->
                    val fullName = "${rower.firstName} ${rower.lastName}".trim()
                    fullName.contains(query, ignoreCase = true)
                }.sortedBy { defaultIndexes[it.id] ?: Int.MAX_VALUE }
            }
            val searchMatchIds = searchMatches.mapTo(mutableSetOf()) { it.id }
            val remainingRowers = unselectedRowers.filterNot { rower ->
                rower.id in searchMatchIds
            }
            val suggestedRowers = remainingRowers
                .filter { (boatRowerCounts[it.id] ?: 0) > 0 }
                .sortedWith(
                    compareByDescending<RowerEntity> { boatRowerCounts[it.id] ?: 0 }
                        .thenBy { defaultIndexes[it.id] ?: Int.MAX_VALUE },
                )
            val fallbackRowers = remainingRowers
                .filter { (boatRowerCounts[it.id] ?: 0) == 0 }
                .sortedBy { defaultIndexes[it.id] ?: Int.MAX_VALUE }

            return selectedRowers + searchMatches + suggestedRowers + fallbackRowers
        }

    val filteredRowerOptions: List<SearchableSelectableOption>
        get() = filteredRowers.map { rower ->
            SearchableSelectableOption(
                key = rower.id.toString(),
                label = "${rower.firstName} ${rower.lastName}".trim(),
                usageCount = rowerUsageCounts[rower.id] ?: 0,
            )
        }

    val availableBoatOptions: List<SearchableSelectableOption>
        get() = availableBoats.map { boat ->
            SearchableSelectableOption(
                key = boat.id.toString(),
                label = "${boat.name} (${boat.seatCount} places)",
                secondaryLabel = boatStatuses[boat.id]?.label(),
                usageCount = boatUsageCounts[boat.id] ?: 0,
            )
        }.sortedWith(compareByDescending<SearchableSelectableOption> { it.usageCount }.thenBy { it.label })

    val derivedStatus: SessionStatus
        get() = if (endTime.isBlank()) SessionStatus.ONGOING else SessionStatus.COMPLETED
}

class NewSessionViewModel(
    private val boatRepository: BoatRepository,
    private val remarkRepository: RemarkRepository,
    private val destinationRepository: DestinationRepository,
    private val rowerRepository: RowerRepository,
    private val sessionRepository: SessionRepository,
    private val boatPhotoStorage: BoatPhotoStorage,
    private val appPreferencesStore: AppPreferencesStore,
    private val crewStore: CrewStore,
    private val appLogStore: AppLogStore,
    private val sessionId: Long? = null,
) : ViewModel() {
    private val uiStateMutable = MutableStateFlow(NewSessionUiState(editingSessionId = sessionId))
    val uiState: StateFlow<NewSessionUiState> = uiStateMutable.asStateFlow()

    private var nextGuestId = 1L
    private var allSessions: List<SessionWithDetails> = emptyList()
    private var ongoingSessionsByBoatId: Map<Long, SessionWithDetails> = emptyMap()

    init {
        observeBoats()
        observeDestinations()
        observeRowers()
        observeUsageAndSuggestions()
        observeBoatStatuses()
        observeCrewPreferences()
        observeCrews()
        if (sessionId != null) {
            loadSession(sessionId)
        }
    }

    fun clearFeedback() {
        uiStateMutable.update {
            it.copy(
                errorMessage = null,
                successMessage = null,
                savedSessionStatus = null,
            )
        }
    }

    fun toggleQuickMode() {
        uiStateMutable.update { state ->
            state.copy(
                isQuickMode = !state.isQuickMode,
                date = currentStorageDate(),
                startTime = currentStorageTime(),
                selectedDestinationId = if (!state.isQuickMode) null else state.selectedDestinationId,
                isCustomDestination = if (!state.isQuickMode) false else state.isCustomDestination,
                destination = if (!state.isQuickMode) "" else state.destination,
                endTime = if (!state.isQuickMode) "" else state.endTime,
                km = if (!state.isQuickMode) "" else state.km,
                remarks = if (!state.isQuickMode) "" else state.remarks,
                sessionRemarkPhotoPaths = if (!state.isQuickMode) emptyList() else state.sessionRemarkPhotoPaths,
                errorMessage = null,
                successMessage = null,
                savedSessionStatus = null,
            )
        }
        refreshCrewOrdering()
    }

    fun resetForm() {
        uiStateMutable.update { state ->
            state.resetPreservingSources(editingSessionId = state.editingSessionId)
        }
        refreshCrewOrdering()
    }

    fun onDateChanged(value: String) {
        uiStateMutable.update {
            it.copy(
                date = value,
                errorMessage = null,
                successMessage = null,
                savedSessionStatus = null,
            )
        }
    }

    fun onBoatSelected(boatId: Long) {
        val currentState = uiState.value
        val status = currentState.boatStatuses[boatId]
        val ongoingConflict = ongoingSessionsByBoatId[boatId]

        if (status == BoatSelectionStatus.IN_REPAIR && currentState.selectedBoatId != boatId) {
            uiStateMutable.update {
                it.copy(
                    boatConflict = BoatConflictUi(
                        boatId = boatId,
                        boatName = currentState.availableBoats.firstOrNull { boat -> boat.id == boatId }?.name.orEmpty(),
                        activeSessionId = ongoingConflict?.session?.id ?: 0L,
                        title = "Bateau en réparation",
                        description = "Ce bateau est actuellement en réparation. Vous devez confirmer avant de l’utiliser.",
                    ),
                    errorMessage = null,
                    successMessage = null,
                )
            }
            return
        }

        if (
            ongoingConflict != null &&
            currentState.editingSessionId != ongoingConflict.session.id &&
            currentState.selectedBoatId != boatId
        ) {
            uiStateMutable.update {
                it.copy(
                    boatConflict = BoatConflictUi(
                        boatId = boatId,
                        boatName = ongoingConflict.boat.name,
                        activeSessionId = ongoingConflict.session.id,
                        title = "Bateau en cours d'utilisation",
                        description = "Ce bateau est en cours d'utilisation dans une sortie en cours. Vous devez confirmer avant de l’utiliser.",
                    ),
                    errorMessage = null,
                    successMessage = null,
                )
            }
            return
        }

        applyBoatSelection(boatId)
    }

    fun dismissBoatConflict() {
        uiStateMutable.update { it.copy(boatConflict = null) }
    }

    fun forceBoatSelection() {
        val conflictBoatId = uiState.value.boatConflict?.boatId ?: return
        applyBoatSelection(conflictBoatId)
    }

    fun onStartTimeChanged(value: String) {
        uiStateMutable.update {
            it.copy(
                startTime = value,
                errorMessage = null,
                successMessage = null,
                savedSessionStatus = null,
            )
        }
    }

    fun onEndTimeChanged(value: String) {
        uiStateMutable.update {
            it.copy(
                endTime = value,
                errorMessage = null,
                successMessage = null,
                savedSessionStatus = null,
            )
        }
    }

    fun onKmChanged(value: String) {
        uiStateMutable.update {
            it.copy(
                km = value,
                errorMessage = null,
                successMessage = null,
                savedSessionStatus = null,
            )
        }
    }

    fun onRemarksChanged(value: String) {
        uiStateMutable.update {
            it.copy(
                remarks = value,
                errorMessage = null,
                successMessage = null,
                savedSessionStatus = null,
            )
        }
    }

    fun onSessionRemarkStatusChanged(status: RemarkStatus) {
        uiStateMutable.update {
            it.copy(
                sessionRemarkStatus = status,
                errorMessage = null,
                successMessage = null,
                savedSessionStatus = null,
            )
        }
    }

    fun addSessionRemarkPhotos(uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                uris.map { uri -> boatPhotoStorage.importCompressedPhoto(uri) }
            }.onSuccess { paths ->
                appLogStore.logAction(
                    actionType = "Ajout de photos",
                    details = "Ajout de ${paths.size} photo(s) à la remarque de session en cours de saisie.",
                )
                uiStateMutable.update {
                    it.copy(
                        sessionRemarkPhotoPaths = (it.sessionRemarkPhotoPaths + paths).distinct(),
                        errorMessage = null,
                        successMessage = null,
                        savedSessionStatus = null,
                    )
                }
            }.onFailure {
                appLogStore.logError(
                    actionType = "Échec d'enregistrement de session",
                    details = "La session n'a pas pu être enregistrée.",
                )
                uiStateMutable.update {
                    it.copy(
                        errorMessage = "Impossible d'ajouter les photos à la remarque.",
                        successMessage = null,
                        savedSessionStatus = null,
                    )
                }
            }
        }
    }

    fun addSessionRemarkPhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            runCatching {
                boatPhotoStorage.saveCompressedBitmap(bitmap)
            }.onSuccess { path ->
                appLogStore.logAction(
                    actionType = "Ajout de photo",
                    details = "Ajout d'une photo à la remarque de session en cours de saisie.",
                )
                uiStateMutable.update {
                    it.copy(
                        sessionRemarkPhotoPaths = (it.sessionRemarkPhotoPaths + path).distinct(),
                        errorMessage = null,
                        successMessage = null,
                        savedSessionStatus = null,
                    )
                }
            }.onFailure {
                uiStateMutable.update {
                    it.copy(
                        errorMessage = "Impossible d'ajouter la photo à la remarque.",
                        successMessage = null,
                        savedSessionStatus = null,
                    )
                }
            }
        }
    }

    fun removeSessionRemarkPhoto(path: String) {
        boatPhotoStorage.deletePhoto(path)
        appLogStore.logAction(
            actionType = "Suppression de photo",
            details = "Suppression d'une photo de la remarque de session en cours de saisie.",
        )
        uiStateMutable.update {
            it.copy(
                sessionRemarkPhotoPaths = it.sessionRemarkPhotoPaths - path,
                errorMessage = null,
                successMessage = null,
                savedSessionStatus = null,
            )
        }
    }

    fun onDestinationSelected(destinationId: Long?) {
        uiStateMutable.update {
            it.copy(
                selectedDestinationId = destinationId,
                isCustomDestination = false,
                destination = it.availableDestinations.firstOrNull { destination -> destination.id == destinationId }?.name.orEmpty(),
                errorMessage = null,
                successMessage = null,
                savedSessionStatus = null,
            )
        }
    }

    fun onCustomDestinationSelected() {
        uiStateMutable.update {
            it.copy(
                selectedDestinationId = null,
                isCustomDestination = true,
                destination = "",
                errorMessage = null,
                successMessage = null,
                savedSessionStatus = null,
            )
        }
    }

    fun onDestinationChanged(value: String) {
        uiStateMutable.update {
            it.copy(
                destination = value,
                isCustomDestination = true,
                selectedDestinationId = null,
                errorMessage = null,
                successMessage = null,
                savedSessionStatus = null,
            )
        }
    }

    fun onRowerSearchQueryChanged(value: String) {
        uiStateMutable.update {
            it.copy(
                rowerSearchQuery = value,
                errorMessage = null,
                successMessage = null,
                savedSessionStatus = null,
            )
        }
    }

    fun onGuestRowerNameChanged(value: String) {
        uiStateMutable.update {
            it.copy(
                guestRowerName = value,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun onRowerChecked(rowerId: Long, checked: Boolean) {
        uiStateMutable.update { state ->
            val updatedIds = state.selectedRowerIds.toMutableSet()
            if (checked) {
                val selectedBoat = state.selectedBoat
                if (selectedBoat != null && state.totalSelectedRowers >= selectedBoat.seatCount) {
                    return@update state.copy(
                        errorMessage = "Le nombre de rameurs sélectionnés ne peut pas dépasser le nombre de places du bateau.",
                        successMessage = null,
                    )
                }
                updatedIds += rowerId
            } else {
                updatedIds -= rowerId
            }

            state.copy(
                selectedRowerIds = updatedIds,
                errorMessage = null,
                successMessage = null,
                savedSessionStatus = null,
            ).withSeatValidation()
        }
        refreshCrewOrdering()
    }

    fun applyCrew(crewId: Long) {
        val crew = uiState.value.availableCrews.firstOrNull { it.id == crewId } ?: return
        uiStateMutable.update { state ->
            state.copy(
                selectedRowerIds = crew.rowerIds.intersect(state.availableRowers.map { it.id }.toSet()),
                guestRowers = emptyList(),
                errorMessage = null,
                successMessage = null,
                savedSessionStatus = null,
            ).withSeatValidation()
        }
        refreshCrewOrdering()
    }

    fun addGuestRower() {
        uiStateMutable.update { state ->
            val fullName = normalizeGuestName(state.guestRowerName)

            if (fullName.isBlank()) {
                return@update state.copy(
                    errorMessage = "Veuillez saisir le nom du rameur invité.",
                    successMessage = null,
                )
            }

            val selectedBoat = state.selectedBoat
            if (selectedBoat != null && state.totalSelectedRowers >= selectedBoat.seatCount) {
                return@update state.copy(
                    errorMessage = "Le nombre de rameurs sélectionnés ne peut pas dépasser le nombre de places du bateau.",
                    successMessage = null,
                )
            }

            state.copy(
                guestRowerName = "",
                guestRowers = state.guestRowers + GuestRowerUi(
                    localId = nextGuestId++,
                    fullName = fullName,
                ),
                errorMessage = null,
                successMessage = null,
                savedSessionStatus = null,
            ).withSeatValidation()
        }
        refreshCrewOrdering()
    }

    fun removeGuestRower(localId: Long) {
        uiStateMutable.update {
            it.copy(
                guestRowers = it.guestRowers.filterNot { guest -> guest.localId == localId },
                errorMessage = null,
                successMessage = null,
            ).withSeatValidation()
        }
        refreshCrewOrdering()
    }

    fun saveSession() {
        val currentState = uiState.value
        val effectiveDate = if (currentState.isQuickMode && !currentState.isEditMode) currentStorageDate() else currentState.date.trim()
        val effectiveStartTime = if (currentState.isQuickMode && !currentState.isEditMode) currentStorageTime() else currentState.startTime.trim()
        val effectiveEndTime = if (currentState.isQuickMode) "" else currentState.endTime.trim()
        val effectiveDestination = if (currentState.isQuickMode) "" else currentState.destination.trim()
        val effectiveCustomDestination = if (currentState.isQuickMode) false else currentState.isCustomDestination
        val effectiveSelectedDestinationId = if (currentState.isQuickMode) null else currentState.selectedDestinationId
        val effectiveKmValue = if (currentState.isQuickMode) "" else currentState.km
        val parsedKm = effectiveKmValue.trim().replace(',', '.').takeIf { it.isNotBlank() }?.toDoubleOrNull()
        val selectedBoat = currentState.selectedBoat

        when {
            effectiveDate.isBlank() -> {
                uiStateMutable.update {
                    it.copy(
                        errorMessage = "Veuillez choisir une date avant d'enregistrer la session.",
                        successMessage = null,
                    )
                }
                return
            }

            selectedBoat == null -> {
                uiStateMutable.update {
                    it.copy(
                        errorMessage = "Veuillez sélectionner un bateau pour cette session.",
                        successMessage = null,
                    )
                }
                return
            }

            effectiveStartTime.isBlank() -> {
                uiStateMutable.update {
                    it.copy(
                        errorMessage = "Veuillez choisir une heure de départ avant d'enregistrer la session.",
                        successMessage = null,
                    )
                }
                return
            }

            effectiveCustomDestination && effectiveDestination.isBlank() -> {
                uiStateMutable.update {
                    it.copy(
                        errorMessage = "Veuillez saisir une destination ou choisir une destination existante.",
                        successMessage = null,
                    )
                }
                return
            }

            effectiveEndTime.isNotBlank() && effectiveEndTime == effectiveStartTime -> {
                uiStateMutable.update {
                    it.copy(
                        errorMessage = "L'heure de fin doit être différente de l'heure de départ pour enregistrer une session terminée.",
                        successMessage = null,
                    )
                }
                return
            }

            effectiveEndTime.isNotBlank() && effectiveEndTime < effectiveStartTime -> {
                uiStateMutable.update {
                    it.copy(
                        errorMessage = "L'heure de fin doit être postérieure à l'heure de départ pour enregistrer une session terminée.",
                        successMessage = null,
                    )
                }
                return
            }

            effectiveKmValue.isNotBlank() && parsedKm == null -> {
                uiStateMutable.update {
                    it.copy(
                        errorMessage = "Veuillez saisir un nombre de kilomètres valide, ou laisser le champ vide.",
                        successMessage = null,
                    )
                }
                return
            }

            parsedKm != null && parsedKm < 0.0 -> {
                uiStateMutable.update {
                    it.copy(
                        errorMessage = "Le nombre de kilomètres ne peut pas être négatif. Veuillez saisir 0 ou une valeur positive.",
                        successMessage = null,
                    )
                }
                return
            }

            currentState.totalSelectedRowers > selectedBoat.seatCount -> {
                uiStateMutable.update {
                    it.copy(
                        errorMessage = "Ce bateau a ${selectedBoat.seatCount} places, vous ne pouvez donc pas sélectionner ${currentState.totalSelectedRowers} rameurs.",
                        successMessage = null,
                    )
                }
                return
            }

            currentState.totalSelectedRowers == 0 -> {
                uiStateMutable.update {
                    it.copy(
                        errorMessage = "Veuillez sélectionner au moins un rameur ou ajouter un rameur invité avant d'enregistrer.",
                        successMessage = null,
                    )
                }
                return
            }

            currentState.sessionRemarkStatus == RemarkStatus.REPAIR_NEEDED && currentState.remarks.trim().isBlank() -> {
                uiStateMutable.update {
                    it.copy(
                        errorMessage = "Veuillez saisir une remarque avant de choisir le statut « réparation nécessaire ».",
                        successMessage = null,
                    )
                }
                return
            }
        }

        viewModelScope.launch {
            uiStateMutable.update {
                it.copy(
                    isSaving = true,
                    errorMessage = null,
                    successMessage = null,
                )
            }

            runCatching {
                val destinationId = when {
                    effectiveSelectedDestinationId != null -> effectiveSelectedDestinationId
                    effectiveDestination.isNotBlank() -> {
                        destinationRepository.getDestinationByName(effectiveDestination)?.id
                            ?: destinationRepository.saveDestination(
                                DestinationEntity(name = effectiveDestination),
                            )
                    }
                    else -> null
                }

                val targetSessionId = currentState.editingSessionId
                val sessionEntity = SessionEntity(
                    id = targetSessionId ?: 0L,
                    date = effectiveDate,
                    boatId = selectedBoat.id,
                    startTime = effectiveStartTime,
                    endTime = effectiveEndTime.ifBlank { null },
                    destinationId = destinationId,
                    km = parsedKm ?: 0.0,
                    remarks = if (currentState.isQuickMode) null else currentState.remarks.trim().ifBlank { null },
                    status = if (effectiveEndTime.isBlank()) SessionStatus.ONGOING else SessionStatus.COMPLETED,
                )

                val savedSessionId = if (targetSessionId == null) {
                    sessionRepository.saveSession(sessionEntity)
                } else {
                    sessionRepository.updateSession(sessionEntity)
                    sessionRepository.clearSessionRowers(targetSessionId)
                    targetSessionId
                }

                sessionRepository.saveSessionRowers(
                    currentState.selectedRowerIds.map { rowerId ->
                        SessionRowerEntity(
                            sessionId = savedSessionId,
                            rowerId = rowerId,
                            guestName = null,
                        )
                    } + currentState.guestRowers.map { guest ->
                        SessionRowerEntity(
                            sessionId = savedSessionId,
                            rowerId = null,
                            guestName = normalizeGuestName(guest.fullName),
                        )
                    },
                )

                val existingLinkedRemark = remarkRepository.getRemarkBySessionId(savedSessionId)
                val shouldStoreLinkedRemark =
                    currentState.sessionRemarkStatus == RemarkStatus.REPAIR_NEEDED ||
                        currentState.sessionRemarkPhotoPaths.isNotEmpty()

                if (shouldStoreLinkedRemark) {
                    val repairRemark = RemarkEntity(
                        id = existingLinkedRemark?.id ?: 0L,
                        boatId = selectedBoat.id,
                        sessionId = savedSessionId,
                        content = currentState.remarks.trim(),
                        date = effectiveDate,
                        status = currentState.sessionRemarkStatus,
                        photoPath = encodeRemarkPhotoPaths(
                            currentState.sessionRemarkPhotoPaths.ifEmpty {
                                decodeRemarkPhotoPaths(existingLinkedRemark?.photoPath)
                            },
                        ),
                    )
                    if (existingLinkedRemark == null) {
                        remarkRepository.saveRemark(repairRemark)
                    } else {
                        remarkRepository.updateRemark(repairRemark)
                    }
                } else {
                    existingLinkedRemark?.let { remarkRepository.deleteRemark(it) }
                }
            }.onSuccess {
                appLogStore.logAction(
                    actionType = if (currentState.isEditMode) "Modification de session" else "Création de session",
                    details = buildString {
                        append(if (currentState.isQuickMode && !currentState.isEditMode) "Sortie rapide" else "Session")
                        append(" enregistrée pour le bateau ${selectedBoat.name}. ")
                        append("${currentState.totalSelectedRowers} rameur(s) sélectionné(s).")
                        if (currentState.sessionRemarkStatus == RemarkStatus.REPAIR_NEEDED) {
                            append(" Une remarque de réparation a été créée.")
                        }
                    },
                )
                uiStateMutable.update { state ->
                    if (state.isEditMode) {
                        state.copy(
                            isSaving = false,
                            errorMessage = null,
                            successMessage = "Modifications enregistrées.",
                            savedSessionStatus = if (effectiveEndTime.isBlank()) SessionStatus.ONGOING else SessionStatus.COMPLETED,
                        )
                    } else {
                        state.resetPreservingSources(editingSessionId = null).copy(
                            successMessage = if (currentState.isQuickMode) {
                                "Sortie démarrée."
                            } else {
                                "Session enregistrée avec succès."
                            },
                            savedSessionStatus = if (effectiveEndTime.isBlank()) SessionStatus.ONGOING else SessionStatus.COMPLETED,
                        )
                    }
                }
                refreshCrewOrdering()
            }.onFailure {
                uiStateMutable.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "La session n'a pas pu être enregistrée. Veuillez vérifier les informations saisies puis réessayer.",
                        successMessage = null,
                        savedSessionStatus = null,
                    )
                }
            }
        }
    }

    private fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            runCatching {
                Pair(
                    sessionRepository.getSessionWithDetails(sessionId),
                    remarkRepository.getRemarkBySessionId(sessionId),
                )
            }.onSuccess { (sessionWithDetails, linkedRemark) ->
                if (sessionWithDetails == null) {
                    uiStateMutable.update {
                        it.copy(
                            errorMessage = "La session à modifier est introuvable.",
                            successMessage = null,
                        )
                    }
                    return@onSuccess
                }

                uiStateMutable.update { state ->
                    state.copy(
                        editingSessionId = sessionWithDetails.session.id,
                        date = sessionWithDetails.session.date,
                        selectedBoatId = sessionWithDetails.boat.id,
                        selectedDestinationId = sessionWithDetails.destination?.id,
                        isCustomDestination = sessionWithDetails.destination == null &&
                            sessionWithDetails.destinationName.isNotBlank(),
                        destination = sessionWithDetails.destinationName,
                        selectedRowerIds = sessionWithDetails.sessionRowers.mapNotNull { it.sessionRower.rowerId }.toSet(),
                        guestRowers = sessionWithDetails.sessionRowers.mapNotNull { participant ->
                            participant.sessionRower.guestName?.let { guestName ->
                                GuestRowerUi(
                                    localId = nextGuestId++,
                                    fullName = normalizeGuestName(guestName),
                                )
                            }
                        },
                        startTime = sessionWithDetails.session.startTime,
                        endTime = sessionWithDetails.session.endTime.orEmpty(),
                        km = if (sessionWithDetails.session.km == 0.0) "" else sessionWithDetails.session.km.toString(),
                        remarks = sessionWithDetails.session.remarks.orEmpty(),
                        sessionRemarkStatus = linkedRemark?.status ?: RemarkStatus.NORMAL,
                        sessionRemarkPhotoPaths = decodeRemarkPhotoPaths(linkedRemark?.photoPath),
                        errorMessage = null,
                        successMessage = null,
                        savedSessionStatus = null,
                    ).withSeatValidation()
                }
                refreshCrewOrdering()
            }.onFailure {
                uiStateMutable.update {
                    it.copy(
                        errorMessage = "Impossible de charger la session à modifier. Veuillez réessayer.",
                        successMessage = null,
                        savedSessionStatus = null,
                    )
                }
            }
        }
    }

    private fun observeBoats() {
        viewModelScope.launch {
            boatRepository.observeBoats().collect { boats ->
                uiStateMutable.update { state ->
                    val selectedBoatId = state.selectedBoatId
                        ?.takeIf { selectedId -> boats.any { boat -> boat.id == selectedId } }

                    state.copy(
                        availableBoats = boats,
                        selectedBoatId = selectedBoatId,
                    ).withSeatValidation()
                }
                refreshCrewOrdering()
            }
        }
    }

    private fun observeDestinations() {
        viewModelScope.launch {
            destinationRepository.observeDestinations().collect { destinations ->
                uiStateMutable.update { state ->
                    val selectedDestinationId = state.selectedDestinationId
                        ?.takeIf { selectedId -> destinations.any { destination -> destination.id == selectedId } }

                    val currentDestinationName = when {
                        state.isCustomDestination -> state.destination
                        selectedDestinationId != null -> destinations.firstOrNull { it.id == selectedDestinationId }?.name.orEmpty()
                        else -> ""
                    }

                    state.copy(
                        availableDestinations = destinations,
                        selectedDestinationId = selectedDestinationId,
                        destination = currentDestinationName,
                    )
                }
            }
        }
    }

    private fun observeRowers() {
        viewModelScope.launch {
            rowerRepository.observeRowers().collect { rowers ->
                uiStateMutable.update { state ->
                    val validSelectedIds = state.selectedRowerIds.filterTo(mutableSetOf()) { selectedId ->
                        rowers.any { rower -> rower.id == selectedId }
                    }
                    val rowerNamesById = rowers.associate { rower ->
                        rower.id to "${rower.firstName} ${rower.lastName}".trim()
                    }
                    state.copy(
                        availableRowers = rowers,
                        selectedRowerIds = validSelectedIds,
                        availableCrews = state.availableCrews.map { crew ->
                            crew.copy(
                                rowerNames = crew.rowerIds.mapNotNull(rowerNamesById::get),
                            )
                        },
                    ).withSeatValidation()
                }
                refreshCrewOrdering()
            }
        }
    }

    private fun observeUsageAndSuggestions() {
        viewModelScope.launch {
            sessionRepository.observeSessionsWithDetails().collect { sessions ->
                allSessions = sessions

                val rowerUsageCounts = buildMap<Long, Int> {
                    sessions.forEach { session ->
                        session.sessionRowers.forEach { participant ->
                            participant.sessionRower.rowerId?.let { rowerId ->
                                put(rowerId, (get(rowerId) ?: 0) + 1)
                            }
                        }
                    }
                }
                val boatUsageCounts = buildMap<Long, Int> {
                    sessions.forEach { session ->
                        val boatId = session.boat.id
                        put(boatId, (get(boatId) ?: 0) + 1)
                    }
                }

                uiStateMutable.update {
                    it.copy(
                        rowerUsageCounts = rowerUsageCounts,
                        boatUsageCounts = boatUsageCounts,
                    )
                }
                refreshCrewOrdering()
            }
        }
    }

    private fun observeCrewPreferences() {
        viewModelScope.launch {
            appPreferencesStore.preferencesFlow.collect { preferences ->
                uiStateMutable.update {
                    it.copy(crewsEnabled = preferences.crewsEnabled)
                }
            }
        }
    }

    private fun observeCrews() {
        viewModelScope.launch {
            crewStore.crewsFlow.collect { crews ->
                val rowerNamesById = uiState.value.availableRowers.associate { rower ->
                    rower.id to "${rower.firstName} ${rower.lastName}".trim()
                }
                uiStateMutable.update {
                    it.copy(
                        availableCrews = crews.map { crew ->
                            CrewSummaryUi(
                                id = crew.id,
                                name = crew.name,
                                rowerNames = crew.rowerIds.mapNotNull(rowerNamesById::get),
                                rowerIds = crew.rowerIds.toSet(),
                            )
                        },
                    )
                }
            }
        }
    }

    private fun observeBoatStatuses() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                boatRepository.observeBoats(),
                remarkRepository.observeRemarks(),
                sessionRepository.observeSessionsWithDetailsByStatus(SessionStatus.ONGOING),
            ) { boats, remarks, ongoingSessions ->
                ongoingSessionsByBoatId = ongoingSessions.associateBy { it.boat.id }
                boats.associate { boat ->
                    boat.id to resolveBoatSelectionStatus(
                        boatId = boat.id,
                        remarks = remarks,
                        ongoingSessions = ongoingSessions,
                    )
                }
            }.collect { statuses ->
                uiStateMutable.update { it.copy(boatStatuses = statuses) }
            }
        }
    }

    private fun applyBoatSelection(boatId: Long) {
        uiStateMutable.update {
            val updated = it.copy(
                selectedBoatId = boatId,
                boatConflict = null,
                errorMessage = null,
                successMessage = null,
            )
            updated.withSeatValidation()
        }
        refreshCrewOrdering()
    }

    private fun refreshCrewOrdering() {
        uiStateMutable.update { state ->
            state.copy(
                boatRowerCounts = buildBoatRowerCounts(state, allSessions),
            )
        }
    }

    private fun buildBoatRowerCounts(
        state: NewSessionUiState,
        sessions: List<SessionWithDetails>,
    ): Map<Long, Int> {
        val selectedBoatId = state.selectedBoatId ?: return emptyMap()
        if (state.availableRowers.isEmpty()) return emptyMap()

        val matchingSessions = sessions.filter { session ->
            session.boat.id == selectedBoatId &&
                session.session.status == SessionStatus.COMPLETED &&
                session.sessionRowers.any { it.sessionRower.rowerId != null }
        }
        val counts = linkedMapOf<Long, Int>()
        matchingSessions.forEach { session ->
            session.sessionRowers
                .mapNotNull { it.sessionRower.rowerId }
                .forEach { rowerId ->
                    counts[rowerId] = (counts[rowerId] ?: 0) + 1
                }
        }

        val selectedBoatName = state.availableBoats.firstOrNull { it.id == selectedBoatId }?.name ?: "bateau inconnu"
        val topFiveRowers = counts.entries
            .sortedByDescending { it.value }
            .take(5)
            .joinToString { entry ->
                val rowerName = state.availableRowers.firstOrNull { it.id == entry.key }?.let { rower ->
                    "${rower.firstName} ${rower.lastName}".trim()
                } ?: "rameur ${entry.key}"
                "$rowerName=${entry.value}"
            }
            .ifBlank { "aucun rameur" }

        Log.d(
            CrewOrderingLogTag,
            "selectedBoat=$selectedBoatName($selectedBoatId), sessionsFound=${matchingSessions.size}, top5=$topFiveRowers",
        )

        return counts
    }

    private fun NewSessionUiState.withSeatValidation(): NewSessionUiState {
        return if (!isSeatCountValid) {
            copy(
                errorMessage = selectedBoat?.let { boat ->
                    "Ce bateau a ${boat.seatCount} places, vous ne pouvez donc pas sélectionner $totalSelectedRowers rameurs."
                } ?: "Le nombre de rameurs sélectionnés dépasse le nombre de places disponibles pour ce bateau.",
                successMessage = null,
                savedSessionStatus = null,
            )
        } else {
            copy(errorMessage = null, savedSessionStatus = null)
        }
    }

    private fun NewSessionUiState.resetPreservingSources(editingSessionId: Long?): NewSessionUiState {
        return NewSessionUiState(
            editingSessionId = editingSessionId,
            availableBoats = availableBoats,
            availableDestinations = availableDestinations,
            availableRowers = availableRowers,
            rowerUsageCounts = rowerUsageCounts,
            boatRowerCounts = emptyMap(),
            boatUsageCounts = boatUsageCounts,
            boatStatuses = boatStatuses,
            crewsEnabled = crewsEnabled,
            availableCrews = availableCrews,
        )
    }

    companion object {
        private const val CrewOrderingLogTag = "CrewOrdering"

        fun factory(
            boatRepository: BoatRepository,
            remarkRepository: RemarkRepository,
            destinationRepository: DestinationRepository,
            rowerRepository: RowerRepository,
            sessionRepository: SessionRepository,
            boatPhotoStorage: BoatPhotoStorage,
            appPreferencesStore: AppPreferencesStore,
            crewStore: CrewStore,
            appLogStore: AppLogStore,
            sessionId: Long? = null,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NewSessionViewModel(
                        boatRepository = boatRepository,
                        remarkRepository = remarkRepository,
                        destinationRepository = destinationRepository,
                        rowerRepository = rowerRepository,
                        sessionRepository = sessionRepository,
                        boatPhotoStorage = boatPhotoStorage,
                        appPreferencesStore = appPreferencesStore,
                        crewStore = crewStore,
                        appLogStore = appLogStore,
                        sessionId = sessionId,
                    ) as T
                }
            }
        }
    }
}

enum class BoatSelectionStatus {
    AVAILABLE,
    IN_USE,
    IN_REPAIR,
}

fun BoatSelectionStatus.label(): String {
    return when (this) {
        BoatSelectionStatus.AVAILABLE -> "● Disponible"
        BoatSelectionStatus.IN_USE -> "● En cours d'utilisation"
        BoatSelectionStatus.IN_REPAIR -> "● En réparation"
    }
}

private fun resolveBoatSelectionStatus(
    boatId: Long,
    remarks: List<RemarkEntity>,
    ongoingSessions: List<SessionWithDetails>,
): BoatSelectionStatus {
    if (remarks.any { it.boatId == boatId && it.status == RemarkStatus.REPAIR_NEEDED }) {
        return BoatSelectionStatus.IN_REPAIR
    }
    return if (ongoingSessions.any { it.boat.id == boatId }) {
        BoatSelectionStatus.IN_USE
    } else {
        BoatSelectionStatus.AVAILABLE
    }
}
