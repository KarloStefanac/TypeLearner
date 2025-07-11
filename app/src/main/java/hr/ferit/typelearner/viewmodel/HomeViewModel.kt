package hr.ferit.typelearner.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.auth.User
import hr.ferit.typelearner.model.UserData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun setUser(user: UserData?) {
        _uiState.value = _uiState.value.copy(user = user)
    }

    fun clearUser() {
        _uiState.value = _uiState.value.copy(user = null)
    }

    data class HomeUiState(
        val user: UserData? = null
    )
}