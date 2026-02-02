/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.accelerometer

import kotlinx.coroutines.flow.Flow

/**
 * A flow of data of accelerometer events. This is separated from [Accelerometer] so that we
 * can wrap platform-specific implementations of data streams separate from any behavior we want
 * to be surfaced from the [Accelerometer] itself.
 */
fun interface AccelerometerEventFlow {
    /**
     * The flow of data.
     */
    fun flowEvents(): Flow<Accelerometer.Sample>
}

/**
 * An accelerometer for measuring changes to a devices motion.
 */
interface Accelerometer {
    /**
     * A stream of accelerometer data that has been normalized for the ever-present
     * forces of gravity.
     */
    fun gravityNormalizedSamples(): Flow<Sample>

    /**
     * A simple data type containing acceleration data at a specific time.
     */
    data class Sample(
        val xAccel: Float,
        val yAccel: Float,
        val zAccel: Float,
        val timestampNs: Long,
    )
}
