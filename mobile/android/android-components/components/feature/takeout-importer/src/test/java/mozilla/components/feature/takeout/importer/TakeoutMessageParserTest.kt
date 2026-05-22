/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.takeout.importer

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TakeoutMessageParserTest {

    @Test
    fun `parse returns Success for success envelope`() {
        val message = JSONObject(
            """{"name":"takeoutStep","data":{"result":{"success":{"actionID":"takeout-loaded"}}}}""",
        )
        val step = TakeoutMessageParser.parse(message)
        assertEquals(TakeoutStep.Success("takeout-loaded"), step)
    }

    @Test
    fun `parse returns Failure with detail for error envelope`() {
        val message = JSONObject(
            """{"name":"takeoutStep","data":{"result":{"error":{"actionID":"next-step","detail":"timeout"}}}}""",
        )
        val step = TakeoutMessageParser.parse(message)
        assertEquals(TakeoutStep.Failure("next-step", "timeout"), step)
    }

    @Test
    fun `parse returns Failure without actionId when error has no id`() {
        val message = JSONObject("""{"data":{"result":{"error":{}}}}""")
        assertEquals(TakeoutStep.Failure(null, null), TakeoutMessageParser.parse(message))
    }

    @Test
    fun `parse returns null for unknown envelope`() {
        assertNull(TakeoutMessageParser.parse(JSONObject("""{"hello":"world"}""")))
        assertNull(TakeoutMessageParser.parse(JSONObject("""{"data":{"result":{}}}""")))
    }

    @Test
    fun `parse returns null when success has no actionId`() {
        val message = JSONObject("""{"data":{"result":{"success":{}}}}""")
        assertNull(TakeoutMessageParser.parse(message))
    }
}
