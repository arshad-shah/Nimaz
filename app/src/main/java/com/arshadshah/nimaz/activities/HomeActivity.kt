package com.arshadshah.nimaz.activities

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.helperClasses.alarms.CreateAlarms
import com.arshadshah.nimaz.helperClasses.fusedLocations.LocationFinderAuto
import com.arshadshah.nimaz.helperClasses.prayertimes.PrayerTimeThread
import com.arshadshah.nimaz.helperClasses.utils.LocationFinder
import com.arshadshah.nimaz.helperClasses.utils.NetworkChecker
import com.google.android.material.bottomnavigation.BottomNavigationView


/**
 * The main activity that contains the code base for Alarms, and navigation
 * @author Arshad shah
 */
class HomeActivity : AppCompatActivity() {

    override fun onStart() {
        super.onStart()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val locationTypeValue = sharedPreferences.getBoolean("locationType", true)
        val latitude = sharedPreferences.getString("latitude", "0.0")!!.toDouble()
        val longitude = sharedPreferences.getString("longitude", "0.0")!!.toDouble()
        if (locationTypeValue) {
            LocationFinderAuto().getLocations(this, 12345)

            LocationFinder().findCityName(this, latitude, longitude)
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val menuSelected = sharedPreferences.getInt("menuSelected", R.id.navigation_home)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.selectedItemId = menuSelected
    }

    override fun onPause() {
        super.onPause()
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (navView.selectedItemId == R.id.navigation_setting) {
            with(sharedPreferences.edit()) {
                putInt("menuSelected", R.id.navigation_home)
                apply()
            }
        } else {
            with(sharedPreferences.edit()) {
                putInt("menuSelected", navView.selectedItemId)
                apply()
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Retrieve values given in the settings activity
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isFirstInstall = sharedPreferences.getBoolean("isFirstInstall", true)
        val isNetworkAvailable = NetworkChecker().networkCheck(this)
        if (!isNetworkAvailable && isFirstInstall) {
            with(sharedPreferences.edit()) {
                putBoolean("isFirstInstall", false)
                apply()
            }
            //show a dialog
            val builder = AlertDialog.Builder(this)
            builder.setTitle("No Internet Connection")
            builder.setMessage("Please connect to the internet to get Prayer Times or Enter Your coordinates in Settings.")
            builder.setPositiveButton("OK") { _, _ ->
                //dismiss the dialog
                builder.create().dismiss()
                Toast.makeText(this, "Using default Values for Prayer times", Toast.LENGTH_SHORT)
                    .show()
            }
            builder.create().show()
        }

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)

        this.onBackPressedDispatcher.addCallback(this) {
            // Handle the back button event
            if (navController.currentDestination?.id == R.id.navigation_home) {
                finish()
            } else {
                navController.navigate(R.id.navigation_home)
                navView.selectedItemId = R.id.navigation_home
            }
        }

        navView.setOnItemSelectedListener { menu ->
            when (menu.itemId) {
                R.id.navigation_home -> {
                    navController.navigate(R.id.navigation_home)
                    true
                }
                R.id.navigation_compass -> {
                    navController.navigate(R.id.navigation_compass)
                    true
                }
                R.id.navigation_quran -> {
                    navController.navigate(R.id.navigation_quran)
                    true
                }
                R.id.navigation_more -> {
                    navController.navigate(R.id.navigation_more)
                    true
                }
                R.id.navigation_setting -> {
                    navController.navigate(R.id.navigation_setting)
                    true
                }
                else -> false
            }
        }

        supportActionBar?.hide()

        //create channels
        CreateAlarms().createChannel(this)

        //alarm lock
        val alarmLock = sharedPreferences.getBoolean("alarmLock", false)
        if (!alarmLock) {
            val prayerThread = PrayerTimeThread(this.applicationContext)
            prayerThread.start()
        }
    } // end of oncreate
} // end of class
