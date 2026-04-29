package com.vetcarnet.ui.screens.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.vetcarnet.domain.model.Animal
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val DATE_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalDetailScreen(
    animalId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: AnimalDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(animalId) { viewModel.loadAnimal(animalId) }
    LaunchedEffect(uiState.isDeleted) { if (uiState.isDeleted) onNavigateBack() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // Dialog suppression
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Supprimer ce dossier ?") },
            text = {
                Text("Le dossier de ${uiState.animal?.nom ?: "cet animal"} sera définitivement supprimé.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete() },
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
                title = { Text(uiState.animal?.nom ?: "Dossier patient") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    uiState.animal?.let {
                        IconButton(onClick = { onNavigateToEdit(it.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier")
                        }
                        IconButton(onClick = { viewModel.requestDelete() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Supprimer",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->

        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.animal == null -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Dossier introuvable", style = MaterialTheme.typography.titleMedium)
            }

            else -> {
                val animal = uiState.animal!!
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically()
                ) {
                    AnimalDetailContent(
                        animal = animal,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimalDetailContent(animal: Animal, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val daysUntilVaccin = animal.dateProchainVaccin?.let { ChronoUnit.DAYS.between(today, it) }
    val age = animal.dateNaissance?.let { Period.between(it, today) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        // ── Header avec photo ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (animal.photoUrl != null) {
                    AsyncImage(
                        model = animal.photoUrl,
                        contentDescription = "Photo de ${animal.nom}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(110.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            Icons.Default.Pets,
                            contentDescription = null,
                            modifier = Modifier.padding(26.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = animal.nom,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = buildString {
                        append(animal.espece.label)
                        if (animal.race.isNotBlank()) append(" · ${animal.race}")
                        age?.let {
                            val ageStr = when {
                                it.years > 0 -> "${it.years} an${if (it.years > 1) "s" else ""}"
                                it.months > 0 -> "${it.months} mois"
                                else -> "${it.days} jour${if (it.days > 1) "s" else ""}"
                            }
                            append(" · $ageStr")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        // ── Statut vaccin ──────────────────────────────────────────────────────
        animal.dateProchainVaccin?.let { date ->
            val (color, icon, message) = when {
                daysUntilVaccin!! < 0 -> Triple(
                    MaterialTheme.colorScheme.errorContainer,
                    Icons.Filled.Warning,
                    "Vaccin en retard depuis ${-daysUntilVaccin} jour(s) · ${date.format(DATE_FMT)}"
                )
                daysUntilVaccin == 0L -> Triple(
                    MaterialTheme.colorScheme.errorContainer,
                    Icons.Filled.Vaccines,
                    "Vaccination prévue AUJOURD'HUI · ${date.format(DATE_FMT)}"
                )
                daysUntilVaccin == 1L -> Triple(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    Icons.Filled.Vaccines,
                    "Vaccination prévue DEMAIN · ${date.format(DATE_FMT)}"
                )
                daysUntilVaccin <= 7 -> Triple(
                    MaterialTheme.colorScheme.secondaryContainer,
                    Icons.Outlined.Vaccines,
                    "Vaccin dans $daysUntilVaccin jours · ${date.format(DATE_FMT)}"
                )
                else -> Triple(
                    MaterialTheme.colorScheme.surfaceVariant,
                    Icons.Outlined.Vaccines,
                    "Prochain vaccin · ${date.format(DATE_FMT)}"
                )
            }

            Surface(color = color, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(icon, contentDescription = null)
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Sections info ──────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

            DetailSection(title = "Propriétaire") {
                DetailRow(Icons.Default.Person, "Nom", animal.nomProprietaire)
                if (animal.telephoneProprietaire.isNotBlank()) {
                    DetailRow(Icons.Default.Phone, "Téléphone", animal.telephoneProprietaire)
                }
            }

            Spacer(Modifier.height(16.dp))

            DetailSection(title = "Animal") {
                DetailRow(Icons.Default.Pets, "Espèce", animal.espece.label)
                if (animal.race.isNotBlank()) {
                    DetailRow(Icons.Default.Category, "Race", animal.race)
                }
                animal.dateNaissance?.let {
                    DetailRow(Icons.Default.Cake, "Naissance", it.format(DATE_FMT))
                }
            }

            if (animal.notes.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                DetailSection(title = "Notes de consultation") {
                    Text(
                        text = animal.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
