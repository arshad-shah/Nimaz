package com.arshadshah.nimaz.activities.quran

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.preference.PreferenceManager
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.fragments.quran.AyaListJuzFragment
import com.arshadshah.nimaz.fragments.quran.AyaListSurahFragment
import com.arshadshah.nimaz.fragments.quran.QuranSearchFragment
import com.arshadshah.nimaz.helperClasses.database.BookmarkDatabaseAccessHelper
import com.arshadshah.nimaz.helperClasses.database.DatabaseAccessHelper

class QuranMainList : AppCompatActivity() {

    var nameOfPage: TextView? = null
    var numberOfPage: TextView? = null
    var fragmentToUse = ""
    var query = ""

    private lateinit var helperBookmarkDatabase: BookmarkDatabaseAccessHelper
    private lateinit var helperQuranDatabase: DatabaseAccessHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quran_main_list)
        supportActionBar?.hide()
        helperBookmarkDatabase = BookmarkDatabaseAccessHelper(this)
        helperQuranDatabase = DatabaseAccessHelper(this)
        helperQuranDatabase.open()

        //get the values from the intent
        val intent = intent

        fragmentToUse = intent.getStringExtra("fragment").toString()

        val backButton: ImageView = findViewById(R.id.backButton2)

        val searchFragmentTitle: LinearLayout = findViewById(R.id.searchFragmentTitle)

        backButton.setOnClickListener {
            val expandIn: Animation =
                AnimationUtils.loadAnimation(this, R.anim.expand_in)
            backButton.startAnimation(expandIn)
            finish()
        }

        nameOfPage = findViewById(R.id.NameToChange)
        numberOfPage = findViewById(R.id.numberOfPage)

        val moreButton: ImageView = findViewById(R.id.moreButton2)

        moreButton.isVisible = fragmentToUse != "search"

        moreButton.setOnClickListener {
            val expandIn: Animation =
                AnimationUtils.loadAnimation(this, R.anim.expand_in)
            moreButton.startAnimation(expandIn)

            val number = intent.getIntExtra("number", 0)
            val name = intent.getStringExtra("name")

            //open the menu called quran_menu
            val menu = PopupMenu(this, moreButton)
            menu.inflate(R.menu.quran_menu_ayat)

            val isBookmark = checkForBookmark(number)
            menu.menu.findItem(R.id.bookmark).isVisible = isBookmark

            launchMenu(this, name.toString(), number, menu)
        }


        if (fragmentToUse == "search") {
            query = intent.getStringExtra("query").toString()

            val numberOfAyas =
                helperQuranDatabase.searchForAyaAmountFound(query, "en_sahih", "text")

            //separate the hashmap into the amount of keys it has into separate arraylist of strings
            //where the array name is the key and the value is the value
            val arrayOfKeys = ArrayList<String>()
            val arrayOfValues = ArrayList<String>()

            numberOfAyas.forEach {
                arrayOfKeys.add(it.key)
                arrayOfValues.add(it.value.toString())
            }

            if (numberOfAyas.isNotEmpty()) {
                val bundle = Bundle()
                bundle.putString("query", query)
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    //add new fragment
                    add<QuranSearchFragment>(R.id.fragmentContainerView3, args = bundle)
                }
                //get string from resources and add the number of ayas found and the query to it
                searchFragmentTitle.isVisible = true

                //for each key in arraylist of keys render a textview with the key and value
                for (i in arrayOfKeys.indices) {
                    val textView = TextView(this)
                    textView.text = "Keyword: " + arrayOfKeys[i] + " was Found " + arrayOfValues[i] + " Times"
                    textView.setTextColor(resources.getColor(R.color.primaryTextColor))
                    textView.setPadding(0, 5, 0, 0)
                    searchFragmentTitle.addView(textView)
                }

                numberOfPage!!.isVisible = false
                nameOfPage?.isVisible = false
                helperQuranDatabase.close()
            } else {
                nameOfPage!!.text = getString(R.string.ayatNotFound)
                numberOfPage!!.isVisible = false
                searchFragmentTitle.isVisible = false

                //show a a custom dialog
                val builder = AlertDialog.Builder(this)

                //set the custom view
                val customView =
                    LayoutInflater.from(this).inflate(R.layout.resultnotfounddialog, null)
                builder.setView(customView)

                //set the message
                val message = customView.findViewById<TextView>(R.id.message)
                message.text = getString(R.string.ayatNotFoundMessage, query)

                val dialog: AlertDialog = builder.create()
                //set the button
                val button = customView.findViewById<Button>(R.id.retry)
                button.setOnClickListener {
                    finish()
                    dialog.dismiss()
                }

                dialog.show()

            }
        } else {
            numberOfPage!!.isVisible = true
            nameOfPage?.isVisible = true
            searchFragmentTitle.isVisible = false
            val number = intent.getIntExtra("number", 0)
            val name = intent.getStringExtra("name")
            helperQuranDatabase.close()
            fragmentSelecterForListDisplay(name!!, number)
        }
    }


    private fun fragmentSelecterForListDisplay(name: String, number: Int) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (fragmentToUse == "juz") {
            val numberOfJuz = number + 1
            nameOfPage!!.text = name
            numberOfPage!!.text = numberOfJuz.toString()
            val bundle = Bundle()
            bundle.putInt("number", number)
            bundle.putBoolean(
                "scrollToBookmark",
                sharedPreferences.getBoolean("scrollToBookmark", false)
            )
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                //add new fragment
                add<AyaListJuzFragment>(R.id.fragmentContainerView3, args = bundle)
            }
        } else {
            val numberOfSurah = number + 1
            nameOfPage!!.text = name
            numberOfPage!!.text = numberOfSurah.toString()
            val bundle = Bundle()
            bundle.putInt("number", number)
            bundle.putBoolean(
                "scrollToBookmark",
                sharedPreferences.getBoolean("scrollToBookmark", false)
            )
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<AyaListSurahFragment>(R.id.fragmentContainerView3, args = bundle)
            }
        }
    }

    private fun launchMenu(context: Context, name: String, number: Int, menu: PopupMenu) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        menu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.bookmark -> {
                    checkForBookmark(number)
                    fragmentSelecterForListDisplay(name, number)
                }
                R.id.gotoAyat -> {
                    //create a dialog box that has the ayat numbers of the fragment to be displayed
                    //the user can select the ayat number and the fragment will be displayed with the list scroll to that ayat
                    val builder: AlertDialog.Builder = AlertDialog.Builder(context)
                    //get layout inflater
                    val inflater: LayoutInflater =
                        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

                    val gotoayadialog = inflater.inflate(R.layout.gotoayadialog, null)
                    val ayatNumber: EditText = gotoayadialog.findViewById(R.id.quranSearch)
                    val startAyaNumber: TextView = gotoayadialog.findViewById(R.id.startAyaNumber)
                    val endAyaNumber: TextView = gotoayadialog.findViewById(R.id.endAyaNumber)
                    val submitBtn: Button = gotoayadialog.findViewById(R.id.dialogSubmit)
                    val cancelbtn: Button = gotoayadialog.findViewById(R.id.dialogCancel)

                    helperQuranDatabase.open()

                    val aya = if (fragmentToUse == "juz") {
                        helperQuranDatabase.getNumberOfAyatJuz(number + 1)
                    } else {
                        helperQuranDatabase.getNumberOfAyatSurah(number + 1)
                    }

                    helperQuranDatabase.close()

                    //get the start index of the aya list and not the value
                    val startAyat = (aya.indexOf(aya.first())) + 1

                    val endAyat = aya.size

                    startAyaNumber.text = startAyat.toString()
                    endAyaNumber.text = endAyat.toString()

                    builder.setView(gotoayadialog)

                    // Set Cancelable false
                    // for when the user clicks on the outside
                    // the Dialog Box then it will remain show
                    builder.setCancelable(false)

                    // Create the Alert dialog
                    val alertDialog: AlertDialog = builder.create()
                    // Show the Alert Dialog box
                    alertDialog.show()

                    submitBtn.setOnClickListener {

                        val ayatToGOTo = ayatNumber.text

                        sharedPreferences.edit().putString(
                            "scrollToAyaNumber",
                            ayatToGOTo.toString()
                        ).apply()
                        fragmentSelecterForListDisplay(name, number)

                        alertDialog.cancel()

                    }

                    cancelbtn.setOnClickListener {
                        alertDialog.cancel()
                    }

                }
            }
            true
        }
        menu.show()
    }

    private fun checkForBookmark(number: Int): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        helperBookmarkDatabase.open()
        helperQuranDatabase.open()
        var isBookmark = false
        val aya = if (fragmentToUse == "juz") {
            helperQuranDatabase.getAllAyaForJuz(number + 1)
        } else {
            helperQuranDatabase.getAllAyaForSurah(number + 1)
        }

        if (fragmentToUse == "juz") {
            //check the juz for the bookmark
            for (i in 0 until aya.size) {
                if (helperBookmarkDatabase.isAyaBookmarkedJuz(
                        aya[i]!!.ayaNumber,
                        aya[i]!!.ayaEnglish,
                        aya[i]!!.ayaArabic
                    )
                ) {
                    isBookmark = true
                    sharedPreferences.edit().putBoolean("scrollToBookmark", true)
                        .apply()
                    sharedPreferences.edit().putInt("scrollToBookmarkNumber", i).apply()
                    break
                }
            }
        } else {
            //check the surah for the bookmark
            for (i in 0 until aya.size) {
                if (helperBookmarkDatabase.isAyaBookmarkedSurah(
                        aya[i]!!.ayaNumber,
                        aya[i]!!.ayaEnglish,
                        aya[i]!!.ayaArabic
                    )
                ) {
                    isBookmark = true
                    sharedPreferences.edit().putBoolean("scrollToBookmark", true)
                        .apply()
                    sharedPreferences.edit().putInt("scrollToBookmarkNumber", i).apply()
                    break
                }
            }
        }
        helperBookmarkDatabase.close()
        helperQuranDatabase.close()
        return isBookmark
    }
}