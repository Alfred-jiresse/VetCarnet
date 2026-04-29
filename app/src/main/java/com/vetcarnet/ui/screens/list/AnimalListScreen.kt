package com.vetcarnet.ui.screens.list

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vetcarnet.ui.components.AnimalCard
import com.vetcarnet.ui.components.VaccinationAlertBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalListScreen(
    onNavigateToForm: (String?) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: AnimalListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Snackbar pour les erreurs
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // Dialog de confirmation de suppression
    uiState.showDeleteDialog?.let { animal ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Supprimer ${animal.nom} ?") },
            text = { Text("Cette action est irréversible. Le dossier de ${animal.nom} sera définitivement supprimé.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete(animal) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) { Text("Annuler") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("VetCarnet", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Carnet de santé vétérinaire",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAnimaux() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToForm(null) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nouveau patient") }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // Barre de recherche
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Bannières d'alerte vaccins
            AnimatedVisibility(visible = uiState.urgentsAujourdhui.isNotEmpty()) {
                VaccinationAlertBanner(
                    animaux = uiState.urgentsAujourdhui,
                    isToday = true
                )
            }
            AnimatedVisibility(visible = uiState.urgentsDemain.isNotEmpty()) {
                VaccinationAlertBanner(
                    animaux = uiState.urgentsDemain,
                    isToday = false
                )
            }

            // Compteur
            if (!uiState.isLoading) {
                Text(
                    text = "${uiState.filteredAnimaux.size} patient(s)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            // Contenu principal
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    uiState.filteredAnimaux.isEmpty() -> {
                        EmptyState(
                            modifier = Modifier.align(Alignment.Center),
                            isSearch = uiState.searchQuery.isNotBlank()
                        )
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp,
                                top = 8.dp, bottom = 80.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = uiState.filteredAnimaux,
                                key = { it.id }
                            ) { animal ->
                                AnimalCard(
                                    animal = animal,
                                    onClick = { onNavigateToDetail(animal.id) },
                                    onEdit = { onNavigateToForm(animal.id) },
                                    onDelete = { viewModel.requestDelete(animal) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Chercher un animal, propriétaire…") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            AnimatedVisibility(visible = query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Effacer")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier, isSearch: Boolean) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Pets,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Text(
            text = if (isSearch) "Aucun résultat" else "Aucun patient enregistré",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (isSearch) "Essayez avec un autre terme de recherche."
            else "Ajoutez votre premier patient en appuyant sur +",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
