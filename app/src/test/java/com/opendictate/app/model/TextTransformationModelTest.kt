package com.opendictate.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TextTransformationModelTest {
    @Test
    fun `defaults to Luna when no transformation model is stored`() {
        assertEquals(TextTransformationModel.LUNA, TextTransformationModel.fromStored(null))
    }

    @Test
    fun `defaults to Luna for an unknown stored model`() {
        assertEquals(TextTransformationModel.LUNA, TextTransformationModel.fromStored("UNKNOWN"))
    }

    @Test
    fun `replaces retired Terra selection with Luna`() {
        assertEquals(TextTransformationModel.LUNA, TextTransformationModel.fromStored("TERRA"))
    }

    @Test
    fun `restores every selectable transformation model`() {
        TextTransformationModel.entries.forEach { model ->
            assertEquals(model, TextTransformationModel.fromStored(model.name))
        }
    }
}
