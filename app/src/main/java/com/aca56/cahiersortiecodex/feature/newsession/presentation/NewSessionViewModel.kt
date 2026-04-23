package com.aca56.cahiersortiecodex.feature.newsession.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aca56.cahiersortiecodex.data.local.entity.BoatEntity
import com.aca56.cahiersortiecodex.data.local.entity.BoatRepairEntity
import com.aca56.cahiersortiecodex.data.local.entity.DestinationEntity
import com.aca56.cahiersortiecodex.data.local.entity.RowerEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionRowerEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionStatus
import com.aca56.cahiersortiecodex.data.local.relation.SessionWithDetails
import com.aca56.cahiersortiecodex.data.repository.BoatRepairRepository
import com.aca56.cahiersortiecodex.data.repository.BoatRepository
import com.aca56.cahiersortiecodex.data.repository.DestinationRepository
import com.aca56.cahiersortiecodex.data.repository.RowerRepository
import com.aca56.cahiersortiecodex.data.repository.SessionRepository
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

data class SuggestedCrewMemberUi(
    val rowerId: Long,
    val fullName: String,
    val reason: String,
    val score: Int,
)

data class SimilarSessionSuggestionUi(
    val sessionId: Long,
    val title: String,
    val subtitle: String,
)

data class BoatConflictUi(
    val boatId: Long,
    val boatName: String,
    val activeSessionId: Long,
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
    val boatUsageCounts: Map<Long, Int> = emptyMap(),
    val boatStatuses: Map<Long, BoatSelectionStatus> = emptyMap(),
    val selectedRowerIds: Set<Long> = emptySet(),
    val rowerSearchQuery: String = "",
    val guestRowerName: String = "",
    val guestRowers: List<GuestRowerUi> = emptyList(),
    val suggestedCrew: List<SuggestedCrewMemberUi> = emptyList(),
    val similarSessionSuggestion: SimilarSessionSuggestionUi? = null,
    val boatConflict: BoatConflictUi? = null,
    val startTime: String = defaultSessionTime(),
    val endTime: String = "",
    val km: String = "",
    val remarks: String = "",
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
        get() = availableRowers.filter { rower ->
            val fullName = "${rower.firstName} ${rower.lastName}".trim()
            rowerSearchQuery.isBlank() || fullName.contains(rowerSearchQuery.trim(), ignoreCase = true)
        }

    val filteredRowerOptions: List<SearchableSelectableOption>
        get() = filteredRowers.map { rower ->
            SearchableSelectableOption(
                key = rower.id.toString(),
                label = "${rower.firstName} ${rower.lastName}".trim(),
                usageCount = rowerUsageCounts[rower.id] ?: 0,
            )
        }.sortedWith(compareByDescending<SearchableSelectableOption> { it.usageCount }.thenBy { it.label })

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
    private val boatRepairRepository: BoatRepairRepository,
    private val destinationRepository: DestinationRepository,
    private val rowerRepository: RowerRepository,
    private val sessionRepository: SessionRepository,
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
                errorMessage = null,
                successMessage = null,
                savedSessionStatus = null,
            )
        }
        refreshSuggestions()
    }

    fun resetForm() {
        uiStateMutable.update { state ->
            state.resetPreservingSources(editingSessionId = state.editingSessionId)
        }
        refreshSuggestions()
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
        val conflict = ongoingSessionsByBoatId[boatId]
        val currentState = uiState.value
        if (
            conflict != null &&
            currentState.editingSessionId != conflict.session.id &&
            currentState.selectedBoatId != boatId
        ) {
            uiStateMutable.update {
                it.copy(
                    boatConflict = BoatConflictUi(
                        boatId = boatId,
                        boatName = conflict.boat.name,
                        activeSessionId = conflict.session.id,
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
        refreshSuggestions()
    }

    fun applySuggestedCrewMember(rowerId: Long) {
        if (uiState.value.selectedRowerIds.contains(rowerId)) return
        onRowerChecked(rowerId = rowerId, checked = true)
    }

    fun applySimilarSessionSuggestion() {
        val suggestionId = uiState.value.similarSessionSuggestion?.sessionId ?: return
        val sessionWithDetails = allSessions.firstOrNull { it.session.id == suggestionId } ?: return

        uiStateMutable.update { state ->
            val baseState = state.copy(
                selectedBoatId = sessionWithDetails.boat.id,
                selectedRowerIds = sessionWithDetails.sessionRowers.mapNotNull { it.sessionRower.rowerId }.toSet(),
                guestRowers = sessionWithDetails.sessionRowers.mapNotNull { participant ->
                    participant.sessionRower.guestName?.let { guestName ->
                        GuestRowerUi(
                            localId = nextGuestId++,
                            fullName = normalizeGuestName(guestName),
                        )
                    }
                },
                selectedDestinationId = if (state.isQuickMode) null else sessionWithDetails.destination?.id,
                isCustomDestination = if (state.isQuickMode) {
                    false
                } else {
                    sessionWithDetails.destination == null && sessionWithDetails.destinationName.isNotBlank()
                },
                destination = if (state.isQuickMode) "" else sessionWithDetails.destinationName,
                errorMessage = null,
                successMessage = null,
                savedSessionStatus = null,
                boatConflict = null,
            )
            baseState.withSeatValidation()
        }
        refreshSuggestions()
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
        refreshSuggestions()
    }

    fun removeGuestRower(localId: Long) {
        uiStateMutable.update {
            it.copy(
                guestRowers = it.guestRowers.filterNot { guest -> guest.localId == localId },
                errorMessage = null,
                successMessage = null,
            ).withSeatValidation()
        }
        refreshSuggestions()
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
            }.onSuccess {
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
                refreshSuggestions()
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
                sessionRepository.getSessionWithDetails(sessionId)
            }.onSuccess { sessionWithDetails ->
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
                        errorMessage = null,
                        successMessage = null,
                        savedSessionStatus = null,
                    ).withSeatValidation()
                }
                refreshSuggestions()
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
                refreshSuggestions()
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
                    state.copy(
                        availableRowers = rowers,
                        selectedRowerIds = validSelectedIds,
                    ).withSeatValidation()
                }
                refreshSuggestions()
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
                refreshSuggestions()
            }
        }
    }

    private fun observeBoatStatuses() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                boatRepository.observeBoats(),
                boatRepairRepository.observeRepairs(),
                sessionRepository.observeSessionsWithDetailsByStatus(SessionStatus.ONGOING),
            ) { boats, repairs, ongoingSessions ->
                ongoingSessionsByBoatId = ongoingSessions.associateBy { it.boat.id }
                boats.associate { boat ->
                    boat.id to resolveBoatSelectionStatus(
                        boatId = boat.id,
                        repairs = repairs,
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
        refreshSuggestions()
    }

    private fun refreshSuggestions() {
        val history = allSessions.filter { it.session.status != SessionStatus.ONGOING }
        uiStateMutable.update { state ->
            state.copy(
                suggestedCrew = buildSuggestedCrew(state, history),
                similarSessionSuggestion = buildSimilarSessionSuggestion(state, history),
            )
        }
    }

    private fun buildSuggestedCrew(
        state: NewSessionUiState,
        sessions: List<SessionWithDetails>,
    ): List<SuggestedCrewMemberUi> {
        if (state.availableRowers.isEmpty()) return emptyList()

        val scores = mutableMapOf<Long, Int>()
        var boatSuggestionUsed = false
        var crewSuggestionUsed = false

        state.selectedBoatId?.let { selectedBoatId ->
            sessions.filter { it.boat.id == selectedBoatId }.forEach { session ->
                session.sessionRowers.mapNotNull { it.sessionRower.rowerId }.forEach { rowerId ->
                    scores[rowerId] = (scores[rowerId] ?: 0) + 3
                }
            }
            boatSuggestionUsed = true
        }

        if (state.selectedRowerIds.isNotEmpty()) {
            sessions.filter { session ->
                val rowerIds = session.sessionRowers.mapNotNull { it.sessionRower.rowerId }.toSet()
                rowerIds.any { it in state.selectedRowerIds }
            }.forEach { session ->
                session.sessionRowers.mapNotNull { it.sessionRower.rowerId }.forEach { rowerId ->
                    scores[rowerId] = (scores[rowerId] ?: 0) + 2
                }
            }
            crewSuggestionUsed = true
        }

        return state.availableRowers.mapNotNull { rower ->
            val fullName = "${rower.firstName} ${rower.lastName}".trim()
            val score = scores[rower.id] ?: 0
            if (score <= 0 || rower.id in state.selectedRowerIds) {
                null
            } else {
                SuggestedCrewMemberUi(
                    rowerId = rower.id,
                    fullName = fullName,
                    reason = when {
                        boatSuggestionUsed && crewSuggestionUsed -> "Souvent utilise avec ce bateau et cet equipage"
                        boatSuggestionUsed -> "Souvent utilise avec ce bateau"
                        crewSuggestionUsed -> "Souvent associe a cet equipage"
                        else -> "Suggestion"
                    },
                    score = score,
                )
            }
        }.sortedWith(compareByDescending<SuggestedCrewMemberUi> { it.score }.thenBy { it.fullName }).take(5)
    }

    private fun buildSimilarSessionSuggestion(
        state: NewSessionUiState,
        sessions: List<SessionWithDetails>,
    ): SimilarSessionSuggestionUi? {
        val candidate = sessions
            .sortedWith(compareByDescending<SessionWithDetails> { it.session.date }.thenByDescending { it.session.startTime })
            .firstOrNull { session ->
                val sessionRowerIds = session.sessionRowers.mapNotNull { it.sessionRower.rowerId }.toSet()
                when {
                    state.selectedBoatId != null && state.selectedRowerIds.isNotEmpty() ->
                        session.boat.id == state.selectedBoatId && sessionRowerIds.intersect(state.selectedRowerIds).isNotEmpty()
                    state.selectedBoatId != null -> session.boat.id == state.selectedBoatId
                    state.selectedRowerIds.isNotEmpty() -> sessionRowerIds.intersect(state.selectedRowerIds).isNotEmpty()
                    else -> false
                }
            } ?: return null

        return SimilarSessionSuggestionUi(
            sessionId = candidate.session.id,
            title = "Reprendre la derniere sortie similaire",
            subtitle = "${candidate.boat.name} - ${candidate.rowerNames.joinToString().ifBlank { "Aucun rameur" }}",
        )
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
            boatUsageCounts = boatUsageCounts,
            boatStatuses = boatStatuses,
        )
    }

    companion object {
        fun factory(
            boatRepository: BoatRepository,
            boatRepairRepository: BoatRepairRepository,
            destinationRepository: DestinationRepository,
            rowerRepository: RowerRepository,
            sessionRepository: SessionRepository,
            sessionId: Long? = null,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NewSessionViewModel(
                        boatRepository = boatRepository,
                        boatRepairRepository = boatRepairRepository,
                        destinationRepository = destinationRepository,
                        rowerRepository = rowerRepository,
                        sessionRepository = sessionRepository,
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
        BoatSelectionStatus.AVAILABLE -> "Disponible"
        BoatSelectionStatus.IN_USE -> "En cours"
        BoatSelectionStatus.IN_REPAIR -> "En reparation"
    }
}

private fun resolveBoatSelectionStatus(
    boatId: Long,
    repairs: List<BoatRepairEntity>,
    ongoingSessions: List<SessionWithDetails>,
): BoatSelectionStatus {
    if (repairs.any { it.boatId == boatId && it.repairedAt.isNullOrBlank() }) {
        return BoatSelectionStatus.IN_REPAIR
    }
    return if (ongoingSessions.any { it.boat.id == boatId }) {
        BoatSelectionStatus.IN_USE
    } else {
        BoatSelectionStatus.AVAILABLE
    }
}
