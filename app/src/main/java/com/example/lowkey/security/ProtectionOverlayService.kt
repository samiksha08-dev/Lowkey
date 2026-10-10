package com.example.lowkey.security

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class ProtectionOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var protectionView: LinearLayout? = null
    private var verifyButton: TextView? = null

    override fun onCreate() {
        super.onCreate()

        windowManager =
            getSystemService(WINDOW_SERVICE) as WindowManager

        showProtectionOverlay()
    }


    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {
            "SHOW_GRANT_ACCESS" -> {
                verifyButton?.visibility = TextView.VISIBLE
            }
        }

        return START_NOT_STICKY
    }


    private fun showProtectionOverlay() {

        if (protectionView != null) return

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        // Main protection layer
        val layout = LinearLayout(this)

        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(50, 50, 50, 50)

        // Semi-transparent protection layer
        layout.setBackgroundColor(
            Color.argb(220, 20, 25, 30)
        )

        // Title
        val title = TextView(this)

        title.text = "🔒  Identity Verification Required"
        title.textSize = 22f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER

        // Message
        val message = TextView(this)

        message.text =
            "Suspicious behaviour detected.\n\n" +
                    "Your screen has been protected.\n" +
                    "Please verify your identity to continue."

        message.textSize = 17f
        message.setTextColor(Color.WHITE)
        message.gravity = Gravity.CENTER
        message.setPadding(20, 30, 20, 40)

        // Verify button
        val button = TextView(this)
        verifyButton = button

        button.text = "GRANT ACCESS"
        button.visibility = TextView.GONE
        button.textSize = 17f
        button.setTextColor(Color.WHITE)
        button.gravity = Gravity.CENTER
        button.setPadding(50, 25, 50, 25)
        button.setBackgroundColor(
            Color.rgb(40, 120, 220)
        )

        button.setOnClickListener {
            stopSelf()
        }

        layout.addView(title)
        layout.addView(message)
        layout.addView(button)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            0,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.CENTER

        windowManager.addView(layout, params)

        protectionView = layout
    }

    override fun onDestroy() {

        protectionView?.let {
            windowManager.removeView(it)
        }

        protectionView = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}