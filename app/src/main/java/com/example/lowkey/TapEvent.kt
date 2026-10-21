package com.example.lowkey

data class TapEvent(
    val timestamp: Long,
    val xPosition: Float?,
    val yPosition: Float?,
    val durationMs: Long?,
    val eventType: String = "TAP"
)
