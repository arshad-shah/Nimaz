package com.arshadshah.nimaz.data.remote.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.remote.models.Classroom
import com.arshadshah.nimaz.utils.auth.AccountServiceImpl
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
	sealed class LoginUiState {
		object Loading : LoginUiState()
		class Success(val data: String) : LoginUiState()
		object LoggedIn : LoginUiState()
		class user(val data: FirebaseUser?) : LoginUiState()
		data class Error(val message: String) : LoginUiState()
	}

	private val _loginUiState = MutableStateFlow(LoginUiState.Loading as LoginUiState)
	val loginUiState = _loginUiState.asStateFlow()


	//class state
	sealed class ClassUiState {
		object Loading : ClassUiState()
		class Success(val data: ArrayList<Classroom>) : ClassUiState()
		data class Error(val message: String) : ClassUiState()
	}


	//live data for the classes this teacher has
	private val _classes = MutableStateFlow(ClassUiState.Loading as ClassUiState)
	val classes = _classes.asStateFlow()

	private val accountService = AccountServiceImpl()

	init {
		checkLoginState()
	}

	fun checkLoginState() = viewModelScope.launch(Dispatchers.IO) {
		if (accountService.isLoggedin()) {
			_loginUiState.value = LoginUiState.LoggedIn
		} else {
			_loginUiState.value = LoginUiState.Error("Not logged in")
		}
	}

	fun getFirebaseUser() {
		viewModelScope.launch(Dispatchers.IO) {
			accountService.getUser {
				if (it != null) {
					_loginUiState.value = LoginUiState.user(it)
				} else {
					_loginUiState.value = LoginUiState.Error("User not found")
				}
			}
		}
	}

	fun login(email: String, password: String) {
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				accountService.authenticate(email, password) { error ->
					if (error == null) {
						_loginUiState.value = LoginUiState.LoggedIn
					} else {
						_loginUiState.value = LoginUiState.Error(error.message!!)
					}
				}
			}
			catch (e: Exception)
			{
				Log.d("AuthViewModel", "login: ${e.message}")
				_loginUiState.value = LoginUiState.Error(e.message!!)
			}
		}
	}

	fun createAccount(email: String, password: String, name:String, role : String) {
		viewModelScope.launch(Dispatchers.IO) {
			accountService.createAccount(email, password , name , role)
			{ error ->
				if (error == null) {
					checkLoginState()
					_loginUiState.value = LoginUiState.Success("Account Created")
				} else {
					_loginUiState.value = LoginUiState.Error(error.message!!)
				}
			}
		}
	}

	//forgot password
	fun sendPasswordResetEmail(email: String) {
		viewModelScope.launch(Dispatchers.IO) {
			accountService.sendPasswordResetEmail(email) { error ->
				if (error == null) {
					_loginUiState.value = LoginUiState.Success("Email Sent")
				} else {
					_loginUiState.value = LoginUiState.Error(error.message!!)
				}
			}
		}
	}

	//create a new class
	fun createClass(className: String, teacherName: String, teacherEmail: String) {
		viewModelScope.launch(Dispatchers.IO) {
			accountService.createClassDocument(className,teacherName, teacherEmail){error ->
				if (error == null) {
					_loginUiState.value = LoginUiState.Success("Class Created")
				} else {
					_loginUiState.value = LoginUiState.Error(error.message!!)
				}
			}
		}
	}

	//get a all the class for a teacher
	//fun getClassesForTeacher(teacherCode: String, onResult: (Throwable?, ArrayList<Classroom>?) -> Unit)
	fun getClasses(teacherCode: String) {
		viewModelScope.launch(Dispatchers.IO) {
			accountService.getClassesForTeacher(teacherCode){error, classes ->
				if (error == null) {
					_classes.value = ClassUiState.Success(classes!!)
				} else {
					_classes.value = ClassUiState.Error(error.message!!)
				}
			}
		}
	}

	//add student to class
	fun addStudentToClass(classCode: String, studentName: String, studentEmail: String, teacherCode : String) {
		viewModelScope.launch(Dispatchers.IO) {
			accountService.addStudentToClass(classCode, studentName, studentEmail){error ->
				if (error == null) {
					//refetch classes
					getClasses(teacherCode)
				} else {
					_loginUiState.value = LoginUiState.Error(error.message!!)
				}
			}
		}
	}
}
