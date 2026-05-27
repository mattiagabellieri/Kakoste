package com.example.omnitext

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. CONTROLLO AUTO-LOGIN: Se l'utente è già loggato, va dritto alla Home delle Chat
        val utenteAttuale = FirebaseAuth.getInstance().currentUser
        if (utenteAttuale != null) {
            val intent = Intent(this, ChatHomepageActivity::class.java)
            startActivity(intent)
            finish() // Chiude la MainActivity per non farci tornare l'utente premendo "Indietro"
            return   // Blocca l'esecuzione del codice successivo
        }

        // 2. Se l'utente NON è loggato, carica normalmente la schermata principale
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

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("ImpostazioniTema", Context.MODE_PRIVATE)

        // Applica lo sfondo salvato (Default: #1A3B6D)
        val colSfondo = prefs.getInt("color_sfondo", "#1A3B6D".toColorInt())
        findViewById<View>(R.id.main)?.setBackgroundColor(colSfondo)
    }
}