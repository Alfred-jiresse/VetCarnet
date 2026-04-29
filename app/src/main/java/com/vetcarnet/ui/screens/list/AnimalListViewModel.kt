package com.vetcarnet.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vetcarnet.data.repository.AnimalRepository
import com.vetcarnet.data.repository.AnimalRepositoryImpl
import com.vetcarnet.domain.model.Animal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

// ─── UI State ─────────────────────────────────────────────────────────────────

data class AnimalListUiState(
    val animaux: List<Animal> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val showDeleteDialog: Animal? = null
) {
    val filteredAnimaux: List<Animal>
        get() = if (searchQuery.isBlank()) animaux
        else animaux.filter {
            it.nom.contains(searchQuery, ignoreCase = true) ||
            it.nomProprietaire.contains(searchQuery, ignoreCase = true) ||
            it.espece.label.contains(searchQuery, ignoreCase = true)
        }

    val urgentsAujourdhui: List<Animal>
        get() = animaux.filter { it.dateProchainVaccin == LocalDate.now() }

    val urgentsDemain: List<Animal>
        get() = animaux.filter { it.dateProchainVaccin == LocalDate.now().plusDays(1) }
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

class AnimalListViewModel(
    private val repository: AnimalRepository = AnimalRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnimalListUiState())
    val uiState: StateFlow<AnimalListUiState> = _uiState.asStateFlow()

    init {
        loadAnimaux()
    }

    fun loadAnimaux() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.getAllAnimaux()
                .onSuccess { list ->
                    _uiState.update { it.copy(animaux = list, isLoading = false) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Erreur de chargement : ${err.message}"
                        )
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun requestDelete(animal: Animal) {
        _uiState.update { it.copy(showDeleteDialog = animal) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(showDeleteDialog = null) }
    }

    fun confirmDelete(animal: Animal) {
        viewModelScope.launch {
            _uiState.update { it.copy(showDeleteDialog = null, isLoading = true) }
            repository.deleteAnimal(animal.id)
                .onSuccess { loadAnimaux() }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Suppression échouée : ${err.message}")
                    }
                }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
