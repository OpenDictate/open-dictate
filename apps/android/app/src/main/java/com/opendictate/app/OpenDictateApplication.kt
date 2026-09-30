package com.opendictate.app

import android.app.Application
import com.opendictate.app.audio.LastDictationAudioStore
import com.opendictate.app.data.TranscriptHistoryStore

class OpenDictateApplication : Application() {
    val transcriptHistoryStore by lazy { TranscriptHistoryStore(this) }
    val lastDictationAudioStore by lazy { LastDictationAudioStore(filesDir) }
}
