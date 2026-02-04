package mozilla.components.lib.accelerometer.sensormanager

import android.hardware.SensorManager
import mozilla.components.concept.accelerometer.Accelerometer

// We want to normalize the samples using Gravity
fun Accelerometer.Sample.normalized() = copy(
    xAccel = xAccel / SensorManager.GRAVITY_EARTH,
    yAccel = yAccel / SensorManager.GRAVITY_EARTH,
    zAccel = zAccel / SensorManager.GRAVITY_EARTH,
)
