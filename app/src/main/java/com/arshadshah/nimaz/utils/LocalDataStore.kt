package com.arshadshah.nimaz.utils

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.AppDatabase
import com.arshadshah.nimaz.data.local.DataStore

object LocalDataStore
{

	private var dataStore : DataStore? = null
	fun init(context : Context)
	{
		if (dataStore == null)
		{
			val db = Room.databaseBuilder(context , AppDatabase::class.java , "database")
				.addMigrations(AppDatabase.Migration1To2())
				.addMigrations(AppDatabase.Migration2To3())
				.addMigrations(AppDatabase.Migration3To4())
				.addMigrations(AppDatabase.Migration4To5())
				.addMigrations(AppDatabase.Migration5To6())
				.addMigrations(AppDatabase.Migration6To7())
				.addMigrations(AppDatabase.Migration7To8())
				.addMigrations(AppDatabase.Migration8To9())
				.addMigrations(AppDatabase.Migration9To10())
				.addMigrations(AppDatabase.Migration10To11())
				.build()
			dataStore = DataStore(db)
			Log.d(AppConstants.DATA_STORE_TAG , "DataStore initialized")
		}
	}

	fun getDataStore() : DataStore
	{
		if (dataStore == null)
		{
			throw IllegalStateException("DataStore not initialized. Call init(context) first.")
		}
		return dataStore !!
	}

	// a function to check if the data store has been initialized
	fun isInitialized() : Boolean
	{
		return dataStore != null
	}

}