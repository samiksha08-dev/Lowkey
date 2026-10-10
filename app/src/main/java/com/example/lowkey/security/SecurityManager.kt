package com.example.lowkey.security

class SecurityManager {

    enum class SecurityState {
        GENUINE,
        NEEDS_VERIFICATION,
        SUSPICIOUS
    }

    fun evaluateConfidence(confidence: Float): SecurityState {

        return when {
            confidence >= 0.80f -> SecurityState.GENUINE

            confidence >= 0.50f -> SecurityState.NEEDS_VERIFICATION

            else -> SecurityState.SUSPICIOUS
        }
    }
}