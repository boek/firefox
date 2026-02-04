package mozilla.components.lib.accelerometer.sensormanager

import kotlinx.coroutines.test.runTest
import mozilla.components.concept.accelerometer.Accelerometer
import org.junit.Assert.assertEquals
import org.junit.Test

class `SampleNormalizedTest` {
    @Test
    fun `gravityNormalizedSamples normalizes sensor values by gravity`() = runTest {
        val normalizedSample = Accelerometer.Sample(
            xAccel = 2.0f,
            yAccel = -1.5f,
            zAccel = 0.25f,
            timestampNs = 2000000L,
        ).normalized()

        assertEquals(2.0f, normalizedSample.xAccel, 0.001f)
        assertEquals(-1.5f, normalizedSample.yAccel, 0.001f)
        assertEquals(0.25f, normalizedSample.zAccel, 0.001f)
    }
}
