package com.arshadshah.nimaz.utils

import android.content.Context
import androidx.room.Room
import com.arshadshah.nimaz.data.local.AppDatabase
import com.arshadshah.nimaz.data.local.DataStore

object LocalDataStore
{

	private var dataStore : DataStore? = null
	fun init(context : Context)
	{
		if (dataStore == null)
		{
			val db = Room.databaseBuilder(context , AppDatabase::class.java , "database").build()
			dataStore = DataStore(db)
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

}