package com.example.omnitext

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnSignUp = findViewById<MaterialButton>(R.id.btnSignUp)

        btnLogin.setOnClickListener {
            val usernameInput = etUsername.text.toString().trim()
            val password = etPassword.text.toString()

            if (usernameInput.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Inserisci i dati", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "Verifica in corso..."

            // 1. Cerchiamo il numero di telefono associato allo username su Firestore
            db.collection("Utenti")
                .whereEqualTo("Username", usernameInput)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Toast.makeText(this, "Utente non trovato", Toast.LENGTH_SHORT).show()
                        btnLogin.isEnabled = true
                        return@addOnSuccessListener
                    }

                    // 2. Trovato lo username, recuperiamo il telefono per fare il login
                    val phone = documents.documents[0].getString("Telefono")
                    val fakeEmail = "$phone@omnitext.com"

                    // 3. Eseguiamo il login vero e proprio con Authenticator
                    auth.signInWithEmailAndPassword(fakeEmail, password)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Accesso eseguito!", Toast.LENGTH_SHORT).show()
                            // Vai alla Homepage
                            // startActivity(Intent(this, HomeActivity::class.java))
                        }
                        .addOnFailureListener {
                            btnLogin.isEnabled = true
                            btnLogin.text = "ACCEDI"
                            Toast.makeText(this, "Password errata", Toast.LENGTH_SHORT).show()
                        }
                }
        }

        btnSignUp.setOnClickListener {
            startActivity(Intent(this, signup_screen_activity::class.java))
        }
    }
}