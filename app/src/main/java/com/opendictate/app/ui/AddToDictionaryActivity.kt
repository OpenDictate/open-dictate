package com.opendictate.app.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.opendictate.app.R
import com.opendictate.app.data.DictionaryAddition
import com.opendictate.app.data.SettingsStore
import com.opendictate.app.data.addDictionaryTerm

class AddToDictionaryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val message = if (intent?.action == Intent.ACTION_PROCESS_TEXT && intent.type == "text/plain") {
            val settings = SettingsStore(this)
            when (val result = addDictionaryTerm(
                settings.prompt,
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString().orEmpty(),
            )) {
                is DictionaryAddition.Added -> {
                    settings.prompt = result.terms
                    R.string.dictionary_added
                }
                DictionaryAddition.Empty -> R.string.dictionary_no_selection
                DictionaryAddition.Duplicate -> R.string.dictionary_already_added
                DictionaryAddition.Full -> R.string.dictionary_full
            }
        } else {
            R.string.dictionary_no_selection
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }
}
