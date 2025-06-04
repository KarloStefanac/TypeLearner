package hr.ferit.typelearner.viewmodel


import androidx.lifecycle.ViewModel
import hr.ferit.typelearner.model.repository.ModelRepository
import hr.ferit.typelearner.model.UserData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel(private val repository: ModelRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    suspend fun login(): Result<UserData> {
        val result = repository.authenticateUser(_uiState.value.username, _uiState.value.password)
        _uiState.value = _uiState.value.copy(
            loginError = if (result.isSuccess) null else result.exceptionOrNull()?.message
        )
        return result
    }

    data class LoginUiState(
        val username: String = "",
        val password: String = "",
        val loginError: String? = null
    )
}