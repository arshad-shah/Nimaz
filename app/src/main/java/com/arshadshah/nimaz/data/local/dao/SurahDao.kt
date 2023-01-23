package com.arshadshah.nimaz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arshadshah.nimaz.data.local.models.LocalSurah

@Dao
interface SurahDao
{

	//get all surahs
	@Query("SELECT * FROM Surah")
	fun getAllSurahs() : List<LocalSurah>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	fun insert(surah : List<LocalSurah>)

	@Query("SELECT COUNT(*) FROM Surah")
	fun count() : Int
}