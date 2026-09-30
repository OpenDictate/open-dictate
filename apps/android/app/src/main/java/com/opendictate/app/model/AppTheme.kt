package com.opendictate.app.model

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK;

    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemIsDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromStored(value: String?): AppTheme = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
