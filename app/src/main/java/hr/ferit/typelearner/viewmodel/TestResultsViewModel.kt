package hr.ferit.typelearner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.ferit.typelearner.model.TestResultData
import hr.ferit.typelearner.model.repository.ModelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TestResultsViewModel(private val repository: ModelRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(TestResultsUiState())
    val uiState: StateFlow<TestResultsUiState> = _uiState.asStateFlow()

    fun loadTestResults(userId: String) {
        viewModelScope.launch {
            val result = repository.getUserTestResults(userId)
            _uiState.value = when {
                result.isSuccess -> TestResultsUiState(testResults = result.getOrNull() ?: emptyList())
                else -> TestResultsUiState(error = result.exceptionOrNull()?.message ?: "Failed to load test results")
            }
        }
    }

    data class TestResultsUiState(
        val testResults: List<TestResultData> = emptyList(),
        val error: String? = null
    )
}