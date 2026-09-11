package com.example.lowkey

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ChangePasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_change_password)

        val current =
            findViewById<EditText>(R.id.etCurrentPassword)

        val newPassword =
            findViewById<EditText>(R.id.etNewPassword)

        val confirm =
            findViewById<EditText>(R.id.etConfirmPassword)

        val update =
            findViewById<Button>(R.id.btnUpdatePassword)

        val preferences = getSharedPreferences(
            "LowkeyUser",
            MODE_PRIVATE
        )

        update.setOnClickListener {

            val savedPassword =
                preferences.getString("password", "")

            val currentText =
                current.text.toString()

            val newText =
                newPassword.text.toString()

            val confirmText =
                confirm.text.toString()

            if (currentText != savedPassword) {
                current.error = "Incorrect current password"
                return@setOnClickListener
            }

            if (newText.length < 6) {
                newPassword.error =
                    "Password must be at least 6 characters"
                return@setOnClickListener
            }

            if (newText != confirmText) {
                confirm.error = "Passwords do not match"
                return@setOnClickListener
            }

            preferences.edit()
                .putString("password", newText)
                .apply()

            Toast.makeText(
                this,
                "Password updated successfully",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }
}