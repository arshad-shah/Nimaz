package com.arshadshah.nimaz.data.local.dao

import androidx.room.*
import com.arshadshah.nimaz.data.local.models.LocalTasbih

@Dao
interface TasbihTrackerDao
{

	@Query("SELECT * FROM Tasbih")
	fun getAll() : List<LocalTasbih>

	//get a list of tasbih that are completed
	@Query("SELECT * FROM Tasbih WHERE count == goal")
	fun getCompleted() : List<LocalTasbih>

	//get a list of tasbih that are not completed
	@Query("SELECT * FROM Tasbih WHERE count != goal")
	fun getNotCompleted() : List<LocalTasbih>

	//get a list of tasbih that are completed today
	@Query("SELECT * FROM Tasbih WHERE count == goal AND date = :date")
	fun getCompletedToday(date : String) : List<LocalTasbih>

	//get a list of tasbih that are not completed today
	@Query("SELECT * FROM Tasbih WHERE count != goal AND date = :date")
	fun getNotCompletedToday(date : String) : List<LocalTasbih>

	//get a list of tasbih for a specific date
	@Query("SELECT * FROM Tasbih WHERE date = :date")
	fun getForDate(date : String) : List<LocalTasbih>

	//get a tasbih by id
	@Query("SELECT * FROM Tasbih WHERE id = :id")
	fun getTasbihById(id : Int) : LocalTasbih

	//get the tasbih i just added
	@Query("SELECT * FROM Tasbih ORDER BY id DESC LIMIT 1")
	fun getLatestTasbih() : LocalTasbih

	//save a tasbih to the database
	@Insert(LocalTasbih::class)
	fun saveTasbih(tasbih : LocalTasbih) : Long

	@Query("UPDATE Tasbih SET count = :count WHERE id = :id")
	fun updateTasbih(id : Int , count : Int)

	//delete a tasbih from the database
	@Delete
	fun deleteTasbih(tasbih : LocalTasbih)
}