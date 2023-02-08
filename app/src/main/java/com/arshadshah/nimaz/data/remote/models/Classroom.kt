package com.arshadshah.nimaz.data.remote.models

class Classroom(
val classCode : String ,
val className : String ,
val teacherCode : String ,
val newAssignments : Boolean ,
val students : ArrayList<Student> ,
val assignments : ArrayList<Assignment> ,
val grades : ArrayList<Grade> ,
)