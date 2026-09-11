package com.example.lowkey

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class EditProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_edit_profile)

        val nameField =
            findViewById<EditText>(R.id.etEditName)

        val emailField =
            findViewById<EditText>(R.id.etEditEmail)

        val saveButton =
            findViewById<Button>(R.id.btnSaveProfile)

        val cancelButton =
            findViewById<Button>(R.id.btnCancelEdit)

        val preferences = getSharedPreferences(
            "LowkeyUser",
            MODE_PRIVATE
        )

        // Load existing information
        nameField.setText(
            preferences.getString("name", "")
        )

        emailField.setText(
            preferences.getString("email", "")
        )

        // Save changes
        saveButton.setOnClickListener {

            val name = nameField.text.toString().trim()
            val email = emailField.text.toString().trim()

            if (name.isEmpty()) {
                nameField.error = "Please enter your name"
                nameField.requestFocus()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                emailField.error = "Please enter your email"
                emailField.requestFocus()
                return@setOnClickListener
            }

            preferences.edit()
                .putString("name", name)
                .putString("email", email)
                .apply()

            finish()
        }

        // Cancel
        cancelButton.setOnClickListener {
            finish()
        }
    }
}