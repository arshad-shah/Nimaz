package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.CompassAccuracy
import kotlinx.coroutines.flow.Flow

/**
 * The device's orientation, as degrees rather than as sensor plumbing.
 *
 * `QiblaViewModel` held a `SensorManager` and a `SensorEventListener`, low-pass-filtered the
 * gravity and geomagnetic vectors itself, and called `SensorManager.getRotationMatrix` /
 * `getOrientation` — so it needed an `@ApplicationContext` and could not be constructed in a
 * JVM test.
 *
 * The filtering and the rotation-matrix fusion are platform math, and they live behind this
 * seam. What the ViewModel keeps is the part that is actually about the qibla: unwrapping the
 * azimuth so the needle does not snap through 360→0, applying magnetic declination, and
 * deciding when the user is facing the Kaaba.
 */
interface CompassSensors {

    /** False when the device has no accelerometer or no magnetometer — no compass is possible. */
    val isAvailable: Boolean

    /**
     * Orientation samples while collected; registers on collect and unregisters on cancel, so
     * the listener's lifetime is the flow's rather than something the caller must remember to
     * undo.
     */
    fun orientation(): Flow<CompassOrientation>
}

/** A single orientation reading, in degrees, plus how much the magnetometer can be trusted. */
data class CompassOrientation(
    /** Heading from **magnetic** north, 0–360. Declination is the caller's to apply. */
    val azimuthDegrees: Float,
    val pitchDegrees: Float,
    val rollDegrees: Float,
    val accuracy: CompassAccuracy,
)

/**
 * A short confirmation buzz.
 *
 * Separate from [CompassSensors] because it is an output, not a reading — and because the
 * qibla screen's haptic is a preference the user can switch off, which is a decision the
 * ViewModel makes and this seam merely carries out.
 */
interface Haptics {
    /** One brief tap. */
    fun tap()
}
