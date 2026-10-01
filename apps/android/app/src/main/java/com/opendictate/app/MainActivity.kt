package com.opendictate.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.opendictate.app.ui.OpenDictateApp

class MainActivity : AppCompatActivity() {
    override fun onResume() {
        super.onResume()
        (application as OpenDictateApplication).driveSync.requestSync()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { OpenDictateApp() }
    }
}
