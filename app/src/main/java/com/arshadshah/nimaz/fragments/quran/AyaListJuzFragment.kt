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
import com.arshadshah.nimaz.helperClasses.quran.AyaListCustomAdapter
import com.arshadshah.nimaz.helperClasses.quran.AyaObject
import kotlin.properties.Delegates

class AyaListJuzFragment : Fragment() {

    var number by Delegates.notNull<Int>()
    private lateinit var helperQuranDatabase: DatabaseAccessHelper

    //call the bookmark database helper
    private lateinit var helperBookmarkDatabase: BookmarkDatabaseAccessHelper


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_aya_list, container, false)
        helperQuranDatabase = DatabaseAccessHelper(requireContext())
        helperBookmarkDatabase = BookmarkDatabaseAccessHelper(requireContext())

        helperQuranDatabase.open()

        //get the juzNumber from bundle
        number = requireArguments().getInt("number")

        val ayaForJuz = helperQuranDatabase.getAllAyaForJuz(number + 1)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val isEnglish = sharedPreferences.getBoolean("isEnglish", true)

        //add the following object to index 0 of ayaForSurah without losing value of index 0 in ayaForSurah
        val ayaNumberOfBismillah = "0"

        val ayaOfBismillah = if (isEnglish) {
            "In the name of Allah, the Entirely Merciful, the Especially Merciful."
        } else {
            "اللہ کے نام سے جو رحمان و رحیم ہے"
        }
        val ayaArabicOfBismillah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"
        val bismillah = AyaObject(ayaNumberOfBismillah, ayaOfBismillah, ayaArabicOfBismillah)

        //find all the objects in arraylist ayaForJuz where ayaForJuz[i]!!.ayaNumber = 1
        //add object bismillah before it for every occurance of ayaForJuz[i]!!.ayaNumber = 1
        var index = 0
        while (index < ayaForJuz.size) {
            if (ayaForJuz[index]!!.ayaArabic != bismillah.ayaArabic) {
                //add bismillah before ayaForJuz[i]
                if (ayaForJuz[index]!!.ayaNumber == "1") {
                    if (number + 1 != 10 && index != 36) {
                        ayaForJuz.add(index, bismillah)
                        //skip the next iteration
                        index++
                    }
                }
            }
            index++
        }

        val ayaList: ListView = root.findViewById(R.id.ayaList)

        ayaList.divider = null

        //create a custom adapter
        val ayaListCustomAdapter = AyaListCustomAdapter(requireContext(), ayaForJuz)

        //set the adapter to the listview
        ayaList.adapter = ayaListCustomAdapter

        helperQuranDatabase.close()

        //on long click
        ayaList.setOnItemClickListener { parent, view, position, id ->
            //retrieve the aya object from the list
            val ayaObject = ayaForJuz[position]

            //start a new thread checking if the aya is a bookmark
            val bookmarkThread = Thread {
                helperBookmarkDatabase.open()

                //check if the aya is already bookmarked
                val isBookmarkedJuz = helperBookmarkDatabase.isAyaBookmarkedJuz(
                    ayaObject!!.ayaNumber,
                    ayaObject.ayaEnglish,
                    ayaObject.ayaArabic
                )

                //if it is already bookmarked, remove the bookmark
                if (isBookmarkedJuz) {
                    val bookmarkRemoved = helperBookmarkDatabase.deleteBookmarkJuz(
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
                    val bookmarkAdded = helperBookmarkDatabase.addBookmarkJuz(
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


    //a function that saves the last position of the listview before the fragment is paused
    override fun onPause() {
        super.onPause()

        helperQuranDatabase.close()
        helperBookmarkDatabase.close()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val ayaList = requireView().findViewById<ListView>(R.id.ayaList)
        sharedPreferences.edit()
            .putInt("lastPositionJuz ${(number + 1)}", ayaList.firstVisiblePosition).apply()
    }

    //a function that restores the last position of the listview before the fragment is created
    override fun onResume() {
        super.onResume()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val lastPosition = sharedPreferences.getInt("lastPositionJuz ${(number + 1)}", 0)
        val scrollToAyaNumber = sharedPreferences.getString("scrollToAyaNumber", "")
        val scrollToBookmark = sharedPreferences.getBoolean("scrollToBookmark", false)
        val scrollToBookmarkNumber = sharedPreferences.getInt("scrollToBookmarkNumber", 0)
        val ayaList = requireView().findViewById<ListView>(R.id.ayaList)

        if (scrollToBookmark) {
            ayaList.setSelection(scrollToBookmarkNumber)
            sharedPreferences.edit().remove("scrollToBookmark").apply()
            sharedPreferences.edit().remove("scrollToBookmarkNumber").apply()
        } else if (!scrollToBookmark) {
            if (scrollToAyaNumber != "") {
                val positionOfAyaGiven = scrollToAyaNumber!!.toInt()
                ayaList.setSelection(positionOfAyaGiven)
                sharedPreferences.edit().remove("scrollToAyaNumber").apply()
            } else {
                ayaList.setSelection(lastPosition)
                sharedPreferences.edit().remove("lastPositionJuz ${(number + 1)}").apply()
            }
        }
    }

}