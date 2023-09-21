package com.arshadshah.nimaz.widgets.prayertimestrackerthin

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.dataStoreFile
import androidx.glance.state.GlanceStateDefinition
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object PrayerTimesTrackerStateDefinition : GlanceStateDefinition<PrayerTimesTrackerWidget>
{

	private const val DATA_STORE_FILENAME = "prayerTimesTracker"

	private val Context.datastore by dataStore(
			 DATA_STORE_FILENAME ,
			 PrayerTimesTrackerWidgetSerializer
											  )

	object PrayerTimesTrackerWidgetSerializer : Serializer<PrayerTimesTrackerWidget>
	{

		override val defaultValue : PrayerTimesTrackerWidget =
			PrayerTimesTrackerWidget.Error("No data found")

		override suspend fun readFrom(input : InputStream) : PrayerTimesTrackerWidget = try
		{
			Json.decodeFromString(
					 PrayerTimesTrackerWidget.serializer() ,
					 input.readBytes().decodeToString()
								 )
		} catch (e : SerializationException)
		{
			throw CorruptionException("Could not read PrayerTimes data: ${e.message}")
		}

		override suspend fun writeTo(t : PrayerTimesTrackerWidget , output : OutputStream)
		{
			output.use {
				it.write(
						 Json.encodeToString(PrayerTimesTrackerWidget.serializer() , t)
							 .toByteArray()
						)
			}
		}

	}

	override suspend fun getDataStore(
		context : Context ,
		fileKey : String ,
									 ) : DataStore<PrayerTimesTrackerWidget>
	{
		return context.datastore
	}

	override fun getLocation(context : Context , fileKey : String) : File
	{
		return context.dataStoreFile(DATA_STORE_FILENAME)
	}


}