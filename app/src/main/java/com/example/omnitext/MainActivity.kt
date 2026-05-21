package com.example.omnitext

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Colleghiamo il tasto ACCEDI dall'XML
        val btnVaiALogin = findViewById<Button>(R.id.btnVaiALogin)
        // Colleghiamo il tasto REGISTRATI dall'XML (diventato un Button standard)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)

        // Quando clicchi ACCEDI, ti porta alla LoginScreenActivity
        btnVaiALogin.setOnClickListener {
            val intent = Intent(this, LoginScreenActivity::class.java)
            startActivity(intent)
        }

        // Quando clicchi REGISTRATI, ti porta alla signup_screen_activity
        btnSignUp.setOnClickListener {
            val intent = Intent(this, signup_screen_activity::class.java)
            startActivity(intent)
        }
    }
}