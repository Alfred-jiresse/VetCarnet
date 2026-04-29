package com.vetcarnet.ui.screens.form

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vetcarnet.data.repository.AnimalRepository
import com.vetcarnet.data.repository.AnimalRepositoryImpl
import com.vetcarnet.domain.model.Animal
import com.vetcarnet.domain.model.Espece
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AnimalFormUiState(
    val nom: String = "",
    val espece: Espece = Espece.CHIEN,
    val race: String = "",
    val nomProprietaire: String = "",
    val telephoneProprietaire: String = "",
    val dateNaissance: LocalDate? = null,
    val dateProchainVaccin: LocalDate? = null,
    val photoUri: Uri? = null,
    val photoUrl: String? = null,
    val notes: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val isEditMode: Boolean = false,
    val animalId: String = "",

    // Validation
    val nomError: String? = null,
    val proprietaireError: String? = null
) {
    val isFormValid: Boolean
        get() = nom.isNotBlank() && nomProprietaire.isNotBlank()
}

class AnimalFormViewModel(
    private val repository: AnimalRepository = AnimalRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnimalFormUiState())
    val uiState: StateFlow<AnimalFormUiState> = _uiState.asStateFlow()

    fun loadAnimal(animalId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getAnimalById(animalId)
                .onSuccess { animal ->
                    _uiState.update {
                        AnimalFormUiState(
                            animalId = animal.id,
                            nom = animal.nom,
                            espece = animal.espece,
                            race = animal.race,
                            nomProprietaire = animal.nomProprietaire,
                            telephoneProprietaire = animal.telephoneProprietaire,
                            dateNaissance = animal.dateNaissance,
                            dateProchainVaccin = animal.dateProchainVaccin,
                            photoUrl = animal.photoUrl,
                            notes = animal.notes,
                            isEditMode = true,
                            isLoading = false
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Erreur : ${err.message}")
                    }
                }
        }
    }

    fun onNomChange(value: String) = _uiState.update { it.copy(nom = value, nomError = null) }
    fun onEspeceChange(value: Espece) = _uiState.update { it.copy(espece = value) }
    fun onRaceChange(value: String) = _uiState.update { it.copy(race = value) }
    fun onNomProprietaireChange(value: String) = _uiState.update { it.copy(nomProprietaire = value, proprietaireError = null) }
    fun onTelephoneChange(value: String) = _uiState.update { it.copy(telephoneProprietaire = value) }
    fun onDateNaissanceChange(value: LocalDate?) = _uiState.update { it.copy(dateNaissance = value) }
    fun onDateVaccinChange(value: LocalDate?) = _uiState.update { it.copy(dateProchainVaccin = value) }
    fun onPhotoUriChange(value: Uri?) = _uiState.update { it.copy(photoUri = value) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }
    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    fun saveAnimal(context: Context) {
        val state = _uiState.value

        // Validation
        var hasError = false
        if (state.nom.isBlank()) {
            _uiState.update { it.copy(nomError = "Le nom est requis") }
            hasError = true
        }
        if (state.nomProprietaire.isBlank()) {
            _uiState.update { it.copy(proprietaireError = "Le propriétaire est requis") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Upload photo si nouvelle sélection
            var photoUrl = state.photoUrl
            if (state.photoUri != null) {
                repository.uploadPhoto(context, state.photoUri)
                    .onSuccess { url -> photoUrl = url }
                    .onFailure { err ->
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = "Erreur upload photo : ${err.message}")
                        }
                        return@launch
                    }
            }

            val animal = Animal(
                id = state.animalId,
                nom = state.nom,
                espece = state.espece,
                race = state.race,
                nomProprietaire = state.nomProprietaire,
                telephoneProprietaire = state.telephoneProprietaire,
                dateNaissance = state.dateNaissance,
                dateProchainVaccin = state.dateProchainVaccin,
                photoUrl = photoUrl,
                notes = state.notes
            )

            val result = if (state.isEditMode) repository.updateAnimal(animal)
                         else repository.createAnimal(animal)

            result
                .onSuccess { _uiState.update { it.copy(isLoading = false, isSaved = true) } }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Enregistrement échoué : ${err.message}")
                    }
                }
        }
    }
}
