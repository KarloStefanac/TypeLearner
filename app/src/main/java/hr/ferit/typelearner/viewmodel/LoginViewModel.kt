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

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun resetState() {
        _uiState.value = LoginUiState()
    }

    suspend fun login(): Result<UserData> {
        return try {
            val result = repository.authenticateUser(_uiState.value.email, _uiState.value.password)
            _uiState.value = _uiState.value.copy(
                loginError = if (result.isSuccess) null else result.exceptionOrNull()?.message
            )
            result
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(loginError = e.message)
            Result.failure(e)
        }
    }

    data class LoginUiState(
        val email: String = "",
        val password: String = "",
        val loginError: String? = null
    )
}