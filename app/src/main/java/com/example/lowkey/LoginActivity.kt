package com.example.lowkey

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        // Input fields
        val email = findViewById<EditText>(R.id.etLoginEmail)
        val password = findViewById<EditText>(R.id.etLoginPassword)

        // Buttons / links
        val loginButton = findViewById<Button>(R.id.btnLoginSubmit)
        val signupLink = findViewById<TextView>(R.id.tvGoSignup)

        // -----------------------------
        // PASSWORD SHOW / HIDE
        // -----------------------------

        var passwordVisible = false

        password.setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_UP) {

                val drawableEnd = password.compoundDrawables[2]

                if (drawableEnd != null &&
                    event.x >= password.width - password.paddingEnd - drawableEnd.bounds.width()
                ) {

                    passwordVisible = !passwordVisible

                    val cursorPosition = password.selectionStart

                    if (passwordVisible) {

                        password.inputType =
                            InputType.TYPE_CLASS_TEXT or
                                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

                    } else {

                        password.inputType =
                            InputType.TYPE_CLASS_TEXT or
                                    InputType.TYPE_TEXT_VARIATION_PASSWORD
                    }

                    password.setSelection(
                        cursorPosition.coerceAtLeast(0)
                    )

                    true

                } else {
                    false
                }

            } else {
                false
            }
        }

        // -----------------------------
        // LOGIN BUTTON
        // -----------------------------

        loginButton.setOnClickListener {

            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString()

            // Empty email
            if (emailText.isEmpty()) {

                email.error = "Please enter your email"
                email.requestFocus()

                return@setOnClickListener
            }

            // Empty password
            if (passwordText.isEmpty()) {

                password.error = "Please enter your password"
                password.requestFocus()

                return@setOnClickListener
            }

            // Get saved signup information
            val preferences = getSharedPreferences(
                "LowkeyUser",
                MODE_PRIVATE
            )

            val savedEmail = preferences.getString("email", null)
            val savedPassword = preferences.getString("password", null)

            // Verify credentials
            if (emailText == savedEmail && passwordText == savedPassword) {

                Toast.makeText(
                    this,
                    "Login successful!",
                    Toast.LENGTH_SHORT
                ).show()

                preferences.edit()
                    .putBoolean("isLoggedIn", true)
                    .apply()

                val intent = Intent(
                    this,
                    DashboardActivity::class.java
                )

                startActivity(intent)

                finish()

            } else {

                Toast.makeText(
                    this,
                    "Incorrect email or password",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // -----------------------------
        // SIGNUP LINK
        // -----------------------------

        signupLink.setOnClickListener {

            val intent = Intent(
                this,
                SignupActivity::class.java
            )

            startActivity(intent)
        }
    }
}