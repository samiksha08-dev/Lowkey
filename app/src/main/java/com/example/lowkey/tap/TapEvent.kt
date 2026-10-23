package com.example.lowkey.tap

data class TapEvent(
    val timestamp: Long,
    val xPosition: Float?,
    val yPosition: Float?,
    val durationMs: Long?,
    val eventType: String = "TAP",

    // Additional scroll behaviour features
    val scrollX: Int? = null,
    val scrollY: Int? = null,
    val scrollDeltaX: Int? = null,
    val scrollDeltaY: Int? = null,
    val maxScrollX: Int? = null,
    val maxScrollY: Int? = null
)