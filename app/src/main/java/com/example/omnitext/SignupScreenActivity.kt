package com.example.omnitext

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class signup_screen_activity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_screen)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // INIZIALIZZAZIONE DEI COMPONENTI DALL'XML
        val etUsername = findViewById<EditText>(R.id.etNewUsername)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etPassword = findViewById<EditText>(R.id.etNewPassword)
        val etConfPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnSignUp = findViewById<Button>(R.id.btnDoSignUp)

        // CORREZIONE: Inizializziamo il pulsante di ritorno che causava l'errore
        val btnBackToLogin = findViewById<MaterialButton>(R.id.btnBackToLogin)

        // AZIONE SUL PULSANTE "REGISTRATI"
        btnSignUp.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confPassword = etConfPassword.text.toString().trim()

            // Validazione dei campi obbligatori
            if (username.isEmpty() || phone.isEmpty() || password.isEmpty() || confPassword.isEmpty()) {
                Toast.makeText(this, "Compila tutti i campi richiesti", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confPassword) {
                Toast.makeText(this, "Le password inserite non coincidono", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "La password deve contenere almeno 6 caratteri", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSignUp.isEnabled = false
            btnSignUp.text = "Creazione account..."

            val fakeEmail = "$phone@omnitext.com"

            auth.createUserWithEmailAndPassword(fakeEmail, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: ""

                    db.collection("Utenti").get().addOnSuccessListener { snapshot ->
                        val idQuattroCifre = String.format("%04d", snapshot.size() + 1)

                        val userMap = hashMapOf(
                            "Username" to username,
                            "Telefono" to phone,
                            "ID_Personale" to idQuattroCifre,
                            "UID" to uid
                        )

                        db.collection("Utenti").document(uid).set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Registrazione completata!", Toast.LENGTH_LONG).show()
                                finish()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    btnSignUp.isEnabled = true
                    btnSignUp.text = "REGISTRATI"
                    Toast.makeText(this, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // AZIONE PER TORNARE INDIETRO AL LOGIN SCREEN
        btnBackToLogin.setOnClickListener {
            val intent = Intent(this, LoginScreenActivity::class.java)
            startActivity(intent)
            finish() // Chiude la registrazione così non rimane aperta nel background
        }
    }
}