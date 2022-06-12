package com.arshadshah.nimaz.fragments.intro

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.activities.HomeActivity
import com.arshadshah.nimaz.helperClasses.fusedLocations.LocationFinderAuto
import com.arshadshah.nimaz.helperClasses.fusedLocations.PermissionUtils
import com.arshadshah.nimaz.helperClasses.utils.NetworkChecker
import com.google.android.material.switchmaterial.SwitchMaterial

class LocationSelectionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_location_selection, container, false)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val locationTypeValue = sharedPreferences.getBoolean("locationType", true)
        val locationFinish: Button = root.findViewById(R.id.locationFinish)
        val locationSkip: Button = root.findViewById(R.id.locationSkip)

        val locationPref: SwitchMaterial = root.findViewById(R.id.locationPref)

        val isNetworkAvailable = NetworkChecker().networkCheck(requireContext())

        if (!locationPref.isChecked) {
            with(sharedPreferences.edit()) {
                putBoolean("locationType", false)
                apply()
            }
            if (isNetworkAvailable) {
                locationFinish.text = "NEXT"
            } else {
                locationFinish.text = "FINISH"
            }
        }

        locationPref.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                with(sharedPreferences.edit()) {
                    putBoolean("locationType", true)
                    apply()
                }
                locationPref.text = "Automatic"
                PermissionUtils.askAccessLocationPermission(
                    requireActivity() as AppCompatActivity,
                    12345
                )

                locationFinish.text = "FINISH"
            } else {
                with(sharedPreferences.edit()) {
                    putBoolean("locationType", false)
                    apply()
                }
                locationPref.text = "Manual"

                if (isNetworkAvailable) {
                    locationFinish.text = "NEXT"
                } else {
                    locationFinish.text = "FINISH"
                }
            }
        }


        val batteryOpt: SwitchMaterial = root.findViewById(R.id.batteryOpt)

        val pm: PowerManager =
            requireContext().getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager

        batteryOpt.isChecked = pm.isIgnoringBatteryOptimizations(requireContext().packageName)

        //if batterOpt is true then disable the switch
        batteryOpt.isEnabled = !batteryOpt.isChecked


        batteryOpt.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            }
        }

        // the Progress container progressContainer, and the mainCard 
        val progressContainer: CardView = root.findViewById(R.id.progressContainer)
        val mainCard: CardView = root.findViewById(R.id.mainCard)

        locationFinish.setOnClickListener {
            val locationTypeValueAfter = sharedPreferences.getBoolean("locationType", true)
            if (isNetworkAvailable) {
                if (!locationTypeValueAfter) {
                    val navcontroller =
                        requireActivity().findNavController(R.id.fragmentContainerView)
                    navcontroller.navigate(R.id.locationInputFragment)
                } else {
                    with(sharedPreferences.edit()) {
                        putBoolean("isFirstInstall", false)
                        putBoolean("navigateToHome", true)
                        putBoolean("channelLock", false)
                        apply()
                    }
                    if (locationTypeValue) {
                        LocationFinderAuto().getLocations(requireContext(), 12345)
                    }

                    //create a thread to show the progress bar
                    //while the longitude and latitude values are "0.0" hide mainCard and show a progress bar
                    //then when the values are set navigate to HomeActivity
                    //wait for the longitude and latitude values to not be null then continue
                    //show the progress bar and hide the mainCard
                    progressContainer.visibility = View.VISIBLE
                    mainCard.visibility = View.GONE
                    val thread = Thread {
                        Looper.prepare()
                        while (true) {
                            if (sharedPreferences.getString("longitude", "0.0") != "0.0"
                                && sharedPreferences.getString("latitude", "0.0") != "0.0"
                            ) {
                                //on ui thread
                                activity?.runOnUiThread {
                                    val intent = Intent(requireContext(), HomeActivity::class.java)
                                    startActivity(intent)
                                    requireActivity().finish()
                                }
                                break
                            }
                        }
                    }
                    thread.start()
                }
            } else {
                if (locationTypeValueAfter) {
                    with(sharedPreferences.edit()) {
                        putBoolean("navigateToHome", true)
                        putBoolean("channelLock", false)
                        putBoolean("locationType", false)
                        apply()
                    }
                    val intent = Intent(requireContext(), HomeActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    with(sharedPreferences.edit()) {
                        putBoolean("navigateToHome", true)
                        putBoolean("channelLock", false)
                        putBoolean("locationType", false)
                        apply()
                    }
                    val intent = Intent(requireContext(), HomeActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                }
            }


        }

        //if skipped
        locationSkip.setOnClickListener {
            createDialog()
        }

        return root
    }


    /***
     * create a dialog to add a reminder
     */
    private fun createDialog() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        // Create the object of
        // AlertDialog Builder class
        val builder = AlertDialog.Builder(requireContext())
        val inflater: LayoutInflater? = activity?.layoutInflater
        val locationInputDialog: View = inflater!!.inflate(R.layout.locationskip, null)
        val dialogYes: Button = locationInputDialog.findViewById(R.id.dialogYes)
        val dialogNo: Button = locationInputDialog.findViewById(R.id.dialogNo)
        builder.setView(locationInputDialog)
        // Set Cancelable false
        builder.setCancelable(false)
        // Create the Alert dialog
        val alertDialog = builder.create()
        // Show the Alert Dialog box
        alertDialog.show()
        dialogYes.setOnClickListener { view: View? ->
            with(sharedPreferences.edit()) {
                putBoolean("isFirstInstall", false)
                putBoolean("channelLock", false)
                putBoolean("locationType", false)
                apply()
            }
            //navigate to homeactivity and finish this activity
            val intent = Intent(requireContext(), HomeActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
            alertDialog.cancel()
        }
        dialogNo.setOnClickListener { view: View? ->
            alertDialog.cancel()
        }
    }
}