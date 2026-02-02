/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.shake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import mozilla.components.concept.accelerometer.Accelerometer
import kotlin.math.sqrt

/**
 * Run accelerometer data through a shake detection function. Uses a rolling window to
 */
fun Accelerometer.detectShakes(sensitivity: ShakeSensitivity = ShakeSensitivity.Medium): Flow<Unit> =
    gravityNormalizedSamples()
        .map { sample ->
            // magnitude of acceleration vector
            val magnitude = sqrt(
                sample.xAccel * sample.xAccel +
                    sample.yAccel * sample.yAccel +
                    sample.zAccel * sample.zAccel,
            )
            magnitude to sample.timestampNs
        }
        .scan(ShakeState()) { state, (magnitude, timestampNs) ->
            state.next(magnitude, timestampNs, sensitivity)
        }
        .filter { it.shouldEmit }
        .map { Unit }

/**
 * Configuration element for the sensitivity of shake detection.
 */
enum class ShakeSensitivity {
    Low,
    Medium,
    High,
}

private data class ShakeState(
    val hits: Int = 0,
    val windowStartNs: Long = 0L,
    val lastShakeNs: Long = 0L,
    val shouldEmit: Boolean = false,
) {
    fun next(
        magnitude: Float,
        timestampNs: Long,
        sensitivity: ShakeSensitivity,
        windowNs: Long = 350_000_000L,
        cooldownNs: Long = 800_000_000L,
        minHits: Int = 2,
    ): ShakeState {
        val threshold = when (sensitivity) {
            ShakeSensitivity.Low -> 3.2
            ShakeSensitivity.Medium -> 2.7
            ShakeSensitivity.High -> 2.3
        }
        val aboveThreshold = magnitude >= threshold
        if (!aboveThreshold) return copy(shouldEmit = false)

        val (newWindowStart, newHits) =
            if (windowStartNs == 0L || timestampNs - windowStartNs > windowNs) {
                timestampNs to 1
            } else {
                windowStartNs to hits + 1
            }

        val inCooldown =
            lastShakeNs != 0L && timestampNs - lastShakeNs < cooldownNs

        return if (!inCooldown && newHits >= minHits) {
            ShakeState(
                hits = 0,
                windowStartNs = 0L,
                lastShakeNs = timestampNs,
                shouldEmit = true,
            )
        } else {
            ShakeState(
                hits = newHits,
                windowStartNs = newWindowStart,
                lastShakeNs = lastShakeNs,
                shouldEmit = false,
            )
        }
    }
}
