package com.opendictate.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class TranscriptHistoryItem(
    val id: Long,
    val text: String,
    val createdAtEpochMillis: Long,
)

class TranscriptHistoryStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {
    init {
        setWriteAheadLoggingEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_HISTORY (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TEXT TEXT NOT NULL,
                $COLUMN_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX history_created_at ON $TABLE_HISTORY($COLUMN_CREATED_AT DESC, $COLUMN_ID DESC)",
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun add(text: String, createdAtEpochMillis: Long = System.currentTimeMillis()): Long {
        val normalized = text.trim()
        require(normalized.isNotEmpty()) { "Transcript must not be blank" }
        return writableDatabase.insertOrThrow(
            TABLE_HISTORY,
            null,
            ContentValues().apply {
                put(COLUMN_TEXT, normalized)
                put(COLUMN_CREATED_AT, createdAtEpochMillis)
            },
        )
    }

    fun getAll(): List<TranscriptHistoryItem> = readableDatabase.query(
        TABLE_HISTORY,
        arrayOf(COLUMN_ID, COLUMN_TEXT, COLUMN_CREATED_AT),
        null,
        null,
        null,
        null,
        "$COLUMN_CREATED_AT DESC, $COLUMN_ID DESC",
    ).use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID)
        val textIndex = cursor.getColumnIndexOrThrow(COLUMN_TEXT)
        val createdAtIndex = cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)
        buildList {
            while (cursor.moveToNext()) {
                add(
                    TranscriptHistoryItem(
                        id = cursor.getLong(idIndex),
                        text = cursor.getString(textIndex),
                        createdAtEpochMillis = cursor.getLong(createdAtIndex),
                    ),
                )
            }
        }
    }

    fun delete(id: Long): Boolean = writableDatabase.delete(
        TABLE_HISTORY,
        "$COLUMN_ID = ?",
        arrayOf(id.toString()),
    ) > 0

    private companion object {
        const val DATABASE_NAME = "transcript-history.db"
        const val DATABASE_VERSION = 1
        const val TABLE_HISTORY = "transcript_history"
        const val COLUMN_ID = "id"
        const val COLUMN_TEXT = "text"
        const val COLUMN_CREATED_AT = "created_at"
    }
}
