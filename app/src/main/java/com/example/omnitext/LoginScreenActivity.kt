package com.example.omnitext

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginScreenActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_screen)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val usernameInput = etUsername.text.toString().trim()
            val password = etPassword.text.toString()

            if (usernameInput.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Inserisci i dati", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "Verifica in corso..."

            db.collection("Utenti")
                .whereEqualTo("Username", usernameInput)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Toast.makeText(this, "Utente non trovato", Toast.LENGTH_SHORT).show()
                        btnLogin.isEnabled = true
                        btnLogin.text = "ACCEDI"
                        return@addOnSuccessListener
                    }

                    val phone = documents.documents[0].getString("Telefono")
                    val fakeEmail = "$phone@omnitext.com"

                    auth.signInWithEmailAndPassword(fakeEmail, password)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Accesso eseguito!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, ChatHomepageActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener {
                            btnLogin.isEnabled = true
                            btnLogin.text = "ACCEDI"
                            Toast.makeText(this, "Password errata", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    btnLogin.isEnabled = true
                    btnLogin.text = "ACCEDI"
                    Toast.makeText(this, "Errore di connessione", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("ImpostazioniTema", Context.MODE_PRIVATE)
        val colSfondo = prefs.getInt("color_sfondo", "#1D4682".toColorInt())
        val colScritte = prefs.getInt("color_scritte", Color.WHITE)
        val colInviati = prefs.getInt("color_inviati", "#FFD400".toColorInt())

        // Sfondo schermata
        findViewById<View>(R.id.main)?.setBackgroundColor(colSfondo)

        // Il LinearLayout interno ha background="#1D4682" hardcoded nel layout — lo sovrascriviamo
        findViewById<View>(R.id.main)
            ?.let { root -> (root as? android.view.ViewGroup)?.getChildAt(0) }
            ?.setBackgroundColor(colSfondo)

        // Pulsante LOGIN: colore inviati come sfondo, sfondo come testo (contrasto)
        findViewById<Button>(R.id.btnLogin)?.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(colInviati)
            setTextColor(colSfondo)
        }

        // Link registrazione
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSignUp)
            ?.setTextColor(colInviati)
    }
}