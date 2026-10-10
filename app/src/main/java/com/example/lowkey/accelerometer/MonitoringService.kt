package com.example.lowkey.accelerometer
import com.example.lowkey.security.SecurityManager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import com.example.lowkey.security.ProtectionOverlayService
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

        startMonitoringService()

        val preferences = getSharedPreferences(
            "LowkeyUser",
            MODE_PRIVATE
        )

        preferences.edit()
            .putBoolean("isMonitoring", true)
            .apply()

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


    private fun triggerProtectionForTest() {
        if (!Settings.canDrawOverlays(this)) {
            Log.e("LOWKEY_SECURITY", "Overlay permission missing")
            return
        }

        val intent = Intent(this, ProtectionOverlayService::class.java)
        startService(intent)

        Log.d("LOWKEY_SECURITY", "Suspicious behaviour detected. Protection triggered.")
    }


    private fun showGrantAccess() {
        if (!Settings.canDrawOverlays(this)) {
            Log.e("LOWKEY_SECURITY", "Overlay permission missing")
            return
        }

        val intent = Intent(this, ProtectionOverlayService::class.java)
        intent.action = "SHOW_GRANT_ACCESS"
        startService(intent)

        Log.d("LOWKEY_SECURITY", "Grant Access requested")
    }


    private fun handleSecurityState(confidence: Float) {
        val securityManager = com.example.lowkey.security.SecurityManager()
        val state = securityManager.evaluateConfidence(confidence)

        when (state) {
            com.example.lowkey.security.SecurityManager.SecurityState.SUSPICIOUS -> {
                triggerProtectionForTest()
            }

            com.example.lowkey.security.SecurityManager.SecurityState.GENUINE -> {
                showGrantAccess()
            }

            com.example.lowkey.security.SecurityManager.SecurityState.NEEDS_VERIFICATION -> {
                Log.d("LOWKEY_SECURITY", "Verification pending")
            }
        }
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