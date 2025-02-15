package com.arshadshah.nimaz.utils

import com.arshadshah.nimaz.libs.prayertimes.objects.Coordinates
import org.junit.Assert.*
import org.junit.Test

class QiblaTest {

    //coordinates of dublin
    private val dublin = Coordinates(53.3498, -6.2603)

    //london
    private val london = Coordinates(51.5074, -0.1278)

    //makkah
    private val makkah = Coordinates(21.4225241, 39.8261818)

    //madina
    private val madina = Coordinates(24.466667, 39.6)

    //new york
    private val newYork = Coordinates(40.7128, -74.0060)

    //los angeles
    private val losAngeles = Coordinates(34.0522, -118.2437)

    //sydney
    private val sydney = Coordinates(-33.8688, 151.2093)

    //dubai
    private val dubai = Coordinates(25.2048, 55.2708)

    @Test
    fun calculateQiblaDirectionForDublin() {
        val qibla = Qibla()
        val qiblaDirection = qibla.calculateQiblaDirection(dublin.latitude, dublin.longitude)
        assertEquals(114.09999086399652, qiblaDirection, 0.1)
    }

    @Test
    fun calculateQiblaDirectionForLondon() {
        val qibla = Qibla()
        val qiblaDirection = qibla.calculateQiblaDirection(london.latitude,london.longitude)
        assertEquals(118.98721898131991, qiblaDirection, 0.1)
    }

    @Test
    fun calculateQiblaDirectionForMakkah() {
        val qibla = Qibla()
        val qiblaDirection = qibla.calculateQiblaDirection(makkah.latitude,makkah.longitude)
        assertEquals(180.0, qiblaDirection, 0.1)
    }

    @Test
    fun calculateQiblaDirectionForMadina() {
        val qibla = Qibla()
        val qiblaDirection = qibla.calculateQiblaDirection(madina.latitude,madina.longitude)
        assertEquals(176.0412330078243, qiblaDirection, 0.1)
    }

    @Test
    fun calculateQiblaDirectionForNewYork() {
        val qibla = Qibla()
        val qiblaDirection = qibla.calculateQiblaDirection(newYork.latitude,newYork.longitude)
        assertEquals(58.48169650089118, qiblaDirection, 0.1)
    }

    @Test
    fun calculateQiblaDirectionForLosAngeles() {
        val qibla = Qibla()
        val qiblaDirection = qibla.calculateQiblaDirection(losAngeles.latitude,losAngeles.longitude)
        assertEquals(23.857084255883077, qiblaDirection, 0.1)
    }

    @Test
    fun calculateQiblaDirectionForSydney() {
        val qibla = Qibla()
        val qiblaDirection = qibla.calculateQiblaDirection(sydney.latitude,sydney.longitude)
        assertEquals(277.4996044487399, qiblaDirection, 0.1)
    }

    @Test
    fun calculateQiblaDirectionForDubai() {
        val qibla = Qibla()
        val qiblaDirection = qibla.calculateQiblaDirection(dubai.latitude,dubai.longitude)
        assertEquals(258.23131617980795, qiblaDirection, 0.1)
    }
}