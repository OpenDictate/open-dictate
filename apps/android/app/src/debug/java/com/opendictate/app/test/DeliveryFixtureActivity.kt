package com.opendictate.app.test

import android.app.Activity
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.view.WindowManager
import android.widget.EditText

/** Native editable target for Accessibility instrumentation; absent from releases. */
class DeliveryFixtureActivity : Activity() {
    lateinit var editor: EditText
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        editor = EditText(this).apply {
            setText("before after")
            setSelection(7)
        }
        setContentView(editor)
        editor.requestFocus()
        editor.postDelayed({
            getSystemService(InputMethodManager::class.java).showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
        }, 300)
    }
}
