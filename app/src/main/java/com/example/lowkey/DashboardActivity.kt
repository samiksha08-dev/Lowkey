
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
import com.example.lowkey.security.ProtectionOverlayService
import com.example.lowkey.tap.TapDatabaseHelper
import com.example.lowkey.tap.TapMonitoringService

class DashboardActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LOWKEY_SERVICE"
        private const val PREFS_NAME = "LowkeyUser"
        private const val MONITORING_KEY = "isMonitoring"
    }

    private var isMonitoring = false

    private lateinit var tapDatabase: TapDatabaseHelper
    private lateinit var tvTapCount: TextView
    private lateinit var tvScrollCount: TextView

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

    private fun saveMonitoringState(enabled: Boolean): Boolean {
        return getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        ).edit()
            .putBoolean(MONITORING_KEY, enabled)
            .commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dashboard)

        tapDatabase = TapDatabaseHelper(this)

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

        // Test Protection independently of behavioural monitoring.
        val btnTestProtection =
            findViewById<Button>(R.id.btnTestProtection)

        btnTestProtection.setOnClickListener {

            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(
                    this,
                    "Please enable overlay permission first",
                    Toast.LENGTH_LONG
                ).show()

                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )

                return@setOnClickListener
            }

            try {
                startService(
                    Intent(
                        this,
                        ProtectionOverlayService::class.java
                    )
                )

                Toast.makeText(
                    this,
                    "Test Protection triggered",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start protection overlay", e)

                Toast.makeText(
                    this,
                    "Protection error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Dashboard views
        val greeting = findViewById<TextView>(R.id.tvGreeting)
        val monitoringState = findViewById<TextView>(R.id.tvMonitoringState)
        val monitoringSwitch = findViewById<SwitchCompat>(R.id.switchMonitoring)
        val confidenceScore = findViewById<TextView>(R.id.tvConfidenceScore)
        val verificationStatus = findViewById<TextView>(R.id.tvVerificationStatus)
        val protectedApps = findViewById<TextView>(R.id.tvProtectedApps)
        val alerts = findViewById<TextView>(R.id.tvAlerts)
        val todayChecks = findViewById<TextView>(R.id.tvTodayChecks)
        val progressConfidence =
            findViewById<ProgressBar>(R.id.progressConfidence)
        val navProfile = findViewById<TextView>(R.id.navProfile)

        tvTapCount = findViewById(R.id.tvTapCount)
        tvScrollCount = findViewById(R.id.tvScrollCount)

        val preferences = getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        )

        val savedName = preferences.getString("name", "User")
        greeting.text = "Hello, $savedName 👋"

        isMonitoring = preferences.getBoolean(
            MONITORING_KEY,
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

        monitoringSwitch.setOnCheckedChangeListener { _, checked ->

            if (isMonitoring == checked) {
                return@setOnCheckedChangeListener
            }

            if (checked && !isTapAccessibilityServiceEnabled()) {

                isMonitoring = false
                saveMonitoringState(false)
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
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

                return@setOnCheckedChangeListener
            }

            if (checked && !Settings.canDrawOverlays(this)) {

                isMonitoring = false
                saveMonitoringState(false)
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

            // Save the master state before starting or stopping services.
            isMonitoring = checked

            if (!saveMonitoringState(checked)) {
                Log.e(TAG, "Could not persist monitoring state")
            }

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
        navProfile.setOnClickListener { anchor ->

            val popupMenu = androidx.appcompat.widget.PopupMenu(
                android.view.ContextThemeWrapper(
                    this,
                    R.style.LowkeyPopupMenuTheme
                ),
                anchor
            )

            popupMenu.menu.add("Home")
                .setOnMenuItemClickListener {
                    startActivity(
                        Intent(this, DashboardActivity::class.java)
                    )
                    true
                }

            popupMenu.menu.add("App")
                .setOnMenuItemClickListener {
                    startActivity(
                        Intent(this, MainActivity::class.java)
                    )
                    true
                }

            popupMenu.menu.add("Profile")
                .setOnMenuItemClickListener {
                    startActivity(
                        Intent(this, ProfileActivity::class.java)
                    )
                    true
                }

            popupMenu.menu.add("Settings")
                .setOnMenuItemClickListener {
                    startActivity(
                        Intent(this, EditProfileActivity::class.java)
                    )
                    true
                }

            popupMenu.show()
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
        tvTapCount.text = "Taps: ${tapDatabase.getTapCount()}"
        tvScrollCount.text = "Scrolls: ${tapDatabase.getScrollCount()}"
    }

    private fun startMonitoringService() {
        try {
            Log.d(TAG, "Starting MonitoringService")

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

            Log.d(TAG, "startForegroundService() called")

        } catch (e: Exception) {

            Log.e(TAG, "Failed to start MonitoringService", e)

            isMonitoring = false
            saveMonitoringState(false)

            Toast.makeText(
                this,
                "Service error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun stopMonitoringService() {

        // 1. Stop accelerometer collection and ML processing.
        val monitoringIntent = Intent(
            this,
            MonitoringService::class.java
        )

        val stopped = stopService(monitoringIntent)

        Log.i(
            TAG,
            "MonitoringService stop requested: $stopped"
        )

        // 2. Ask the AccessibilityService to disable itself.
        // The service itself calls disableSelf().
        val disableIntent = Intent(
            TapMonitoringService.ACTION_DISABLE_ACCESSIBILITY
        ).setPackage(packageName)

        sendBroadcast(disableIntent)

        Log.i(
            TAG,
            "Accessibility disable request broadcast sent"
        )

        Toast.makeText(
            this,
            "LOWKEY Monitoring stopped",
            Toast.LENGTH_SHORT
        ).show()
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
