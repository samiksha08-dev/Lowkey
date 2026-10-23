package com.example.lowkey.tap

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class TapMonitoringService : AccessibilityService() {

    private lateinit var tapRepository: TapRepository

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
        Log.d("LowkeyTap", "Tap monitoring service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val enabled = getSharedPreferences(
            "LowkeyUser",
            MODE_PRIVATE
        ).getBoolean("isMonitoring", false)

        if (!enabled) return

        when (event.eventType) {

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
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

                        Log.d(
                            "LowkeyTap",
                            "Tap source available; bounds=$bounds"
                        )
                    } catch (e: Exception) {
                        Log.e("LowkeyTap", "Could not read tap bounds", e)
                    } finally {
                        node.recycle()
                    }
                } else {
                    Log.d("LowkeyTap", "Tap source unavailable")
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
                        "LowkeyTap",
                        "Tap saved: x=$x, y=$y, rowId=$result"
                    )
                } catch (e: Exception) {
                    Log.e("LowkeyTap", "Could not save tap event", e)
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
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
                        "LowkeyTap",
                        "Scroll saved: x=${event.scrollX}, " +
                                "y=${event.scrollY}, " +
                                "deltaX=$deltaX, deltaY=$deltaY, " +
                                "rowId=$result"
                    )
                } catch (e: Exception) {
                    Log.e("LowkeyTap", "Could not save scroll event", e)
                }
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // No database record needed for this event.
            }
        }
    }

    override fun onInterrupt() {
        Log.d("LowkeyTap", "Monitoring interrupted")
    }

    override fun onDestroy() {
        if (::tapRepository.isInitialized) {
            tapRepository.close()
        }

        Log.d("LowkeyTap", "Tap monitoring service destroyed")
        super.onDestroy()
    }
}