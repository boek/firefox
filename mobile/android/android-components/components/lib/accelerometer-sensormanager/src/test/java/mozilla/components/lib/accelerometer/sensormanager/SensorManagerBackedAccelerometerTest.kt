/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.accelerometer.sensormanager

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import mozilla.components.support.test.mock
import mozilla.components.support.test.whenever
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SensorManagerBackedAccelerometerTest {

    private lateinit var mockSensorManager: SensorManager
    private lateinit var mockSensor: Sensor
    private lateinit var eventFlow: SensorManagerBackedAccelerometer
    private val logMessages = mutableListOf<String>()

    @Before
    fun setUp() {
        mockSensorManager = mock()
        mockSensor = mock()
        whenever(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(mockSensor)

        logMessages.clear()
        eventFlow = SensorManagerBackedAccelerometer(
            sensorManager = mockSensorManager,
            logger = { message -> logMessages.add(message) },
        )
    }

    @Test
    fun `onResume registers sensor listener and logs`() {
        val lifecycleOwner = FakeLifecycleOwner()

        eventFlow.onResume(lifecycleOwner)

        org.mockito.Mockito.verify(mockSensorManager).registerListener(
            eventFlow,
            mockSensor,
            SensorManager.SENSOR_DELAY_UI,
        )
        assertEquals(listOf("Registering self as sensor listener"), logMessages)
    }

    @Test
    fun `onPause unregisters sensor listener and logs`() {
        val lifecycleOwner = FakeLifecycleOwner()

        eventFlow.onResume(lifecycleOwner)
        eventFlow.onPause(lifecycleOwner)

        org.mockito.Mockito.verify(mockSensorManager).unregisterListener(eventFlow)
        assertEquals(
            listOf("Registering self as sensor listener", "Unregistering self as sensor listener"),
            logMessages,
        )
    }
}

class FakeLifecycleOwner : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}
