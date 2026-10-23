package com.example.lowkey

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.lowkey.tap.TapDatabaseHelper
import com.example.lowkey.tap.TapEvent

class MainActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val tapDatabase = TapDatabaseHelper(this)

        val loginButton = findViewById<Button>(R.id.btnLogin)
        val signupButton = findViewById<Button>(R.id.btnSignup)

        loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        signupButton.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }
    private fun saveTap(
        x: Float,
        y: Float,
        durationMs: Long
    ) {
        val tap = TapEvent(
            timestamp = System.currentTimeMillis(),
            xPosition = x,
            yPosition = y,
            durationMs = durationMs
        )

        TapDatabaseHelper(this).insertTap(tap)
    }
}