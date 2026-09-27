package com.opendictate.app.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.opendictate.app.R
import com.opendictate.app.data.SettingsStore
import com.opendictate.app.data.addToDictionary

class ShareToDictionaryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val message = if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            SettingsStore(this).addToDictionary(intent.getCharSequenceExtra(Intent.EXTRA_TEXT))
        } else {
            R.string.dictionary_no_selection
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }
}
