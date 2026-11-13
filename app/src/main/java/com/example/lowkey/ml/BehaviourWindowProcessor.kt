
package com.example.lowkey.ml

import android.content.Context
import android.util.Log

import com.example.lowkey.accelerometer.BehaviourRecord
import com.example.lowkey.tap.TapDatabaseHelper

import java.util.TreeMap
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

import kotlin.math.sqrt

class BehaviourWindowProcessor(
    context: Context,
    private val onInferenceResult: (
        Long,
        Long,
        LowkeyBehaviourFeatures,
        LowkeyInferenceResult
    ) -> Unit = { _, _, _, _ -> }
) : AutoCloseable {

    companion object {
        private const val TAG = "LOWKEY_ML"

        private const val WINDOW_SIZE_MS = 10_000L
        private const val WINDOW_GRACE_MS = 1_000L

        private const val MINIMUM_ACCELEROMETER_SAMPLES = 30
    }

    private val appContext = context.applicationContext

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "LOWKEY-Behaviour-ML")
    }

    private val closed = AtomicBoolean(false)

    private val accelerometerWindows =
        TreeMap<Long, MutableList<Double>>()

    private lateinit var tapDatabaseHelper: TapDatabaseHelper
    private lateinit var onnxEngine: LowkeyOnnxEngine

    private var initializationFailed = false

    init {
        initializeProcessor()
    }

    private fun initializeProcessor() {
        executor.execute {
            try {
                tapDatabaseHelper = TapDatabaseHelper(appContext)

                onnxEngine = LowkeyOnnxEngine(appContext)

                Log.i(
                    TAG,
                    "Behaviour processor initialized successfully"
                )

            } catch (e: Exception) {
                initializationFailed = true

                if (::tapDatabaseHelper.isInitialized) {
                    tapDatabaseHelper.close()
                }

                Log.e(
                    TAG,
                    "Failed to initialize behaviour processor",
                    e
                )
            }
        }
    }

    fun onAccelerometerRecord(record: BehaviourRecord) {
        if (closed.get()) return

        try {
            executor.execute {
                if (closed.get() || initializationFailed) {
                    return@execute
                }

                collectRecord(record)
            }

        } catch (e: RejectedExecutionException) {
            Log.w(
                TAG,
                "Record ignored because processor is closing"
            )
        }
    }

    private fun collectRecord(record: BehaviourRecord) {
        val x = record.x.toDouble()
        val y = record.y.toDouble()
        val z = record.z.toDouble()

        val magnitude = sqrt(x * x + y * y + z * z)

        if (!magnitude.isFinite()) {
            Log.w(TAG, "Invalid acceleration magnitude ignored")
            return
        }

        val windowStart =
            Math.floorDiv(record.timestamp, WINDOW_SIZE_MS) *
                    WINDOW_SIZE_MS

        accelerometerWindows
            .getOrPut(windowStart) { mutableListOf() }
            .add(magnitude)

        val completedWindows = accelerometerWindows.keys
            .filter { start ->
                start + WINDOW_SIZE_MS + WINDOW_GRACE_MS <=
                        record.timestamp
            }

        for (start in completedWindows) {
            val samples = accelerometerWindows.remove(start)
                ?: continue

            evaluateWindow(
                windowStart = start,
                samples = samples
            )
        }
    }

    private fun evaluateWindow(
        windowStart: Long,
        samples: List<Double>
    ) {
        val windowEnd = windowStart + WINDOW_SIZE_MS

        try {
            if (samples.size < MINIMUM_ACCELEROMETER_SAMPLES) {
                Log.d(
                    TAG,
                    "Window skipped: insufficient samples=${samples.size}"
                )
                return
            }

            val counts = tapDatabaseHelper.getEventCountsBetween(
                startTimestamp = windowStart,
                endTimestamp = windowEnd
            )

            val tapCount = counts.first
            val scrollCount = counts.second

            if (tapCount == 0 && scrollCount == 0) {
                Log.d(
                    TAG,
                    "Window skipped: no touch activity"
                )
                return
            }

            val features = calculateFeatures(
                tapCount = tapCount,
                scrollCount = scrollCount,
                samples = samples
            )

            // Diagnostic output: exact seven values sent to Model B.
            Log.i(
                TAG,
                """
                MODEL INPUT FEATURES
                tap_count=${features.tapCount}
                scroll_count=${features.scrollCount}
                magnitude_mean=${features.magnitudeMean}
                magnitude_std=${features.magnitudeStd}
                magnitude_min=${features.magnitudeMin}
                magnitude_max=${features.magnitudeMax}
                magnitude_rms=${features.magnitudeRms}
                accelerometer_sample_count=${features.accelerometerSampleCount}
                """.trimIndent()
            )

            val result = onnxEngine.predict(features)

            Log.i(
                TAG,
                """
                INFERENCE COMPLETED
                Window start=$windowStart
                Window end=$windowEnd
                Anomaly score=${result.anomalyScore}
                Threshold=${result.anomalyThreshold}
                Anomalous=${result.isAnomalous}
                """.trimIndent()
            )

            onInferenceResult(
                windowStart,
                windowEnd,
                features,
                result
            )

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to evaluate window starting at $windowStart",
                e
            )
        }
    }

    private fun calculateFeatures(
        tapCount: Int,
        scrollCount: Int,
        samples: List<Double>
    ): LowkeyBehaviourFeatures {

        val count = samples.size

        var sum = 0.0
        var squaredSum = 0.0
        var minimum = Double.MAX_VALUE
        var maximum = -Double.MAX_VALUE

        for (value in samples) {
            sum += value
            squaredSum += value * value

            if (value < minimum) {
                minimum = value
            }

            if (value > maximum) {
                maximum = value
            }
        }

        val mean = sum / count

        // Sample standard deviation: ddof = 1.
        var squaredDifferences = 0.0

        for (value in samples) {
            val difference = value - mean
            squaredDifferences += difference * difference
        }

        val standardDeviation = sqrt(
            squaredDifferences / (count - 1)
        )

        val rms = sqrt(squaredSum / count)

        return LowkeyBehaviourFeatures(
            tapCount = tapCount.toDouble(),
            scrollCount = scrollCount.toDouble(),
            magnitudeMean = mean,
            magnitudeStd = standardDeviation,
            magnitudeMin = minimum,
            magnitudeMax = maximum,
            magnitudeRms = rms,
            accelerometerSampleCount = count
        )
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) {
            return
        }

        try {
            executor.execute {
                accelerometerWindows.clear()

                if (::onnxEngine.isInitialized) {
                    onnxEngine.close()
                }

                if (::tapDatabaseHelper.isInitialized) {
                    tapDatabaseHelper.close()
                }

                Log.i(
                    TAG,
                    "Behaviour processor closed"
                )
            }

            executor.shutdown()

        } catch (e: RejectedExecutionException) {
            Log.w(
                TAG,
                "Processor was already shutting down"
            )
        }
    }
}
