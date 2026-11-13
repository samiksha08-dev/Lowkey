package com.example.lowkey.ml

import android.content.Context

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

import org.json.JSONObject

data class LowkeyBehaviourFeatures(
    val tapCount: Double,
    val scrollCount: Double,
    val magnitudeMean: Double,
    val magnitudeStd: Double,
    val magnitudeMin: Double,
    val magnitudeMax: Double,
    val magnitudeRms: Double,
    val accelerometerSampleCount: Int
)

data class LowkeyInferenceResult(
    val anomalyScore: Double,
    val anomalyThreshold: Double,
    val isAnomalous: Boolean
)
class LowkeyOnnxEngine(context: Context) : AutoCloseable {

    companion object {

        private const val MODEL_FILE =
            "lowkey_model_b_compact.onnx"

        private const val METADATA_FILE =
            "lowkey_model_b_onnx_metadata.json"

        private const val EXPECTED_INPUT_NAME =
            "scaled_features"

        private const val EXPECTED_OUTPUT_NAME =
            "score_samples"

        private const val FEATURE_COUNT = 7
    }

    private val environment: OrtEnvironment =
    OrtEnvironment.getEnvironment()

private val session: OrtSession

private val featureNames: List<String>

private val scalerMean: DoubleArray

private val scalerScale: DoubleArray

private val anomalyThreshold: Double

private val minimumAccelerometerSamples: Int

private val inputName: String

private val outputName: String


init {
    val appContext = context.applicationContext

    // 1. Read model metadata JSON
    val metadataText = appContext.assets
        .open(METADATA_FILE)
        .bufferedReader()
        .use { it.readText() }

    val metadata = JSONObject(metadataText)

    // 2. Read the seven feature names
    val featureArray = metadata.getJSONArray("features")

    featureNames = List(featureArray.length()) { index ->
        featureArray.getString(index)
    }

    val expectedFeatures = listOf(
        "tap_count",
        "scroll_count",
        "magnitude_mean",
        "magnitude_std",
        "magnitude_min",
        "magnitude_max",
        "magnitude_rms"
    )

    require(featureNames == expectedFeatures) {
        "Incorrect LOWKEY model feature order"
    }

    // 3. Read StandardScaler values
    val meanArray = metadata.getJSONArray("scaler_mean")
    val scaleArray = metadata.getJSONArray("scaler_scale")

    scalerMean = DoubleArray(meanArray.length()) { index ->
        meanArray.getDouble(index)
    }

    scalerScale = DoubleArray(scaleArray.length()) { index ->
        scaleArray.getDouble(index)
    }

    require(
        scalerMean.size == FEATURE_COUNT &&
        scalerScale.size == FEATURE_COUNT &&
        scalerScale.all { it.isFinite() && it > 0.0 }
    ) {
        "Invalid LOWKEY scaler configuration"
    }

    // 4. Read anomaly threshold
    anomalyThreshold = metadata.getDouble("anomaly_threshold")

    require(anomalyThreshold.isFinite()) {
        "Invalid anomaly threshold"
    }

    // 5. Read minimum sensor samples
    minimumAccelerometerSamples =
        metadata.getInt("minimum_accelerometer_samples")

    // 6. Load ONNX model from Android assets
    val modelBytes = appContext.assets
        .open(MODEL_FILE)
        .use { it.readBytes() }

    val loadedSession = environment.createSession(modelBytes)

    // 7. Verify ONNX input and output names
    val inputNames = loadedSession.inputNames
    val outputNames = loadedSession.outputNames

    if (
        inputNames.size != 1 ||
        outputNames.size != 1 ||
        inputNames.first() != EXPECTED_INPUT_NAME ||
        outputNames.first() != EXPECTED_OUTPUT_NAME
    ) {
        loadedSession.close()
        throw IllegalStateException(
            "LOWKEY ONNX model input/output mismatch"
        )
    }

    // 8. Store the loaded session
    session = loadedSession

    inputName = inputNames.first()
    outputName = outputNames.first()
}


@Synchronized
fun predict(
    features: LowkeyBehaviourFeatures
): LowkeyInferenceResult {

    // 1. Validate sensor availability
    require(
        features.accelerometerSampleCount >=
            minimumAccelerometerSamples
    ) {
        "Insufficient accelerometer samples"
    }

    // 2. Validate touch activity
    require(
        features.tapCount >= 0.0 &&
        features.scrollCount >= 0.0
    ) {
        "Invalid touch counts"
    }

    require(
        features.tapCount > 0.0 ||
        features.scrollCount > 0.0
    ) {
        "No touch activity available"
    }

    // 3. Arrange features in exact training order
    val rawValues = doubleArrayOf(
        features.tapCount,
        features.scrollCount,
        features.magnitudeMean,
        features.magnitudeStd,
        features.magnitudeMin,
        features.magnitudeMax,
        features.magnitudeRms
    )

    require(rawValues.all { it.isFinite() }) {
        "Invalid behavioural feature values"
    }

    // 4. Apply Python StandardScaler parameters
    val scaledValues = FloatArray(FEATURE_COUNT) { index ->
        (
            (rawValues[index] - scalerMean[index]) /
                scalerScale[index]
        ).toFloat()
    }

    // 5. Create ONNX input tensor
    val inputData = arrayOf(scaledValues)

    OnnxTensor.createTensor(
        environment,
        inputData
    ).use { inputTensor ->

        // 6. Run Isolation Forest inference
        session.run(
            mapOf(inputName to inputTensor)
        ).use { result ->

            val output = result
                .get(outputName)
                .orElseThrow {
                    IllegalStateException(
                        "ONNX anomaly score missing"
                    )
                }

            val score = extractFirstScore(output.value)

            require(score.isFinite()) {
                "Invalid anomaly score"
            }

            // 7. Apply saved anomaly threshold
            val isAnomalous = score < anomalyThreshold

            return LowkeyInferenceResult(
                anomalyScore = score,
                anomalyThreshold = anomalyThreshold,
                isAnomalous = isAnomalous
            )
        }
    }
}


// Extract score from ONNX output tensor
private fun extractFirstScore(value: Any): Double {

    return when (value) {

        is Number -> value.toDouble()

        is FloatArray -> {
            value.firstOrNull()?.toDouble()
                ?: error("Empty ONNX output")
        }

        is DoubleArray -> {
            value.firstOrNull()
                ?: error("Empty ONNX output")
        }

        is Array<*> -> {
            extractFirstScore(
                value.firstOrNull()
                    ?: error("Empty ONNX output")
            )
        }

        else -> {
            error("Unsupported ONNX output: ${value.javaClass.name}")
        }
    }
}

   @Synchronized
   override fun close() {
     session.close()
    }

}