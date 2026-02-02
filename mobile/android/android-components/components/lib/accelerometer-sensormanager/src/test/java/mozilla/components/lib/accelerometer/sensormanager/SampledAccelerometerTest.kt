package mozilla.components.lib.accelerometer.sensormanager

import android.hardware.SensorManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import mozilla.components.concept.accelerometer.Accelerometer
import mozilla.components.concept.accelerometer.AccelerometerEventFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class SampledAccelerometerTest {
    @Test
    fun `gravityNormalizedSamples normalizes sensor values by gravity`() = runTest {
        val eventFlow = AccelerometerEventFlow {
            flow {
                emit(
                    Accelerometer.Sample(
                        xAccel = SensorManager.GRAVITY_EARTH * 2.0f,
                        yAccel = SensorManager.GRAVITY_EARTH * -1.5f,
                        zAccel = SensorManager.GRAVITY_EARTH * 0.25f,
                        timestampNs = 2000000L,
                    ),
                )
            }
        }

        val result = SampledAccelerometer(eventFlow).gravityNormalizedSamples().first()

        assertEquals(2.0f, result.xAccel, 0.001f)
        assertEquals(-1.5f, result.yAccel, 0.001f)
        assertEquals(0.25f, result.zAccel, 0.001f)
    }

    @Test
    fun `gravityNormalizedSamples emits normalized samples for multiple events`() = runTest {
        val eventFlow = AccelerometerEventFlow {
            flowOf(
                Accelerometer.Sample(
                    SensorManager.GRAVITY_EARTH * 0.5f,
                    SensorManager.GRAVITY_EARTH * 1.0f,
                    SensorManager.GRAVITY_EARTH * 0.0f,
                    1000000L,
                ),
                Accelerometer.Sample(
                    SensorManager.GRAVITY_EARTH * 1.5f,
                    SensorManager.GRAVITY_EARTH * -0.5f,
                    SensorManager.GRAVITY_EARTH * 2.0f,
                    2000000L,
                ),
            )
        }

        val samples = SampledAccelerometer(eventFlow).gravityNormalizedSamples().take(2).toList()

        assertEquals(2, samples.size)
        assertEquals(0.5f, samples[0].xAccel, 0.001f)
        assertEquals(1.0f, samples[0].yAccel, 0.001f)
        assertEquals(0.0f, samples[0].zAccel, 0.001f)
        assertEquals(1000000L, samples[0].timestampNs)

        assertEquals(1.5f, samples[1].xAccel, 0.001f)
        assertEquals(-0.5f, samples[1].yAccel, 0.001f)
        assertEquals(2.0f, samples[1].zAccel, 0.001f)
        assertEquals(2000000L, samples[1].timestampNs)
    }

    @Test
    fun `gravityNormalizedSamples handles zero values`() = runTest {
        val eventFlow = AccelerometerEventFlow {
            flowOf(Accelerometer.Sample(0.0f, 0.0f, 0.0f, 1000000L))
        }

        val samples = SampledAccelerometer(eventFlow).gravityNormalizedSamples().take(1).toList()

        assertEquals(1, samples.size)
        assertEquals(0.0f, samples[0].xAccel, 0.001f)
        assertEquals(0.0f, samples[0].yAccel, 0.001f)
        assertEquals(0.0f, samples[0].zAccel, 0.001f)
    }

    @Test
    fun `gravityNormalizedSamples handles negative values`() = runTest {
        val eventFlow = AccelerometerEventFlow {
            flowOf(
                Accelerometer.Sample(
                    -SensorManager.GRAVITY_EARTH,
                    -SensorManager.GRAVITY_EARTH * 2.0f,
                    -SensorManager.GRAVITY_EARTH * 0.5f,
                    3000000L,
                ),
            )
        }

        val samples = SampledAccelerometer(eventFlow).gravityNormalizedSamples().take(1).toList()

        assertEquals(1, samples.size)
        assertEquals(-1.0f, samples[0].xAccel, 0.001f)
        assertEquals(-2.0f, samples[0].yAccel, 0.001f)
        assertEquals(-0.5f, samples[0].zAccel, 0.001f)
        assertEquals(3000000L, samples[0].timestampNs)
    }
}
