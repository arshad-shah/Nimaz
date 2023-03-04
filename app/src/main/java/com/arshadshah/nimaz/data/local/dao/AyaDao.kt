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
	@Query("SELECT * FROM Aya WHERE suraNumber = :surahNumber")
	suspend fun getAyasOfSurah(surahNumber : Int) : List<LocalAya>

	//get all the ayas of a juz
	@Query("SELECT * FROM Aya WHERE juzNumber = :juzNumber")
	suspend fun getAyasOfJuz(juzNumber : Int) : List<LocalAya>

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

	//add a audio local path to an aya
	//audioFileLocation
	@Query("UPDATE Aya SET audioFileLocation = :audioFileLocation WHERE suraNumber = :surahNumber AND ayaNumberInSurah = :ayaNumberInSurah")
	suspend fun addAudioToAya(
		surahNumber : Int ,
		ayaNumberInSurah : Int ,
		audioFileLocation : String ,
							 )

	@Insert(entity = LocalAya::class , onConflict = OnConflictStrategy.REPLACE)
	suspend fun insert(aya : List<LocalAya>)

	//count the number of ayas
	@Query("SELECT COUNT(*) FROM Aya WHERE juzNumber = :juzNumber")
	suspend fun countJuzAya(juzNumber : Int) : Int

	@Query("SELECT COUNT(*) FROM Aya WHERE suraNumber = :surahNumber")
	suspend fun countSurahAya(surahNumber : Int) : Int

	//get a random aya
	@Query("SELECT * FROM Aya ORDER BY RANDOM() LIMIT 1")
	suspend fun getRandomAya() : LocalAya
}