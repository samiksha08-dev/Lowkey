package com.example.lowkey

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dashboard)

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

        // Views
        val greeting =
            findViewById<TextView>(R.id.tvGreeting)

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

        // Saved user information
        val preferences = getSharedPreferences(
            "LowkeyUser",
            MODE_PRIVATE
        )

        val savedName =
            preferences.getString("name", "User")

        greeting.text = "Hello, $savedName 👋"

        // --------------------------------------------------
        // RESTORE MONITORING STATE
        // --------------------------------------------------

        isMonitoring = preferences.getBoolean(
            "isMonitoring",
            false
        )

        // Show saved state on switch
        monitoringSwitch.isChecked = isMonitoring

        // Show saved state on dashboard
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

        // --------------------------------------------------
        // MONITORING SWITCH
        // --------------------------------------------------

        monitoringSwitch.setOnCheckedChangeListener { _, checked ->

            if (isMonitoring == checked) {
                return@setOnCheckedChangeListener
            }

            isMonitoring = checked

            // Save monitoring state
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

        // --------------------------------------------------
        // PROFILE
        // --------------------------------------------------

        navProfile.setOnClickListener {

            val intent = Intent(
                this,
                ProfileActivity::class.java
            )

            startActivity(intent)
        }
    }

    // ------------------------------------------------------
    // START MONITORING SERVICE
    // ------------------------------------------------------

    private fun startMonitoringService() {

        try {

            Log.d(
                "LOWKEY_SERVICE",
                "Starting MonitoringService"
            )

            val intent = Intent(
                this,
                MonitoringService::class.java
            )

            ContextCompat.startForegroundService(
                this,
                intent
            )

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

    // ------------------------------------------------------
    // STOP MONITORING SERVICE
    // ------------------------------------------------------

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

    // ------------------------------------------------------
    // UPDATE DASHBOARD UI
    // ------------------------------------------------------

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

            monitoringState.setTextColor(
                Color.rgb(45, 170, 105)
            )

            // Demo values for now
            confidenceScore.text = "92%"

            verificationStatus.text =
                "✓ User Verified"

            protectedApps.text = "3"

            alerts.text = "0"

            todayChecks.text = "24"

            progressConfidence.progress = 92

        } else {

            monitoringState.text = "OFF"

            monitoringState.setTextColor(
                Color.rgb(96, 114, 126)
            )

            confidenceScore.text = "--"

            verificationStatus.text =
                "Not yet available"

            protectedApps.text = "0"

            alerts.text = "0"

            todayChecks.text = "0"

            progressConfidence.progress = 0
        }
    }
}