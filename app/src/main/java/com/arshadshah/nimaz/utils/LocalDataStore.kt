package com.arshadshah.nimaz.utils

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.AppDatabase
import com.arshadshah.nimaz.data.local.DataStore

object LocalDataStore {

    private var dataStore: DataStore? = null
    fun init(context: Context) {
        if (dataStore == null) {
            val db = Room
                .databaseBuilder(context, AppDatabase::class.java, "database")
                .createFromAsset("databases/quran_room_compatible.db")
                .fallbackToDestructiveMigration()
                .build()
            dataStore = DataStore(db)
            Log.d(AppConstants.DATA_STORE_TAG, "DataStore initialized")
        }
    }

    fun getDataStore(): DataStore {
        if (dataStore == null) {
            throw IllegalStateException("DataStore not initialized. Call init(context) first.")
        }
        return dataStore!!
    }

    // a function to check if the data store has been initialized
    fun isInitialized(): Boolean {
        return dataStore != null
    }

}