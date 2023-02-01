package com.arshadshah.nimaz.data.remote.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.utils.auth.AccountServiceImpl
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

	// Login state
	sealed class LoginState {
		object Loading : LoginState()
		class Success : LoginState()
		data class Error(val message : String) : LoginState()
	}

	private val _loginUiState = MutableStateFlow<LoginState>(LoginState.Loading)
	val loginUiState: StateFlow<LoginState> = _loginUiState

	// User state
	sealed class UserState {
		object Loading : UserState()
		data class Success(val data: FirebaseUser?) : UserState()
		data class Error(val message: String) : UserState()
	}

	private val _userStateFlow = MutableStateFlow<UserState>(UserState.Loading)
	val userStateFlow: StateFlow<UserState> = _userStateFlow

	private val accountService = AccountServiceImpl()

	init {
		checkLogin()
		getUser()
	}

	fun checkLogin() = viewModelScope.launch(Dispatchers.IO) {
		if (accountService.isLoggedin()) {
			_loginUiState.value = LoginState.Success()
		} else {
			_loginUiState.value = LoginState.Error("Not logged in")
		}
	}

	fun getUser() = viewModelScope.launch(Dispatchers.IO) {
		accountService.getUser {
			if (it != null) {
				_userStateFlow.value = UserState.Success(it)
			} else {
				_userStateFlow.value = UserState.Error("User not found")
			}
		}
	}

	fun logout() = viewModelScope.launch(Dispatchers.IO) {
		accountService.logout()
		checkLogin()
	}
}
