package com.arshadshah.nimaz.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.arshadshah.nimaz.data.local.dao.*
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@SmallTest
class DataStoreTest
{
//
//	private lateinit var db: AppDatabase
//	private lateinit var ayaDao: AyaDao
//	private lateinit var juzDao: JuzDao
//	private lateinit var surahDao: SurahDao
//	private lateinit var prayerTimesDao: PrayerTimesDao
//	private lateinit var duaDao: DuaDao
//	private lateinit var dataStore: DataStore
//
//	//list of all the ayas
//	// val id: Int = 0,
//	//    val ayaNumber: Int,
//	//    val ayaArabic: String,
//	//    val translation: String,
//	//    val suraNumber: Int,
//	//    val ayaNumberInSurah: Int,
//	//    val bookmark: Boolean,
//	//    val favorite: Boolean,
//	//    val note: String,
//	//    val audioFileLocation: String,
//	//    val sajda: Boolean,
//	//    val sajdaType: String,
//	//    val ruku: Int,
//	//    val juzNumber: Int,
//	//    val ayaType: String,
//	//    val numberOfType: Int,
//	//    val translationLanguage: String
//	private val ayas = listOf(
//		LocalAya(
//			ayaNumber = 1 ,
//			ayaArabic = "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ" ,
//			translation = "In the name of Allah, the Entirely Merciful, the Especially Merciful." ,
//			suraNumber = 1 ,
//			ayaNumberInSurah = 1 ,
//			bookmark = false ,
//			favorite = false ,
//			note = "" ,
//			audioFileLocation = "" ,
//			sajda = false ,
//			sajdaType = "" ,
//			ruku = 0 ,
//			juzNumber = 1 ,
//			ayaType = "juz" ,
//			numberOfType = 1 ,
//			translationLanguage = "en"
//		) ,
//		LocalAya(
//			ayaNumber = 2 ,
//			ayaArabic = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ" ,
//			translation = "All praise is due to Allah, Lord of the worlds." ,
//			suraNumber = 1 ,
//			ayaNumberInSurah = 2 ,
//			bookmark = false ,
//			favorite = false ,
//			note = "" ,
//			audioFileLocation = "" ,
//			sajda = false ,
//			sajdaType = "" ,
//			ruku = 0 ,
//			juzNumber = 1 ,
//			ayaType = "juz" ,
//			numberOfType = 1 ,
//			translationLanguage = "en"
//		) ,
//		LocalAya(
//			ayaNumber = 3 ,
//			ayaArabic = "الرَّحْمَنِ الرَّحِيمِ" ,
//			translation = "The Entirely Merciful, the Especially Merciful." ,
//			suraNumber = 1 ,
//			ayaNumberInSurah = 3 ,
//			bookmark = false ,
//			favorite = false ,
//			note = "" ,
//			audioFileLocation = "" ,
//			sajda = false ,
//			sajdaType = "" ,
//			ruku = 0 ,
//			juzNumber = 1 ,
//			ayaType = "juz" ,
//			numberOfType = 1 ,
//			translationLanguage = "en"
//		) ,
//		LocalAya(
//			ayaNumber = 4 ,
//			ayaArabic = "مَالِكِ يَوْمِ الدِّينِ" ,
//			translation = "Master of the Day of Judgment." ,
//			suraNumber = 1 ,
//			ayaNumberInSurah = 4 ,
//			bookmark = false ,
//			favorite = false ,
//			note = "" ,
//			audioFileLocation = "" ,
//			sajda = false ,
//			sajdaType = "" ,
//			ruku = 0 ,
//			juzNumber = 1 ,
//			ayaType = "juz" ,
//			numberOfType = 1 ,
//			translationLanguage = "en"
//		) ,
//		LocalAya(
//			ayaNumber = 5 ,
//			ayaArabic = "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ" ,
//			translation = "You alone do we worship and You alone do we ask for help." ,
//			suraNumber = 1 ,
//			ayaNumberInSurah = 5 ,
//			bookmark = false ,
//			favorite = false ,
//			note = "" ,
//			audioFileLocation = "" ,
//			sajda = false ,
//			sajdaType = "" ,
//			ruku = 0 ,
//			juzNumber = 1 ,
//			ayaType = "juz" ,
//			numberOfType = 1 ,
//			translationLanguage = "en"
//		) ,
//							 )
//
//	@Before
//	fun setUp()
//	{
//		val context : Context = ApplicationProvider.getApplicationContext()
//		db = Room.inMemoryDatabaseBuilder(context , AppDatabase::class.java).allowMainThreadQueries().build()
//		ayaDao = db.ayaDao
//		juzDao = db.juz
//		surahDao = db.surah
//		prayerTimesDao = db.prayerTimes
//		duaDao = db.dua
//
//		dataStore = DataStore(db)
//	}
//
//	@After
//	@Throws(Exception::class)
//	fun tearDown()
//	{
//		db.close()
//	}
//
//	@Test
//	fun insertAyats()
//	{
//		runBlocking {
//			ayaDao.insert(ayas)
//			val ayasFromDb = ayaDao.getAllAyas()
//			for (i in ayas.indices)
//			{
//				assertEquals(ayas[i].ayaNumber , ayasFromDb[i].ayaNumber)
//				assertEquals(ayas[i].ayaArabic , ayasFromDb[i].ayaArabic)
//				assertEquals(ayas[i].translation , ayasFromDb[i].translation)
//				assertEquals(ayas[i].suraNumber , ayasFromDb[i].suraNumber)
//				assertEquals(ayas[i].ayaNumberInSurah , ayasFromDb[i].ayaNumberInSurah)
//				assertEquals(ayas[i].bookmark , ayasFromDb[i].bookmark)
//				assertEquals(ayas[i].favorite , ayasFromDb[i].favorite)
//				assertEquals(ayas[i].note , ayasFromDb[i].note)
//				assertEquals(ayas[i].audioFileLocation , ayasFromDb[i].audioFileLocation)
//				assertEquals(ayas[i].sajda , ayasFromDb[i].sajda)
//				assertEquals(ayas[i].sajdaType , ayasFromDb[i].sajdaType)
//				assertEquals(ayas[i].ruku , ayasFromDb[i].ruku)
//				assertEquals(ayas[i].juzNumber , ayasFromDb[i].juzNumber)
//				assertEquals(ayas[i].ayaType , ayasFromDb[i].ayaType)
//				assertEquals(ayas[i].numberOfType , ayasFromDb[i].numberOfType)
//				assertEquals(ayas[i].translationLanguage , ayasFromDb[i].translationLanguage)
//			}
//		}
//	}
//
//	@Test
//	fun bookmarkAya()
//	{
//		runBlocking {
//			ayaDao.insert(ayas)
//			dataStore.bookmarkAya(5, true)
//			val ayasFromDb = ayaDao.getAllAyas()
//			assertEquals(true , ayasFromDb[4].bookmark)
//		}
//	}
//
//	@Test
//	fun removeBookmarkAya()
//	{
//		runBlocking {
//			ayaDao.insert(ayas)
//			dataStore.bookmarkAya(5, true)
//			dataStore.bookmarkAya(5, false)
//			val ayasFromDb = ayaDao.getAllAyas()
//			assertEquals(false , ayasFromDb[4].bookmark)
//		}
//	}
//
//	@Test
//	fun favoriteAya()
//	{
//		runBlocking {
//			ayaDao.insert(ayas)
//			dataStore.favoriteAya(5, true)
//			val ayasFromDb = ayaDao.getAllAyas()
//			assertEquals(true , ayasFromDb[4].favorite)
//		}
//	}
//
//	@Test
//	fun removeFavoriteAya()
//	{
//		runBlocking {
//			ayaDao.insert(ayas)
//			dataStore.favoriteAya(5, true)
//			dataStore.favoriteAya(5, false)
//			val ayasFromDb = ayaDao.getAllAyas()
//			assertEquals(false , ayasFromDb[4].favorite)
//		}
//	}
//
//	@Test
//	fun addNoteToAya()
//	{
//		runBlocking {
//			ayaDao.insert(ayas)
//			dataStore.addNoteToAya(5, "test")
//			val ayasFromDb = ayaDao.getAllAyas()
//			assertEquals("test" , ayasFromDb[4].note)
//		}
//	}
//
//	@Test
//	fun removeNoteFromAya()
//	{
//		runBlocking {
//			ayaDao.insert(ayas)
//			dataStore.addNoteToAya(5, "test")
//			dataStore.addNoteToAya(5, "")
//			val ayasFromDb = ayaDao.getAllAyas()
//			assertEquals("" , ayasFromDb[4].note)
//		}
//	}
}