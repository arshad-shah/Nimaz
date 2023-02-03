package com.arshadshah.nimaz.data.local.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface QiblaDao
{

	//get the qibla direction which has the latest timestamp
	@Query("SELECT direction FROM Qibla ORDER BY timestamp DESC LIMIT 1")
	suspend fun getQiblaDirection() : Double

	//set the qibla direction
	@Query("INSERT INTO Qibla (timestamp,direction) VALUES (:timestamp,:direction)")
	suspend fun setQiblaDirection(timestamp : Long , direction : Double)

	//count the number of qibla directions
	@Query("SELECT COUNT(*) FROM Qibla")
	suspend fun countQiblaDirections() : Int
}