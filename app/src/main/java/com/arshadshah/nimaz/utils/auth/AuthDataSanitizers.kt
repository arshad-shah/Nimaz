package com.arshadshah.nimaz.utils.auth

import android.util.Patterns

class AuthDataSanitizers
{
	//email validation
	fun emailIsValid(email : String) : Boolean
	{
		return Patterns.EMAIL_ADDRESS.matcher(email).matches()
	}


	//password and confirm password must match
	fun passwordsMatch(password : String, confirmPassword : String) : Boolean
	{
		return password == confirmPassword
	}

	fun passwordStrength(password : String) : String
	{
		//8 characters long and contains a number and a special character and an uppercase letter
		val weakPassword = Regex("^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#\$%^&*])(?=.{8,})")
		//8 characters long and contains a number and a special character and an uppercase letter and a lowercase letter and a number
		val mediumPassword = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#\$%^&*])(?=.{8,})")
		//10 characters long and contains a number and a special character and an uppercase letter and a lowercase letter and a number and a special character with brackets
		val strongPassword = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#\$%^&*])(?=.*[(){}\\[\\]<>]).{10,}\$")
		val veryStrongPassword = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#\$%^&*])(?=.*[(){}\\[\\]<>]).{16,}\$")

		var message = "Password strength:"

		//check the password strength by using regex
		message += if (password.matches(veryStrongPassword)) {
			"Very Strong"
		} else if (password.matches(strongPassword)) {
			"Strong"
		} else if (password.matches(mediumPassword)) {
			"Medium"
		} else if (password.matches(weakPassword)) {
			"Weak"
		} else {
			"Very Weak"
		}

		return message
	}
}