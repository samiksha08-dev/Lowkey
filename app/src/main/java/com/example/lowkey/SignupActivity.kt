package com.example.lowkey

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_signup)

        val name = findViewById<EditText>(R.id.etSignupName)
        val email = findViewById<EditText>(R.id.etSignupEmail)
        val password = findViewById<EditText>(R.id.etSignupPassword)
        val confirmPassword = findViewById<EditText>(R.id.etSignupConfirmPassword)

        val signupButton = findViewById<Button>(R.id.btnSignupSubmit)
        val loginLink = findViewById<TextView>(R.id.tvGoLogin)

        signupButton.setOnClickListener {

            val nameText = name.text.toString().trim()
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString()
            val confirmPasswordText = confirmPassword.text.toString()

            when {
                nameText.isEmpty() -> {
                    name.error = "Please enter your name"
                }

                emailText.isEmpty() -> {
                    email.error = "Please enter your email"
                }

                passwordText.isEmpty() -> {
                    password.error = "Please enter a password"
                }

                passwordText.length < 6 -> {
                    password.error = "Password must be at least 6 characters"
                }

                confirmPasswordText.isEmpty() -> {
                    confirmPassword.error = "Please confirm your password"
                }

                passwordText != confirmPasswordText -> {
                    confirmPassword.error = "Passwords do not match"
                }

                else -> {

                    val preferences = getSharedPreferences(
                        "LowkeyUser",
                        MODE_PRIVATE
                    )

                    preferences.edit()
                        .putString("name", nameText)
                        .putString("email", emailText)
                        .putString("password", passwordText)
                        .apply()

                    Toast.makeText(
                        this,
                        "Account created successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(this, DashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }

        loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}