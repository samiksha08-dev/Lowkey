package com.example.lowkey.accelerometer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

class MonitoringService : Service() {

    private lateinit var accelerometerCollector: AccelerometerCollector

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        accelerometerCollector = AccelerometerCollector(this) { record ->
            saveRecord(record)
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val preferences = getSharedPreferences(
            "LowkeyUser",
            MODE_PRIVATE
        )

        preferences.edit()
            .putBoolean("isMonitoring", true)
            .apply()

        startMonitoringService()

        accelerometerCollector.start()

        return START_STICKY
    }

    private fun startMonitoringService() {

        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )

        } else {

            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }
    }

    override fun onDestroy() {

        accelerometerCollector.stop()

        val preferences = getSharedPreferences(
            "LowkeyUser",
            MODE_PRIVATE
        )

        preferences.edit()
            .putBoolean("isMonitoring", false)
            .apply()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun saveRecord(record: BehaviourRecord) {

        val fileName = "accelerometer_data.csv"

        val fileExists = fileList().contains(fileName)

        openFileOutput(
            fileName,
            MODE_APPEND
        ).use { outputStream ->

            if (!fileExists) {
                outputStream.write(
                    "timestamp,x,y,z,accuracy\n".toByteArray()
                )
            }

            val data =
                "${record.timestamp},${record.x},${record.y},${record.z},${record.accuracy}\n"

            outputStream.write(data.toByteArray())
        }
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "LOWKEY Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )

            val notificationManager =
                getSystemService(NotificationManager::class.java)

            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("LOWKEY Monitoring")
                .setContentText("Accelerometer monitoring is active")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build()

        } else {

            Notification.Builder(this)
                .setContentTitle("LOWKEY Monitoring")
                .setContentText("Accelerometer monitoring is active")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build()
        }
    }

    companion object {

        private const val CHANNEL_ID = "LOWKEY_MONITORING"
        private const val NOTIFICATION_ID = 1001
    }
}