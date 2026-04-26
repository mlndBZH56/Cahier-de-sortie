package com.aca56.cahiersortiecodex.feature.remarks.presentation

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aca56.cahiersortiecodex.data.logging.AppLogStore
import com.aca56.cahiersortiecodex.data.local.entity.BoatEntity
import com.aca56.cahiersortiecodex.data.local.entity.decodeRemarkPhotoPaths
import com.aca56.cahiersortiecodex.data.local.entity.encodeRemarkPhotoPaths
import com.aca56.cahiersortiecodex.data.local.entity.RemarkEntity
import com.aca56.cahiersortiecodex.data.local.entity.RemarkStatus
import com.aca56.cahiersortiecodex.data.local.entity.RepairUpdateEntity
import com.aca56.cahiersortiecodex.data.media.BoatPhotoStorage
import com.aca56.cahiersortiecodex.data.repository.BoatRepository
import com.aca56.cahiersortiecodex.data.repository.RemarkRepository
import com.aca56.cahiersortiecodex.data.repository.RepairUpdateRepository
import com.aca56.cahiersortiecodex.data.repository.SessionRepository
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialogType
import com.aca56.cahiersortiecodex.ui.components.SearchableSelectableOption
import com.aca56.cahiersortiecodex.ui.components.currentStorageDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val status: RemarkStatus,
    val photoPaths: List<String>,
    val sessionId: Long? = null,
) {
    val key: String = "${source.name.lowercase()}-$id"
    val sourceLabel: String
        get() = when (source) {
            RemarkSource.STANDALONE -> if (sessionId != null) "Remarque liée à une session" else "Remarque indépendante"
            RemarkSource.SESSION -> "Remarque de session"
        }
}

data class RepairUpdateItemUi(
    val id: Long,
    val remarkId: Long,
    val content: String,
    val photoPaths: List<String>,
    val createdAt: String,
)

enum class RepairUpdateMode {
    FOLLOW_UP,
    CLOSE_REPAIR,
}

data class RemarksUiState(
    val availableBoats: List<BoatEntity> = emptyList(),
    val boatUsageCounts: Map<Long, Int> = emptyMap(),
    val remarks: List<RemarkItemUi> = emptyList(),
    val repairUpdatesByRemarkId: Map<Long, List<RepairUpdateItemUi>> = emptyMap(),
    val selectedBoatId: Long? = null,
    val selectedDateFilter: String? = null,
    val isEditorVisible: Boolean = false,
    val editingRemarkKey: String? = null,
    val editorDateInput: String = defaultRemarkDate(),
    val editorContentInput: String = "",
    val editorStatus: RemarkStatus = RemarkStatus.NORMAL,
    val editorPhotoPaths: List<String> = emptyList(),
    val selectedBoatForEditorId: Long? = null,
    val repairUpdateRemarkKey: String? = null,
    val editingRepairUpdateId: Long? = null,
    val repairUpdateMode: RepairUpdateMode = RepairUpdateMode.FOLLOW_UP,
    val repairUpdateContentInput: String = "",
    val repairUpdatePhotoPaths: List<String> = emptyList(),
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
        get() = remarks
            .filter { remark ->
                (selectedBoatId == null || remark.boatId == selectedBoatId) &&
                    (selectedDateFilter.isNullOrBlank() || remark.date == selectedDateFilter)
            }
            .sortedWith(
                compareBy<RemarkItemUi> { it.status.priority() }
                    .thenByDescending { it.date }
                    .thenByDescending { it.id },
            )

    val editingRemark: RemarkItemUi?
        get() = remarks.firstOrNull { it.key == editingRemarkKey }

    val isEditing: Boolean
        get() = editingRemarkKey != null

    val activeRepairRemark: RemarkItemUi?
        get() = remarks.firstOrNull { it.key == repairUpdateRemarkKey }
}

class RemarksViewModel(
    private val remarkRepository: RemarkRepository,
    private val repairUpdateRepository: RepairUpdateRepository,
    private val boatRepository: BoatRepository,
    private val sessionRepository: SessionRepository,
    private val boatPhotoStorage: BoatPhotoStorage,
    private val appLogStore: AppLogStore,
    private val initialBoatId: Long? = null,
    private val autoStartEditor: Boolean = false,
) : ViewModel() {
    private val uiStateMutable = MutableStateFlow(RemarksUiState())
    val uiState: StateFlow<RemarksUiState> = uiStateMutable.asStateFlow()

    init {
        observeData()
        if (autoStartEditor) {
            startAddingRemark()
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
                editorStatus = RemarkStatus.NORMAL,
                editorPhotoPaths = emptyList(),
                selectedBoatForEditorId = initialBoatId,
                repairUpdateRemarkKey = null,
                editingRepairUpdateId = null,
                repairUpdateContentInput = "",
                repairUpdatePhotoPaths = emptyList(),
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
                editorStatus = RemarkStatus.NORMAL,
                editorPhotoPaths = emptyList(),
                selectedBoatForEditorId = null,
                repairUpdateRemarkKey = null,
                editingRepairUpdateId = null,
                repairUpdateContentInput = "",
                repairUpdatePhotoPaths = emptyList(),
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
                editorStatus = remark.status,
                editorPhotoPaths = remark.photoPaths,
                selectedBoatForEditorId = remark.boatId,
                repairUpdateRemarkKey = null,
                editingRepairUpdateId = null,
                repairUpdateContentInput = "",
                repairUpdatePhotoPaths = emptyList(),
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

    fun onEditorStatusChanged(status: RemarkStatus) {
        uiStateMutable.update {
            it.copy(editorStatus = status, message = null, messageType = null)
        }
    }

    fun onBoatForEditorSelected(boatId: Long?) {
        uiStateMutable.update {
            it.copy(selectedBoatForEditorId = boatId, message = null, messageType = null)
        }
    }

    fun startRepairUpdate(remarkKey: String) {
        val remark = uiState.value.remarks.firstOrNull { it.key == remarkKey } ?: return
        uiStateMutable.update {
            it.copy(
                repairUpdateRemarkKey = remark.key,
                repairUpdateMode = RepairUpdateMode.FOLLOW_UP,
                repairUpdateContentInput = "",
                repairUpdatePhotoPaths = emptyList(),
                editingRepairUpdateId = null,
                isEditorVisible = false,
                editingRemarkKey = null,
                message = null,
                messageType = null,
            )
        }
    }

    fun startRepairClosure(remarkKey: String) {
        val remark = uiState.value.remarks.firstOrNull { it.key == remarkKey } ?: return
        uiStateMutable.update {
            it.copy(
                repairUpdateRemarkKey = remark.key,
                repairUpdateMode = RepairUpdateMode.CLOSE_REPAIR,
                repairUpdateContentInput = "",
                repairUpdatePhotoPaths = emptyList(),
                editingRepairUpdateId = null,
                isEditorVisible = false,
                editingRemarkKey = null,
                message = null,
                messageType = null,
            )
        }
    }

    fun cancelRepairUpdate() {
        uiStateMutable.update {
            it.copy(
                repairUpdateRemarkKey = null,
                editingRepairUpdateId = null,
                repairUpdateMode = RepairUpdateMode.FOLLOW_UP,
                repairUpdateContentInput = "",
                repairUpdatePhotoPaths = emptyList(),
                message = null,
                messageType = null,
            )
        }
    }

    fun onRepairUpdateContentChanged(value: String) {
        uiStateMutable.update {
            it.copy(repairUpdateContentInput = value, message = null, messageType = null)
        }
    }

    fun addPhoto(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    boatPhotoStorage.importCompressedPhoto(uri)
                }
            }.onSuccess { filePath ->
                appLogStore.logAction(
                    actionType = "Ajout de photo",
                    details = "Ajout d'une photo depuis la galerie.",
                )
                uiStateMutable.update {
                    if (it.repairUpdateRemarkKey != null) {
                        it.copy(
                            repairUpdatePhotoPaths = (it.repairUpdatePhotoPaths + filePath).distinct(),
                            message = null,
                            messageType = null,
                        )
                    } else {
                        it.copy(
                            editorPhotoPaths = (it.editorPhotoPaths + filePath).distinct(),
                            message = null,
                            messageType = null,
                        )
                    }
                }
            }.onFailure {
                appLogStore.logError(
                    actionType = "Échec d'ajout de photo",
                    details = "Impossible d'ajouter une photo à la remarque.",
                )
                uiStateMutable.update {
                    it.copy(
                        message = "Impossible d'ajouter la photo à la remarque.",
                        messageType = FeedbackDialogType.ERROR,
                    )
                }
            }
        }
    }

    fun addPhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    boatPhotoStorage.saveCompressedBitmap(bitmap)
                }
            }.onSuccess { filePath ->
                appLogStore.logAction(
                    actionType = "Ajout de photo",
                    details = "Ajout d'une photo prise avec l'appareil.",
                )
                uiStateMutable.update {
                    if (it.repairUpdateRemarkKey != null) {
                        it.copy(
                            repairUpdatePhotoPaths = (it.repairUpdatePhotoPaths + filePath).distinct(),
                            message = null,
                            messageType = null,
                        )
                    } else {
                        it.copy(
                            editorPhotoPaths = (it.editorPhotoPaths + filePath).distinct(),
                            message = null,
                            messageType = null,
                        )
                    }
                }
            }.onFailure {
                appLogStore.logError(
                    actionType = "Échec d'ajout de photo",
                    details = "Impossible d'ajouter une photo prise avec l'appareil.",
                )
                uiStateMutable.update {
                    it.copy(
                        message = "Impossible d'ajouter la photo.",
                        messageType = FeedbackDialogType.ERROR,
                    )
                }
            }
        }
    }

    fun removePhoto(path: String? = null) {
        val currentState = uiState.value
        val filePath = if (currentState.repairUpdateRemarkKey != null) {
            path ?: currentState.repairUpdatePhotoPaths.lastOrNull()
        } else {
            path
        }
        filePath?.let(boatPhotoStorage::deletePhoto)
        if (filePath != null) {
            appLogStore.logAction(
                actionType = "Suppression de photo",
                details = "Suppression d'une photo liée à une remarque ou à un suivi.",
            )
        }
        uiStateMutable.update {
            if (it.repairUpdateRemarkKey != null) {
                it.copy(
                    repairUpdatePhotoPaths = filePath?.let { photo -> it.repairUpdatePhotoPaths - photo } ?: it.repairUpdatePhotoPaths,
                    message = null,
                    messageType = null,
                )
            } else {
                it.copy(
                    editorPhotoPaths = filePath?.let { photo -> it.editorPhotoPaths - photo } ?: it.editorPhotoPaths,
                    message = null,
                    messageType = null,
                )
            }
        }
    }

    fun startEditingRepairUpdate(updateId: Long) {
        val update = uiState.value.repairUpdatesByRemarkId.values.flatten().firstOrNull { it.id == updateId } ?: return
        val ownerRemark = uiState.value.remarks.firstOrNull { it.id == update.remarkId } ?: return
        uiStateMutable.update {
            it.copy(
                repairUpdateRemarkKey = ownerRemark.key,
                editingRepairUpdateId = update.id,
                repairUpdateMode = RepairUpdateMode.FOLLOW_UP,
                repairUpdateContentInput = update.content,
                repairUpdatePhotoPaths = update.photoPaths,
                isEditorVisible = false,
                editingRemarkKey = null,
                message = null,
                messageType = null,
            )
        }
    }

    fun deleteRepairUpdate(updateId: Long) {
        val update = uiState.value.repairUpdatesByRemarkId.values.flatten().firstOrNull { it.id == updateId } ?: return
        viewModelScope.launch {
            runCatching {
                update.photoPaths.forEach(boatPhotoStorage::deletePhoto)
                repairUpdateRepository.deleteUpdate(
                    RepairUpdateEntity(
                        id = update.id,
                        remarkId = update.remarkId,
                        content = update.content,
                        photoPath = encodeRemarkPhotoPaths(update.photoPaths),
                        createdAt = update.createdAt,
                    ),
                )
            }.onSuccess {
                appLogStore.logAction(
                    actionType = "Suppression de suivi",
                    details = "Suppression d'un suivi de réparation.",
                )
                uiStateMutable.update {
                    it.copy(
                        message = "Le suivi a été supprimé.",
                        messageType = FeedbackDialogType.SUCCESS,
                        editingRepairUpdateId = if (it.editingRepairUpdateId == updateId) null else it.editingRepairUpdateId,
                        repairUpdateRemarkKey = if (it.editingRepairUpdateId == updateId) null else it.repairUpdateRemarkKey,
                        repairUpdateContentInput = if (it.editingRepairUpdateId == updateId) "" else it.repairUpdateContentInput,
                        repairUpdatePhotoPaths = if (it.editingRepairUpdateId == updateId) emptyList() else it.repairUpdatePhotoPaths,
                    )
                }
            }.onFailure {
                appLogStore.logError(
                    actionType = "Échec de suppression de suivi",
                    details = "Le suivi de réparation n'a pas pu être supprimé.",
                )
                uiStateMutable.update {
                    it.copy(
                        message = "Le suivi de réparation n'a pas pu être supprimé.",
                        messageType = FeedbackDialogType.ERROR,
                    )
                }
            }
        }
    }

    fun saveRemark() {
        val state = uiState.value
        val editingRemark = state.editingRemark
        val content = state.editorContentInput.trim()

        if (content.isBlank()) {
            uiStateMutable.update {
                it.copy(
                    message = "Veuillez saisir le texte de la remarque pour pouvoir l'enregistrer.",
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
                                sessionId = editingRemark.sessionId,
                                content = content,
                                date = state.editorDateInput,
                                status = state.editorStatus,
                                photoPath = encodeRemarkPhotoPaths(state.editorPhotoPaths),
                            ),
                        )
                    }

                    null -> {
                        remarkRepository.saveRemark(
                            RemarkEntity(
                                boatId = state.selectedBoatForEditorId,
                                content = content,
                                date = state.editorDateInput,
                                status = state.editorStatus,
                                photoPath = encodeRemarkPhotoPaths(state.editorPhotoPaths),
                            ),
                        )
                    }
                }
            }.onSuccess {
                appLogStore.logAction(
                    actionType = if (editingRemark == null) "Ajout de remarque" else "Modification de remarque",
                    details = buildString {
                        append("Remarque ")
                        append(if (editingRemark == null) "ajoutée" else "mise à jour")
                        append(" avec le statut ${state.editorStatus.logLabel()}.")
                    },
                )
                uiStateMutable.update {
                    it.copy(
                        isSaving = false,
                        isEditorVisible = false,
                        editingRemarkKey = null,
                        editorDateInput = defaultRemarkDate(),
                        editorContentInput = "",
                        editorStatus = RemarkStatus.NORMAL,
                        editorPhotoPaths = emptyList(),
                        selectedBoatForEditorId = null,
                        message = if (editingRemark == null) "Remarque ajoutée." else "Remarque mise à jour.",
                        messageType = FeedbackDialogType.SUCCESS,
                    )
                }
            }.onFailure {
                appLogStore.logError(
                    actionType = "Échec d'enregistrement de remarque",
                    details = "La remarque n'a pas pu être enregistrée.",
                )
                uiStateMutable.update {
                    it.copy(
                        isSaving = false,
                        message = "La remarque n'a pas pu être enregistrée. Vérifiez le contenu et réessayez.",
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
                        remark.photoPaths.forEach(boatPhotoStorage::deletePhoto)
                        uiState.value.repairUpdatesByRemarkId[remark.id].orEmpty()
                            .forEach { update -> update.photoPaths.forEach(boatPhotoStorage::deletePhoto) }
                        remarkRepository.deleteRemark(
                            RemarkEntity(
                                id = remark.id,
                                boatId = remark.boatId,
                                sessionId = remark.sessionId,
                                content = remark.content,
                                date = remark.date,
                                status = remark.status,
                                photoPath = encodeRemarkPhotoPaths(remark.photoPaths),
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
                appLogStore.logAction(
                    actionType = "Suppression de remarque",
                    details = "Suppression d'une remarque (${remark.status.logLabel()}).",
                )
                uiStateMutable.update {
                    val resetEditor = it.editingRemarkKey == remark.key
                    it.copy(
                        isSaving = false,
                        isEditorVisible = if (resetEditor) false else it.isEditorVisible,
                        editingRemarkKey = if (resetEditor) null else it.editingRemarkKey,
                        editorDateInput = if (resetEditor) defaultRemarkDate() else it.editorDateInput,
                        editorContentInput = if (resetEditor) "" else it.editorContentInput,
                        editorStatus = if (resetEditor) RemarkStatus.NORMAL else it.editorStatus,
                        editorPhotoPaths = if (resetEditor) emptyList() else it.editorPhotoPaths,
                        selectedBoatForEditorId = if (resetEditor) null else it.selectedBoatForEditorId,
                        repairUpdateRemarkKey = if (it.repairUpdateRemarkKey == remark.key) null else it.repairUpdateRemarkKey,
                        editingRepairUpdateId = if (it.repairUpdateRemarkKey == remark.key) null else it.editingRepairUpdateId,
                        repairUpdateMode = if (it.repairUpdateRemarkKey == remark.key) RepairUpdateMode.FOLLOW_UP else it.repairUpdateMode,
                        repairUpdateContentInput = if (it.repairUpdateRemarkKey == remark.key) "" else it.repairUpdateContentInput,
                        repairUpdatePhotoPaths = if (it.repairUpdateRemarkKey == remark.key) emptyList() else it.repairUpdatePhotoPaths,
                        message = "Remarque supprimée.",
                        messageType = FeedbackDialogType.SUCCESS,
                    )
                }
            }.onFailure {
                appLogStore.logError(
                    actionType = "Échec de suppression de remarque",
                    details = "La remarque n'a pas pu être supprimée.",
                )
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

    fun saveRepairUpdate() {
        val state = uiState.value
        val activeRemark = state.activeRepairRemark ?: return
        val content = state.repairUpdateContentInput.trim()

        if (
            state.repairUpdateMode == RepairUpdateMode.FOLLOW_UP &&
            content.isBlank() &&
            state.repairUpdatePhotoPaths.isEmpty()
        ) {
            uiStateMutable.update {
                it.copy(
                    message = "Ajoutez un texte ou une photo pour enregistrer le suivi.",
                    messageType = FeedbackDialogType.ERROR,
                )
            }
            return
        }

        viewModelScope.launch {
            uiStateMutable.update { it.copy(isSaving = true, message = null, messageType = null) }
            runCatching {
                repairUpdateRepository.saveUpdate(
                    RepairUpdateEntity(
                        id = state.editingRepairUpdateId ?: 0,
                        remarkId = activeRemark.id,
                        content = content.ifBlank {
                            if (state.repairUpdateMode == RepairUpdateMode.CLOSE_REPAIR) {
                                "Réparation marquée comme terminée."
                            } else {
                                "Suivi de réparation"
                            }
                        },
                        photoPath = encodeRemarkPhotoPaths(state.repairUpdatePhotoPaths),
                        createdAt = currentRepairUpdateTimestamp(),
                    ),
                )
                if (state.repairUpdateMode == RepairUpdateMode.CLOSE_REPAIR && activeRemark.source == RemarkSource.STANDALONE) {
                    remarkRepository.updateRemark(
                        RemarkEntity(
                            id = activeRemark.id,
                            boatId = activeRemark.boatId,
                            sessionId = activeRemark.sessionId,
                            content = activeRemark.content,
                            date = activeRemark.date,
                            status = RemarkStatus.REPAIRED,
                            photoPath = encodeRemarkPhotoPaths(activeRemark.photoPaths),
                        ),
                    )
                }
            }.onSuccess {
                appLogStore.logAction(
                    actionType = if (state.repairUpdateMode == RepairUpdateMode.CLOSE_REPAIR) {
                        "Réparation terminée"
                    } else if (state.editingRepairUpdateId == null) {
                        "Ajout de suivi"
                    } else {
                        "Modification de suivi"
                    },
                    details = "Mise à jour de l'historique de réparation pour la remarque ${activeRemark.id}.",
                )
                uiStateMutable.update {
                    it.copy(
                        isSaving = false,
                        repairUpdateRemarkKey = null,
                        editingRepairUpdateId = null,
                        repairUpdateMode = RepairUpdateMode.FOLLOW_UP,
                        repairUpdateContentInput = "",
                        repairUpdatePhotoPaths = emptyList(),
                        message = if (state.repairUpdateMode == RepairUpdateMode.CLOSE_REPAIR) {
                            "La réparation est marquée comme terminée."
                        } else if (state.editingRepairUpdateId != null) {
                            "Le suivi a été modifié."
                        } else {
                            "Le suivi a été ajouté."
                        },
                        messageType = FeedbackDialogType.SUCCESS,
                    )
                }
            }.onFailure {
                appLogStore.logError(
                    actionType = "Échec d'enregistrement de suivi",
                    details = "Le suivi de réparation n'a pas pu être enregistré.",
                )
                uiStateMutable.update {
                    it.copy(
                        isSaving = false,
                        message = "Le suivi de réparation n'a pas pu être enregistré.",
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
            ?: error("Session $sessionId introuvable.")

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
                repairUpdateRepository.observeUpdates(),
                sessionRepository.observeSessionsWithDetails(),
            ) { boats, standaloneRemarks, repairUpdates, sessions ->
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
                        status = remark.status,
                        photoPaths = decodeRemarkPhotoPaths(remark.photoPath),
                        sessionId = remark.sessionId,
                    )
                }

                val linkedSessionIds = standaloneRemarks.mapNotNull { it.sessionId }.toSet()

                val sessionItems = sessions.mapNotNull { sessionWithDetails ->
                    val remarks = sessionWithDetails.session.remarks?.trim().orEmpty()
                    if (remarks.isBlank() || linkedSessionIds.contains(sessionWithDetails.session.id)) {
                        null
                    } else {
                        RemarkItemUi(
                            id = sessionWithDetails.session.id,
                            source = RemarkSource.SESSION,
                            date = sessionWithDetails.session.date,
                            content = remarks,
                            boatId = sessionWithDetails.boat.id,
                            boatName = sessionWithDetails.boat.name,
                            status = RemarkStatus.NORMAL,
                            photoPaths = emptyList(),
                            sessionId = sessionWithDetails.session.id,
                        )
                    }
                }

                Triple(
                    boats,
                    boatUsageCounts,
                    Pair(
                        standaloneItems + sessionItems,
                        repairUpdates,
                    ),
                )
            }.collect { (boats, boatUsageCounts, remarkData) ->
                val remarks = remarkData.first
                val repairUpdates = remarkData.second
                val repairUpdatesByRemarkId = repairUpdates
                    .groupBy { it.remarkId }
                    .mapValues { (_, updates) ->
                        updates.map { update ->
                            RepairUpdateItemUi(
                                id = update.id,
                                remarkId = update.remarkId,
                                content = update.content,
                                photoPaths = decodeRemarkPhotoPaths(update.photoPath),
                                createdAt = update.createdAt,
                            )
                        }
                    }
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
                        repairUpdatesByRemarkId = repairUpdatesByRemarkId,
                        selectedBoatId = when {
                            validFilterBoatId != null -> validFilterBoatId
                            initialBoatId != null && boats.any { it.id == initialBoatId } -> initialBoatId
                            else -> null
                        },
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
                        editorStatus = updatedEditingRemark?.status ?: state.editorStatus,
                        editorPhotoPaths = updatedEditingRemark?.photoPaths ?: state.editorPhotoPaths,
                        repairUpdateRemarkKey = state.repairUpdateRemarkKey?.takeIf { key ->
                            remarks.any { item -> item.key == key }
                        },
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
            repairUpdateRepository: RepairUpdateRepository,
            boatRepository: BoatRepository,
            sessionRepository: SessionRepository,
            boatPhotoStorage: BoatPhotoStorage,
            appLogStore: AppLogStore,
            initialBoatId: Long? = null,
            autoStartEditor: Boolean = false,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RemarksViewModel(
                        remarkRepository = remarkRepository,
                        repairUpdateRepository = repairUpdateRepository,
                        boatRepository = boatRepository,
                        sessionRepository = sessionRepository,
                        boatPhotoStorage = boatPhotoStorage,
                        appLogStore = appLogStore,
                        initialBoatId = initialBoatId,
                        autoStartEditor = autoStartEditor,
                    ) as T
                }
            }
        }
    }
}

private fun currentRepairUpdateTimestamp(): String {
    return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date())
}

private fun RemarkStatus.logLabel(): String {
    return when (this) {
        RemarkStatus.NORMAL -> "normale"
        RemarkStatus.REPAIR_NEEDED -> "réparation nécessaire"
        RemarkStatus.REPAIRED -> "réparée"
    }
}

private fun RemarkStatus.priority(): Int {
    return when (this) {
        RemarkStatus.REPAIR_NEEDED -> 0
        RemarkStatus.REPAIRED -> 1
        RemarkStatus.NORMAL -> 2
    }
}
