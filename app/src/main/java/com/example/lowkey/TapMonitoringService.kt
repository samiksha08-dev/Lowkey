package com.example.lowkey

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
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

        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100

        setServiceInfo(info)

        Log.d("LowkeyTap", "Tap monitoring service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                try {
                    Log.d("LowkeyTap", "Click source: ${event.source}")

                    val tap = TapEvent(
                        timestamp = System.currentTimeMillis(),
                        xPosition = null,
                        yPosition = null,
                        durationMs = null,
                        eventType = "TAP"
                    )

                    tapRepository.saveTap(tap)

                    Log.d("LowkeyTap", "Tap event saved")
                } catch (e: Exception) {
                    Log.e("LowkeyTap", "Could not save tap event", e)
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                try {
                    val scrollEvent = TapEvent(
                        timestamp = System.currentTimeMillis(),
                        xPosition = null,
                        yPosition = null,
                        durationMs = null,
                        eventType = "SCROLL"
                    )

                    tapRepository.saveTap(scrollEvent)

                    Log.d("LowkeyTap", "Scroll event saved")
                } catch (e: Exception) {
                    Log.e("LowkeyTap", "Could not save scroll event", e)
                }
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                Log.d("LowkeyTap", "Window content changed")
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