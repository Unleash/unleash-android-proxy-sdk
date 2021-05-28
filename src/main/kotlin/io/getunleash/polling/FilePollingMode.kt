package io.getunleash.polling

import java.io.File

class FilePollingMode(val fileToLoadFrom: File) : PollingMode {
    override fun pollingIdentifier(): String = "file"
}