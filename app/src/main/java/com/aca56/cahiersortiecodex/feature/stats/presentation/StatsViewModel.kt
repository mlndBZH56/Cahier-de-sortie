package com.aca56.cahiersortiecodex.feature.stats.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aca56.cahiersortiecodex.data.local.relation.SessionRowerWithRower
import com.aca56.cahiersortiecodex.data.local.relation.SessionWithDetails
import com.aca56.cahiersortiecodex.data.repository.SessionRepository
import com.aca56.cahiersortiecodex.ui.components.SearchableSelectableOption
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatLineUi(
    val key: String,
    val label: String,
    val totalSessions: Int,
    val totalKm: Double,
)

data class RowerSessionItemUi(
    val id: Long,
    val date: String,
    val boatName: String,
    val km: Double,
)

data class RowerStatUi(
    val key: String,
    val label: String,
    val totalSessions: Int,
    val totalKm: Double,
    val sessions: List<RowerSessionItemUi>,
)

data class GlobalStatsUi(
    val totalSessions: Int = 0,
    val totalKm: Double = 0.0,
    val ongoingSessions: Int = 0,
    val completedSessions: Int = 0,
)

enum class StatsQuickPeriod {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    CUSTOM,
}

data class StatsUiState(
    val globalStats: GlobalStatsUi = GlobalStatsUi(),
    val rowerStats: List<RowerStatUi> = emptyList(),
    val boatStats: List<StatLineUi> = emptyList(),
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val selectedQuickPeriod: StatsQuickPeriod = StatsQuickPeriod.CUSTOM,
    val rowerSearchQuery: String = "",
    val boatSearchQuery: String = "",
    val selectedRowerKeys: Set<String> = emptySet(),
    val selectedBoatKeys: Set<String> = emptySet(),
    val availableRowerOptions: List<SearchableSelectableOption> = emptyList(),
    val availableBoatOptions: List<SearchableSelectableOption> = emptyList(),
) {
    val filteredRowerOptions: List<SearchableSelectableOption>
        get() = availableRowerOptions.filter { option ->
            rowerSearchQuery.isBlank() || option.label.contains(rowerSearchQuery.trim(), ignoreCase = true)
        }

    val filteredBoatOptions: List<SearchableSelectableOption>
        get() = availableBoatOptions.filter { option ->
            boatSearchQuery.isBlank() || option.label.contains(boatSearchQuery.trim(), ignoreCase = true)
        }
}

class StatsViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val uiStateMutable = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = uiStateMutable.asStateFlow()

    private var allSessions: List<SessionWithDetails> = emptyList()

    init {
        observeStats()
    }

    fun onRowerSearchQueryChanged(value: String) {
        uiStateMutable.update { it.copy(rowerSearchQuery = value) }
    }

    fun onDateFromChanged(value: String?) {
        uiStateMutable.update { state ->
            val adjustedTo = if (
                value != null &&
                state.dateTo != null &&
                state.dateTo < value
            ) {
                value
            } else {
                state.dateTo
            }
            state.copy(
                dateFrom = value,
                dateTo = adjustedTo,
                selectedQuickPeriod = StatsQuickPeriod.CUSTOM,
            )
        }
        recomputeStats()
    }

    fun onDateToChanged(value: String?) {
        uiStateMutable.update { state ->
            val adjustedFrom = if (
                value != null &&
                state.dateFrom != null &&
                state.dateFrom > value
            ) {
                value
            } else {
                state.dateFrom
            }
            state.copy(
                dateFrom = adjustedFrom,
                dateTo = value,
                selectedQuickPeriod = StatsQuickPeriod.CUSTOM,
            )
        }
        recomputeStats()
    }

    fun onQuickPeriodSelected(period: StatsQuickPeriod) {
        val range = when (period) {
            StatsQuickPeriod.TODAY -> currentDateRange()
            StatsQuickPeriod.THIS_WEEK -> currentWeekRange()
            StatsQuickPeriod.THIS_MONTH -> currentMonthRange()
            StatsQuickPeriod.CUSTOM -> null to null
        }
        uiStateMutable.update {
            it.copy(
                selectedQuickPeriod = period,
                dateFrom = range.first,
                dateTo = range.second,
            )
        }
        recomputeStats()
    }

    fun onBoatSearchQueryChanged(value: String) {
        uiStateMutable.update { it.copy(boatSearchQuery = value) }
    }

    fun onRowerToggled(key: String) {
        uiStateMutable.update { state ->
            val updatedKeys = state.selectedRowerKeys.toMutableSet()
            if (!updatedKeys.add(key)) {
                updatedKeys.remove(key)
            }
            state.copy(selectedRowerKeys = updatedKeys)
        }
        recomputeStats()
    }

    fun onBoatToggled(key: String) {
        uiStateMutable.update { state ->
            val updatedKeys = state.selectedBoatKeys.toMutableSet()
            if (!updatedKeys.add(key)) {
                updatedKeys.remove(key)
            }
            state.copy(selectedBoatKeys = updatedKeys)
        }
        recomputeStats()
    }

    private fun observeStats() {
        viewModelScope.launch {
            sessionRepository.observeSessionsWithDetails().collect { sessions ->
                allSessions = sessions
                recomputeStats()
            }
        }
    }

    private fun recomputeStats() {
        val state = uiState.value
        val periodFilteredSessions = allSessions.filter { session ->
            val dateMatchesFrom = state.dateFrom == null || session.session.date >= state.dateFrom
            val dateMatchesTo = state.dateTo == null || session.session.date <= state.dateTo
            dateMatchesFrom && dateMatchesTo
        }
        val availableRowerOptions = periodFilteredSessions.toAvailableRowerOptions()
        val availableBoatOptions = periodFilteredSessions.toAvailableBoatOptions()
        val validSelectedRowerKeys = state.selectedRowerKeys.filterTo(mutableSetOf()) { selectedKey ->
            availableRowerOptions.any { option -> option.key == selectedKey }
        }
        val validSelectedBoatKeys = state.selectedBoatKeys.filterTo(mutableSetOf()) { selectedKey ->
            availableBoatOptions.any { option -> option.key == selectedKey }
        }
        val filteredSessions = periodFilteredSessions.filter { session ->
            val rowerMatches = validSelectedRowerKeys.isEmpty() || session.sessionRowers.any { participant ->
                participant.statKey in validSelectedRowerKeys
            }
            val boatMatches = validSelectedBoatKeys.isEmpty() || session.boat.statKey in validSelectedBoatKeys
            rowerMatches && boatMatches
        }

        uiStateMutable.update { current ->
            current.copy(
                selectedRowerKeys = validSelectedRowerKeys,
                selectedBoatKeys = validSelectedBoatKeys,
                globalStats = filteredSessions.toGlobalStats(),
                rowerStats = filteredSessions.toRowerStats()
                    .filter { stat -> validSelectedRowerKeys.isEmpty() || stat.key in validSelectedRowerKeys },
                boatStats = filteredSessions.toBoatStats()
                    .filter { stat -> validSelectedBoatKeys.isEmpty() || stat.key in validSelectedBoatKeys },
                availableRowerOptions = availableRowerOptions,
                availableBoatOptions = availableBoatOptions,
            )
        }
    }

    private fun List<SessionWithDetails>.toGlobalStats(): GlobalStatsUi {
        return GlobalStatsUi(
            totalSessions = size,
            totalKm = sumOf { it.session.km },
            ongoingSessions = count { it.session.status.name == "ONGOING" },
            completedSessions = count { it.session.status.name == "COMPLETED" },
        )
    }

    private fun List<SessionWithDetails>.toRowerStats(): List<RowerStatUi> {
        return flatMap { session ->
            session.sessionRowers.map { participant ->
                RowerSessionContribution(
                    key = participant.statKey,
                    label = participant.displayName,
                    session = RowerSessionItemUi(
                        id = session.session.id,
                        date = session.session.date,
                        boatName = session.boat.name,
                        km = session.session.km,
                    ),
                )
            }
        }.groupBy { it.key }
            .values
            .map { entries ->
                RowerStatUi(
                    key = entries.first().key,
                    label = entries.first().label,
                    totalSessions = entries.size,
                    totalKm = entries.sumOf { it.session.km },
                    sessions = entries
                        .map { it.session }
                        .sortedWith(
                            compareByDescending<RowerSessionItemUi> { it.date }
                                .thenBy { it.boatName },
                        ),
                )
            }
            .sortedBy { it.label }
    }

    private fun List<SessionWithDetails>.toBoatStats(): List<StatLineUi> {
        return groupBy { it.boat.statKey }
            .values
            .map { boatEntries ->
                StatLineUi(
                    key = boatEntries.first().boat.statKey,
                    label = boatEntries.first().boat.name,
                    totalSessions = boatEntries.size,
                    totalKm = boatEntries.sumOf { it.session.km },
                )
            }
            .sortedBy { it.label }
    }

    private fun List<SessionWithDetails>.toAvailableRowerOptions(): List<SearchableSelectableOption> {
        return flatMap { session ->
            session.sessionRowers.map { participant ->
                SearchableSelectableOption(
                    key = participant.statKey,
                    label = participant.displayName,
                    usageCount = 1,
                )
            }
        }.groupBy { it.key }
            .values
            .map { entries ->
                SearchableSelectableOption(
                    key = entries.first().key,
                    label = entries.first().label,
                    usageCount = entries.sumOf { it.usageCount },
                )
            }
            .sortedWith(compareByDescending<SearchableSelectableOption> { it.usageCount }.thenBy { it.label })
    }

    private fun List<SessionWithDetails>.toAvailableBoatOptions(): List<SearchableSelectableOption> {
        return map { session ->
            SearchableSelectableOption(
                key = session.boat.statKey,
                label = session.boat.name,
                usageCount = 1,
            )
        }.groupBy { it.key }
            .values
            .map { entries ->
                SearchableSelectableOption(
                    key = entries.first().key,
                    label = entries.first().label,
                    usageCount = entries.sumOf { it.usageCount },
                )
            }
            .sortedWith(compareByDescending<SearchableSelectableOption> { it.usageCount }.thenBy { it.label })
    }

    private val SessionRowerWithRower.statKey: String
        get() = sessionRower.rowerId?.let { "rower:$it" }
            ?: "guest:${displayName.trim().lowercase()}"

    private val com.aca56.cahiersortiecodex.data.local.entity.BoatEntity.statKey: String
        get() = "boat:$id"

    private data class RowerSessionContribution(
        val key: String,
        val label: String,
        val session: RowerSessionItemUi,
    )

    companion object {
        fun factory(
            sessionRepository: SessionRepository,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StatsViewModel(
                        sessionRepository = sessionRepository,
                    ) as T
                }
            }
        }
    }
}

private fun currentDateRange(): Pair<String, String> {
    val today = storageDate(Date())
    return today to today
}

private fun currentWeekRange(): Pair<String, String> {
    val calendar = Calendar.getInstance()
    calendar.firstDayOfWeek = Calendar.MONDAY
    while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        calendar.add(Calendar.DAY_OF_MONTH, -1)
    }
    val start = storageDate(calendar.time)
    calendar.add(Calendar.DAY_OF_MONTH, 6)
    val end = storageDate(calendar.time)
    return start to end
}

private fun currentMonthRange(): Pair<String, String> {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val start = storageDate(calendar.time)
    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
    val end = storageDate(calendar.time)
    return start to end
}

private fun storageDate(date: Date): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
}
