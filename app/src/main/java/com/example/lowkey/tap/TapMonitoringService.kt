
package com.example.lowkey.tap

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class TapMonitoringService : AccessibilityService() {

    companion object {
        private const val TAG = "LowkeyTap"

        const val ACTION_DISABLE_ACCESSIBILITY =
            "com.example.lowkey.action.DISABLE_TAP_ACCESSIBILITY"
    }

    private lateinit var tapRepository: TapRepository
    private var disableReceiverRegistered = false

    private val disableReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_DISABLE_ACCESSIBILITY) {
                Log.i(
                    TAG,
                    "Master Monitoring OFF received. Disabling Accessibility Service."
                )

                disableSelf()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        tapRepository = TapRepository(this)

        val info = serviceInfo

        info.eventTypes =
            AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED

        info.feedbackType =
            AccessibilityServiceInfo.FEEDBACK_GENERIC

        info.notificationTimeout = 100

        setServiceInfo(info)

        registerDisableReceiver()

        Log.i(TAG, "Tap monitoring service connected")
    }

    private fun registerDisableReceiver() {
        if (disableReceiverRegistered) return

        val filter = IntentFilter(ACTION_DISABLE_ACCESSIBILITY)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                disableReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(disableReceiver, filter)
        }

        disableReceiverRegistered = true

        Log.d(TAG, "Private disable receiver registered")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val enabled = getSharedPreferences(
            "LowkeyUser",
            MODE_PRIVATE
        ).getBoolean("isMonitoring", false)

        // Never save events while master monitoring is OFF.
        if (!enabled) return

        when (event.eventType) {

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handleTap(event)
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                handleScroll(event)
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // No database record needed.
            }
        }
    }

    private fun handleTap(event: AccessibilityEvent) {
        var x: Float? = null
        var y: Float? = null

        val node = event.source

        if (node != null) {
            try {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)

                if (!bounds.isEmpty) {
                    x = (bounds.left + bounds.right) / 2f
                    y = (bounds.top + bounds.bottom) / 2f
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not read tap bounds", e)
            } finally {
                node.recycle()
            }
        }

        try {
            val result = tapRepository.saveTap(
                TapEvent(
                    timestamp = System.currentTimeMillis(),
                    xPosition = x,
                    yPosition = y,
                    durationMs = null,
                    eventType = "TAP"
                )
            )

            Log.d(
                TAG,
                "Tap saved: x=$x, y=$y, rowId=$result"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Could not save tap event", e)
        }
    }

    private fun handleScroll(event: AccessibilityEvent) {
        try {
            val deltaX = if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            ) event.scrollDeltaX else null

            val deltaY = if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            ) event.scrollDeltaY else null

            val result = tapRepository.saveTap(
                TapEvent(
                    timestamp = System.currentTimeMillis(),
                    xPosition = null,
                    yPosition = null,
                    durationMs = null,
                    eventType = "SCROLL",
                    scrollX = event.scrollX,
                    scrollY = event.scrollY,
                    scrollDeltaX = deltaX,
                    scrollDeltaY = deltaY,
                    maxScrollX = event.maxScrollX,
                    maxScrollY = event.maxScrollY
                )
            )

            Log.d(
                TAG,
                "Scroll saved: x=${event.scrollX}, " +
                        "y=${event.scrollY}, " +
                        "deltaX=$deltaX, deltaY=$deltaY, rowId=$result"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Could not save scroll event", e)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Monitoring interrupted")
    }

    override fun onDestroy() {
        if (disableReceiverRegistered) {
            try {
                unregisterReceiver(disableReceiver)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Disable receiver was already unregistered")
            }

            disableReceiverRegistered = false
        }

        if (::tapRepository.isInitialized) {
            tapRepository.close()
        }

        Log.i(TAG, "Tap monitoring service destroyed")

        super.onDestroy()
    }
}
