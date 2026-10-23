package com.example.lowkey.tap

import android.view.MotionEvent
import android.view.View

class TapTouchTracker(
    private val onTap: (TapEvent) -> Unit
) : View.OnTouchListener {

    private var downTime = 0L
    private var downX = 0f
    private var downY = 0f

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                downX = event.x
                downY = event.y
            }

            MotionEvent.ACTION_UP -> {
                val duration = System.currentTimeMillis() - downTime

                onTap(
                    TapEvent(
                        timestamp = System.currentTimeMillis(),
                        xPosition = downX,
                        yPosition = downY,
                        durationMs = duration
                    )
                )
            }
        }

        return false
    }
}