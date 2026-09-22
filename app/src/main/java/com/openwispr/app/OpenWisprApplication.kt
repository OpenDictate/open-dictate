package com.openwispr.app

import android.app.Application
import com.openwispr.app.data.TranscriptHistoryStore

class OpenWisprApplication : Application() {
    val transcriptHistoryStore by lazy { TranscriptHistoryStore(this) }
}
