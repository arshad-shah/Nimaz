package com.arshadshah.nimaz.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

/**
 * Checks if a device has network connection
 * @author Arshad Shah
 */
class NetworkChecker {

    /**
     * Checks all connections present on a device
     * @author Arshad Shah
     * @param context The context of the Application
     * @return True if connection is detected, False if connection is not detected
     */
    private fun isNetworkAvailable(context: Context?): Boolean {
        if (context == null) return false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    return true
                }

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    return true
                }

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    return true
                }
            }
        }
        return false
    }


    /**
     * Check if there is internet connection
     *
     * @param context the context of the Application
     * */
    fun networkCheck(context: Context): Boolean {
        val networkCheck = isNetworkAvailable(context)
        return if (networkCheck) {
            Log.d("Network", "Network is Successfully connected")
            true
        } else {
            Log.d("Network", "Network is not connected")
            false
        }
    }
}
