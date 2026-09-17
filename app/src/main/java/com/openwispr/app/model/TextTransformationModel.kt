package com.openwispr.app.model

enum class TextTransformationModel(val apiName: String) {
    LUNA("gpt-5.6-luna"),
    TERRA("gpt-5.6-terra"),
    SOL("gpt-5.6-sol");

    companion object {
        fun fromStored(value: String?): TextTransformationModel =
            entries.firstOrNull { it.name == value } ?: LUNA
    }
}
