package com.example.lowkey.accelerometer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class AccelerometerCollector(
    context: Context,
    private val onDataReceived: (BehaviourRecord) -> Unit
) : SensorEventListener {

    private var receivedCount = 0

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    fun start() {
        if (accelerometer == null) {
            Log.e("LOWKEY_ACCEL", "No accelerometer sensor found")
            return
        }

        val registered = sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        Log.i(
            "LOWKEY_ACCEL",
            "Listener registered=$registered, sensor=${accelerometer.name}"
        )
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        Log.i("LOWKEY_ACCEL", "Sensor listener stopped")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        receivedCount++

        if (receivedCount == 1 || receivedCount % 100 == 0) {
            Log.d(
                "LOWKEY_ACCEL",
                "Sensor events received=$receivedCount"
            )
        }

        val record = BehaviourRecord(
            timestamp = System.currentTimeMillis(),
            x = event.values[0],
            y = event.values[1],
            z = event.values[2],
            accuracy = event.accuracy
        )

        onDataReceived(record)
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
        // Nothing needed here for now.
    }
}