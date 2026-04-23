package com.aca56.cahiersortiecodex.feature.ongoingsessions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aca56.cahiersortiecodex.data.local.entity.DestinationEntity
import com.aca56.cahiersortiecodex.data.local.entity.SessionStatus
import com.aca56.cahiersortiecodex.data.local.relation.SessionWithDetails
import com.aca56.cahiersortiecodex.data.repository.DestinationRepository
import com.aca56.cahiersortiecodex.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OngoingSessionItemUi(
    val id: Long,
    val date: String,
    val boatId: Long,
    val boatName: String,
    val rowerNames: List<String>,
    val startTime: String,
    val destination: String,
    val endTime: String,
    val km: String,
    val remarks: String,
)

data class OngoingSessionsUiState(
    val sessions: List<OngoingSessionItemUi> = emptyList(),
    val openedSessionId: Long? = null,
    val isBulkSelectionMode: Boolean = false,
    val isBulkCompletionEditorOpen: Boolean = false,
    val selectedSessionIds: Set<Long> = emptySet(),
    val availableDestinations: List<DestinationEntity> = emptyList(),
    val selectedDestinationId: Long? = null,
    val isCustomDestination: Boolean = false,
    val destination: String = "",
    val endTime: String = "",
    val km: String = "",
    val remarks: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    private fun normalizedKmValue(): String? = km.trim().replace(',', '.').takeIf { it.isNotBlank() }

    val openedSession: OngoingSessionItemUi?
        get() = sessions.firstOrNull { it.id == openedSessionId }

    val selectedSessions: List<OngoingSessionItemUi>
        get() = sessions.filter { it.id in selectedSessionIds }

    val canComplete: Boolean
        get() = !isSaving &&
            openedSessionId != null &&
            endTime.isNotBlank() &&
            (normalizedKmValue() == null || normalizedKmValue()?.toDoubleOrNull() != null) &&
            openedSession != null

    val canBulkComplete: Boolean
        get() = !isSaving &&
            selectedSessionIds.isNotEmpty() &&
            endTime.isNotBlank() &&
            (normalizedKmValue() == null || normalizedKmValue()?.toDoubleOrNull() != null)
}

class OngoingSessionsViewModel(
    private val sessionRepository: SessionRepository,
    private val destinationRepository: DestinationRepository,
) : ViewModel() {
    private val uiStateMutable = MutableStateFlow(OngoingSessionsUiState())
    val uiState: StateFlow<OngoingSessionsUiState> = uiStateMutable.asStateFlow()

    init {
        observeOngoingSessions()
        observeDestinations()
    }

    fun clearFeedback() {
        uiStateMutable.update {
            it.copy(errorMessage = null, successMessage = null)
        }
    }

    fun toggleBulkSelectionMode() {
        uiStateMutable.update { state ->
            if (state.isBulkSelectionMode) {
                state.copy(
                    isBulkSelectionMode = false,
                    isBulkCompletionEditorOpen = false,
                    selectedSessionIds = emptySet(),
                    selectedDestinationId = null,
                    isCustomDestination = false,
                    destination = "",
                    endTime = "",
                    km = "",
                    remarks = "",
                    errorMessage = null,
                    successMessage = null,
                )
            } else {
                state.copy(
                    isBulkSelectionMode = true,
                    isBulkCompletionEditorOpen = false,
                    openedSessionId = null,
                    selectedSessionIds = emptySet(),
                    selectedDestinationId = null,
                    isCustomDestination = false,
                    destination = "",
                    endTime = "",
                    km = "",
                    remarks = "",
                    errorMessage = null,
                    successMessage = null,
                )
            }
        }
    }

    fun toggleSessionSelection(sessionId: Long) {
        uiStateMutable.update { state ->
            if (!state.isBulkSelectionMode) return@update state
            val updatedSelection = state.selectedSessionIds.toMutableSet().apply {
                if (!add(sessionId)) remove(sessionId)
            }
            state.copy(
                selectedSessionIds = updatedSelection,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun openBulkCompletionEditor() {
        uiStateMutable.update { state ->
            when {
                !state.isBulkSelectionMode -> state.copy(
                    errorMessage = "Activez d'abord la sélection multiple.",
                    successMessage = null,
                )
                state.selectedSessionIds.isEmpty() -> state.copy(
                    errorMessage = "Sélectionnez au moins une session avant de continuer.",
                    successMessage = null,
                )
                else -> state.copy(
                    isBulkCompletionEditorOpen = true,
                    errorMessage = null,
                    successMessage = null,
                )
            }
        }
    }

    fun closeBulkCompletionEditor() {
        uiStateMutable.update {
            it.copy(
                isBulkCompletionEditorOpen = false,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun openSession(sessionId: Long) {
        viewModelScope.launch {
            if (uiState.value.isBulkSelectionMode) return@launch
            val sessionWithDetails = sessionRepository.getSessionWithDetails(sessionId)
            if (sessionWithDetails == null) {
                uiStateMutable.update {
                    it.copy(
                        errorMessage = "La session sélectionnée est introuvable.",
                        successMessage = null,
                    )
                }
                return@launch
            }
            uiStateMutable.update { state ->
                state.copy(
                    openedSessionId = sessionId,
                ).fillFromSession(sessionWithDetails)
            }
        }
    }

    fun closeSessionDetails() {
        uiStateMutable.update {
            it.copy(
                openedSessionId = null,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun onEndTimeChanged(value: String) {
        uiStateMutable.update { it.copy(endTime = value, errorMessage = null, successMessage = null) }
    }

    fun onKmChanged(value: String) {
        uiStateMutable.update { it.copy(km = value, errorMessage = null, successMessage = null) }
    }

    fun onRemarksChanged(value: String) {
        uiStateMutable.update { it.copy(remarks = value, errorMessage = null, successMessage = null) }
    }

    fun onDestinationSelected(destinationId: Long?) {
        uiStateMutable.update {
            it.copy(
                selectedDestinationId = destinationId,
                isCustomDestination = false,
                destination = it.availableDestinations.firstOrNull { destination -> destination.id == destinationId }?.name.orEmpty(),
                errorMessage = null,
                successMessage = null,
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
            )
        }
    }

    fun onDestinationChanged(value: String) {
        uiStateMutable.update { it.copy(destination = value, errorMessage = null, successMessage = null) }
    }

    fun completeOpenedSession() {
        val state = uiState.value
        val openedSessionId = state.openedSessionId ?: run {
            uiStateMutable.update { it.copy(errorMessage = "Veuillez ouvrir une session avant d'essayer de la terminer.") }
            return
        }
        val openedSession = state.openedSession ?: run {
            uiStateMutable.update { it.copy(errorMessage = "La session sélectionnée est introuvable.") }
            return
        }
        val parsedKm = state.km.trim().replace(',', '.').takeIf { it.isNotBlank() }?.toDoubleOrNull() ?: 0.0

        when {
            state.endTime.isBlank() -> {
                uiStateMutable.update { it.copy(errorMessage = "Veuillez indiquer une heure de fin.") }
                return
            }
            state.endTime <= openedSession.startTime -> {
                uiStateMutable.update { it.copy(errorMessage = "L'heure de fin doit être postérieure à l'heure de départ avant de terminer la session.") }
                return
            }
            parsedKm < 0.0 -> {
                uiStateMutable.update { it.copy(errorMessage = "Les kilomètres ne peuvent pas être négatifs. Veuillez saisir 0 ou une valeur positive.") }
                return
            }
            state.isCustomDestination && state.destination.isBlank() -> {
                uiStateMutable.update { it.copy(errorMessage = "Veuillez saisir une destination ou choisir une destination existante.") }
                return
            }
        }

        viewModelScope.launch {
            uiStateMutable.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }

            runCatching {
                val sessionWithDetails = sessionRepository.getSessionWithDetails(openedSessionId)
                    ?: error("Session introuvable.")

                val destinationId = resolveDestinationId(state)
                val updatedSession = sessionWithDetails.session.copy(
                    destinationId = destinationId,
                    endTime = state.endTime.trim(),
                    km = parsedKm,
                    remarks = state.remarks.trim().ifBlank { null },
                    status = SessionStatus.COMPLETED,
                )

                sessionRepository.updateSession(updatedSession)
            }.onSuccess {
                uiStateMutable.update {
                    it.copy(
                        openedSessionId = null,
                        selectedDestinationId = null,
                        isCustomDestination = false,
                        destination = "",
                        endTime = "",
                        km = "",
                        remarks = "",
                        isSaving = false,
                        errorMessage = null,
                        successMessage = "Modifications enregistrées.",
                    )
                }
            }.onFailure {
                uiStateMutable.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "La session n'a pas pu être mise à jour. Veuillez vérifier les champs saisis et réessayer.",
                        successMessage = null,
                    )
                }
            }
        }
    }

    fun completeSelectedSessions() {
        val state = uiState.value
        val selectedSessions = state.selectedSessions
        if (!state.isBulkSelectionMode || selectedSessions.isEmpty()) {
            uiStateMutable.update {
                it.copy(errorMessage = "Veuillez sélectionner au moins une session à terminer.")
            }
            return
        }

        val parsedKm = state.km.trim().replace(',', '.').takeIf { it.isNotBlank() }?.toDoubleOrNull() ?: 0.0

        when {
            state.endTime.isBlank() -> {
                uiStateMutable.update { it.copy(errorMessage = "Veuillez indiquer une heure de fin.") }
                return
            }
            parsedKm < 0.0 -> {
                uiStateMutable.update {
                    it.copy(errorMessage = "Les kilomètres ne peuvent pas être négatifs. Veuillez saisir 0 ou une valeur positive.")
                }
                return
            }
            state.isCustomDestination && state.destination.isBlank() -> {
                uiStateMutable.update {
                    it.copy(errorMessage = "Veuillez saisir une destination ou choisir une destination existante.")
                }
                return
            }
            selectedSessions.any { state.endTime <= it.startTime } -> {
                uiStateMutable.update {
                    it.copy(errorMessage = "L'heure de fin doit être postérieure à l'heure de départ pour toutes les sessions sélectionnées.")
                }
                return
            }
        }

        viewModelScope.launch {
            uiStateMutable.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }

            runCatching {
                val destinationId = resolveDestinationId(state)
                selectedSessions.forEach { selectedSession ->
                    val sessionWithDetails = sessionRepository.getSessionWithDetails(selectedSession.id)
                        ?: error("Session introuvable.")
                    val updatedSession = sessionWithDetails.session.copy(
                        destinationId = destinationId,
                        endTime = state.endTime.trim(),
                        km = parsedKm,
                        remarks = state.remarks.trim().ifBlank { null },
                        status = SessionStatus.COMPLETED,
                    )
                    sessionRepository.updateSession(updatedSession)
                }
            }.onSuccess {
                uiStateMutable.update {
                    it.copy(
                        isBulkSelectionMode = false,
                        isBulkCompletionEditorOpen = false,
                        selectedSessionIds = emptySet(),
                        selectedDestinationId = null,
                        isCustomDestination = false,
                        destination = "",
                        endTime = "",
                        km = "",
                        remarks = "",
                        isSaving = false,
                        errorMessage = null,
                        successMessage = "${selectedSessions.size} session(s) terminée(s).",
                    )
                }
            }.onFailure {
                uiStateMutable.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Les sessions sélectionnées n'ont pas pu être mises à jour. Veuillez vérifier les champs saisis et réessayer.",
                        successMessage = null,
                    )
                }
            }
        }
    }

    fun deleteOpenedSession() {
        val openedSessionId = uiState.value.openedSessionId ?: run {
            uiStateMutable.update { it.copy(errorMessage = "Veuillez ouvrir une session avant d'essayer de la supprimer.") }
            return
        }

        viewModelScope.launch {
            uiStateMutable.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }

            runCatching {
                val sessionWithDetails = sessionRepository.getSessionWithDetails(openedSessionId)
                    ?: error("Session introuvable.")
                sessionRepository.deleteSession(sessionWithDetails.session)
            }.onSuccess {
                closeSessionDetails()
                uiStateMutable.update {
                    it.copy(
                        isSaving = false,
                        successMessage = "Session supprimée.",
                    )
                }
            }.onFailure {
                uiStateMutable.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "La session n'a pas pu être supprimée. Veuillez réessayer.",
                        successMessage = null,
                    )
                }
            }
        }
    }

    private suspend fun resolveDestinationId(state: OngoingSessionsUiState): Long? {
        if (state.isCustomDestination) {
            val customName = state.destination.trim()
            if (customName.isBlank()) return null
            val existing = destinationRepository.getDestinationByName(customName)
            return existing?.id ?: destinationRepository.saveDestination(DestinationEntity(name = customName))
        }
        return state.selectedDestinationId
    }

    private fun observeOngoingSessions() {
        viewModelScope.launch {
            sessionRepository.observeSessionsWithDetailsByStatus(SessionStatus.ONGOING).collect { sessions ->
                uiStateMutable.update { state ->
                    val mappedSessions = sessions.map { it.toUi() }
                    val filteredSelection = state.selectedSessionIds.filterTo(mutableSetOf()) { selectedId ->
                        mappedSessions.any { it.id == selectedId }
                    }
                    val openedSession = state.openedSessionId?.let { openedId ->
                        sessions.firstOrNull { it.session.id == openedId }
                    }

                    if (state.openedSessionId != null && openedSession == null) {
                        state.copy(
                            sessions = mappedSessions,
                            openedSessionId = null,
                            isBulkCompletionEditorOpen = false,
                            selectedSessionIds = filteredSelection,
                            selectedDestinationId = null,
                            isCustomDestination = false,
                            destination = "",
                            endTime = "",
                            km = "",
                            remarks = "",
                        )
                    } else {
                        state.copy(
                            sessions = mappedSessions,
                            isBulkCompletionEditorOpen = state.isBulkCompletionEditorOpen && filteredSelection.isNotEmpty(),
                            selectedSessionIds = filteredSelection,
                        )
                    }
                }
            }
        }
    }

    private fun observeDestinations() {
        viewModelScope.launch {
            destinationRepository.observeDestinations().collect { destinations ->
                uiStateMutable.update { state ->
                    state.copy(
                        availableDestinations = destinations,
                        selectedDestinationId = state.selectedDestinationId?.takeIf { selectedId ->
                            destinations.any { it.id == selectedId }
                        },
                    )
                }
            }
        }
    }

    private fun OngoingSessionsUiState.fillFromSession(sessionWithDetails: SessionWithDetails): OngoingSessionsUiState {
        return copy(
            selectedDestinationId = sessionWithDetails.destination?.id,
            isCustomDestination = sessionWithDetails.destination == null && sessionWithDetails.destinationName.isNotBlank(),
            destination = sessionWithDetails.destinationName,
            endTime = sessionWithDetails.session.endTime.orEmpty(),
            km = if (sessionWithDetails.session.km == 0.0) "" else sessionWithDetails.session.km.toString(),
            remarks = sessionWithDetails.session.remarks.orEmpty(),
            errorMessage = null,
            successMessage = null,
        )
    }

    private fun SessionWithDetails.toUi(): OngoingSessionItemUi {
        return OngoingSessionItemUi(
            id = session.id,
            date = session.date,
            boatId = boat.id,
            boatName = boat.name,
            rowerNames = rowerNames,
            startTime = session.startTime,
            destination = destinationName,
            endTime = session.endTime.orEmpty(),
            km = if (session.km == 0.0) "" else session.km.toString(),
            remarks = session.remarks.orEmpty(),
        )
    }

    companion object {
        fun factory(
            sessionRepository: SessionRepository,
            destinationRepository: DestinationRepository,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OngoingSessionsViewModel(
                        sessionRepository = sessionRepository,
                        destinationRepository = destinationRepository,
                    ) as T
                }
            }
        }
    }
}
