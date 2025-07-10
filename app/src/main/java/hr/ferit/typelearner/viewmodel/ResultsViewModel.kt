package hr.ferit.typelearner.viewmodel

import androidx.lifecycle.ViewModel
import hr.ferit.typelearner.model.repository.ModelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ResultsViewModel(private val repository: ModelRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    suspend fun initialize(wpm: Float, accuracy: Float, duration: Float, userId: String){
        val stats = repository.getStatistics(userId).getOrNull()
        _uiState.value = ResultsUiState(
            wpm = wpm,
            accuracy = accuracy,
            duration = duration,
            averageWpm = stats?.wpm ?: 0.0f,
            topWpm = stats?.topWpm ?: 0.0f,
            testsFinished = stats?.testsFinished ?: 0
        )
    }

    data class ResultsUiState(
        val wpm: Float = 0.0f,
        val accuracy: Float = 0.0f,
        val duration: Float = 0.0f,
        val averageWpm: Float = 0.0f,
        val averageAccuracy: Float = 0.0f,
        val topWpm: Float = 0.0f,
        val testsFinished: Int = 0
    )
}