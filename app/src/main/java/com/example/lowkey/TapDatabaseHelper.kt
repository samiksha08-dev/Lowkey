
package com.example.lowkey

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class TapDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "lowkey_taps.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE tap_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp INTEGER NOT NULL,
                x_position REAL,
                y_position REAL,
                duration_ms INTEGER,
                event_type TEXT NOT NULL DEFAULT 'TAP'
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        if (oldVersion < 2) {
            db.execSQL(
                "ALTER TABLE tap_events ADD COLUMN event_type TEXT NOT NULL DEFAULT 'TAP'"
            )
        }
    }

    fun insertTap(event: TapEvent): Long {
        val values = ContentValues().apply {
            put("timestamp", event.timestamp)
            put("x_position", event.xPosition)
            put("y_position", event.yPosition)
            put("duration_ms", event.durationMs)
            put("event_type", event.eventType)
        }

        val result = writableDatabase.insert(
            "tap_events",
            null,
            values
        )

        Log.d(
            "LowkeyTap",
            "Database insert result: $result, eventType: ${event.eventType}"
        )

        return result
    }

    fun getAllTaps(): List<TapEvent> {
        val taps = mutableListOf<TapEvent>()

        val cursor = readableDatabase.rawQuery(
            """
            SELECT timestamp, x_position, y_position,
                   duration_ms, event_type
            FROM tap_events
            ORDER BY id DESC
            """.trimIndent(),
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                taps.add(
                    TapEvent(
                        timestamp = it.getLong(0),
                        xPosition = if (it.isNull(1)) null else it.getFloat(1),
                        yPosition = if (it.isNull(2)) null else it.getFloat(2),
                        durationMs = if (it.isNull(3)) null else it.getLong(3),
                        eventType = it.getString(4)
                    )
                )
            }
        }

        return taps
    }

    fun getTapCount(): Int {
        val cursor = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM tap_events WHERE event_type = 'TAP'",
            null
        )

        cursor.use {
            return if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun getScrollCount(): Int {
        val cursor = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM tap_events WHERE event_type = 'SCROLL'",
            null
        )

        cursor.use {
            return if (it.moveToFirst()) it.getInt(0) else 0
        }
    }
}
