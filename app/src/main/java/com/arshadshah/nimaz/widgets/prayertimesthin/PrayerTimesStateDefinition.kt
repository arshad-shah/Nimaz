package com.arshadshah.nimaz.widgets.prayertimesthin

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

object PrayerTimesStateDefinition : GlanceStateDefinition<PrayerTimesWidget>
{

	private const val DATA_STORE_FILENAME = "prayerTimes"

	private val Context.datastore by dataStore(DATA_STORE_FILENAME , PrayerTimesWidgetSerializer)

	object PrayerTimesWidgetSerializer : Serializer<PrayerTimesWidget>
	{

		override val defaultValue : PrayerTimesWidget = PrayerTimesWidget.Error("No data found")

		override suspend fun readFrom(input : InputStream) : PrayerTimesWidget = try
		{
			Json.decodeFromString(
					 PrayerTimesWidget.serializer() ,
					 input.readBytes().decodeToString()
								 )
		} catch (e : SerializationException)
		{
			throw CorruptionException("Could not read PrayerTimes data: ${e.message}")
		}

		override suspend fun writeTo(t : PrayerTimesWidget , output : OutputStream)
		{
			output.use {
				it.write(
						 Json.encodeToString(PrayerTimesWidget.serializer() , t).toByteArray()
						)
			}
		}

	}

	override suspend fun getDataStore(
		context : Context ,
		fileKey : String ,
									 ) : DataStore<PrayerTimesWidget>
	{
		return context.datastore
	}

	override fun getLocation(context : Context , fileKey : String) : File
	{
		return context.dataStoreFile(DATA_STORE_FILENAME)
	}


}