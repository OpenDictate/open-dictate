package com.opendictate.app

import android.app.Application
import com.opendictate.app.audio.LastDictationAudioStore
import com.opendictate.app.data.TranscriptHistoryStore

class OpenDictateApplication : Application() {
    val replacementStore by lazy { com.opendictate.app.data.ReplacementStore(this) }
    val driveSync by lazy { com.opendictate.app.data.GoogleDriveSync(this) }

    override fun onCreate() {
        super.onCreate()
        driveSync
    }
    val transcriptHistoryStore by lazy { TranscriptHistoryStore(this) }
    val lastDictationAudioStore by lazy { LastDictationAudioStore(filesDir) }
}
