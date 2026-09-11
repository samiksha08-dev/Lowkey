package com.example.lowkey

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent

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


        // Load saved user's name
        val preferences = getSharedPreferences(
            "LowkeyUser",
            MODE_PRIVATE
        )

        val savedName =
            preferences.getString("name", "User")

        greeting.text = "Hello, $savedName 👋"

        // Initial state
        updateDashboard(
            enabled = false,
            monitoringState = monitoringState,
            confidenceScore = confidenceScore,
            verificationStatus = verificationStatus,
            protectedApps = protectedApps,
            alerts = alerts,
            todayChecks = todayChecks,
            progressConfidence = progressConfidence
        )

        // Switch
        monitoringSwitch.setOnCheckedChangeListener { _, checked ->

            if (isMonitoring == checked) {
                return@setOnCheckedChangeListener
            }

            isMonitoring = checked

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
        navProfile.setOnClickListener {

            val intent = Intent(
                this,
                ProfileActivity::class.java
            )

            startActivity(intent)
        }

    }


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

            confidenceScore.text = "92%"
            verificationStatus.text = "✓  User Verified"

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
            verificationStatus.text = "Not yet available"

            protectedApps.text = "0"
            alerts.text = "0"
            todayChecks.text = "0"

            progressConfidence.progress = 0
        }
    }
}