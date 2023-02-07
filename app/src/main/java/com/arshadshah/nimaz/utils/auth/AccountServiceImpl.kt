package com.arshadshah.nimaz.utils.auth

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class AccountServiceImpl: AccountService
{

	override fun isLoggedin() : Boolean
	{
		return Firebase.auth.currentUser != null
	}


	override fun authenticate(email : String , password : String , onResult : (Throwable?) -> Unit)
	{
		Firebase.auth.signInWithEmailAndPassword(email , password)
			.addOnCompleteListener { onResult(it.exception) }
	}

	override fun createAccount(email : String , password : String , onResult : (Throwable?) -> Unit)
	{
		Firebase.auth.createUserWithEmailAndPassword(email , password)
			.addOnCompleteListener { onResult(it.exception) }
	}


	override fun linkAccount(email : String , password : String , onResult : (Throwable?) -> Unit)
	{
		val credential = EmailAuthProvider.getCredential(email , password)

		Firebase.auth.currentUser!!.linkWithCredential(credential)
			.addOnCompleteListener { onResult(it.exception) }
	}

	//forgot password
	override fun sendPasswordResetEmail(email : String , onResult : (Throwable?) -> Unit)
	{
		Firebase.auth.sendPasswordResetEmail(email)
			.addOnCompleteListener { onResult(it.exception) }
	}

	override fun getUser(onResult : (FirebaseUser?) -> Unit)
	{
		onResult(Firebase.auth.currentUser)
	}

	override fun logout()
	{
		Firebase.auth.signOut()
	}
}