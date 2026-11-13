package com.example.lowkey.ml

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LowkeyOnnxEngineInstrumentedTest {

    @Test
    fun modelLoadsAndRunsInference() {

        val context =
            InstrumentationRegistry.getInstrumentation()
                .targetContext

        val engine = LowkeyOnnxEngine(context)

        try {
            val sample = LowkeyBehaviourFeatures(
                tapCount = 1.0,
                scrollCount = 5.0,
                magnitudeMean = 9.9,
                magnitudeStd = 0.5,
                magnitudeMin = 9.0,
                magnitudeMax = 10.8,
                magnitudeRms = 9.91,
                accelerometerSampleCount = 60
            )

            val result = engine.predict(sample)

            assertTrue(
                "Anomaly score must be finite",
                result.anomalyScore.isFinite()
            )

            assertEquals(
                -0.519905614103085,
                result.anomalyThreshold,
                0.000000001
            )

            println("LOWKEY ONNX MODEL LOADED SUCCESSFULLY")
            println("Anomaly Score: ${result.anomalyScore}")
            println("Threshold: ${result.anomalyThreshold}")
            println("Anomalous: ${result.isAnomalous}")

        } finally {
            engine.close()
        }
    }
}