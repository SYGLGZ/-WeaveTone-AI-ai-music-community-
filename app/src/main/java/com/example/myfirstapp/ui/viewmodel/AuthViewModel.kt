package com.example.myfirstapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfirstapp.data.remote.MusicApi
import com.example.myfirstapp.data.remote.dto.AuthRequest
import com.example.myfirstapp.data.remote.dto.AuthResponse
import com.example.myfirstapp.data.remote.dto.ErrorResponse
import com.example.myfirstapp.data.security.SecurePreferences
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val token: String? = null,
    val userId: Int? = null,
    val username: String? = null,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val musicApi: MusicApi,
    private val securePreferences: SecurePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        refreshAuthState()
    }

    fun refreshAuthState() {
        val savedToken = securePreferences.getAuthToken()
        if (savedToken != null) {
            _uiState.value = AuthUiState(
                isLoggedIn = true,
                token = savedToken,
                userId = securePreferences.getUserId(),
                username = securePreferences.getUsername()
            )
        } else {
            _uiState.value = AuthUiState()
        }
    }

    fun register(username: String, password: String, email: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = musicApi.register(AuthRequest(username, password, email))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    securePreferences.saveAuthToken(body.token)
                    body.userId?.let { securePreferences.saveUserId(it) }
                    body.username?.let { securePreferences.saveUsername(it) }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        token = body.token,
                        userId = body.userId,
                        username = body.username
                    )
                } else {
                    val errorMsg = parseErrorMessage(response)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "网络错误"
                )
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = musicApi.login(AuthRequest(username, password))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    securePreferences.saveAuthToken(body.token)
                    body.userId?.let { securePreferences.saveUserId(it) }
                    body.username?.let { securePreferences.saveUsername(it) }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        token = body.token,
                        userId = body.userId,
                        username = body.username
                    )
                } else {
                    val errorMsg = parseErrorMessage(response)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "网络错误"
                )
            }
        }
    }

    fun logout() {
        securePreferences.clearAuth()
        _uiState.value = AuthUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun parseErrorMessage(response: retrofit2.Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string() ?: return "错误 (${response.code()})"
            val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
            errorResponse.error
        } catch (_: Exception) {
            "错误 (${response.code()})"
        }
    }
}
