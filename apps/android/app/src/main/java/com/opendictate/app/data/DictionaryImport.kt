package com.opendictate.app.data

import com.opendictate.app.R

internal fun SettingsStore.addToDictionary(text: CharSequence?): Int =
    when (val result = addDictionaryTerm(prompt, text?.toString().orEmpty())) {
        is DictionaryAddition.Added -> {
            prompt = result.terms
            R.string.dictionary_added
        }
        DictionaryAddition.Empty -> R.string.dictionary_no_selection
        DictionaryAddition.Duplicate -> R.string.dictionary_already_added
        DictionaryAddition.Full -> R.string.dictionary_full
    }
