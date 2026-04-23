package com.aca56.cahiersortiecodex.feature.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aca56.cahiersortiecodex.data.local.relation.SessionWithDetails
import com.aca56.cahiersortiecodex.data.repository.SessionRepository
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialogType
import com.aca56.cahiersortiecodex.ui.components.SearchableSelectableOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistorySessionUi(
    val id: Long,
    val date: String,
    val boatId: Long,
    val boatName: String,
    val participants: List<HistoryParticipantUi>,
    val destination: String,
    val startTime: String,
    val endTime: String,
    val km: String,
    val remarks: String,
    val status: String,
) {
    val rowerNames: List<String>
        get() = participants.map { it.name }

    val participantLabels: List<String>
        get() = participants.map { participant ->
            if (participant.isGuest) {
                "${participant.name} (Invité)"
            } else {
                participant.name
            }
        }
}

data class HistoryParticipantUi(
    val name: String,
    val isGuest: Boolean,
)

data class HistoryUiState(
    val allSessions: List<HistorySessionUi> = emptyList(),
    val isLoaded: Boolean = false,
    val selectedRowers: Set<String> = emptySet(),
    val rowerSearchQuery: String = "",
    val selectedBoatId: Long? = null,
    val dateFromFilter: String = "",
    val dateToFilter: String = "",
    val selectedDestination: String? = null,
    val message: String? = null,
    val messageType: FeedbackDialogType? = null,
) {
    val availableRowerOptions: List<SearchableSelectableOption>
        get() = allSessions
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

    val filteredAvailableRowerOptions: List<SearchableSelectableOption>
        get() = availableRowerOptions.filter { option ->
            rowerSearchQuery.isBlank() || option.label.contains(rowerSearchQuery.trim(), ignoreCase = true)
        }

    val availableBoatOptions: List<SearchableSelectableOption>
        get() = allSessions
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

    val availableDestinationOptions: List<SearchableSelectableOption>
        get() = allSessions
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

    val filteredSessions: List<HistorySessionUi>
        get() = allSessions.filter { session ->
            val rowerMatches = selectedRowers.isEmpty() || session.rowerNames.any { it in selectedRowers }
            val boatMatches = selectedBoatId == null || session.boatId == selectedBoatId
            val fromDateMatches = dateFromFilter.isBlank() || session.date >= dateFromFilter.trim()
            val toDateMatches = dateToFilter.isBlank() || session.date <= dateToFilter.trim()
            val destinationMatches = selectedDestination.isNullOrBlank() || session.destination == selectedDestination

            rowerMatches && boatMatches && fromDateMatches && toDateMatches && destinationMatches
        }
}

class HistoryViewModel(
    private val sessionRepository: SessionRepository,
    private val initialBoatId: Long? = null,
) : ViewModel() {
    private val uiStateMutable = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = uiStateMutable.asStateFlow()

    init {
        observeSessions()
    }

    fun onRowerSearchChanged(value: String) {
        uiStateMutable.update {
            it.copy(rowerSearchQuery = value, message = null, messageType = null)
        }
    }

    fun onRowerToggled(rower: String) {
        uiStateMutable.update { state ->
            val updated = state.selectedRowers.toMutableSet()
            if (!updated.add(rower)) {
                updated.remove(rower)
            }
            state.copy(selectedRowers = updated, message = null, messageType = null)
        }
    }

    fun onBoatSelected(boatId: Long?) {
        uiStateMutable.update {
            it.copy(selectedBoatId = boatId, message = null, messageType = null)
        }
    }

    fun onDateFromFilterChanged(value: String) {
        uiStateMutable.update {
            it.copy(dateFromFilter = value, message = null, messageType = null)
        }
    }

    fun onDateToFilterChanged(value: String) {
        uiStateMutable.update {
            it.copy(dateToFilter = value, message = null, messageType = null)
        }
    }

    fun onDestinationFilterChanged(value: String?) {
        uiStateMutable.update {
            it.copy(selectedDestination = value, message = null, messageType = null)
        }
    }

    fun clearFilters() {
        uiStateMutable.update {
            it.copy(
                selectedRowers = emptySet(),
                rowerSearchQuery = "",
                selectedBoatId = null,
                dateFromFilter = "",
                dateToFilter = "",
                selectedDestination = null,
                message = null,
                messageType = null,
            )
        }
    }

    fun clearMessage() {
        uiStateMutable.update {
            it.copy(
                message = null,
                messageType = null,
            )
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            runCatching {
                val sessionWithDetails = sessionRepository.getSessionWithDetails(sessionId)
                    ?: error("Session introuvable.")
                sessionRepository.deleteSession(sessionWithDetails.session)
            }.onSuccess {
                uiStateMutable.update {
                    it.copy(
                        message = "Session supprimée.",
                        messageType = FeedbackDialogType.SUCCESS,
                    )
                }
            }.onFailure {
                uiStateMutable.update {
                    it.copy(
                        message = "La session n'a pas pu être supprimée. Veuillez réessayer.",
                        messageType = FeedbackDialogType.ERROR,
                    )
                }
            }
        }
    }

    private fun observeSessions() {
        viewModelScope.launch {
            sessionRepository.observeSessionsWithDetails().collect { sessions ->
                uiStateMutable.update { state ->
                    val mapped = sessions.map { it.toUi() }
                    val availableRowers = mapped.flatMap { it.rowerNames }.distinct().toSet()
                    val availableBoatIds = mapped.map { it.boatId }.toSet()
                    val availableDestinations = mapped.mapNotNull { it.destination.takeIf(String::isNotBlank) }.toSet()
                    state.copy(
                        allSessions = mapped,
                        isLoaded = true,
                        selectedBoatId = when {
                            state.selectedBoatId != null && state.selectedBoatId in availableBoatIds -> state.selectedBoatId
                            initialBoatId != null && initialBoatId in availableBoatIds -> initialBoatId
                            else -> null
                        },
                        selectedRowers = state.selectedRowers.filterTo(mutableSetOf()) { selectedRower ->
                            selectedRower in availableRowers
                        },
                        selectedDestination = state.selectedDestination?.takeIf { it in availableDestinations },
                    )
                }
            }
        }
    }

    fun sessionById(sessionId: Long): HistorySessionUi? {
        return uiState.value.allSessions.firstOrNull { it.id == sessionId }
    }

    private fun SessionWithDetails.toUi(): HistorySessionUi {
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
            }.sortedBy { it.name },
            destination = destinationName,
            startTime = session.startTime,
            endTime = session.endTime.orEmpty(),
            km = if (session.km == 0.0) "" else session.km.toString(),
            remarks = session.remarks.orEmpty(),
            status = session.status.toDisplayLabel(),
        )
    }

    companion object {
        fun factory(
            sessionRepository: SessionRepository,
            initialBoatId: Long? = null,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HistoryViewModel(
                        sessionRepository = sessionRepository,
                        initialBoatId = initialBoatId,
                    ) as T
                }
            }
        }
    }
}

private fun com.aca56.cahiersortiecodex.data.local.entity.SessionStatus.toDisplayLabel(): String {
    return when (this) {
        com.aca56.cahiersortiecodex.data.local.entity.SessionStatus.ONGOING -> "EN COURS"
        com.aca56.cahiersortiecodex.data.local.entity.SessionStatus.COMPLETED -> "TERMINÉE"
        com.aca56.cahiersortiecodex.data.local.entity.SessionStatus.NOT_COMPLETED -> "NON TERMINÉE"
    }
}
