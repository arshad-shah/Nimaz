package com.arshadshah.nimaz.data.remote.models

class Student(
	val id : String,
	val name : String,
	val email : String,
	val classIds : ArrayList<Classroom>,
			 )