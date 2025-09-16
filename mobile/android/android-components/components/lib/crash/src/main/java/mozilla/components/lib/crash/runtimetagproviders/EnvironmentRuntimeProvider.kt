/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.crash.runtimetagproviders

import mozilla.components.lib.crash.RuntimeTag
import mozilla.components.lib.crash.RuntimeTagProvider
import java.util.Locale
import java.util.concurrent.TimeUnit

private val startTime = System.currentTimeMillis()

/**
 * Includes information about environment values with the crash so that it can be persisted.
 */
class EnvironmentRuntimeProvider : RuntimeTagProvider {
    override fun invoke(): Map<String, String> {
        return mapOf(
            RuntimeTag.LOCALE to Locale.getDefault().toString(),
            RuntimeTag.START_TIME to TimeUnit.MILLISECONDS.toSeconds(startTime).toString(),
        )
    }
}
