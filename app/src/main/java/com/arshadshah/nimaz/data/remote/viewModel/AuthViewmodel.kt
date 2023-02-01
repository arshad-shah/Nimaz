package com.arshadshah.nimaz.data.remote.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.utils.auth.AccountServiceImpl
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
	sealed class LoginUiState {
		object Loading : LoginUiState()
		class Success(val data: String) : LoginUiState()
		data class Error(val message: String) : LoginUiState()
	}

	private val _loginUiState = MutableStateFlow(LoginUiState.Loading as LoginUiState)
	val loginUiState: StateFlow<LoginUiState>
		get() = _loginUiState

	private val _isLoggedIn = MutableStateFlow(false)
	val isLoggedIn: StateFlow<Boolean>
		get() = _isLoggedIn

	private val _firebaseUser = MutableStateFlow<FirebaseUser?>(null)
	val firebaseUser: StateFlow<FirebaseUser?>
		get() = _firebaseUser

	private val accountService = AccountServiceImpl()

	init {
		checkLoginState()
	}

	private fun checkLoginState() {
		viewModelScope.launch(Dispatchers.IO) {
			_isLoggedIn.value = accountService.isLoggedin()
		}
	}

	fun getFirebaseUser() {
		viewModelScope.launch(Dispatchers.IO) {
			accountService.getUser {
				_firebaseUser.value = it
			}
		}
	}

	fun login(email: String, password: String) {
		viewModelScope.launch(Dispatchers.IO) {
			accountService.authenticate(email, password) { error ->
				if (error == null) {
					checkLoginState()
					_loginUiState.value = LoginUiState.Success("Login Success")
				} else {
					_loginUiState.value = LoginUiState.Error(error.message!!)
				}
			}
		}
	}

	fun createAccount(email: String, password: String) {
		viewModelScope.launch(Dispatchers.IO) {
			accountService.createAccount(email, password) { error ->
				if (error == null) {
					checkLoginState()
					_loginUiState.value = LoginUiState.Success("Account Created")
				} else {
					_loginUiState.value = LoginUiState.Error(error.message!!)
				}
			}
		}
	}
}
