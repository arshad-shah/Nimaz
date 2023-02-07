package com.arshadshah.nimaz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arshadshah.nimaz.data.local.models.LocalChapter

@Dao
interface DuaDao
{

	//get all the chapters
	@Query("SELECT * FROM Chapter")
	suspend fun getAllChapters() : List<LocalChapter>

	//get duas of a chapter by chapter id
	@Query("SELECT * FROM Chapter WHERE _id = :chapterId")
	suspend fun getDuasOfChapter(chapterId : Int) : LocalChapter

	//save a list of chapters
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun saveChapters(chapters : List<LocalChapter>)

	//save a one chapter
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun saveDuas(duas : LocalChapter)

	//count
	@Query("SELECT COUNT(*) FROM Chapter")
	suspend fun countChapters() : Int

	//count
	@Query("SELECT COUNT(*) FROM Dua")
	suspend fun countDuas() : Int
}