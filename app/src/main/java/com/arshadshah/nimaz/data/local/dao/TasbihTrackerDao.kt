package com.arshadshah.nimaz.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.arshadshah.nimaz.data.local.models.LocalTasbih

@Dao
interface TasbihTrackerDao
{
	@Query("SELECT * FROM Tasbih")
	fun getAll() : List<LocalTasbih>

	//get a list of tasbih that are completed
	@Query("SELECT * FROM Tasbih WHERE isCompleted = 1")
	fun getCompleted() : List<LocalTasbih>

	//get a list of tasbih that are not completed
	@Query("SELECT * FROM Tasbih WHERE isCompleted = 0")
	fun getNotCompleted() : List<LocalTasbih>

	//get a list of tasbih that are completed today
	@Query("SELECT * FROM Tasbih WHERE isCompleted = 1 AND date = :date")
	fun getCompletedToday(date : String) : List<LocalTasbih>

	//get a list of tasbih that are not completed today
	@Query("SELECT * FROM Tasbih WHERE isCompleted = 0 AND date = :date")
	fun getNotCompletedToday(date : String) : List<LocalTasbih>

	//get a list of tasbih for a specific date
	@Query("SELECT * FROM Tasbih WHERE date = :date")
	fun getForDate(date : String) : List<LocalTasbih>

	//get a tasbih by id
	@Query("SELECT * FROM Tasbih WHERE id = :id")
	fun getTasbihById(id : Int) : LocalTasbih

	//save a tasbih to the database
	@Insert(LocalTasbih::class)
	fun saveTasbih(tasbih : LocalTasbih)

	//update a tasbih in the database
	@Query("UPDATE Tasbih SET completed = :completed , isCompleted = :isCompleted WHERE id = :id")
	fun updateTasbih(id : Int , completed : Int , isCompleted : Boolean)

	//delete a tasbih from the database
	@Delete
	fun deleteTasbih(tasbih : LocalTasbih)
}