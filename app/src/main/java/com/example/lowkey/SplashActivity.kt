package com.example.lowkey

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({

            val preferences = getSharedPreferences(
                "LowkeyUser",
                MODE_PRIVATE
            )

            val isLoggedIn = preferences.getBoolean(
                "isLoggedIn",
                false
            )

            val nextScreen = if (isLoggedIn) {
                DashboardActivity::class.java
            } else {
                MainActivity::class.java
            }

            startActivity(
                Intent(this, nextScreen)
            )

            finish()

        }, 1500)
    }
}