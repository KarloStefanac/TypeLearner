package hr.ferit.typelearner.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.ferit.typelearner.model.repository.ModelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: ModelRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            val user = repository.getUser(userId).getOrNull()
            Log.d("TypingVM", "User: ${user?.id}, ${user?.username}")
            val stats = repository.getStatistics(userId)
            _uiState.value = ProfileUiState(
                username = user?.username ?: "Unknown",
                wpm = stats?.wpm ?: 0.0f,
                accuracy = stats?.accuracy ?: 0.0f,
                topWpm = stats?.topWpm ?: 0.0f,
                testsFinished = stats?.testsFinished ?: 0,
                error = if (user == null || stats == null) "Failed to load profile data" else null
            )
        }
    }

    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit){
        viewModelScope.launch {
            Log.d("ProfileViewModel", "Starting account deletion")
            val result = repository.deleteUser()
            if (result.isSuccess) {
                Log.d("ProfileViewModel", "Account deletion successful")
                _uiState.value = _uiState.value.copy(deleteError = null)
                onSuccess()
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Failed to delete account"
                Log.e("ProfileViewModel", "Account deletion failed: $errorMessage")
                _uiState.value = _uiState.value.copy(deleteError = errorMessage)
                onError(errorMessage)
            }
        }
    }

    data class ProfileUiState(
        val username: String = "",
        val wpm: Float = 0.0f,
        val accuracy: Float = 0.0f,
        val topWpm: Float = 0.0f,
        val testsFinished: Int = 0,
        val error: String? = null,
        val deleteError: String? = null
    )
}