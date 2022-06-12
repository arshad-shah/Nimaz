package com.arshadshah.nimaz.fragments.quran

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.helperClasses.database.BookmarkDatabaseAccessHelper
import com.arshadshah.nimaz.helperClasses.database.DatabaseAccessHelper
import com.arshadshah.nimaz.helperClasses.quran.AyaListCustomAdapterSurah
import com.arshadshah.nimaz.helperClasses.quran.AyaObject
import kotlin.properties.Delegates

class AyaListSurahFragment : Fragment() {
    var number by Delegates.notNull<Int>()
    private lateinit var helperQuranDatabase: DatabaseAccessHelper

    //call the bookmark database helper
    private lateinit var helperBookmarkDatabase: BookmarkDatabaseAccessHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_aya_surah_list, container, false)

        helperQuranDatabase = DatabaseAccessHelper(requireContext())
        helperBookmarkDatabase = BookmarkDatabaseAccessHelper(requireContext())


        helperQuranDatabase.open()

        //get the juzNumber from bundle
        number = requireArguments().getInt("number")

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val isEnglish = sharedPreferences.getBoolean("isEnglish", true)

        val ayaForsurah = helperQuranDatabase.getAllAyaForSurah(number + 1)

        val ayaList: ListView = root.findViewById(R.id.ayaListSurah)

        val ayaNumberOfBismillah = "0"
        val ayaOfBismillah = if (isEnglish) {
            "In the name of Allah, the Entirely Merciful, the Especially Merciful."
        } else {
            "اللہ کے نام سے جو رحمان و رحیم ہے"
        }
        val ayaArabicOfBismillah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"
        val bismillah = AyaObject(ayaNumberOfBismillah, ayaOfBismillah, ayaArabicOfBismillah)
        //first check if an object like this is already in the list
        //check all the attributes of the object bisimillah with the attributes of the object in the list at index 0
        if (ayaForsurah[0]!!.ayaArabic != ayaArabicOfBismillah) {
            if (number + 1 != 9) {
                ayaForsurah.add(0, bismillah)
            }
        }


        ayaList.divider = null

        //create a custom adapter
        val ayaListCustomAdapter = AyaListCustomAdapterSurah(requireContext(), ayaForsurah)

        //set the adapter to the listview
        ayaList.adapter = ayaListCustomAdapter

        helperQuranDatabase.close()

        //on long click
        ayaList.setOnItemClickListener { parent, view, position, id ->
            //retrieve the aya object from the list
            val ayaObject = ayaForsurah[position]

            //start a new thread checking if the aya is a bookmark
            val bookmarkThread = Thread {
                helperBookmarkDatabase.open()

                //check if the aya is already bookmarked
                val isBookmarkedSurah = helperBookmarkDatabase.isAyaBookmarkedSurah(
                    ayaObject!!.ayaNumber,
                    ayaObject.ayaEnglish,
                    ayaObject.ayaArabic
                )

                //if it is already bookmarked, remove the bookmark
                if (isBookmarkedSurah) {
                    val bookmarkRemoved = helperBookmarkDatabase.deleteBookmarkSurah(
                        ayaObject.ayaNumber,
                        (number + 1).toString()
                    )
                    if (bookmarkRemoved) {
                        //run on ui thread
                        activity?.runOnUiThread {
                            val bookmark: ImageView? = if (isEnglish) {
                                view?.findViewById(R.id.bookmarkButton)
                            } else {
                                view?.findViewById(R.id.bookmarkButton2)
                            }
                            //change the tint color of the bookmark button
                            bookmark!!.isVisible = false
                        }
                    }
                } else {
                    val bookmarkAdded = helperBookmarkDatabase.addBookmarkSurah(
                        ayaObject.ayaNumber,
                        ayaObject.ayaEnglish,
                        ayaObject.ayaArabic,
                        (number + 1).toString()
                    )
                    if (bookmarkAdded) {
                        //run on ui thread
                        activity?.runOnUiThread {
                            val bookmark: ImageView? = if (isEnglish) {
                                view?.findViewById(R.id.bookmarkButton)
                            } else {
                                view?.findViewById(R.id.bookmarkButton2)
                            }
                            //change the tint color of the bookmark button
                            bookmark!!.isVisible = true
                        }
                    }
                }
                helperBookmarkDatabase.close()
            }
            bookmarkThread.start()
        }
        return root
    }

    //a function that saves the last position of the listview before the fragment is destroyed
    override fun onPause() {
        super.onPause()

        helperQuranDatabase.close()
        helperBookmarkDatabase.close()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val ayaList = requireView().findViewById<ListView>(R.id.ayaListSurah)
        sharedPreferences.edit()
            .putInt("lastPositionSurah ${(number + 1)}", ayaList.firstVisiblePosition).apply()
    }

    //a function that restores the last position of the listview after the fragment is created
    override fun onResume() {
        super.onResume()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val lastPosition = sharedPreferences.getInt("lastPositionSurah ${(number + 1)}", 0)
        val scrollToAyaNumber = sharedPreferences.getString("scrollToAyaNumber", "")
        val scrollToBookmark = sharedPreferences.getBoolean("scrollToBookmark", false)
        val scrollToBookmarkNumber = sharedPreferences.getInt("scrollToBookmarkNumber", 0)

        val ayaList = requireView().findViewById<ListView>(R.id.ayaListSurah)
        ayaList.setSelection(lastPosition)

        if (scrollToBookmark) {
            ayaList.setSelection(scrollToBookmarkNumber)
        } else {
            if (scrollToAyaNumber != "") {
                val positionOfAyaGiven = scrollToAyaNumber!!.toInt()
                ayaList.setSelection(positionOfAyaGiven)
                sharedPreferences.edit().remove("scrollToAyaNumber").apply()
            } else {
                ayaList.setSelection(lastPosition)
                sharedPreferences.edit().remove("lastPositionSurah ${(number + 1)}").apply()
            }
        }

    }
}