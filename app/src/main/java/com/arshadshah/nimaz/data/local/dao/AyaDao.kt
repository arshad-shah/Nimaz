package com.arshadshah.nimaz.data.local.dao

import androidx.room.*
import com.arshadshah.nimaz.data.local.models.LocalAya

@Dao
interface AyaDao
{

	//get all the ayas
	@Query("SELECT * FROM Aya")
	suspend fun getAllAyas() : List<LocalAya>

	//get all the ayas of a surah
	@Query("SELECT * FROM Aya WHERE suraNumber = :surahNumber AND translationLanguage = :translationLanguage")
	suspend fun getAyasOfSurah(surahNumber : Int , translationLanguage : String) : List<LocalAya>

	//get all the ayas of a juz
	@Query("SELECT * FROM Aya WHERE juzNumber = :juzNumber AND translationLanguage = :translationLanguage")
	suspend fun getAyasOfJuz(juzNumber : Int , translationLanguage : String) : List<LocalAya>

	@Query("SELECT * FROM Aya WHERE ayaNumberInQuran = 0 AND translationLanguage = :translationLanguage")
	suspend fun getBismillah(translationLanguage : String) : LocalAya

	//bookmark an aya
	@Query("UPDATE Aya SET bookmark = :bookmark WHERE ayaNumber = :ayaNumber AND suraNumber = :surahNumber AND ayaNumberInSurah = :ayaNumberInSurah")
	suspend fun bookmarkAya(
		ayaNumber : Int ,
		surahNumber : Int ,
		ayaNumberInSurah : Int ,
		bookmark : Boolean ,
						   )

	//favorite an aya
	@Query("UPDATE Aya SET favorite = :favorite WHERE ayaNumber = :ayaNumber AND suraNumber = :surahNumber AND ayaNumberInSurah = :ayaNumberInSurah")
	suspend fun favoriteAya(
		ayaNumber : Int ,
		surahNumber : Int ,
		ayaNumberInSurah : Int ,
		favorite : Boolean ,
						   )

	//add a note to an aya
	@Query("UPDATE Aya SET note = :note WHERE ayaNumber = :ayaNumber AND suraNumber = :surahNumber AND ayaNumberInSurah = :ayaNumberInSurah")
	suspend fun addNoteToAya(
		ayaNumber : Int ,
		surahNumber : Int ,
		ayaNumberInSurah : Int ,
		note : String ,
							)

	//get a  note fro an aya
	@Query("SELECT note FROM Aya WHERE ayaNumber = :ayaNumber AND suraNumber = :surahNumber AND ayaNumberInSurah = :ayaNumberInSurah")
	suspend fun getNoteOfAya(ayaNumber : Int , surahNumber : Int , ayaNumberInSurah : Int) : String


	//get all the bookmarked ayas
	@Query("SELECT * FROM Aya WHERE bookmark = 1")
	suspend fun getBookmarkedAyas() : List<LocalAya>

	//get all the favorited ayas
	@Query("SELECT * FROM Aya WHERE favorite = 1")
	suspend fun getFavoritedAyas() : List<LocalAya>

	//get all the ayas with notes
	@Query("SELECT * FROM Aya WHERE note != ''")
	suspend fun getAyasWithNotes() : List<LocalAya>

	@Insert(entity = LocalAya::class , onConflict = OnConflictStrategy.REPLACE)
	suspend fun insert(aya : List<LocalAya>)

	//count the number of ayas
	@Query("SELECT COUNT(*) FROM Aya WHERE juzNumber = :juzNumber AND translationLanguage = :translationLanguage")
	suspend fun countJuzAya(juzNumber : Int , translationLanguage : String) : Int

	@Query("SELECT COUNT(*) FROM Aya WHERE suraNumber = :surahNumber AND translationLanguage = :translationLanguage")
	suspend fun countSurahAya(surahNumber : Int , translationLanguage : String) : Int
}