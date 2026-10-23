package com.example.lowkey.tap

import android.content.Context
import java.io.File

class TapCsvLogger(context: Context) {

    private val appContext = context.applicationContext

    @Synchronized
    fun appendTap(event: TapEvent) {

        val fileName = "tap_data.csv"
        val file = File(appContext.filesDir, fileName)
        val writeHeader = !file.exists() || file.length() == 0L

        appContext.openFileOutput(
            fileName,
            Context.MODE_APPEND
        ).bufferedWriter().use { writer ->

            if (writeHeader) {
                writer.appendLine(
                    "timestamp,xPosition,yPosition,durationMs,eventType"
                )
            }

            writer.appendLine(
                "${event.timestamp}," +
                        "${event.xPosition ?: ""}," +
                        "${event.yPosition ?: ""}," +
                        "${event.durationMs ?: ""}," +
                        event.eventType
            )
        }
    }
}