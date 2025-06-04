package hr.ferit.typelearner.viewmodel

import androidx.lifecycle.ViewModel
import hr.ferit.typelearner.model.repository.ModelRepository
import hr.ferit.typelearner.model.UserData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RegisterViewModel(private val repository: ModelRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    suspend fun register(): Result<UserData> {
        val user = UserData(
            username = _uiState.value.username,
            email = _uiState.value.email
        )
        val result = repository.addUser(user, _uiState.value.password)
        _uiState.value = _uiState.value.copy(
            registerError = if (result.isSuccess) null else result.exceptionOrNull()?.message
        )
        return if (result.isSuccess) Result.success(user) else Result.failure(result.exceptionOrNull() ?: Exception("Registration failed"))
    }

    data class RegisterUiState(
        val username: String = "",
        val email: String = "",
        val password: String = "",
        val registerError: String? = null
    )
}