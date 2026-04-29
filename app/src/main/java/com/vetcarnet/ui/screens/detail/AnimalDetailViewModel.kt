package com.vetcarnet.ui.screens.detail

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

data class AnimalDetailUiState(
    val animal: Animal? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showDeleteDialog: Boolean = false,
    val isDeleted: Boolean = false
)

class AnimalDetailViewModel(
    private val repository: AnimalRepository = AnimalRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnimalDetailUiState())
    val uiState: StateFlow<AnimalDetailUiState> = _uiState.asStateFlow()

    fun loadAnimal(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getAnimalById(id)
                .onSuccess { animal ->
                    _uiState.update { it.copy(animal = animal, isLoading = false) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Erreur : ${err.message}")
                    }
                }
        }
    }

    fun requestDelete() = _uiState.update { it.copy(showDeleteDialog = true) }
    fun cancelDelete() = _uiState.update { it.copy(showDeleteDialog = false) }

    fun confirmDelete() {
        val id = _uiState.value.animal?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(showDeleteDialog = false, isLoading = true) }
            repository.deleteAnimal(id)
                .onSuccess { _uiState.update { it.copy(isLoading = false, isDeleted = true) } }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Suppression échouée : ${err.message}")
                    }
                }
        }
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }
}
