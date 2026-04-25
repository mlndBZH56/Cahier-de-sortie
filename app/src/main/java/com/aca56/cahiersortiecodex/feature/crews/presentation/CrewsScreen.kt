package com.aca56.cahiersortiecodex.feature.crews.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aca56.cahiersortiecodex.CahierSortieApplication
import com.aca56.cahiersortiecodex.data.crew.CrewStore
import com.aca56.cahiersortiecodex.data.logging.AppLogStore
import com.aca56.cahiersortiecodex.data.repository.RowerRepository
import com.aca56.cahiersortiecodex.ui.components.AppTextField
import com.aca56.cahiersortiecodex.ui.components.AppTextFieldType
import com.aca56.cahiersortiecodex.ui.components.DeleteConfirmationDialog
import com.aca56.cahiersortiecodex.ui.components.SearchableSelectableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CrewItemUi(
    val id: Long,
    val name: String,
    val rowerIds: Set<Long>,
    val rowerNames: List<String>,
)

data class CrewsUiState(
    val crews: List<CrewItemUi> = emptyList(),
    val searchQuery: String = "",
    val isEditorDialogOpen: Boolean = false,
    val editingCrewId: Long? = null,
    val crewNameInput: String = "",
    val rowerSearchQuery: String = "",
    val selectedRowerIds: Set<Long> = emptySet(),
    val availableRowers: List<com.aca56.cahiersortiecodex.data.local.entity.RowerEntity> = emptyList(),
    val message: String? = null,
) {
    val filteredCrews: List<CrewItemUi>
        get() = crews.filter { crew ->
            searchQuery.isBlank() || crew.name.contains(searchQuery.trim(), ignoreCase = true)
        }
}

class CrewsViewModel(
    private val crewStore: CrewStore,
    private val rowerRepository: RowerRepository,
    private val appLogStore: AppLogStore,
) : ViewModel() {
    private val uiStateMutable = MutableStateFlow(CrewsUiState())
    val uiState: StateFlow<CrewsUiState> = uiStateMutable.asStateFlow()

    init {
        observeCrews()
        observeRowers()
    }

    fun onSearchQueryChanged(value: String) {
        uiStateMutable.update { it.copy(searchQuery = value) }
    }

    fun openCreateDialog() {
        uiStateMutable.update {
            it.copy(
                isEditorDialogOpen = true,
                editingCrewId = null,
                crewNameInput = "",
                rowerSearchQuery = "",
                selectedRowerIds = emptySet(),
                message = null,
            )
        }
    }

    fun openEditDialog(crewId: Long) {
        val crew = crewStore.currentCrews().firstOrNull { it.id == crewId } ?: return
        uiStateMutable.update {
            it.copy(
                isEditorDialogOpen = true,
                editingCrewId = crew.id,
                crewNameInput = crew.name,
                rowerSearchQuery = "",
                selectedRowerIds = crew.rowerIds.toSet(),
                message = null,
            )
        }
    }

    fun closeCreateDialog() {
        uiStateMutable.update {
            it.copy(
                isEditorDialogOpen = false,
                editingCrewId = null,
                message = null,
            )
        }
    }

    fun onCrewNameChanged(value: String) {
        uiStateMutable.update { it.copy(crewNameInput = value, message = null) }
    }

    fun onRowerSearchQueryChanged(value: String) {
        uiStateMutable.update { it.copy(rowerSearchQuery = value) }
    }

    fun onRowerToggled(rowerId: Long, selected: Boolean) {
        uiStateMutable.update { state ->
            val updated = state.selectedRowerIds.toMutableSet().apply {
                if (selected) add(rowerId) else remove(rowerId)
            }
            state.copy(selectedRowerIds = updated, message = null)
        }
    }

    fun saveCrew() {
        val state = uiState.value
        when {
            state.crewNameInput.isBlank() -> {
                uiStateMutable.update { it.copy(message = "Veuillez saisir un nom d'équipage.") }
                return
            }
            state.selectedRowerIds.isEmpty() -> {
                uiStateMutable.update { it.copy(message = "Veuillez sélectionner au moins un rameur.") }
                return
            }
        }

        crewStore.saveCrew(
            id = state.editingCrewId,
            name = state.crewNameInput,
            rowerIds = state.selectedRowerIds.toList(),
        )
        appLogStore.logAction(
            actionType = if (state.editingCrewId == null) "Création d'équipage" else "Modification d'équipage",
            details = "Équipage enregistré: ${state.crewNameInput} (${state.selectedRowerIds.size} rameur(s)).",
        )
        uiStateMutable.update {
            it.copy(
                isEditorDialogOpen = false,
                editingCrewId = null,
                crewNameInput = "",
                rowerSearchQuery = "",
                selectedRowerIds = emptySet(),
                message = null,
            )
        }
    }

    fun deleteCrew(crewId: Long) {
        crewStore.deleteCrew(crewId)
        appLogStore.logAction(
            actionType = "Suppression d'équipage",
            details = "Équipage supprimé: $crewId.",
        )
        uiStateMutable.update {
            if (it.editingCrewId == crewId) {
                it.copy(
                    isEditorDialogOpen = false,
                    editingCrewId = null,
                    crewNameInput = "",
                    rowerSearchQuery = "",
                    selectedRowerIds = emptySet(),
                    message = null,
                )
            } else {
                it
            }
        }
    }

    private fun observeCrews() {
        viewModelScope.launch {
            crewStore.crewsFlow.collect { crews ->
                val rowerNamesById = uiState.value.availableRowers.associate { rower ->
                    rower.id to "${rower.firstName} ${rower.lastName}".trim()
                }
                uiStateMutable.update { state ->
                    state.copy(
                        crews = crews.map { crew ->
                            CrewItemUi(
                                id = crew.id,
                                name = crew.name,
                                rowerIds = crew.rowerIds.toSet(),
                                rowerNames = crew.rowerIds.mapNotNull(rowerNamesById::get),
                            )
                        },
                    )
                }
            }
        }
    }

    private fun observeRowers() {
        viewModelScope.launch {
            rowerRepository.observeRowers().collect { rowers ->
                uiStateMutable.update { state ->
                    val rowerNamesById = rowers.associate { rower ->
                        rower.id to "${rower.firstName} ${rower.lastName}".trim()
                    }
                    state.copy(
                        availableRowers = rowers,
                        crews = crewStore.currentCrews().map { crew ->
                            CrewItemUi(
                                id = crew.id,
                                name = crew.name,
                                rowerIds = crew.rowerIds.toSet(),
                                rowerNames = crew.rowerIds.mapNotNull(rowerNamesById::get),
                            )
                        },
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            crewStore: CrewStore,
            rowerRepository: RowerRepository,
            appLogStore: AppLogStore,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CrewsViewModel(
                    crewStore = crewStore,
                    rowerRepository = rowerRepository,
                    appLogStore = appLogStore,
                ) as T
            }
        }
    }
}

@Composable
fun CrewsRoute(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as CahierSortieApplication).appContainer
    val viewModel: CrewsViewModel = viewModel(
        factory = CrewsViewModel.factory(
            crewStore = appContainer.crewStore,
            rowerRepository = appContainer.rowerRepository,
            appLogStore = appContainer.appLogStore,
        ),
    )
    val uiState by viewModel.uiState.collectAsState()

    CrewsScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onOpenCreateDialog = viewModel::openCreateDialog,
        onOpenEditDialog = viewModel::openEditDialog,
        onDeleteCrew = viewModel::deleteCrew,
        onCloseCreateDialog = viewModel::closeCreateDialog,
        onCrewNameChanged = viewModel::onCrewNameChanged,
        onRowerSearchQueryChanged = viewModel::onRowerSearchQueryChanged,
        onRowerToggled = viewModel::onRowerToggled,
        onSaveCrew = viewModel::saveCrew,
    )
}

@Composable
fun CrewsScreen(
    contentPadding: PaddingValues,
    uiState: CrewsUiState,
    onSearchQueryChanged: (String) -> Unit,
    onOpenCreateDialog: () -> Unit,
    onOpenEditDialog: (Long) -> Unit,
    onDeleteCrew: (Long) -> Unit,
    onCloseCreateDialog: () -> Unit,
    onCrewNameChanged: (String) -> Unit,
    onRowerSearchQueryChanged: (String) -> Unit,
    onRowerToggled: (Long, Boolean) -> Unit,
    onSaveCrew: () -> Unit,
) {
    var crewPendingDeleteId by remember { mutableStateOf<Long?>(null) }

    if (uiState.isEditorDialogOpen) {
        CrewEditorDialog(
            uiState = uiState,
            onDismiss = onCloseCreateDialog,
            onCrewNameChanged = onCrewNameChanged,
            onRowerSearchQueryChanged = onRowerSearchQueryChanged,
            onRowerToggled = onRowerToggled,
            onSaveCrew = onSaveCrew,
        )
    }

    crewPendingDeleteId?.let { crewId ->
        DeleteConfirmationDialog(
            onConfirm = {
                onDeleteCrew(crewId)
                crewPendingDeleteId = null
            },
            onDismiss = { crewPendingDeleteId = null },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Équipages",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )

        AppTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            label = "Rechercher un équipage",
            modifier = Modifier.fillMaxWidth(),
            type = AppTextFieldType.SEARCH,
        )

        Button(
            onClick = onOpenCreateDialog,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Créer un équipage")
        }

        if (uiState.filteredCrews.isEmpty()) {
            Text(
                text = "Aucun équipage enregistré.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                uiState.filteredCrews.forEach { crew ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 2.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = crew.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = crew.rowerNames.joinToString().ifBlank { "Aucun rameur" },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { onOpenEditDialog(crew.id) },
                                ) {
                                    Text("Modifier")
                                }
                                OutlinedButton(
                                    onClick = { crewPendingDeleteId = crew.id },
                                ) {
                                    Text("Supprimer")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CrewEditorDialog(
    uiState: CrewsUiState,
    onDismiss: () -> Unit,
    onCrewNameChanged: (String) -> Unit,
    onRowerSearchQueryChanged: (String) -> Unit,
    onRowerToggled: (Long, Boolean) -> Unit,
    onSaveCrew: () -> Unit,
) {
    val rowerOptions = uiState.availableRowers
        .filter { rower ->
            val fullName = "${rower.firstName} ${rower.lastName}".trim()
            uiState.rowerSearchQuery.isBlank() || fullName.contains(uiState.rowerSearchQuery.trim(), ignoreCase = true)
        }
        .map { rower ->
            com.aca56.cahiersortiecodex.ui.components.SearchableSelectableOption(
                key = rower.id.toString(),
                label = "${rower.firstName} ${rower.lastName}".trim(),
            )
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (uiState.editingCrewId == null) {
                    "Créer un équipage"
                } else {
                    "Modifier l'équipage"
                },
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppTextField(
                    value = uiState.crewNameInput,
                    onValueChange = onCrewNameChanged,
                    label = "Nom de l'équipage",
                    modifier = Modifier.fillMaxWidth(),
                    type = AppTextFieldType.SIMPLE,
                )
                SearchableSelectableList(
                    searchQuery = uiState.rowerSearchQuery,
                    onSearchQueryChanged = onRowerSearchQueryChanged,
                    searchLabel = "Rechercher des rameurs",
                    selectedKeys = uiState.selectedRowerIds.map { it.toString() }.toSet(),
                    options = rowerOptions,
                    emptyLabel = "Aucun rameur disponible.",
                    noResultsLabel = "Aucun rameur ne correspond à la recherche.",
                    onOptionToggled = { optionKey ->
                        optionKey.toLongOrNull()?.let { rowerId ->
                            onRowerToggled(rowerId, rowerId !in uiState.selectedRowerIds)
                        }
                    },
                )
                uiState.message?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onSaveCrew) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
    )
}
