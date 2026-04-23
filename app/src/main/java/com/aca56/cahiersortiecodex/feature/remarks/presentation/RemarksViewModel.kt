package com.aca56.cahiersortiecodex.feature.remarks.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aca56.cahiersortiecodex.data.local.entity.BoatEntity
import com.aca56.cahiersortiecodex.data.local.entity.RemarkEntity
import com.aca56.cahiersortiecodex.data.repository.BoatRepository
import com.aca56.cahiersortiecodex.data.repository.RemarkRepository
import com.aca56.cahiersortiecodex.data.repository.SessionRepository
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialogType
import com.aca56.cahiersortiecodex.ui.components.SearchableSelectableOption
import com.aca56.cahiersortiecodex.ui.components.currentStorageDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private fun defaultRemarkDate(): String = currentStorageDate()

enum class RemarkSource {
    STANDALONE,
    SESSION,
}

data class RemarkItemUi(
    val id: Long,
    val source: RemarkSource,
    val date: String,
    val content: String,
    val boatId: Long?,
    val boatName: String,
    val sessionId: Long? = null,
) {
    val key: String = "${source.name.lowercase()}-$id"
    val sourceLabel: String
        get() = when (source) {
            RemarkSource.STANDALONE -> "Remarque indépendante"
            RemarkSource.SESSION -> "Remarque de session"
        }
}

data class RemarksUiState(
    val availableBoats: List<BoatEntity> = emptyList(),
    val boatUsageCounts: Map<Long, Int> = emptyMap(),
    val remarks: List<RemarkItemUi> = emptyList(),
    val selectedBoatId: Long? = null,
    val selectedDateFilter: String? = null,
    val isEditorVisible: Boolean = false,
    val editingRemarkKey: String? = null,
    val editorDateInput: String = defaultRemarkDate(),
    val editorContentInput: String = "",
    val selectedBoatForEditorId: Long? = null,
    val isSaving: Boolean = false,
    val message: String? = null,
    val messageType: FeedbackDialogType? = null,
) {
    val availableBoatOptions: List<SearchableSelectableOption>
        get() = availableBoats.map { boat ->
            SearchableSelectableOption(
                key = boat.id.toString(),
                label = boat.name,
                usageCount = boatUsageCounts[boat.id] ?: 0,
            )
        }.sortedWith(compareByDescending<SearchableSelectableOption> { it.usageCount }.thenBy { it.label })

    val filteredRemarks: List<RemarkItemUi>
        get() = remarks.filter { remark ->
            (selectedBoatId == null || remark.boatId == selectedBoatId) &&
                (selectedDateFilter.isNullOrBlank() || remark.date == selectedDateFilter)
        }

    val editingRemark: RemarkItemUi?
        get() = remarks.firstOrNull { it.key == editingRemarkKey }

    val isEditing: Boolean
        get() = editingRemarkKey != null
}

class RemarksViewModel(
    private val remarkRepository: RemarkRepository,
    private val boatRepository: BoatRepository,
    private val sessionRepository: SessionRepository,
    private val initialBoatId: Long? = null,
) : ViewModel() {
    private val uiStateMutable = MutableStateFlow(RemarksUiState())
    val uiState: StateFlow<RemarksUiState> = uiStateMutable.asStateFlow()

    init {
        observeData()
    }

    fun clearMessage() {
        uiStateMutable.update {
            it.copy(
                message = null,
                messageType = null,
            )
        }
    }

    fun onBoatFilterSelected(boatId: Long?) {
        uiStateMutable.update {
            it.copy(selectedBoatId = boatId, message = null, messageType = null)
        }
    }

    fun onDateFilterSelected(value: String?) {
        uiStateMutable.update {
            it.copy(selectedDateFilter = value, message = null, messageType = null)
        }
    }

    fun startAddingRemark() {
        uiStateMutable.update {
            it.copy(
                isEditorVisible = true,
                editingRemarkKey = null,
                editorDateInput = defaultRemarkDate(),
                editorContentInput = "",
                selectedBoatForEditorId = null,
                message = null,
                messageType = null,
            )
        }
    }

    fun cancelEditing() {
        uiStateMutable.update {
            it.copy(
                isEditorVisible = false,
                editingRemarkKey = null,
                editorDateInput = defaultRemarkDate(),
                editorContentInput = "",
                selectedBoatForEditorId = null,
                message = null,
                messageType = null,
            )
        }
    }

    fun startEditingRemark(remarkKey: String) {
        val remark = uiState.value.remarks.firstOrNull { it.key == remarkKey } ?: run {
            uiStateMutable.update {
                it.copy(
                    message = "La remarque sélectionnée est introuvable.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return
        }

        uiStateMutable.update {
            it.copy(
                isEditorVisible = true,
                editingRemarkKey = remark.key,
                editorDateInput = remark.date,
                editorContentInput = remark.content,
                selectedBoatForEditorId = remark.boatId,
                message = null,
                messageType = null,
            )
        }
    }

    fun onEditorDateChanged(value: String) {
        uiStateMutable.update {
            it.copy(editorDateInput = value, message = null, messageType = null)
        }
    }

    fun onEditorContentChanged(value: String) {
        uiStateMutable.update {
            it.copy(editorContentInput = value, message = null, messageType = null)
        }
    }

    fun onBoatForEditorSelected(boatId: Long?) {
        uiStateMutable.update {
            it.copy(selectedBoatForEditorId = boatId, message = null, messageType = null)
        }
    }

    fun saveRemark() {
        val state = uiState.value
        val editingRemark = state.editingRemark
        val content = state.editorContentInput.trim()

        if (content.isBlank()) {
            uiStateMutable.update {
                it.copy(
                    message = "Veuillez saisir le texte de la remarque pour pouvoir enregistrer les modifications.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return
        }

        if (editingRemark?.source != RemarkSource.SESSION) {
            val date = state.editorDateInput.trim()
            if (date.isBlank()) {
                uiStateMutable.update {
                    it.copy(
                        message = "Veuillez choisir une date pour la remarque.",
                        messageType = FeedbackDialogType.ERROR,
                    )
                }
                return
            }
        }

        viewModelScope.launch {
            uiStateMutable.update {
                it.copy(isSaving = true, message = null, messageType = null)
            }

            runCatching {
                when (editingRemark?.source) {
                    RemarkSource.SESSION -> updateSessionRemark(
                        sessionId = editingRemark.sessionId ?: editingRemark.id,
                        content = content,
                    )

                    RemarkSource.STANDALONE -> {
                        remarkRepository.updateRemark(
                            RemarkEntity(
                                id = editingRemark.id,
                                boatId = state.selectedBoatForEditorId,
                                content = content,
                                date = state.editorDateInput,
                            ),
                        )
                    }

                    null -> {
                        remarkRepository.saveRemark(
                            RemarkEntity(
                                boatId = state.selectedBoatForEditorId,
                                content = content,
                                date = state.editorDateInput,
                            ),
                        )
                    }
                }
            }.onSuccess {
                uiStateMutable.update {
                    it.copy(
                        isSaving = false,
                        isEditorVisible = false,
                        editingRemarkKey = null,
                        editorDateInput = defaultRemarkDate(),
                        editorContentInput = "",
                        selectedBoatForEditorId = null,
                        message = if (editingRemark == null) "Remarque ajoutée." else "Modifications enregistrées.",
                        messageType = FeedbackDialogType.SUCCESS,
                    )
                }
            }.onFailure {
                uiStateMutable.update {
                    it.copy(
                        isSaving = false,
                        message = "La remarque n'a pas pu être enregistrée. Veuillez vérifier les informations saisies puis réessayer.",
                        messageType = FeedbackDialogType.ERROR,
                    )
                }
            }
        }
    }

    fun deleteRemark(remarkKey: String) {
        val remark = uiState.value.remarks.firstOrNull { it.key == remarkKey } ?: run {
            uiStateMutable.update {
                it.copy(
                    message = "La remarque sélectionnée est introuvable.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return
        }

        viewModelScope.launch {
            uiStateMutable.update {
                it.copy(isSaving = true, message = null, messageType = null)
            }

            runCatching {
                when (remark.source) {
                    RemarkSource.STANDALONE -> {
                        remarkRepository.deleteRemark(
                            RemarkEntity(
                                id = remark.id,
                                boatId = remark.boatId,
                                content = remark.content,
                                date = remark.date,
                            ),
                        )
                    }

                    RemarkSource.SESSION -> {
                        updateSessionRemark(
                            sessionId = remark.sessionId ?: remark.id,
                            content = null,
                        )
                    }
                }
            }.onSuccess {
                uiStateMutable.update {
                    val resetEditor = it.editingRemarkKey == remark.key
                    it.copy(
                        isSaving = false,
                        isEditorVisible = if (resetEditor) false else it.isEditorVisible,
                        editingRemarkKey = if (resetEditor) null else it.editingRemarkKey,
                        editorDateInput = if (resetEditor) defaultRemarkDate() else it.editorDateInput,
                        editorContentInput = if (resetEditor) "" else it.editorContentInput,
                        selectedBoatForEditorId = if (resetEditor) null else it.selectedBoatForEditorId,
                        message = "Remarque supprimée.",
                        messageType = FeedbackDialogType.SUCCESS,
                    )
                }
            }.onFailure {
                uiStateMutable.update {
                    it.copy(
                        isSaving = false,
                        message = "La remarque n'a pas pu être supprimée. Veuillez réessayer.",
                        messageType = FeedbackDialogType.ERROR,
                    )
                }
            }
        }
    }

    private suspend fun updateSessionRemark(
        sessionId: Long,
        content: String?,
    ) {
        val sessionWithDetails = sessionRepository.getSessionWithDetails(sessionId)
            ?: error("Session $sessionId not found")

        sessionRepository.updateSession(
            sessionWithDetails.session.copy(
                remarks = content?.takeIf { it.isNotBlank() },
            ),
        )
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                boatRepository.observeBoats(),
                remarkRepository.observeRemarks(),
                sessionRepository.observeSessionsWithDetails(),
            ) { boats, standaloneRemarks, sessions ->
                val boatNames = boats.associateBy { it.id }
                val boatUsageCounts = buildMap<Long, Int> {
                    standaloneRemarks.forEach { remark ->
                        remark.boatId?.let { boatId ->
                            put(boatId, (get(boatId) ?: 0) + 1)
                        }
                    }
                    sessions.forEach { session ->
                        val boatId = session.boat.id
                        put(boatId, (get(boatId) ?: 0) + 1)
                    }
                }

                val standaloneItems = standaloneRemarks.map { remark ->
                    RemarkItemUi(
                        id = remark.id,
                        source = RemarkSource.STANDALONE,
                        date = remark.date,
                        content = remark.content,
                        boatId = remark.boatId,
                        boatName = boatNames[remark.boatId]?.name.orEmpty(),
                    )
                }

                val sessionItems = sessions.mapNotNull { sessionWithDetails ->
                    val remarks = sessionWithDetails.session.remarks?.trim().orEmpty()
                    if (remarks.isBlank()) {
                        null
                    } else {
                        RemarkItemUi(
                            id = sessionWithDetails.session.id,
                            source = RemarkSource.SESSION,
                            date = sessionWithDetails.session.date,
                            content = remarks,
                            boatId = sessionWithDetails.boat.id,
                            boatName = sessionWithDetails.boat.name,
                            sessionId = sessionWithDetails.session.id,
                        )
                    }
                }

                Triple(
                    boats,
                    boatUsageCounts,
                    (standaloneItems + sessionItems)
                    .sortedWith(
                        compareByDescending<RemarkItemUi> { it.date }
                            .thenByDescending { it.id },
                    ),
                )
            }.collect { (boats, boatUsageCounts, remarks) ->
                uiStateMutable.update { state ->
                    val validFilterBoatId = state.selectedBoatId?.takeIf { selectedId ->
                        boats.any { it.id == selectedId }
                    }
                    val validEditorBoatId = state.selectedBoatForEditorId?.takeIf { selectedId ->
                        boats.any { it.id == selectedId }
                    }
                    val updatedEditingRemark = state.editingRemarkKey?.let { key ->
                        remarks.firstOrNull { it.key == key }
                    }

                    state.copy(
                        availableBoats = boats,
                        boatUsageCounts = boatUsageCounts,
                        remarks = remarks,
                        selectedBoatId = when {
                            validFilterBoatId != null -> validFilterBoatId
                            initialBoatId != null && boats.any { it.id == initialBoatId } -> initialBoatId
                            else -> null
                        },
                        selectedDateFilter = state.selectedDateFilter,
                        selectedBoatForEditorId = when (updatedEditingRemark?.source) {
                            RemarkSource.SESSION -> updatedEditingRemark.boatId
                            RemarkSource.STANDALONE -> updatedEditingRemark.boatId
                            null -> validEditorBoatId
                        },
                        editorDateInput = when (updatedEditingRemark?.source) {
                            RemarkSource.SESSION -> updatedEditingRemark.date
                            RemarkSource.STANDALONE -> updatedEditingRemark.date
                            null -> state.editorDateInput
                        },
                        editorContentInput = updatedEditingRemark?.content ?: state.editorContentInput,
                        editingRemarkKey = updatedEditingRemark?.key ?: state.editingRemarkKey?.takeIf {
                            remarks.any { item -> item.key == it }
                        },
                        isEditorVisible = state.isEditorVisible && (state.editingRemarkKey == null || updatedEditingRemark != null),
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            remarkRepository: RemarkRepository,
            boatRepository: BoatRepository,
            sessionRepository: SessionRepository,
            initialBoatId: Long? = null,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RemarksViewModel(
                        remarkRepository = remarkRepository,
                        boatRepository = boatRepository,
                        sessionRepository = sessionRepository,
                        initialBoatId = initialBoatId,
                    ) as T
                }
            }
        }
    }
}
