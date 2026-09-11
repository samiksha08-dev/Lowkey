package com.example.lowkey

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PinActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_pin)

        val pin =
            findViewById<EditText>(R.id.etPin)

        val confirmPin =
            findViewById<EditText>(R.id.etConfirmPin)

        val save =
            findViewById<Button>(R.id.btnSavePin)

        save.setOnClickListener {

            val pinText = pin.text.toString()
            val confirmText = confirmPin.text.toString()

            if (pinText.length != 4) {
                pin.error = "PIN must be 4 digits"
                return@setOnClickListener
            }

            if (pinText != confirmText) {
                confirmPin.error = "PINs do not match"
                return@setOnClickListener
            }

            getSharedPreferences(
                "LowkeyUser",
                MODE_PRIVATE
            ).edit()
                .putString("pin", pinText)
                .apply()

            Toast.makeText(
                this,
                "PIN saved successfully",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }
}