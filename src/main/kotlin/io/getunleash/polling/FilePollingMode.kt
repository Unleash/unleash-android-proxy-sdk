package io.getunleash.polling

import io.getunleash.polling.PollingMode
import java.io.File

/**
 * Configuration for FilePollingPolicy. Sets up where the policy loads the toggles from
 * @param fileToLoadFrom
 * @param readyListener - Will broadcast a ready event (Once the File is loaded and the toggle cache is populated)
 * @since 0.2
 */
class FilePollingMode(val fileToLoadFrom: File, val readyListener: ReadyListener? = null) : PollingMode {
    override fun pollingIdentifier(): String = "file"
}