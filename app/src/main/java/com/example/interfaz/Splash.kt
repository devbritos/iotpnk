package com.example.interfaz

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
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val isLoggedIn = prefs.getBoolean("is_logged_in", false)

            val nextActivity = if (isLoggedIn) MenuPrincipalActivity::class.java else LoginActivity::class.java
            val intent = Intent(this, nextActivity)
            startActivity(intent)
            finish()
        }, 1500)
    }
}
