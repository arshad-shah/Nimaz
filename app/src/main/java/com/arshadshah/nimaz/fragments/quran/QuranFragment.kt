package com.arshadshah.nimaz.fragments.quran

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.activities.quran.QuranMainList
import com.arshadshah.nimaz.helperClasses.quran.QuranFragmentAdapter
import com.google.android.material.tabs.TabLayout

class QuranFragment : Fragment() {

    private lateinit var quranTabLayout: TabLayout
    private lateinit var quranViewPager: ViewPager2
    private lateinit var quranFragmentAdapter: FragmentStateAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_quran, container, false)

        val moreButton: ImageButton = root.findViewById(R.id.moreButton)
        moreButton.setOnClickListener {
            val expandIn: Animation =
                AnimationUtils.loadAnimation(requireContext(), R.anim.expand_in)
            moreButton.startAnimation(expandIn)


            //open the menu called quran_menu
            val menu = PopupMenu(requireContext(), moreButton)
            menu.inflate(R.menu.quran_menu)

            menu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.translation -> {
                        // Create the object of
                        // AlertDialog Builder class
                        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
                        val moreDialog = inflater.inflate(R.layout.translation_dialog, null)
                        val englishTranslation: RadioButton =
                            moreDialog.findViewById(R.id.englishTranslation)
                        val urduTranslation: RadioButton =
                            moreDialog.findViewById(R.id.urduTranslation)
                        val submitBtn: Button = moreDialog.findViewById(R.id.dialogSubmit)
                        val cancelbtn: Button = moreDialog.findViewById(R.id.dialogCancel)

                        val sharedPreferences =
                            PreferenceManager.getDefaultSharedPreferences(requireContext())
                        val isEnglish = sharedPreferences.getBoolean("isEnglish", true)
                        builder.setView(moreDialog)

                        // Set Cancelable false
                        // for when the user clicks on the outside
                        // the Dialog Box then it will remain show
                        builder.setCancelable(false)
                        // Create the Alert dialog
                        val alertDialog: AlertDialog = builder.create()
                        // Show the Alert Dialog box
                        alertDialog.show()

                        var translationSelected = ""
                        if (isEnglish) {
                            englishTranslation.isChecked = true
                        } else {
                            urduTranslation.isChecked = true
                        }

                        englishTranslation.setOnClickListener {
                            translationSelected = "english"
                        }

                        urduTranslation.setOnClickListener {
                            translationSelected = "urdu"
                        }

                        submitBtn.setOnClickListener {
                            if (translationSelected == "english") {
                                with(sharedPreferences.edit()) {
                                    putBoolean("isEnglish", true)
                                    apply()
                                }
                            } else {
                                with(sharedPreferences.edit()) {
                                    putBoolean("isEnglish", false)
                                    apply()
                                }
                            }

                            alertDialog.cancel()
                        }

                        cancelbtn.setOnClickListener {
                            alertDialog.cancel()
                        }
                    }
                    R.id.search -> {
                        // Create the object of
                        // AlertDialog Builder class
                        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
                        val moreDialog = inflater.inflate(R.layout.quransearch, null)
                        val quranSearch: EditText = moreDialog.findViewById(R.id.quranSearch)
                        val submitBtn: Button = moreDialog.findViewById(R.id.dialogSubmit)
                        val cancelbtn: Button = moreDialog.findViewById(R.id.dialogCancel)

                        val sharedPreferences =
                            PreferenceManager.getDefaultSharedPreferences(requireContext())
                        builder.setView(moreDialog)

                        // Set Cancelable false
                        // for when the user clicks on the outside
                        // the Dialog Box then it will remain show
                        builder.setCancelable(false)
                        // Create the Alert dialog
                        val alertDialog: AlertDialog = builder.create()
                        // Show the Alert Dialog box
                        alertDialog.show()


                        submitBtn.setOnClickListener {
                            // get the input from the user
                            val searchText = quranSearch.text.toString()

                            if (searchText.isNotEmpty()) {
                                // if the user has entered text then
                                // open the search fragment
                                val intent = Intent(requireContext(), QuranMainList::class.java)
                                intent.putExtra("query", searchText)
                                intent.putExtra("fragment", "search")
                                startActivity(intent)
                                alertDialog.cancel()
                            } else {
                                // if the user has not entered any text
                                // then show a toast message
                                Toast.makeText(
                                    requireContext(),
                                    "Please enter some text",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
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


        quranTabLayout = root.findViewById(R.id.quranTabLayout)
        quranViewPager = root.findViewById(R.id.quranViewPager)

        val fragmentManager = childFragmentManager
        quranFragmentAdapter = QuranFragmentAdapter(fragmentManager, lifecycle)
        quranViewPager.adapter = quranFragmentAdapter

        val tab1 = quranTabLayout.newTab().setText(R.string.surah)
        val tab2 = quranTabLayout.newTab().setText(R.string.juz)
        quranTabLayout.addTab(tab1)
        quranTabLayout.addTab(tab2)

        quranTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                quranViewPager.currentItem = tab!!.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

        })

        quranViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                quranTabLayout.selectTab(quranTabLayout.getTabAt(position))
            }
        })
        return root
    }
}