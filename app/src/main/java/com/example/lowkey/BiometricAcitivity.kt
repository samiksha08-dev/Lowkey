package com.example.lowkey

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class BiometricActivity : AppCompatActivity() {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_biometric)

        val biometricSwitch =
            findViewById<SwitchCompat>(R.id.switchBiometric)

        val statusText =
            findViewById<TextView>(R.id.tvBiometricStatus)

        val testButton =
            findViewById<Button>(R.id.btnTestBiometric)

        val preferences = getSharedPreferences(
            "LowkeyUser",
            MODE_PRIVATE
        )

        val isEnabled =
            preferences.getBoolean("biometric_enabled", false)

        biometricSwitch.isChecked = isEnabled

        statusText.text =
            if (isEnabled) {
                "Biometric is enabled"
            } else {
                "Biometric is not enabled"
            }

        val executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)

                    preferences.edit()
                        .putBoolean("biometric_enabled", true)
                        .apply()

                    biometricSwitch.isChecked = true

                    statusText.text = "Biometric is enabled"

                    Toast.makeText(
                        this@BiometricActivity,
                        "Biometric enabled successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()

                    Toast.makeText(
                        this@BiometricActivity,
                        "Fingerprint not recognised",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(
                        errorCode,
                        errString
                    )

                    biometricSwitch.isChecked =
                        preferences.getBoolean(
                            "biometric_enabled",
                            false
                        )

                    Toast.makeText(
                        this@BiometricActivity,
                        errString,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verify your identity")
            .setSubtitle("Use your fingerprint")
            .setNegativeButtonText("Cancel")
            .build()

        biometricSwitch.setOnCheckedChangeListener { _, checked ->

            if (checked) {

                val biometricManager =
                    BiometricManager.from(this)

                val canAuthenticate =
                    biometricManager.canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG
                    )

                when (canAuthenticate) {

                    BiometricManager.BIOMETRIC_SUCCESS -> {
                        biometricPrompt.authenticate(promptInfo)
                    }

                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {

                        biometricSwitch.isChecked = false

                        Toast.makeText(
                            this,
                            "Biometric hardware not available",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {

                        biometricSwitch.isChecked = false

                        Toast.makeText(
                            this,
                            "Please register a fingerprint on your phone first",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    else -> {

                        biometricSwitch.isChecked = false

                        Toast.makeText(
                            this,
                            "Biometric authentication is unavailable",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } else {

                preferences.edit()
                    .putBoolean("biometric_enabled", false)
                    .apply()

                statusText.text =
                    "Biometric is not enabled"

                Toast.makeText(
                    this,
                    "Biometric disabled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        testButton.setOnClickListener {

            val biometricManager =
                BiometricManager.from(this)

            val canAuthenticate =
                biometricManager.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                )

            if (canAuthenticate ==
                BiometricManager.BIOMETRIC_SUCCESS
            ) {

                biometricPrompt.authenticate(promptInfo)

            } else {

                Toast.makeText(
                    this,
                    "Biometric authentication is unavailable",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}