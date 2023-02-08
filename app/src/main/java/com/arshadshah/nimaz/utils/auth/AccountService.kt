package com.arshadshah.nimaz.utils.auth

import com.arshadshah.nimaz.data.remote.models.Classroom
import com.google.firebase.auth.FirebaseUser
import java.io.File

interface AccountService {
	fun isLoggedin(): Boolean

	fun authenticate(email: String, password: String, onResult: (Throwable?) -> Unit)
	fun createAccount(email: String, password: String,name:String, role : String, onResult: (Throwable?) -> Unit)
	fun linkAccount(email: String, password: String, onResult: (Throwable?) -> Unit)
	//forgot password
	fun sendPasswordResetEmail(email: String, onResult: (Throwable?) -> Unit)

	fun createUserDocument(role: String, email: String, name: String, onResult: (Throwable?) -> Unit)

	fun createClassDocument(
		className: String,
		teacherName: String,
		teacherEmail: String,
		onResult: (Throwable?) -> Unit,
	)

	fun addStudentToClass(
		classCode: String,
		studentName: String,
		studentEmail: String,
		onResult: (Throwable?) -> Unit,
	)

	fun joinClassAsStudent(
		classCode: String,
		studentName: String,
		studentEmail: String,
		onResult: (Throwable?) -> Unit,
	)

	//delete a class using a class code
	fun deleteClassByCode(classCode: String, onResult: (Throwable?) -> Unit)

	//create an assignment
	//it should have the ability to upload a file
	fun createAssignment(
		classCode: String,
		assignmentCode: String,
		assignmentName: String,
		assignmentDescription: String,
		assignmentDueDate: String,
		files : ArrayList<File>,
		supportingFilesLocation: ArrayList<String>,
		onResult: (Throwable?) -> Unit,
	)

	//delete an assignment
	fun deleteAssignment(
		classCode: String,
		assignmentCode: String,
		onResult: (Throwable?) -> Unit,
	)

	//make a submission
	fun makeSubmission(
		classCode: String,
		assignmentCode: String,
		studentName: String,
		submissionCode: String,
		submissionDescription: String,
		files : ArrayList<File>,
		submissionFilesLocation: ArrayList<String>,
		onResult: (Throwable?) -> Unit,
	)

	//delete a submission
	fun deleteSubmission(
		classCode: String,
		assignmentCode: String,
		submissionCode: String,
		onResult: (Throwable?) -> Unit,
	)

	//score a submission
	fun scoreSubmission(
		classCode: String,
		assignmentCode: String,
		submissionCode: String,
		score: Int,
		submissionFeedback: String,
		files : ArrayList<File>,
		submissionFilesLocation: ArrayList<String>,
		onResult: (Throwable?) -> Unit,
	)

	//get the current user's role from the database
	fun getRole(onResult: (Throwable?, String?) -> Unit)

	//get all classes for a teacher
	fun getClassesForTeacher(teacherCode: String, onResult: (Throwable?, ArrayList<Classroom>?) -> Unit)

	//get all classes for a student
	fun getClassesForStudent(studentCode: String, onResult: (Throwable?) -> Unit)

	//get all assignments for a class
	fun getAssignmentsForClass(classCode: String, onResult: (Throwable?) -> Unit)

	//get all submissions for an assignment so that a teacher can score them
	fun getSubmissionsForAssignment(classCode: String, assignmentCode: String, onResult: (Throwable?) -> Unit)

	//file upload and download
	fun uploadFile(
		fileName: String,
		fileLocation: String,
		onResult: (Throwable?) -> Unit,
	)

	fun downloadFile(
		fileName: String,
		fileLocation: String,
		onResult: (Throwable?) -> Unit,
	)


	//retrieve a class using a class code
	fun getClassByCode(classCode: String, onResult: (Throwable?) -> Unit)

	//get user details
	fun getUser(onResult: (FirebaseUser?) -> Unit)

	//logout
	fun logout()
}