package com.example.lowkey.tap

import android.content.Context

class TapRepository(context: Context) {

    private val dbHelper = TapDatabaseHelper(context)

    fun saveTap(event: TapEvent): Long {
        return dbHelper.insertTap(event)
    }

    fun getTapCount(): Int {
        return dbHelper.getTapCount()
    }

    fun getScrollCount(): Int {
        return dbHelper.getScrollCount()
    }

    fun close() {
        dbHelper.close()
    }
}