/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.takeout.importer

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.webextension.MessageHandler
import mozilla.components.concept.engine.webextension.Port
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.webextensions.BuiltInWebExtensionController
import org.json.JSONObject
import java.io.File

/**
 * Feature that installs the Takeout WebExtension, drives the bookmark export
 * flow on `takeout.google.com`, and surfaces extracted file paths from the
 * resulting ZIP download.
 *
 * Designed to be a building block for a higher-level importer (e.g. bookmarks
 * or passwords) — this feature only produces files on disk and does not write
 * to any storage backend itself.
 *
 * @param context Used to locate the cache directory where extracted files are
 * written.
 * @param engine The application's [Engine]; the WebExtension is installed here.
 * @param store The application's [BrowserStore]. Observed for download
 * completion events from `takeout.google.com`.
 * @param onStep Called for each [TakeoutStep] surfaced by the extension or by
 * the native pipeline.
 * @param onCompleted Called once with the terminal result of a single import
 * attempt (success with extracted files, or failure).
 * @param mainDispatcher Dispatcher used to observe [store].
 * @param ioDispatcher Dispatcher used for blocking ZIP extraction.
 * @param extractor Pluggable ZIP extractor, exposed for testing.
 */
class TakeoutImporterFeature(
    private val context: Context,
    private val engine: Engine,
    private val store: BrowserStore,
    private val onStep: (TakeoutStep) -> Unit = {},
    private val onCompleted: (Result) -> Unit = {},
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val extractor: TakeoutZipExtractor = TakeoutZipExtractor(),
) : LifecycleAwareFeature {

    private val logger = Logger("TakeoutImporterFeature")
    private var scope: CoroutineScope? = null
    private val processedDownloadIds = mutableSetOf<String>()

    @VisibleForTesting
    internal var extensionController = BuiltInWebExtensionController(
        TAKEOUT_EXTENSION_ID,
        TAKEOUT_EXTENSION_URL,
        TAKEOUT_PORT_NAME,
    )

    /** Terminal result of a single Takeout import attempt. */
    sealed class Result {
        /** Extraction completed; [extractedFiles] holds the files written to cache. */
        data class Success(val extractedFiles: List<File>) : Result()

        /** Extraction failed. [reason] is a short identifier for the failure cause. */
        data class Failure(val reason: String) : Result()
    }

    override fun start() {
        ensureExtensionInstalled()
        scope = store.flowScoped(dispatcher = mainDispatcher) { flow ->
            flow.collect { state ->
                state.downloads.values
                    .firstOrNull { it.isFromTakeout() && it.isCompleted() && it.id !in processedDownloadIds }
                    ?.let { download ->
                        processedDownloadIds += download.id
                        onStep(TakeoutStep.DownloadDetected)
                        handleDownload(download)
                    }
            }
        }
    }

    override fun stop() {
        scope?.cancel()
        scope = null
    }

    private fun ensureExtensionInstalled() {
        extensionController.registerBackgroundMessageHandler(TakeoutPortHandler())
        extensionController.install(
            engine,
            onError = { throwable ->
                logger.error("Failed to install Takeout extension", throwable)
                onCompleted(Result.Failure("extension-install-failed"))
            },
        )
    }

    private fun handleDownload(download: DownloadState) {
        val downloadScope = scope ?: return
        downloadScope.launch(ioDispatcher) {
            val sourceFile = File(download.filePath)
            if (!sourceFile.exists()) {
                onCompleted(Result.Failure("download-file-missing"))
                return@launch
            }
            runCatching {
                val destination = File(context.cacheDir, "takeout-import")
                sourceFile.inputStream().use { input -> extractor.extract(input, destination) }
            }.onSuccess { files ->
                val absolutePaths = files.map { it.absolutePath }
                onStep(TakeoutStep.Extracted(absolutePaths))
                onCompleted(Result.Success(files))
            }.onFailure { throwable ->
                logger.error("Failed to extract Takeout ZIP", throwable)
                onCompleted(Result.Failure("zip-extract-failed"))
            }
        }
    }

    private fun DownloadState.isFromTakeout(): Boolean =
        runCatching { url.toUri().host?.equals(TAKEOUT_HOST, ignoreCase = true) == true }.getOrDefault(false)

    private fun DownloadState.isCompleted(): Boolean = status == DownloadState.Status.COMPLETED

    private inner class TakeoutPortHandler : MessageHandler {
        override fun onPortMessage(message: Any, port: Port) {
            if (message !is JSONObject) {
                return
            }
            val step = TakeoutMessageParser.parse(message) ?: return
            onStep(step)
            if (step is TakeoutStep.Failure) {
                onCompleted(Result.Failure(step.actionId ?: "unknown-step"))
            }
        }
    }

    companion object {
        internal const val TAKEOUT_EXTENSION_ID = "takeout-importer@mozac.org"
        internal const val TAKEOUT_EXTENSION_URL = "resource://android/assets/extensions/takeout/"
        internal const val TAKEOUT_PORT_NAME = "mozacTakeoutImporter"
        internal const val TAKEOUT_HOST = "takeout.google.com"
    }
}
