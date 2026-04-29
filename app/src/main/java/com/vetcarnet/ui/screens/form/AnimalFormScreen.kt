package com.vetcarnet.ui.screens.form

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.vetcarnet.domain.model.Espece
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalFormScreen(
    animalId: String?,
    onNavigateBack: () -> Unit,
    viewModel: AnimalFormViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Charger l'animal si mode édition
    LaunchedEffect(animalId) {
        if (animalId != null) viewModel.loadAnimal(animalId)
    }

    // Navigation après sauvegarde
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    // Erreurs
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // Launcher pour la galerie
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> viewModel.onPhotoUriChange(uri) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isEditMode) "Modifier le dossier" else "Nouveau patient")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Photo de profil ──────────────────────────────────────────────────
            Box(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.BottomEnd
            ) {
                val imageModel = uiState.photoUri ?: uiState.photoUrl
                if (imageModel != null) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = "Photo de l'animal",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") }
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            Icons.Outlined.PhotoCamera,
                            contentDescription = "Ajouter une photo",
                            modifier = Modifier.padding(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                SmallFloatingActionButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Changer photo", modifier = Modifier.size(16.dp))
                }
            }

            // ── Section Animal ───────────────────────────────────────────────────
            SectionTitle("Informations de l'animal")

            OutlinedTextField(
                value = uiState.nom,
                onValueChange = viewModel::onNomChange,
                label = { Text("Nom de l'animal *") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.nomError != null,
                supportingText = uiState.nomError?.let { { Text(it) } },
                leadingIcon = { Icon(Icons.Default.Pets, contentDescription = null) },
                singleLine = true
            )

            // Sélecteur d'espèce
            EspeceSelector(
                selected = uiState.espece,
                onSelect = viewModel::onEspeceChange
            )

            OutlinedTextField(
                value = uiState.race,
                onValueChange = viewModel::onRaceChange,
                label = { Text("Race") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            DatePickerField(
                label = "Date de naissance",
                date = uiState.dateNaissance,
                onDateSelected = viewModel::onDateNaissanceChange
            )

            // ── Section Propriétaire ─────────────────────────────────────────────
            SectionTitle("Propriétaire")

            OutlinedTextField(
                value = uiState.nomProprietaire,
                onValueChange = viewModel::onNomProprietaireChange,
                label = { Text("Nom du propriétaire *") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.proprietaireError != null,
                supportingText = uiState.proprietaireError?.let { { Text(it) } },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.telephoneProprietaire,
                onValueChange = viewModel::onTelephoneChange,
                label = { Text("Téléphone") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                singleLine = true
            )

            // ── Section Vaccination ──────────────────────────────────────────────
            SectionTitle("Vaccination")

            DatePickerField(
                label = "Date du prochain vaccin",
                date = uiState.dateProchainVaccin,
                onDateSelected = viewModel::onDateVaccinChange,
                minDate = LocalDate.now()
            )

            // ── Notes ────────────────────────────────────────────────────────────
            SectionTitle("Notes")

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes de consultation") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                maxLines = 6
            )

            // ── Bouton de sauvegarde ─────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.saveAnimal(context) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !uiState.isLoading
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (uiState.isEditMode) "Mettre à jour" else "Enregistrer le patient")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.primaryContainer)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EspeceSelector(
    selected: Espece,
    onSelect: (Espece) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Espèce") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Espece.entries.forEach { espece ->
                DropdownMenuItem(
                    text = { Text(espece.label) },
                    onClick = {
                        onSelect(espece)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    date: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    minDate: LocalDate? = null
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date?.toEpochDay()?.times(86400000L)
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val selectedDate = LocalDate.ofEpochDay(millis / 86400000L)
                        onDateSelected(selectedDate)
                    }
                    showDialog = false
                }) { Text("Confirmer") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Annuler") }
            }
        ) { DatePicker(state = state) }
    }

    OutlinedTextField(
        value = date?.format(DATE_FORMATTER) ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().clickable { showDialog = true },
        enabled = false,
        trailingIcon = {
            Row {
                AnimatedVisibility(visible = date != null) {
                    IconButton(onClick = { onDateSelected(null) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Effacer")
                    }
                }
                IconButton(onClick = { showDialog = true }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Choisir une date")
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
