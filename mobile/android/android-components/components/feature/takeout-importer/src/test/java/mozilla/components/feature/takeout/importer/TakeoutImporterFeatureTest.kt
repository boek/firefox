/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.takeout.importer

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.action.DownloadAction
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.webextension.Port
import mozilla.components.feature.takeout.importer.TakeoutImporterFeature.Companion.TAKEOUT_EXTENSION_ID
import mozilla.components.feature.takeout.importer.TakeoutImporterFeature.Companion.TAKEOUT_EXTENSION_URL
import mozilla.components.support.test.any
import mozilla.components.support.test.eq
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.webextensions.BuiltInWebExtensionController
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class TakeoutImporterFeatureTest {

    @After
    fun tearDown() {
        BuiltInWebExtensionController.installedBuiltInExtensions.clear()
    }

    @Test
    fun `start installs the takeout extension`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val engine: Engine = mock()
        val feature = TakeoutImporterFeature(
            context = testContext,
            engine = engine,
            store = BrowserStore(),
            mainDispatcher = dispatcher,
            ioDispatcher = dispatcher,
        )

        feature.start()

        verify(engine, times(1)).installBuiltInWebExtension(
            eq(TAKEOUT_EXTENSION_ID),
            eq(TAKEOUT_EXTENSION_URL),
            any(),
            any(),
        )
    }

    @Test
    fun `completed takeout download triggers extraction and reports files`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val zipFile = writeTempZip("Bookmarks.html" to "<html></html>")
        val store = BrowserStore()
        val steps = mutableListOf<TakeoutStep>()
        val results = mutableListOf<TakeoutImporterFeature.Result>()

        val feature = TakeoutImporterFeature(
            context = testContext,
            engine = mock(),
            store = store,
            onStep = { steps += it },
            onCompleted = { results += it },
            mainDispatcher = dispatcher,
            ioDispatcher = dispatcher,
        )
        feature.start()

        store.dispatch(
            DownloadAction.AddDownloadAction(
                DownloadState(
                    id = "dl-1",
                    url = "https://takeout.google.com/takeout/download?id=abc",
                    fileName = zipFile.name,
                    directoryPath = checkNotNull(zipFile.parentFile).absolutePath,
                    status = DownloadState.Status.COMPLETED,
                ),
            ),
        )
        testScheduler.advanceUntilIdle()

        assertTrue("expected DownloadDetected step", steps.contains(TakeoutStep.DownloadDetected))
        val extracted = steps.filterIsInstance<TakeoutStep.Extracted>().single()
        assertEquals(1, extracted.files.size)
        val success = results.single() as TakeoutImporterFeature.Result.Success
        assertEquals("Bookmarks.html", success.extractedFiles.single().name)
    }

    @Test
    fun `non-takeout downloads are ignored`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val store = BrowserStore()
        val results = mutableListOf<TakeoutImporterFeature.Result>()

        TakeoutImporterFeature(
            context = testContext,
            engine = mock(),
            store = store,
            onCompleted = { results += it },
            mainDispatcher = dispatcher,
            ioDispatcher = dispatcher,
        ).start()

        store.dispatch(
            DownloadAction.AddDownloadAction(
                DownloadState(
                    id = "dl-2",
                    url = "https://example.com/file.zip",
                    fileName = "file.zip",
                    status = DownloadState.Status.COMPLETED,
                ),
            ),
        )
        testScheduler.advanceUntilIdle()

        assertTrue(results.isEmpty())
    }

    @Test
    fun `port message routes parsed success step through onStep`() {
        val steps = mutableListOf<TakeoutStep>()
        val feature = TakeoutImporterFeature(
            context = testContext,
            engine = mock(),
            store = BrowserStore(),
            onStep = { steps += it },
            mainDispatcher = StandardTestDispatcher(),
            ioDispatcher = StandardTestDispatcher(),
        )

        val handler = feature.javaClass.declaredClasses
            .single { it.simpleName == "TakeoutPortHandler" }
            .declaredConstructors
            .single()
            .apply { isAccessible = true }
            .newInstance(feature) as mozilla.components.concept.engine.webextension.MessageHandler

        val message = JSONObject(
            """{"name":"takeoutStep","data":{"result":{"success":{"actionID":"create-export"}}}}""",
        )
        handler.onPortMessage(message, mock<Port>())

        assertEquals(listOf(TakeoutStep.Success("create-export")), steps)
    }

    private fun writeTempZip(vararg entries: Pair<String, String>): File {
        val target = File.createTempFile("takeout", ".zip", testContext.cacheDir)
        ByteArrayOutputStream().use { buffer ->
            ZipOutputStream(buffer).use { zip ->
                entries.forEach { (name, contents) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(contents.toByteArray())
                    zip.closeEntry()
                }
            }
            target.writeBytes(buffer.toByteArray())
        }
        return target
    }
}
