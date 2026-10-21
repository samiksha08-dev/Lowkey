package com.example.lowkey

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.lowkey.accelerometer.MonitoringService

class DashboardActivity : AppCompatActivity() {

    private var isMonitoring = false
    private lateinit var tapDatabase: TapDatabaseHelper

    private lateinit var tvTapCount: TextView
    private lateinit var tvScrollCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        tapDatabase = TapDatabaseHelper(this)

        // System bars
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { view, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // Test protection button
        val btnTestProtection =
            findViewById<Button>(R.id.btnTestProtection)

        btnTestProtection.setOnClickListener {
            val intent = Intent(this, MonitoringService::class.java)
            intent.action = "TEST_PROTECTION"

            Toast.makeText(
                this,
                "Test Protection button clicked",
                Toast.LENGTH_SHORT
            ).show()

            startService(intent)
        }

        // Track touch events on the test button
        val tapTracker = TapTouchTracker { event ->
            tapDatabase.insertTap(event)
            refreshTapScrollCounts()
        }

        btnTestProtection.setOnTouchListener(tapTracker)

        // Dashboard views
        val greeting = findViewById<TextView>(R.id.tvGreeting)
        val monitoringState =
            findViewById<TextView>(R.id.tvMonitoringState)
        val monitoringSwitch =
            findViewById<SwitchCompat>(R.id.switchMonitoring)
        val confidenceScore =
            findViewById<TextView>(R.id.tvConfidenceScore)
        val verificationStatus =
            findViewById<TextView>(R.id.tvVerificationStatus)
        val protectedApps =
            findViewById<TextView>(R.id.tvProtectedApps)
        val alerts =
            findViewById<TextView>(R.id.tvAlerts)
        val todayChecks =
            findViewById<TextView>(R.id.tvTodayChecks)
        val progressConfidence =
            findViewById<ProgressBar>(R.id.progressConfidence)
        val navProfile =
            findViewById<TextView>(R.id.navProfile)

        // Tap and scroll count views
        tvTapCount = findViewById(R.id.tvTapCount)
        tvScrollCount = findViewById(R.id.tvScrollCount)

        // Saved user information
        val preferences = getSharedPreferences(
            "LowkeyUser",
            MODE_PRIVATE
        )

        val savedName = preferences.getString("name", "User")
        greeting.text = "Hello, $savedName 👋"

        // Restore monitoring state
        isMonitoring = preferences.getBoolean("isMonitoring", false)
        monitoringSwitch.isChecked = isMonitoring

        updateDashboard(
            enabled = isMonitoring,
            monitoringState = monitoringState,
            confidenceScore = confidenceScore,
            verificationStatus = verificationStatus,
            protectedApps = protectedApps,
            alerts = alerts,
            todayChecks = todayChecks,
            progressConfidence = progressConfidence
        )

        // Load saved tap and scroll totals
        refreshTapScrollCounts()

        // Monitoring switch
        monitoringSwitch.setOnCheckedChangeListener { _, checked ->
            if (isMonitoring == checked) {
                return@setOnCheckedChangeListener
            }

            // Overlay permission is required for protection
            if (checked && !Settings.canDrawOverlays(this)) {
                monitoringSwitch.isChecked = false

                Toast.makeText(
                    this,
                    "Please allow 'Display over other apps' permission",
                    Toast.LENGTH_LONG
                ).show()

                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )

                startActivity(intent)
                return@setOnCheckedChangeListener
            }

            isMonitoring = checked

            preferences.edit()
                .putBoolean("isMonitoring", checked)
                .apply()

            if (checked) {
                startMonitoringService()
            } else {
                stopMonitoringService()
            }

            updateDashboard(
                enabled = checked,
                monitoringState = monitoringState,
                confidenceScore = confidenceScore,
                verificationStatus = verificationStatus,
                protectedApps = protectedApps,
                alerts = alerts,
                todayChecks = todayChecks,
                progressConfidence = progressConfidence
            )
        }

        // Profile
        navProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        if (::tapDatabase.isInitialized &&
            ::tvTapCount.isInitialized &&
            ::tvScrollCount.isInitialized
        ) {
            refreshTapScrollCounts()
        }
    }

    private fun refreshTapScrollCounts() {
        tvTapCount.text = "Taps: ${tapDatabase.getTapCount()}"
        tvScrollCount.text = "Scrolls: ${tapDatabase.getScrollCount()}"
    }

    // Start monitoring service
    private fun startMonitoringService() {
        try {
            Log.d("LOWKEY_SERVICE", "Starting MonitoringService")

            val intent = Intent(
                this,
                MonitoringService::class.java
            )

            ContextCompat.startForegroundService(this, intent)

            Toast.makeText(
                this,
                "Monitoring started",
                Toast.LENGTH_SHORT
            ).show()

            Log.d(
                "LOWKEY_SERVICE",
                "startForegroundService() called"
            )
        } catch (e: Exception) {
            Log.e(
                "LOWKEY_SERVICE",
                "Failed to start MonitoringService",
                e
            )

            Toast.makeText(
                this,
                "Service error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Stop monitoring service
    private fun stopMonitoringService() {
        val intent = Intent(
            this,
            MonitoringService::class.java
        )

        stopService(intent)

        Toast.makeText(
            this,
            "Monitoring stopped",
            Toast.LENGTH_SHORT
        ).show()

        Log.d(
            "LOWKEY_SERVICE",
            "MonitoringService stopped"
        )
    }

    // Update dashboard UI
    private fun updateDashboard(
        enabled: Boolean,
        monitoringState: TextView,
        confidenceScore: TextView,
        verificationStatus: TextView,
        protectedApps: TextView,
        alerts: TextView,
        todayChecks: TextView,
        progressConfidence: ProgressBar
    ) {
        if (enabled) {
            monitoringState.text = "ON"
            monitoringState.setTextColor(Color.rgb(45, 170, 105))

            confidenceScore.text = "92%"
            verificationStatus.text = "✓ User Verified"
            protectedApps.text = "3"
            alerts.text = "0"
            todayChecks.text = "24"
            progressConfidence.progress = 92
        } else {
            monitoringState.text = "OFF"
            monitoringState.setTextColor(Color.rgb(96, 114, 126))

            confidenceScore.text = "--"
            verificationStatus.text = "Not yet available"
            protectedApps.text = "0"
            alerts.text = "0"
            todayChecks.text = "0"
            progressConfidence.progress = 0
        }
    }

    override fun onDestroy() {
        if (::tapDatabase.isInitialized) {
            tapDatabase.close()
        }
        super.onDestroy()
    }
}