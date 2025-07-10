package hr.ferit.typelearner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.ferit.typelearner.model.TestData
import hr.ferit.typelearner.model.repository.ModelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TestsViewModel(private val repository: ModelRepository) : ViewModel(){

    private val _uiState = MutableStateFlow(TestUiState())
    val uiState: StateFlow<TestUiState> = _uiState.asStateFlow()

    fun loadTests(){
        viewModelScope.launch {
            val result = repository.getAllTests()
            _uiState.value = when {
                result.isSuccess -> TestUiState(tests = result.getOrNull() ?: emptyList())
                else -> TestUiState(error = result.exceptionOrNull()?.message ?: "Failed to load tests")
            }
        }
    }

    data class TestUiState(
        val tests: List<TestData> = emptyList(),
        val error: String? = null
    )
}