package com.opendictate.app.data

import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class WordReplacementTest {
    @Test fun sharedPlatformCases() {
        val root = generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
            .first { File(it, "shared/word-replacement-cases.json").isFile }
        val cases = JSONArray(File(root, "shared/word-replacement-cases.json").readText())
        for (index in 0 until cases.length()) {
            val fixture = cases.getJSONObject(index)
            val entries = fixture.getJSONArray("rules")
            val rules = (0 until entries.length()).map {
                val rule = entries.getJSONObject(it)
                WordReplacement(source = rule.getString("source"), replacement = rule.getString("replacement"), enabled = rule.optBoolean("enabled", true))
            }
            assertEquals(fixture.getString("name"), fixture.getString("expected"),
                WordReplacementEngine(rules).apply(fixture.getString("text"), fixture.optBoolean("final", true)))
        }
    }
    @Test fun sessionSnapshotAndCumulativeUpdates() {
        val rule = WordReplacement(source = "cat", replacement = "dog")
        val session = WordReplacementEngine(listOf(rule))
        assertEquals("dog ", session.apply("cat ", final = false))
        assertEquals("dog dog.", session.apply("cat cat.", final = false))
        assertEquals("bird", WordReplacementEngine(listOf(rule.copy(replacement = "bird"))).apply("cat"))
    }
    @Test fun documentRoundTripAndValidation() {
        val rule = WordReplacement(source = "тест", replacement = "Test")
        val document = ReplacementDocument(listOf(rule))
        assertEquals(document, ReplacementDocument.fromJson(document.toJson()))
        assertFalse(ReplacementDocument(listOf(rule, rule)).isValid)
        assertFalse(WordReplacement(source = " ", replacement = "Test").isValid)
        assertFalse(WordReplacement(source = "test", replacement = "").isValid)
        assertFalse(WordReplacement(source = "x".repeat(257), replacement = "Test").isValid)
        assertFalse(document.copy(schemaVersion = 2).isValid)
    }
    @Test fun malformedWireTypesAndFutureVersionsAreRejected() {
        val document = ReplacementDocument(listOf(WordReplacement(source = "cat", replacement = "dog"))).toJson()
        listOf(document.replace("\"schemaVersion\":1", "\"schemaVersion\":1.5"),
            document.replace("\"enabled\":true", "\"enabled\":\"true\""),
            document.replace("\"source\":\"cat\"", "\"source\":123")).forEach {
            assertThrows(Exception::class.java) { ReplacementDocument.fromJson(it) }
        }
    }
}
