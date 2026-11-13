
package com.example.lowkey.accelerometer

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

import com.example.lowkey.ml.BehaviourWindowProcessor
import com.example.lowkey.security.ProtectionOverlayService
import com.example.lowkey.security.SecurityManager

class MonitoringService : Service() {

    private lateinit var accelerometerCollector: AccelerometerCollector
    private lateinit var behaviourWindowProcessor: BehaviourWindowProcessor

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        // Initialize the ML processing pipeline.
        behaviourWindowProcessor =
            BehaviourWindowProcessor(applicationContext) {
                    windowStart,
                    windowEnd,
                    features,
                    result ->

                Log.i(
                    "LOWKEY_SECURITY",
                    """
                    Model B inference completed
                    Window: $windowStart - $windowEnd
                    TAP count: ${features.tapCount}
                    SCROLL count: ${features.scrollCount}
                    Accelerometer samples: ${features.accelerometerSampleCount}
                    Anomaly score: ${result.anomalyScore}
                    Threshold: ${result.anomalyThreshold}
                    Anomalous: ${result.isAnomalous}
                    """.trimIndent()
                )
            }

        // Initialize accelerometer collection.
        accelerometerCollector = AccelerometerCollector(this) { record ->

            // Preserve existing CSV data collection.
            saveRecord(record)

            // Also send the record to the ML processor.
            behaviourWindowProcessor.onAccelerometerRecord(record)
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        startMonitoringService()

        if (intent?.action == "TEST_PROTECTION") {
            triggerProtectionForTest()
            return START_NOT_STICKY
        }

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

        if (::accelerometerCollector.isInitialized) {
            accelerometerCollector.stop()
        }

        if (::behaviourWindowProcessor.isInitialized) {
            behaviourWindowProcessor.close()
        }

        val preferences = getSharedPreferences(
            "LowkeyUser",
            MODE_PRIVATE
        )

        preferences.edit()
            .putBoolean("isMonitoring", false)
            .apply()

        Log.i(
            "LOWKEY_SERVICE",
            "Monitoring service destroyed"
        )

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

        val intent = Intent(
            this,
            ProtectionOverlayService::class.java
        )

        startService(intent)

        Log.d(
            "LOWKEY_SECURITY",
            "Test protection triggered"
        )
    }

    private fun showGrantAccess() {

        if (!Settings.canDrawOverlays(this)) {
            Log.e("LOWKEY_SECURITY", "Overlay permission missing")
            return
        }

        val intent = Intent(
            this,
            ProtectionOverlayService::class.java
        )

        intent.action = "SHOW_GRANT_ACCESS"

        startService(intent)

        Log.d(
            "LOWKEY_SECURITY",
            "Grant Access requested"
        )
    }

    private fun handleSecurityState(confidence: Float) {

        val securityManager = SecurityManager()
        val state = securityManager.evaluateConfidence(confidence)

        when (state) {

            SecurityManager.SecurityState.SUSPICIOUS -> {
                triggerProtectionForTest()
            }

            SecurityManager.SecurityState.GENUINE -> {
                showGrantAccess()
            }

            SecurityManager.SecurityState.NEEDS_VERIFICATION -> {
                Log.d(
                    "LOWKEY_SECURITY",
                    "Verification pending"
                )
            }
        }
    }

    private var savedRecordCount = 0

    private fun saveRecord(record: BehaviourRecord) {

        val fileName = "accelerometer_data.csv"

        try {

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
                    "${record.timestamp},${record.x},${record.y}," +
                            "${record.z},${record.accuracy}\n"

                outputStream.write(data.toByteArray())
            }

            savedRecordCount++

            if (savedRecordCount == 1 || savedRecordCount % 100 == 0) {

                Log.d(
                    "LOWKEY_ACCEL",
                    "Records saved=$savedRecordCount, latest=${record.timestamp}"
                )
            }

        } catch (e: Exception) {

            Log.e(
                "LOWKEY_ACCEL",
                "Failed to save accelerometer record",
                e
            )
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
                .setContentText("Behavioural monitoring is active")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build()

        } else {

            Notification.Builder(this)
                .setContentTitle("LOWKEY Monitoring")
                .setContentText("Behavioural monitoring is active")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build()
        }
    }

    companion object {

        private const val CHANNEL_ID = "LOWKEY_MONITORING"
        private const val NOTIFICATION_ID = 1001
    }
}
