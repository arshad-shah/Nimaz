package com.arshadshah.nimaz.utils.auth

import com.google.firebase.auth.FirebaseUser

interface AccountService {
	fun isLoggedin(): Boolean
	fun authenticate(email: String, password: String, onResult: (Throwable?) -> Unit)
	fun createAccount(email: String, password: String, onResult: (Throwable?) -> Unit)
	fun linkAccount(email: String, password: String, onResult: (Throwable?) -> Unit)
	//forgot password
	fun sendPasswordResetEmail(email: String, onResult: (Throwable?) -> Unit)

	//get user details
	fun getUser(onResult: (FirebaseUser?) -> Unit)

	//logout
	fun logout()
}