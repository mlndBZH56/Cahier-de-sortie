package com.aca56.cahiersortiecodex.feature.boats.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aca56.cahiersortiecodex.data.local.entity.BoatEntity
import com.aca56.cahiersortiecodex.data.local.entity.BoatPhotoEntity
import com.aca56.cahiersortiecodex.data.local.entity.RemarkEntity
import com.aca56.cahiersortiecodex.data.local.entity.RemarkStatus
import com.aca56.cahiersortiecodex.data.local.entity.SessionStatus
import com.aca56.cahiersortiecodex.data.local.relation.SessionWithDetails
import com.aca56.cahiersortiecodex.data.media.BoatPhotoStorage
import com.aca56.cahiersortiecodex.data.repository.BoatPhotoRepository
import com.aca56.cahiersortiecodex.data.repository.BoatRepository
import com.aca56.cahiersortiecodex.data.repository.RemarkRepository
import com.aca56.cahiersortiecodex.data.repository.SessionRepository
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialogType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BoatStatusUi {
    AVAILABLE,
    IN_USE,
    IN_REPAIR,
}

data class BoatListItemUi(
    val id: Long,
    val name: String,
    val status: BoatStatusUi,
)

data class BoatSessionSummaryUi(
    val id: Long,
    val date: String,
    val rowers: List<String>,
)

data class BoatPhotoUi(
    val id: Long,
    val filePath: String,
    val createdAt: String,
)

data class BoatDetailUi(
    val id: Long = 0,
    val name: String = "",
    val seatCount: String = "",
    val type: String = "",
    val weightSingleValue: String = "",
    val weightMinValue: String = "",
    val weightMaxValue: String = "",
    val useWeightRange: Boolean = false,
    val riggingCouple: Boolean = false,
    val riggingPointe: Boolean = false,
    val year: String = "",
    val notes: String = "",
    val status: BoatStatusUi = BoatStatusUi.AVAILABLE,
    val remarks: List<RemarkEntity> = emptyList(),
    val photos: List<BoatPhotoUi> = emptyList(),
    val recentSessions: List<BoatSessionSummaryUi> = emptyList(),
) {
    val hasPersistentBoat: Boolean
        get() = id != 0L

    val weightRangeDisplay: String
        get() = when {
            useWeightRange && weightMinValue.isNotBlank() && weightMaxValue.isNotBlank() ->
                "${weightMinValue}–${weightMaxValue} kg"
            weightSingleValue.isNotBlank() -> "${weightSingleValue} kg"
            else -> "Non défini"
        }

    val riggingTypeDisplay: String
        get() = buildList {
            if (riggingCouple) add("Couple")
            if (riggingPointe) add("Pointe")
        }.ifEmpty { listOf("Non défini") }.joinToString(" / ")
}

data class BoatsUiState(
    val searchQuery: String = "",
    val boats: List<BoatListItemUi> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
    val messageType: FeedbackDialogType? = null,
) {
    val filteredBoats: List<BoatListItemUi>
        get() = boats.filter { boat ->
            searchQuery.isBlank() || boat.name.contains(searchQuery.trim(), ignoreCase = true)
        }
}

data class BoatDetailUiState(
    val isNewBoat: Boolean = true,
    val isEditMode: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val boat: BoatDetailUi = BoatDetailUi(),
    val message: String? = null,
    val messageType: FeedbackDialogType? = null,
) {
    val canSave: Boolean
        get() = boat.name.isNotBlank() && (boat.seatCount.toIntOrNull() ?: 0) > 0
}

class BoatsViewModel(
    private val boatRepository: BoatRepository,
    private val remarkRepository: RemarkRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val uiStateMutable = MutableStateFlow(BoatsUiState())
    val uiState: StateFlow<BoatsUiState> = uiStateMutable.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                boatRepository.observeBoats(),
                remarkRepository.observeRemarks(),
                sessionRepository.observeSessionsWithDetailsByStatus(SessionStatus.ONGOING),
            ) { boats, remarks, ongoingSessions ->
                boats.map { boat ->
                    BoatListItemUi(
                        id = boat.id,
                        name = boat.name,
                        status = resolveBoatStatus(
                            boatId = boat.id,
                            remarks = remarks,
                            ongoingSessions = ongoingSessions,
                        ),
                    )
                }.sortedBy { it.name.lowercase() }
            }.collect { items ->
                uiStateMutable.update {
                    it.copy(
                        boats = items,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(value: String) {
        uiStateMutable.update { it.copy(searchQuery = value) }
    }

    fun clearMessage() {
        uiStateMutable.update { it.copy(message = null, messageType = null) }
    }

    companion object {
        fun factory(
            boatRepository: BoatRepository,
            remarkRepository: RemarkRepository,
            sessionRepository: SessionRepository,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BoatsViewModel(
                        boatRepository = boatRepository,
                        remarkRepository = remarkRepository,
                        sessionRepository = sessionRepository,
                    ) as T
                }
            }
        }
    }
}

class BoatDetailViewModel(
    private val boatId: Long?,
    private val boatRepository: BoatRepository,
    private val boatPhotoRepository: BoatPhotoRepository,
    private val remarkRepository: RemarkRepository,
    private val sessionRepository: SessionRepository,
    private val boatPhotoStorage: BoatPhotoStorage,
) : ViewModel() {
    private val uiStateMutable = MutableStateFlow(
        BoatDetailUiState(
            isNewBoat = boatId == null,
            isLoading = boatId != null,
        ),
    )
    val uiState: StateFlow<BoatDetailUiState> = uiStateMutable.asStateFlow()

    init {
        if (boatId == null) {
            uiStateMutable.update { it.copy(isLoading = false) }
        } else {
            observeBoatDetails(boatId)
        }
    }

    fun onNameChanged(value: String) {
        updateBoatFields { copy(name = value) }
    }

    fun onSeatCountChanged(value: String) {
        updateBoatFields { copy(seatCount = value.filter(Char::isDigit)) }
    }

    fun onTypeChanged(value: String) {
        updateBoatFields { copy(type = value) }
    }

    fun onWeightSingleChanged(value: String) {
        updateBoatFields { copy(weightSingleValue = sanitizeWeightValue(value)) }
    }

    fun onWeightMinChanged(value: String) {
        updateBoatFields { copy(weightMinValue = sanitizeWeightValue(value)) }
    }

    fun onWeightMaxChanged(value: String) {
        updateBoatFields { copy(weightMaxValue = sanitizeWeightValue(value)) }
    }

    fun onUseWeightRangeChanged(enabled: Boolean) {
        updateBoatFields {
            if (enabled) {
                copy(
                    useWeightRange = true,
                    weightSingleValue = "",
                )
            } else {
                copy(
                    useWeightRange = false,
                    weightMinValue = "",
                    weightMaxValue = "",
                )
            }
        }
    }

    fun onRiggingCoupleChanged(enabled: Boolean) {
        updateBoatFields { copy(riggingCouple = enabled) }
    }

    fun onRiggingPointeChanged(enabled: Boolean) {
        updateBoatFields { copy(riggingPointe = enabled) }
    }

    fun onYearChanged(value: String) {
        updateBoatFields { copy(year = value.filter(Char::isDigit)) }
    }

    fun onNotesChanged(value: String) {
        updateBoatFields { copy(notes = value) }
    }

    fun startEditing() {
        uiStateMutable.update { it.copy(isEditMode = true, message = null, messageType = null) }
    }

    fun cancelEditing() {
        val currentBoatId = uiState.value.boat.id
        if (currentBoatId == 0L) {
            uiStateMutable.update { it.copy(isEditMode = false) }
            return
        }
        observeBoatDetails(currentBoatId)
    }

    fun saveBoat() {
        val state = uiState.value
        val seatCount = state.boat.seatCount.toIntOrNull()
        val minWeight = state.boat.weightMinValue.toIntOrNull()
        val maxWeight = state.boat.weightMaxValue.toIntOrNull()
        val weightRange = buildWeightRangeValue(state.boat)

        when {
            state.boat.name.isBlank() -> {
                showError("Veuillez saisir un nom de bateau.")
                return
            }
            seatCount == null || seatCount <= 0 -> {
                showError("Veuillez saisir un nombre de places valide.")
                return
            }
            (state.boat.weightMinValue.isNotBlank() && state.boat.weightMaxValue.isBlank()) ||
                (state.boat.weightMinValue.isBlank() && state.boat.weightMaxValue.isNotBlank()) -> {
                showError("Veuillez choisir un poids minimum et un poids maximum.")
                return
            }
            minWeight != null && maxWeight != null && minWeight > maxWeight -> {
                showError("Le poids minimum doit être inférieur ou égal au poids maximum.")
                return
            }
        }

        val entity = BoatEntity(
            id = state.boat.id,
            name = state.boat.name.trim(),
            seatCount = seatCount,
            type = state.boat.type.trim(),
            weightRange = weightRange,
            riggingType = buildRiggingValue(state.boat),
            year = state.boat.year.toIntOrNull(),
            notes = state.boat.notes.trim(),
        )

        viewModelScope.launch {
            uiStateMutable.update { it.copy(isSaving = true, message = null, messageType = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    if (entity.id == 0L) {
                        boatRepository.saveBoat(entity)
                    } else {
                        boatRepository.updateBoat(entity)
                        entity.id
                    }
                }
            }.onSuccess { savedId ->
                uiStateMutable.update {
                    it.copy(
                        isSaving = false,
                        isNewBoat = false,
                        isEditMode = false,
                        message = "Bateau enregistré.",
                        messageType = FeedbackDialogType.SUCCESS,
                    )
                }
                if (boatId == null && savedId != 0L) {
                    observeBoatDetails(savedId)
                }
            }.onFailure {
                showError("Impossible d'enregistrer le bateau.", isSaving = false)
            }
        }
    }

    fun addPhoto(uri: Uri) {
        val currentBoatId = uiState.value.boat.id
        if (currentBoatId == 0L) {
            showError("Enregistrez d'abord le bateau avant d'ajouter une photo.")
            return
        }

        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val filePath = boatPhotoStorage.importCompressedPhoto(uri)
                    boatPhotoRepository.savePhoto(
                        BoatPhotoEntity(
                            boatId = currentBoatId,
                            filePath = filePath,
                            createdAt = currentStorageDate(),
                        ),
                    )
                }
            }.onSuccess {
                uiStateMutable.update {
                    it.copy(
                        message = "Photo ajoutée.",
                        messageType = FeedbackDialogType.SUCCESS,
                    )
                }
            }.onFailure {
                showError("Impossible d'ajouter la photo.")
            }
        }
    }

    fun deletePhoto(photoId: Long) {
        val photo = uiState.value.boat.photos.firstOrNull { it.id == photoId } ?: return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    boatPhotoRepository.deletePhoto(
                        BoatPhotoEntity(
                            id = photo.id,
                            boatId = uiState.value.boat.id,
                            filePath = photo.filePath,
                            createdAt = photo.createdAt,
                        ),
                    )
                    boatPhotoStorage.deletePhoto(photo.filePath)
                }
            }.onSuccess {
                uiStateMutable.update {
                    it.copy(
                        message = "Photo supprimée.",
                        messageType = FeedbackDialogType.SUCCESS,
                    )
                }
            }.onFailure {
                showError("Impossible de supprimer la photo.")
            }
        }
    }

    fun clearMessage() {
        uiStateMutable.update { it.copy(message = null, messageType = null) }
    }

    private fun observeBoatDetails(observedBoatId: Long) {
        viewModelScope.launch {
            combine(
                boatRepository.observeBoat(observedBoatId),
                boatPhotoRepository.observePhotosByBoat(observedBoatId),
                remarkRepository.observeRemarksByBoat(observedBoatId),
                sessionRepository.observeSessionsWithDetails(),
                sessionRepository.observeSessionsWithDetailsByStatus(SessionStatus.ONGOING),
            ) { values ->
                val boat = values[0] as BoatEntity?
                val photos = values[1] as List<BoatPhotoEntity>
                val remarks = values[2] as List<RemarkEntity>
                val sessions = values[3] as List<SessionWithDetails>
                val ongoingSessions = values[4] as List<SessionWithDetails>
                boat?.toDetailUi(
                    photos = photos,
                    remarks = remarks,
                    sessions = sessions,
                    ongoingSessions = ongoingSessions,
                )
            }.collect { detail ->
                uiStateMutable.update { state ->
                    if (detail == null) {
                        state.copy(isLoading = false)
                    } else {
                        state.copy(
                            isLoading = false,
                            isNewBoat = false,
                            isEditMode = false,
                            boat = detail,
                        )
                    }
                }
            }
        }
    }

    private fun BoatEntity.toDetailUi(
        photos: List<BoatPhotoEntity>,
        remarks: List<RemarkEntity>,
        sessions: List<SessionWithDetails>,
        ongoingSessions: List<SessionWithDetails>,
    ): BoatDetailUi {
        val status = resolveBoatStatus(id, remarks, ongoingSessions)
        val recentSessions = sessions
            .filter { it.boat.id == id }
            .sortedWith(
                compareByDescending<SessionWithDetails> { it.session.date }
                    .thenByDescending { it.session.startTime },
            )
            .take(2)
            .map { session ->
                BoatSessionSummaryUi(
                    id = session.session.id,
                    date = session.session.date,
                    rowers = session.sessionRowers.map { it.displayName }.sorted(),
                )
            }

        return BoatDetailUi(
            id = id,
            name = name,
            seatCount = seatCount.toString(),
            type = type,
            weightSingleValue = parseWeightSingleValue(weightRange),
            weightMinValue = parseWeightMinValue(weightRange),
            weightMaxValue = parseWeightMaxValue(weightRange),
            useWeightRange = parseWeightMinValue(weightRange).isNotBlank() && parseWeightMaxValue(weightRange).isNotBlank(),
            riggingCouple = riggingType.contains("Couple", ignoreCase = true),
            riggingPointe = riggingType.contains("Pointe", ignoreCase = true),
            year = year?.toString().orEmpty(),
            notes = notes,
            status = status,
            remarks = remarks.sortedWith(compareBy<RemarkEntity> { it.status.priority() }.thenByDescending { it.date }.thenByDescending { it.id }),
            photos = photos.map { photo ->
                BoatPhotoUi(
                    id = photo.id,
                    filePath = photo.filePath,
                    createdAt = photo.createdAt,
                )
            },
            recentSessions = recentSessions,
        )
    }

    private fun updateBoatFields(transform: BoatDetailUi.() -> BoatDetailUi) {
        uiStateMutable.update {
            it.copy(
                boat = it.boat.transform(),
                message = null,
                messageType = null,
            )
        }
    }

    private fun showError(message: String, isSaving: Boolean = false) {
        uiStateMutable.update {
            it.copy(
                isSaving = isSaving,
                message = message,
                messageType = FeedbackDialogType.ERROR,
            )
        }
    }

    companion object {
        fun factory(
            boatId: Long?,
            boatRepository: BoatRepository,
            boatPhotoRepository: BoatPhotoRepository,
            remarkRepository: RemarkRepository,
            sessionRepository: SessionRepository,
            boatPhotoStorage: BoatPhotoStorage,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BoatDetailViewModel(
                        boatId = boatId,
                        boatRepository = boatRepository,
                        boatPhotoRepository = boatPhotoRepository,
                        remarkRepository = remarkRepository,
                        sessionRepository = sessionRepository,
                        boatPhotoStorage = boatPhotoStorage,
                    ) as T
                }
            }
        }
    }
}

private fun sanitizeWeightValue(value: String): String = value.filter(Char::isDigit)

private fun parseWeightSingleValue(weightRange: String): String {
    val trimmed = weightRange.trim()
    return if (trimmed.contains("–") || trimmed.contains("-")) {
        ""
    } else {
        trimmed.filter(Char::isDigit)
    }
}

private fun parseWeightMinValue(weightRange: String): String {
    val rangeSeparator = when {
        weightRange.contains("–") -> "–"
        weightRange.contains("-") -> "-"
        else -> return ""
    }
    return weightRange.substringBefore(rangeSeparator).filter(Char::isDigit)
}

private fun parseWeightMaxValue(weightRange: String): String {
    val rangeSeparator = when {
        weightRange.contains("–") -> "–"
        weightRange.contains("-") -> "-"
        else -> return ""
    }
    return weightRange.substringAfter(rangeSeparator).filter(Char::isDigit)
}

private fun buildWeightRangeValue(boat: BoatDetailUi): String {
    return when {
        boat.weightMinValue.isNotBlank() && boat.weightMaxValue.isNotBlank() ->
            "${boat.weightMinValue}-${boat.weightMaxValue} kg"
        boat.weightMinValue.isNotBlank() ->
            "${boat.weightMinValue} kg"
        boat.weightSingleValue.isNotBlank() ->
            "${boat.weightSingleValue} kg"
        else -> ""
    }
}

private fun buildRiggingValue(boat: BoatDetailUi): String {
    return buildList {
        if (boat.riggingCouple) add("Couple")
        if (boat.riggingPointe) add("Pointe")
    }.joinToString(" / ")
}

private fun currentStorageDate(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}

private fun resolveBoatStatus(
    boatId: Long,
    remarks: List<RemarkEntity>,
    ongoingSessions: List<SessionWithDetails>,
): BoatStatusUi {
    val hasOpenRepair = remarks.any { it.boatId == boatId && it.status == RemarkStatus.REPAIR_NEEDED }
    if (hasOpenRepair) {
        return BoatStatusUi.IN_REPAIR
    }

    val isInUse = ongoingSessions.any { it.boat.id == boatId }
    return if (isInUse) {
        BoatStatusUi.IN_USE
    } else {
        BoatStatusUi.AVAILABLE
    }
}

private fun RemarkStatus.priority(): Int {
    return when (this) {
        RemarkStatus.REPAIR_NEEDED -> 0
        RemarkStatus.REPAIRED -> 1
        RemarkStatus.NORMAL -> 2
    }
}
