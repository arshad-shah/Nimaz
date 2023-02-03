package com.arshadshah.nimaz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arshadshah.nimaz.data.local.models.LocalAya

@Dao
interface AyaDao
{

	//get all the ayas
	@Query("SELECT * FROM Aya")
	suspend fun getAllAyas() : List<LocalAya>

	//get all the ayas of a surah
	@Query("SELECT * FROM Aya WHERE ayaType = 'Surah' AND numberOfType = :surahNumber AND translationLanguage = :translationLanguage")
	suspend fun getAyasOfSurah(surahNumber : Int , translationLanguage : String) : List<LocalAya>

	//get all the ayas of a juz
	@Query("SELECT * FROM Aya WHERE ayaType = 'Juz' AND numberOfType = :juzNumber AND translationLanguage = :translationLanguage")
	suspend fun getAyasOfJuz(juzNumber : Int , translationLanguage : String) : List<LocalAya>

	//insert all the ayas
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insert(aya : List<LocalAya>)

	//count the number of ayas
	@Query("SELECT COUNT(*) FROM Aya WHERE ayaType = 'Juz' AND  numberOfType = :juzNumber AND translationLanguage = :translationLanguage")
	suspend fun countJuzAya(juzNumber : Int , translationLanguage : String) : Int

	@Query("SELECT COUNT(*) FROM Aya WHERE ayaType = 'Surah' AND numberOfType = :surahNumber AND translationLanguage = :translationLanguage")
	suspend fun countSurahAya(surahNumber : Int , translationLanguage : String) : Int
}