package com.example.lowkey.accelerometer

data class BehaviourRecord(
    val timestamp: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val accuracy: Int
)