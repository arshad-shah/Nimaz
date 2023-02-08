package com.arshadshah.nimaz.utils.auth

import android.net.Uri
import com.arshadshah.nimaz.data.remote.models.Assignment
import com.arshadshah.nimaz.data.remote.models.Classroom
import com.arshadshah.nimaz.data.remote.models.Grade
import com.arshadshah.nimaz.data.remote.models.Student
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File

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

	override fun createAccount(
		email : String ,
		password : String ,
		name : String ,
		role : String ,
		onResult : (Throwable?) -> Unit
							  )
	{
		Firebase.auth.createUserWithEmailAndPassword(email , password)
			.addOnCompleteListener { task ->
				if (task.isSuccessful)
				{
					createUserDocument(role , email , name , onResult)
				}
				else
				{
					onResult(task.exception)
				}
			}
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

	override fun createUserDocument(
		role : String ,
		email : String ,
		name : String ,
		onResult : (Throwable?) -> Unit ,
								   )
	{
		//add user to database
		Firebase.firestore.collection("users")
			.document(Firebase.auth.currentUser!!.uid)
			.set(
				mapOf(
					"role" to role ,
					"email" to email ,
					"name" to name ,
					"profilePicture" to "" ,
					"classes" to listOf<String>()
						)
				 )
			.addOnCompleteListener { onResult(it.exception) }
	}

	override fun createClassDocument(
		className : String ,
		teacherName : String ,
		teacherEmail : String ,
		onResult : (Throwable?) -> Unit ,
									)
	{
		Firebase.firestore.collection("classes")
			.document()
			.set(
				mapOf(
					"className" to className ,
					"classCode" to Firebase.firestore.collection("classes").document().id ,
					"teacherCode" to Firebase.auth.currentUser!!.uid ,
					"teacherName" to teacherName ,
					"teacherEmail" to teacherEmail ,
					"newAssignments" to false ,
					"assignments" to listOf<Assignment>() ,
					"grades" to listOf<Grade>() ,
					"students" to listOf<Student>() ,
						)
				 )
			.addOnCompleteListener { onResult(it.exception) }
	}

	override fun addStudentToClass(
		classCode : String ,
		studentName : String ,
		studentEmail : String ,
		onResult : (Throwable?) -> Unit ,
								  )
	{
		Firebase.firestore.collection("classes")
			.whereEqualTo("classCode" , classCode)
			.get()
			.addOnCompleteListener { task ->
				if (task.isSuccessful)
				{
					val document = task.result!!.documents[0]
					val students = document.get("students") as List<*>
					val newStudents = students.toMutableList()
					newStudents.add(studentName)
					document.reference.update("students" , newStudents)
						.addOnCompleteListener { onResult(it.exception) }
				}
			}
	}

	override fun joinClassAsStudent(
		classCode : String ,
		studentName : String ,
		studentEmail : String ,
		onResult : (Throwable?) -> Unit ,
								   )
	{
		Firebase.firestore.collection("classes")
			.whereEqualTo("classCode" , classCode)
			.get()
			.addOnCompleteListener { task ->
				if (task.isSuccessful)
				{
					val document = task.result!!.documents[0]
					val students = document.get("students") as List<*>
					val newStudents = students.toMutableList()
					newStudents.add(studentName)
					document.reference.update("students" , newStudents)
						.addOnCompleteListener { onResult(it.exception) }
				}
			}
	}

	override fun deleteClassByCode(classCode : String , onResult : (Throwable?) -> Unit)
	{
		Firebase.firestore.collection("classes")
			.whereEqualTo("classCode" , classCode)
			.get()
			.addOnCompleteListener { task ->
				if (task.isSuccessful)
				{
					val document = task.result!!.documents[0]
					document.reference.delete()
						.addOnCompleteListener { onResult(it.exception) }
				}
			}
	}

	override fun createAssignment(
		classCode : String ,
		assignmentCode : String ,
		assignmentName : String ,
		assignmentDescription : String ,
		assignmentDueDate : String ,
		files : ArrayList<File> ,
		supportingFilesLocation : ArrayList<String> ,
		onResult : (Throwable?) -> Unit ,
								 )
	{
		//upload the files to firebase storage and get the download url for each file then create a ArrayList<String> of the download urls and add it to the assignment document
		val downloadUrls = ArrayList<String>()
		for (file in files)
		{
			val storageRef = Firebase.storage.reference
			val fileRef = storageRef.child("assignments/${assignmentCode}/${file.name}")
			val uploadTask = fileRef.putFile(Uri.fromFile(file))
			uploadTask.addOnSuccessListener {
				fileRef.downloadUrl.addOnSuccessListener { uri ->
					downloadUrls.add(uri.toString())
					if (downloadUrls.size == files.size)
					{
						Firebase.firestore.collection("classes")
							.whereEqualTo("classCode" , classCode)
							.get()
							.addOnCompleteListener { task ->
								if (task.isSuccessful)
								{
									val document = task.result!!.documents[0]
									val assignments = document.get("assignments") as List<*>
									val newAssignments = assignments.toMutableList()
									newAssignments.add(
										mapOf(
											"assignmentCode" to assignmentCode ,
											"assignmentName" to assignmentName ,
											"assignmentDescription" to assignmentDescription ,
											"assignmentDueDate" to assignmentDueDate ,
											"supportingFilesLocation" to downloadUrls ,
												)
										 )
									document.reference.update("assignments" , newAssignments)
										.addOnCompleteListener { onResult(it.exception) }
								}
							}
					}
				}
			}
		}
	}

	override fun deleteAssignment(
		classCode : String ,
		assignmentCode : String ,
		onResult : (Throwable?) -> Unit ,
								 )
	{
		Firebase.firestore.collection("classes")
			.whereEqualTo("classCode" , classCode)
			.get()
			.addOnCompleteListener { task ->
				if (task.isSuccessful)
				{
					val document = task.result!!.documents[0]
					val assignments = document.get("assignments") as List<*>
					val newAssignments = assignments.toMutableList()
					newAssignments.removeIf { (it as Map<*, *>)["assignmentCode"] == assignmentCode }
					document.reference.update("assignments" , newAssignments)
						.addOnCompleteListener { onResult(it.exception) }
				}
			}
	}

	override fun makeSubmission(
		classCode : String ,
		assignmentCode : String ,
		studentName : String ,
		submissionCode : String ,
		submissionDescription : String ,
		files : ArrayList<File> ,
		submissionFilesLocation : ArrayList<String> ,
		onResult : (Throwable?) -> Unit
							   )
	{
		//upload the files to firebase storage and get the download url for each file then create a ArrayList<String> of the download urls and add it to the assignment document
		val downloadUrls = ArrayList<String>()
		for (file in files)
		{
			val storageRef = Firebase.storage.reference
			val fileRef = storageRef.child("submissions/${studentName}/${submissionCode}/${file.name}")
			val uploadTask = fileRef.putFile(Uri.fromFile(file))
			uploadTask.addOnSuccessListener {
				fileRef.downloadUrl.addOnSuccessListener { uri ->
					downloadUrls.add(uri.toString())
					if (downloadUrls.size == files.size)
					{
						Firebase.firestore.collection("classes")
							.whereEqualTo("classCode" , classCode)
							.get()
							.addOnCompleteListener { task ->
								if (task.isSuccessful)
								{
									val document = task.result!!.documents[0]
									val assignments = document.get("assignments") as List<*>
									val newAssignments = assignments.toMutableList()
									for (assignment in newAssignments)
									{
										if ((assignment as Map<*, *>)["assignmentCode"] == assignmentCode)
										{
											val submissions = assignment["submissions"] as List<*>
											val newSubmissions = submissions.toMutableList()
											newSubmissions.add(
												mapOf(
													"submissionCode" to submissionCode ,
													"submissionDescription" to submissionDescription ,
													"submissionFilesLocation" to downloadUrls ,
													"studentName" to studentName ,
														)
												 )
											assignment["submissions"] = newSubmissions
										}
									}
									document.reference.update("assignments" , newAssignments)
										.addOnCompleteListener { onResult(it.exception) }
								}
							}
					}
				}
			}
		}
	}

	override fun deleteSubmission(
		classCode : String ,
		assignmentCode : String ,
		submissionCode : String ,
		onResult : (Throwable?) -> Unit ,
								 )
	{
		Firebase.firestore.collection("classes")
			.whereEqualTo("classCode" , classCode)
			.get()
			.addOnCompleteListener { task ->
				if (task.isSuccessful)
				{
					val document = task.result!!.documents[0]
					val assignments = document.get("assignments") as List<*>
					val newAssignments = assignments.toMutableList()
					for (assignment in newAssignments)
					{
						if ((assignment as Map<*, *>)["assignmentCode"] == assignmentCode)
						{
							val submissions = assignment["submissions"] as List<*>
							val newSubmissions = submissions.toMutableList()
							newSubmissions.removeIf { (it as Map<*, *>)["submissionCode"] == submissionCode }
							assignment["submissions"] = newSubmissions
						}
					}
					document.reference.update("assignments" , newAssignments)
						.addOnCompleteListener { onResult(it.exception) }
				}
			}
	}

	override fun scoreSubmission(
		classCode : String ,
		assignmentCode : String ,
		submissionCode : String ,
		score : Int ,
		submissionFeedback : String ,
		files : ArrayList<File> ,
		submissionFilesLocation : ArrayList<String> ,
		onResult : (Throwable?) -> Unit ,
								)
	{
		//upload the files to firebase storage and get the download url for each file then create a ArrayList<String> of the download urls and add it to the assignment document
		val downloadUrls = ArrayList<String>()
		for (file in files)
		{
			val storageRef = Firebase.storage.reference
			val fileRef = storageRef.child("submissions/${submissionCode}/${file.name}")
			val uploadTask = fileRef.putFile(Uri.fromFile(file))
			uploadTask.addOnSuccessListener {
				fileRef.downloadUrl.addOnSuccessListener { uri ->
					downloadUrls.add(uri.toString())
					if (downloadUrls.size == files.size)
					{
						Firebase.firestore.collection("classes")
							.whereEqualTo("classCode" , classCode)
							.get()
							.addOnCompleteListener { task ->
								if (task.isSuccessful)
								{
									val document = task.result!!.documents[0]
									val assignments = document.get("assignments") as List<*>
									val newAssignments = assignments.toMutableList()
									for (assignment in newAssignments)
									{
										if ((assignment as Map<*, *>)["assignmentCode"] == assignmentCode)
										{
											val submissions = assignment["submissions"] as ArrayList<*>
											val newSubmissions = submissions.toMutableList()
											for (submission in newSubmissions)
											{
												if ((submission as Map<*, *>)["submissionCode"] == submissionCode)
												{
													submission["submissionScore"] = score
													submission["submissionFeedback"] = submissionFeedback
													submission["submissionFilesLocation"] = downloadUrls
												}
											}
											assignment["submissions"] = newSubmissions
										}
									}
									document.reference.update("assignments" , newAssignments)
										.addOnCompleteListener { onResult(it.exception) }
								}
							}
					}
				}
			}
		}
	}

	override fun getRole(onResult : (Throwable? , String?) -> Unit)
	{
		Firebase.firestore.collection("users")
			.whereEqualTo("userCode" , Firebase.auth.currentUser?.uid)
			.get()
			.addOnCompleteListener { task ->
				if (task.isSuccessful)
				{
					val document = task.result!!.documents[0]
					val role = document.get("role") as String
					onResult(null , role)
				}
				else
				{
					onResult(task.exception , null)
				}
			}
	}

	override fun getClassesForTeacher(teacherCode : String , onResult : (Throwable? , ArrayList<Classroom>?) -> Unit)
	{
		var classesForTeachers = ArrayList<Classroom>()
		Firebase.firestore.collection("classes")
			.whereEqualTo("teacherCode" , teacherCode)
			.get()
			.addOnCompleteListener { task ->
				if (task.isSuccessful)
				{
					for (document in task.result!!)
					{
						val classCode = document.get("classCode") as String
						val className = document.get("className") as String
						val teacherCode = document.get("teacherCode") as String
						val newAssignments = document.get("newAssignments") as Boolean
						val students = document.get("students") as ArrayList<*>
						val assignments = document.get("assignments") as ArrayList<*>
						val grades = document.get("grades") as ArrayList<*>

						//get all the students in the class
						val studentsInClass = ArrayList<Student>()
						for (student in students)
						{
							val studentCode = (student as Map<*, *>)["studentCode"] as String
							val studentName = student["studentName"] as String
							val studentEmail = student["studentEmail"] as String
							studentsInClass.add(Student(studentCode , studentName , studentEmail , ArrayList()))
						}

						//get all the assignments in the class
						val assignmentsInClass = ArrayList<Assignment>()
						for (assignment in assignments)
						{
							val assignmentCode = (assignment as Map<*, *>)["assignmentCode"] as String
							val assignmentName = assignment["assignmentName"] as String
							val assignmentDescription = assignment["assignmentDescription"] as String
							val assignmentDueDate = assignment["assignmentDueDate"] as String
							val assignmentFilesLocation = assignment["assignmentFilesLocation"] as ArrayList<*>
							val assignmentFiles = ArrayList<String>()
							for (file in assignmentFilesLocation)
							{
								assignmentFiles.add(file as String)
							}
							assignmentsInClass.add(Assignment(assignmentCode , assignmentName , assignmentDescription , assignmentDueDate , assignmentFiles ))
						}

						//get all the grades in the class
						val gradesInClass = ArrayList<Grade>()
						for (grade in grades)
						{
							val assignmentCode = (grade as Map<*, *>)["assignmentCode"] as String
							val studentCode = grade["studentCode"] as String
							val grade = grade["grade"] as String
							gradesInClass.add(Grade(assignmentCode , studentCode , grade.toInt()))
						}
						classesForTeachers.add(
							Classroom(
									classCode ,
									className ,
									teacherCode ,
									newAssignments ,
									studentsInClass ,
									assignmentsInClass ,
									gradesInClass
										)
								   )
					}
					classesForTeachers = classesForTeachers.sortedWith(compareBy { it.className }).toCollection(ArrayList())
					onResult(null, classesForTeachers)
				}
				else
				{
					onResult(task.exception, null)
				}
			}
	}

	override fun getClassesForStudent(studentCode : String , onResult : (Throwable?) -> Unit)
	{
		var classesForStudent = ArrayList<Classroom>()
		Firebase.firestore.collection("classes")
			.whereArrayContains("students" , studentCode)
			.get()
			.addOnCompleteListener { task ->
				if (task.isSuccessful)
				{
					for (document in task.result!!)
					{
						val classCode = document.get("classCode") as String
						val className = document.get("className") as String
						val teacherCode = document.get("teacherCode") as String
						val newAssignments = document.get("newAssignments") as Boolean
						val students = document.get("students") as ArrayList<*>
						val assignments = document.get("assignments") as ArrayList<*>
						val grades = document.get("grades") as ArrayList<*>
						//map students to Student objects
						//map assignments to Assignment objects
						//map grades to Grade objects
						classesForStudent.add(
							Classroom(
									classCode ,
									className ,
									teacherCode ,
									newAssignments ,
									students as ArrayList<Student> ,
									assignments as ArrayList<Assignment> ,
									grades as ArrayList<Grade> ,
										)
								   )
					}
					classesForStudent = classesForStudent.sortedWith(compareBy { it.className }).toCollection(ArrayList())
					onResult(null)
				}
				else
				{
					onResult(task.exception)
				}
			}
	}

	override fun getAssignmentsForClass(classCode : String , onResult : (Throwable?) -> Unit)
	{
		var assignmentsForClass = ArrayList<Assignment>()
		Firebase.firestore.collection("classes")
			.whereEqualTo("classCode" , classCode)
			.get()
			.addOnCompleteListener { task ->
				if (task.isSuccessful)
				{
					for (document in task.result!!)
					{
						val assignments = document.get("assignments") as ArrayList<*>
						//map assignments to Assignment objects
						assignmentsForClass = assignments as ArrayList<Assignment>
						onResult(null)
					}
				}
				else
				{
					onResult(task.exception)
				}
			}
	}

	override fun getSubmissionsForAssignment(
		classCode : String ,
		assignmentCode : String ,
		onResult : (Throwable?) -> Unit ,
											)
	{
	}

	override fun uploadFile(
		fileName : String ,
		fileLocation : String ,
		onResult : (Throwable?) -> Unit ,
						   )
	{
		TODO("Not yet implemented")
	}

	override fun downloadFile(
		fileName : String ,
		fileLocation : String ,
		onResult : (Throwable?) -> Unit ,
							 )
	{
		TODO("Not yet implemented")
	}

	override fun getClassByCode(classCode : String , onResult : (Throwable?) -> Unit)
	{
		Firebase.firestore.collection("classes")
			.whereEqualTo("classCode" , classCode)
			.get()
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

private operator fun <K , V> Map<K , V>.set(v : V , value : V)
{
	val map = this as MutableMap<K , V>
	map[v] = value
}
