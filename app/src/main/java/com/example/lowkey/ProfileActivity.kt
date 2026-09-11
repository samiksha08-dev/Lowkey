package com.example.lowkey

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_profile)

        val profileName =
            findViewById<TextView>(R.id.tvProfileName)

        val profileEmail =
            findViewById<TextView>(R.id.tvProfileEmail)

        val detailName =
            findViewById<TextView>(R.id.tvDetailName)

        val detailEmail =
            findViewById<TextView>(R.id.tvDetailEmail)

        val homeTab =
            findViewById<TextView>(R.id.profileNavHome)

        val pinRow =
            findViewById<LinearLayout>(R.id.rowPin)

        val biometricRow =
            findViewById<LinearLayout>(R.id.rowBiometric)

        val editProfile =
            findViewById<Button>(R.id.btnEditProfile)

        val changePassword =
            findViewById<Button>(R.id.btnChangePassword)

        val logout =
            findViewById<Button>(R.id.btnLogout)

        val preferences = getSharedPreferences(
            "LowkeyUser",
            MODE_PRIVATE
        )

        val name =
            preferences.getString("name", "User")

        val email =
            preferences.getString("email", "Not available")

        profileName.text = name
        profileEmail.text = email

        detailName.text = name
        detailEmail.text = email

        // HOME
        homeTab.setOnClickListener {

            startActivity(
                Intent(this, DashboardActivity::class.java)
            )

            finish()
        }

        // EDIT PROFILE
        editProfile.setOnClickListener {

            startActivity(
                Intent(this, EditProfileActivity::class.java)
            )
        }

        // PIN
        pinRow.setOnClickListener {

            startActivity(
                Intent(this, PinActivity::class.java)
            )
        }

        // BIOMETRIC
        biometricRow.setOnClickListener {

            startActivity(
                Intent(this, BiometricActivity::class.java)
            )
        }

        // CHANGE PASSWORD
        changePassword.setOnClickListener {

            startActivity(
                Intent(this, ChangePasswordActivity::class.java)
            )
        }

        // LOG OUT
        logout.setOnClickListener {

            preferences.edit().clear().apply()

            val intent = Intent(
                this,
                MainActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
        }
    }
}