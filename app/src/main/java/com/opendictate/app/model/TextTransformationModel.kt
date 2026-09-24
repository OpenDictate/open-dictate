package com.opendictate.app.model

enum class TextTransformationModel(val apiName: String) {
    LUNA("gpt-6-luna"),
    SOL("gpt-6-sol");

    companion object {
        fun fromStored(value: String?): TextTransformationModel =
            entries.firstOrNull { it.name == value } ?: LUNA
    }
}
