package com.arshadshah.nimaz.data.remote.repositories

import android.content.Context
import android.util.Log
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.internal.StaticCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.services.s3.AmazonS3Client
import es.dmoral.toasty.Toasty
import java.io.File


interface SpaceRegionRepresentable
{

	fun endpoint() : String
}

/**
 * Represents a region in which a Digital Ocean Space can be created
 */
enum class SpaceRegion : SpaceRegionRepresentable
{

	//new york
	NYC
	{

		override fun endpoint() : String
		{
			return "https://nyc3.digitaloceanspaces.com"
		}
	} ,
}

class SpacesFileRepository(context : Context)
{

	private val accesskey = "DO00P7G3HY2YDGKTKTLL"
	private val secretkey = "dkCegR9Rb7B5LexhVifi85mfSTComyDpy8Z/sl6dY/U"
	private val spacename = "quran-audio"
	private val spaceregion = SpaceRegion.NYC

	private val filetype = "mp3"

	private var transferUtility : TransferUtility
	private var appContext : Context

	init
	{
		val credentials = StaticCredentialsProvider(BasicAWSCredentials(accesskey , secretkey))
		val client = AmazonS3Client(credentials , Region.getRegion("us-east-1"))
		client.endpoint = spaceregion.endpoint()
		transferUtility = TransferUtility.builder().s3Client(client).context(context).build()
		appContext = context
	}

	/**
	 * Downloads example file from a DO Space
	 */
	fun downloadAyaFile(
		suraNumber : Int ,
		ayaNumber : Int ,
		callback : (File? , Exception? , progress : Int , completed : Boolean) -> Unit ,
					   )
	{
		val surahNumber = if (suraNumber < 10)
			"00$suraNumber"
		else if (suraNumber < 100)
			"0$suraNumber"
		else
			"$suraNumber"

		val ayahNumber = if (ayaNumber < 10)
			"00$ayaNumber"
		else if (ayaNumber < 100)
			"0$ayaNumber"
		else
			"$ayaNumber"

		//Create a local File object to save the remote file to
		val file = File("${appContext.filesDir}/quran/$surahNumber/$ayahNumber.$filetype")

		//Download the file from DO Space
		//mishary/quran-surah-001-verse-001.mp3
		val listener = transferUtility.download(
				spacename ,
				"mishary/quran-surah-$surahNumber-verse-$ayahNumber.$filetype" ,
				file
											   )

		//Listen to the progress of the download, and call the callback when the download is complete
		listener.setTransferListener(object : TransferListener
									 {
										 override fun onProgressChanged(
											 id : Int ,
											 bytesCurrent : Long ,
											 bytesTotal : Long ,
																	   )
										 {
											 Log.i(
													 "S3 Download" ,
													 "Progress ${((bytesCurrent / bytesTotal) * 100)}"
												  )
											 callback(
													 null ,
													 null ,
													 ((bytesCurrent / bytesTotal) * 100).toInt() ,
													 false
													 )
										 }

										 override fun onStateChanged(
											 id : Int ,
											 state : TransferState? ,
																	)
										 {
											 when (state)
											 {
												 TransferState.COMPLETED ->
												 {
													 Log.i("S3 Download" , "Completed")
													 callback(file , null , 100 , true)
													 Toasty.success(
															 appContext ,
															 "Downloaded successfully!"
																   ).show()
												 }

												 TransferState.IN_PROGRESS ->
												 {
													 Log.i("S3 Download" , "In Progress")
													 callback(null , null , 0 , false)
												 }

												 TransferState.FAILED ->
												 {
													 Log.i("S3 Download" , "Failed")
													 callback(
															 null ,
															 Exception("Failed to download file") ,
															 0 ,
															 false
															 )
													 Toasty.error(
															 appContext ,
															 "Failed to download file"
																 ).show()
												 }

												 TransferState.CANCELED ->
												 {
													 Log.i("S3 Download" , "Canceled")
													 callback(
															 null ,
															 Exception("Canceled") ,
															 0 ,
															 false
															 )
													 Toasty.error(appContext , "Canceled").show()
												 }

												 else ->
												 {
													 Log.i("S3 Download" , "Other")
													 callback(null , Exception("Other") , 0 , false)
													 Toasty.error(appContext , "Other").show()
												 }
											 }
										 }

										 override fun onError(id : Int , ex : Exception?)
										 {
											 Log.e("S3 Download" , ex.toString())
											 callback(null , ex , 0 , false)
											 Toasty.error(appContext , ex.toString()).show()
										 }
									 })
	}
}