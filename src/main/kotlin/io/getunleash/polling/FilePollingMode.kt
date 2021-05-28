package io.getunleash.polling

import java.io.File

/**
 * Configuration for FilePollingPolicy. Sets up where the policy loads the toggles from
 * @param fileToLoadFrom
 * @since 0.2
 */
class FilePollingMode(val fileToLoadFrom: File) : PollingMode {
    override fun pollingIdentifier(): String = "file"
}