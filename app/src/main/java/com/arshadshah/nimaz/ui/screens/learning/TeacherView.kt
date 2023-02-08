package com.arshadshah.nimaz.ui.screens.learning

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.data.remote.models.Classroom
import com.arshadshah.nimaz.data.remote.viewModel.AuthViewModel
import com.arshadshah.nimaz.utils.auth.AccountServiceImpl
import es.dmoral.toasty.Toasty

@Composable
fun TeacherView() {

	val context = LocalContext.current
	val accountService = AccountServiceImpl()
	val viewModel = viewModel<AuthViewModel>()

	val userId = remember { mutableStateOf("") }

	//observe the classes this teacher has and update the list when it changes
	val classes = viewModel.classes.collectAsState()

	LaunchedEffect(key1 = classes.value) {
		accountService.getUser {
			userId.value = it?.uid ?: ""
		}
		viewModel.getClasses(userId.value)
	}

	val listOfClassesAvailable = remember { mutableStateOf(listOf<Classroom>()) }

	//a button to create a new class
	Button(onClick ={
		viewModel.createClass("testClass", "testTeacher", "testTeacherEmail")
	}) {
		Text(text = "Create Class")
	}

	//add a student to a class
	Button(onClick ={
		viewModel.addStudentToClass(listOfClassesAvailable.value[0].classCode, "testStudent", "testStudentEmail", userId.value,)
	}) {
		Text(text = "Add Student")
	}

	when(val listOfClasses = classes.value)
	{
		is AuthViewModel.ClassUiState.Loading ->
		{
			Text(text = "Loading")
		}

		is AuthViewModel.ClassUiState.Error ->
		{
			Text(text = "Error")
		}

		is AuthViewModel.ClassUiState.Success ->
		{
			listOfClasses.data.forEach {
				listOfClassesAvailable.value = listOfClassesAvailable.value + it
			}
			//if te teacher has no classes then display a message
			if (listOfClasses.data.isEmpty())
			{
				Text(text = "You have no classes")
			}else{
				Text(text = "You have ${listOfClasses.data.size} classes")
			}
			//a list of all the classes this teacher has
			LazyColumn {
				//a list of all the classes this teacher has
				listOfClasses.data.forEach {
					item {
						//editable text thatallows copying so that the teacher can share the class code with students
						val text = AnnotatedString(it.classCode)
						ClickableText(text = text , onClick = {
							//copy the class code to the clipboard
							//get system clipboard
							val clipboard =
								context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
							//copy the class code to the clipboard
							clipboard.setPrimaryClip(
									android.content.ClipData.newPlainText(
											"classCode" ,
											text.text
																		 )
													)
							Toasty.success(context , "Class Code Copied").show()
						}
									 )
						Text(text = it.className)
						Text(text = "Assignments: ${it.assignments.size}")
						Text(text = "Students: ${it.students.size}")
					}
				}
			}
		}
	}
}