package com.example.lowkey

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin

class MonitoringOrbView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // Sends the ON/OFF state back to DashboardActivity
    var onMonitoringStateChanged: ((Boolean) -> Unit)? = null

    private val density = resources.displayMetrics.density

    // Orbit sizes
    private val outerOrbitRadius = 88f * density
    private val innerOrbitRadius = 52f * density

    // Animation state
    private var angle = 0f
    private var glowAmount = 0f
    private var isMonitoring = false

    private var orbitAnimator: ValueAnimator? = null

    // Paints
    private val outerOrbitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }

    private val innerOrbitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    init {
        // Allows soft shadow/glow effects
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        // Tapping the orb changes the monitoring state
        setOnClickListener {
            if (isMonitoring) {
                deactivateMonitoring()
            } else {
                activateMonitoring()
            }
        }
    }

    // --------------------------------------------------
    // CONTINUOUS ORBIT
    // --------------------------------------------------

    private fun startOrbitAnimation() {

        orbitAnimator?.cancel()

        orbitAnimator = ValueAnimator.ofFloat(0f, 360f).apply {

            duration = 5000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()

            addUpdateListener {
                angle += 1.5f

                if (angle >= 360f) {
                    angle = 0f
                }

                invalidate()
            }

            start()
        }
    }

    // --------------------------------------------------
    // TURN MONITORING ON
    // --------------------------------------------------

    private fun activateMonitoring() {
            startOrbitAnimation()
        // Prevent multiple activation animations
        if (glowAmount > 0f) return

        ValueAnimator.ofFloat(0f, 1f).apply {

            duration = 1000L

            addUpdateListener { animation ->

                glowAmount = animation.animatedValue as Float

                invalidate()
            }

            addListener(
                object : android.animation.AnimatorListenerAdapter() {

                    override fun onAnimationEnd(
                        animation: android.animation.Animator
                    ) {
                        isMonitoring = true
                        glowAmount = 1f

                        onMonitoringStateChanged?.invoke(true)

                        invalidate()
                    }
                }
            )

            start()
        }
    }

    // --------------------------------------------------
    // TURN MONITORING OFF
    // --------------------------------------------------

    private fun deactivateMonitoring() {

        ValueAnimator.ofFloat(1f, 0f).apply {

            duration = 600L

            addUpdateListener { animation ->

                glowAmount = animation.animatedValue as Float

                invalidate()
            }

            addListener(
                object : android.animation.AnimatorListenerAdapter() {

                    override fun onAnimationEnd(
                        animation: android.animation.Animator
                    ) {
                        isMonitoring = false
                        glowAmount = 0f

                        orbitAnimator?.cancel()
                        orbitAnimator = null
                        angle = 0f

                        onMonitoringStateChanged?.invoke(false)

                        invalidate()
                    }
                }
            )

            start()
        }
    }

    // --------------------------------------------------
    // DRAW EVERYTHING
    // --------------------------------------------------

    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        // ==============================================
        // OUTER ORBIT
        // ==============================================

        outerOrbitPaint.color = Color.argb(
            75,
            35,
            155,
            192
        )

        canvas.drawCircle(
            centerX,
            centerY,
            outerOrbitRadius,
            outerOrbitPaint
        )

        // ==============================================
        // INNER ORBIT
        // ==============================================

        innerOrbitPaint.color = Color.argb(
            100,
            35,
            155,
            192
        )

        canvas.drawCircle(
            centerX,
            centerY,
            innerOrbitRadius,
            innerOrbitPaint
        )

        // ==============================================
        // CENTRAL GLOW
        // ==============================================

        val glowRadius =
            (28f + (42f * glowAmount)) * density

        corePaint.shader = RadialGradient(
            centerX,
            centerY,
            glowRadius,
            intArrayOf(
                Color.argb(
                    (240 * glowAmount).toInt(),
                    255,
                    255,
                    255
                ),

                Color.argb(
                    (180 * glowAmount).toInt(),
                    90,
                    210,
                    235
                ),

                Color.TRANSPARENT
            ),
            null,
            Shader.TileMode.CLAMP
        )

        canvas.drawCircle(
            centerX,
            centerY,
            glowRadius,
            corePaint
        )

        corePaint.shader = null

        // ==============================================
        // CENTER CORE
        // ==============================================

        corePaint.color =
            if (isMonitoring) {
                Color.rgb(83, 195, 220)
            } else {
                Color.rgb(232, 245, 248)
            }

        corePaint.setShadowLayer(
            if (isMonitoring) {
                24f * density
            } else {
                5f * density
            },
            0f,
            0f,
            Color.rgb(75, 190, 220)
        )

        canvas.drawCircle(
            centerX,
            centerY,
            28f * density,
            corePaint
        )

        corePaint.clearShadowLayer()

        // ==============================================
        // OUTER ORBIT DOT
        // ==============================================

        val outerAngle =
            Math.toRadians(angle.toDouble())

        val outerX =
            centerX +
                    outerOrbitRadius *
                    cos(outerAngle).toFloat()

        val outerY =
            centerY +
                    outerOrbitRadius *
                    sin(outerAngle).toFloat()

        // ==============================================
        // INNER ORBIT DOT
        // ==============================================

        val innerAngle =
            Math.toRadians((-angle * 1.5f).toDouble())

        val innerX =
            centerX +
                    innerOrbitRadius *
                    cos(innerAngle).toFloat()

        val innerY =
            centerY +
                    innerOrbitRadius *
                    sin(innerAngle).toFloat()

        // ==============================================
        // DRAW ORBIT DOTS
        // ==============================================

        dotPaint.color = Color.rgb(
            54,
            176,
            207
        )

        dotPaint.setShadowLayer(
            10f * density,
            0f,
            0f,
            Color.rgb(65, 190, 220)
        )

        // Outer dot
        canvas.drawCircle(
            outerX,
            outerY,
            5f * density,
            dotPaint
        )

        // Inner dot
        canvas.drawCircle(
            innerX,
            innerY,
            4f * density,
            dotPaint
        )

        dotPaint.clearShadowLayer()

        // ==============================================
        // CENTER DOT
        // ==============================================

        dotPaint.color = Color.WHITE

        canvas.drawCircle(
            centerX,
            centerY,
            4f * density,
            dotPaint
        )
    }

    // Stop animator when view is detached
    override fun onDetachedFromWindow() {
        orbitAnimator?.cancel()
        orbitAnimator = null

        super.onDetachedFromWindow()
    }
}