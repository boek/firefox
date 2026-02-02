/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.accelerometer.sensormanager

import android.hardware.SensorManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mozilla.components.concept.accelerometer.Accelerometer
import mozilla.components.concept.accelerometer.AccelerometerEventFlow

/**
 *
 */
class SampledAccelerometer(
    private val flow: AccelerometerEventFlow,
) : Accelerometer {

    override fun gravityNormalizedSamples(): Flow<Accelerometer.Sample> = flow
        .flowEvents()
        .map { sample ->
            sample.copy(
                xAccel = sample.xAccel / SensorManager.GRAVITY_EARTH,
                yAccel = sample.yAccel / SensorManager.GRAVITY_EARTH,
                zAccel = sample.zAccel / SensorManager.GRAVITY_EARTH,
            )
        }
}
