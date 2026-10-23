package com.example.lowkey.tap

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class TapDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "lowkey_taps.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE tap_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp INTEGER NOT NULL,
                x_position REAL,
                y_position REAL,
                duration_ms INTEGER,
                event_type TEXT NOT NULL DEFAULT 'TAP',
                scroll_x INTEGER,
                scroll_y INTEGER,
                scroll_delta_x INTEGER,
                scroll_delta_y INTEGER,
                max_scroll_x INTEGER,
                max_scroll_y INTEGER
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

        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE tap_events ADD COLUMN scroll_x INTEGER")
            db.execSQL("ALTER TABLE tap_events ADD COLUMN scroll_y INTEGER")
            db.execSQL("ALTER TABLE tap_events ADD COLUMN scroll_delta_x INTEGER")
            db.execSQL("ALTER TABLE tap_events ADD COLUMN scroll_delta_y INTEGER")
            db.execSQL("ALTER TABLE tap_events ADD COLUMN max_scroll_x INTEGER")
            db.execSQL("ALTER TABLE tap_events ADD COLUMN max_scroll_y INTEGER")
        }
    }

    fun insertTap(event: TapEvent): Long {
        val values = ContentValues().apply {
            put("timestamp", event.timestamp)
            put("x_position", event.xPosition)
            put("y_position", event.yPosition)
            put("duration_ms", event.durationMs)
            put("event_type", event.eventType)

            put("scroll_x", event.scrollX)
            put("scroll_y", event.scrollY)
            put("scroll_delta_x", event.scrollDeltaX)
            put("scroll_delta_y", event.scrollDeltaY)
            put("max_scroll_x", event.maxScrollX)
            put("max_scroll_y", event.maxScrollY)
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
                   duration_ms, event_type,
                   scroll_x, scroll_y,
                   scroll_delta_x, scroll_delta_y,
                   max_scroll_x, max_scroll_y
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
                        eventType = it.getString(4),
                        scrollX = if (it.isNull(5)) null else it.getInt(5),
                        scrollY = if (it.isNull(6)) null else it.getInt(6),
                        scrollDeltaX = if (it.isNull(7)) null else it.getInt(7),
                        scrollDeltaY = if (it.isNull(8)) null else it.getInt(8),
                        maxScrollX = if (it.isNull(9)) null else it.getInt(9),
                        maxScrollY = if (it.isNull(10)) null else it.getInt(10)
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