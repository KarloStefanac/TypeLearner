package hr.ferit.typelearner.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CustomTestViewModel  : ViewModel() {
    private val _uiState = MutableStateFlow(CustomTestUiState())
    val uiState: StateFlow<CustomTestUiState> = _uiState.asStateFlow()

    fun updateCustomText(text: String) {
        _uiState.value = _uiState.value.copy(customText = text)
    }

    fun updateTimeLimit(time: String) {
        _uiState.value = _uiState.value.copy(timeLimit = time)
    }

    fun updateMinAccuracy(accuracy: String) {
        _uiState.value = _uiState.value.copy(minAccuracy = accuracy)
    }

    fun resetState() {
        _uiState.value = CustomTestUiState()
    }

    data class CustomTestUiState(
        val customText: String = "",
        val timeLimit: String = "",
        val minAccuracy: String = ""
    )
}