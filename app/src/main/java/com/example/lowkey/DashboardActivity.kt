package com.example.lowkey

import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import com.example.lowkey.accelerometer.MonitoringService
import com.example.lowkey.tap.TapDatabaseHelper
import com.example.lowkey.tap.TapTouchTracker
import com.example.lowkey.tap.TapCsvLogger
import com.example.lowkey.tap.TapMonitoringService

class DashboardActivity : AppCompatActivity() {

    private var isMonitoring = false

    private lateinit var tapDatabase: TapDatabaseHelper
    private lateinit var tapCsvLogger: TapCsvLogger

    private lateinit var tvTapCount: TextView
    private lateinit var tvScrollCount: TextView

    // Check whether Accessibility Service is enabled
    private fun isTapAccessibilityServiceEnabled(): Boolean {
        val expectedService = ComponentName(
            this,
            TapMonitoringService::class.java
        )

        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)

        while (splitter.hasNext()) {
            val component = ComponentName.unflattenFromString(
                splitter.next()
            )

            if (component == expectedService) {
                return true
            }
        }

        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dashboard)

        tapDatabase = TapDatabaseHelper(this)
        tapCsvLogger = TapCsvLogger(this)

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

        // Test Protection button
        val btnTestProtection =
            findViewById<Button>(R.id.btnTestProtection)

        btnTestProtection.setOnClickListener {
            val intent = Intent(
                this,
                MonitoringService::class.java
            )

            intent.action = "TEST_PROTECTION"

            Toast.makeText(
                this,
                "Test Protection button clicked",
                Toast.LENGTH_SHORT
            ).show()

            startService(intent)
        }

        // Track touch events on Test Protection button
        val tapTracker = TapTouchTracker { event ->

            tapDatabase.insertTap(event)
            tapCsvLogger.appendTap(event)

            refreshTapScrollCounts()
        }

        btnTestProtection.setOnTouchListener(tapTracker)

        // Dashboard views
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

        // Tap and scroll counts
        tvTapCount = findViewById(R.id.tvTapCount)
        tvScrollCount = findViewById(R.id.tvScrollCount)

        // Saved user information
        val preferences = getSharedPreferences(
            "LowkeyUser",
            MODE_PRIVATE
        )

        val savedName = preferences.getString("name", "User")
        greeting.text = "Hello, $savedName 👋"

        // Restore saved monitoring state
        isMonitoring = preferences.getBoolean(
            "isMonitoring",
            false
        )

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

        refreshTapScrollCounts()

        // Monitoring switch
        monitoringSwitch.setOnCheckedChangeListener { _, checked ->

            if (isMonitoring == checked) {
                return@setOnCheckedChangeListener
            }

            // Check Accessibility permission
            if (checked && !isTapAccessibilityServiceEnabled()) {

                isMonitoring = false

                preferences.edit()
                    .putBoolean("isMonitoring", false)
                    .apply()

                monitoringSwitch.isChecked = false

                AlertDialog.Builder(this)
                    .setTitle("Enable Tap Behaviour Monitoring")
                    .setMessage(
                        "LOWKEY needs Accessibility permission " +
                                "to collect tap and scroll behaviour.\n\n" +
                                "Please follow these steps:\n\n" +
                                "1. Tap Open Settings.\n" +
                                "2. Open Installed apps or Installed services.\n" +
                                "3. Select Tap Behaviour Monitoring.\n" +
                                "4. Enable the service and allow the permission.\n" +
                                "5. Return to LOWKEY and turn Monitoring ON again."
                    )
                    .setPositiveButton("Open Settings") { _, _ ->

                        startActivity(
                            Intent(
                                Settings.ACTION_ACCESSIBILITY_SETTINGS
                            )
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

                return@setOnCheckedChangeListener
            }

            // Check overlay permission
            if (checked && !Settings.canDrawOverlays(this)) {

                isMonitoring = false

                preferences.edit()
                    .putBoolean("isMonitoring", false)
                    .apply()

                monitoringSwitch.isChecked = false

                AlertDialog.Builder(this)
                    .setTitle("Allow Display Over Other Apps")
                    .setMessage(
                        "LOWKEY needs this permission for its " +
                                "protection overlay.\n\n" +
                                "Tap Open Settings and allow " +
                                "Display over other apps. Then return " +
                                "to LOWKEY and turn Monitoring ON again."
                    )
                    .setPositiveButton("Open Settings") { _, _ ->

                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )

                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

                return@setOnCheckedChangeListener
            }

            // All required permissions are available
            isMonitoring = checked

            preferences.edit()
                .putBoolean("isMonitoring", checked)
                .apply()

            // Start or stop the existing monitoring service
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

        // Profile navigation
        navProfile.setOnClickListener {
            val intent = Intent(
                this,
                ProfileActivity::class.java
            )

            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        if (
            ::tapDatabase.isInitialized &&
            ::tvTapCount.isInitialized &&
            ::tvScrollCount.isInitialized
        ) {
            refreshTapScrollCounts()
        }
    }

    private fun refreshTapScrollCounts() {
        tvTapCount.text =
            "Taps: ${tapDatabase.getTapCount()}"

        tvScrollCount.text =
            "Scrolls: ${tapDatabase.getScrollCount()}"
    }

    // Start monitoring service
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
            monitoringState.setTextColor(
                Color.rgb(45, 170, 105)
            )

            confidenceScore.text = "92%"
            verificationStatus.text = "✓ User Verified"
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

    override fun onDestroy() {
        if (::tapDatabase.isInitialized) {
            tapDatabase.close()
        }

        super.onDestroy()
    }
}