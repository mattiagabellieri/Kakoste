package com.example.omnitext

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.example.omnitext.R // IMPORTANTE: questo collega l'XML al Codice

class signup_screen_activity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_screen)

        db = FirebaseFirestore.getInstance()

        // Se questi rimangono rossi, l'errore è nell'XML (punto 1)
        val etUsername = findViewById<EditText>(R.id.etNewUsername)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etPassword = findViewById<EditText>(R.id.etNewPassword)
        val etConfPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnSignUp = findViewById<Button>(R.id.btnDoSignUp)

        btnSignUp.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val conf = etConfPassword.text.toString().trim()

            if (username.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Compila tutti i campi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != conf) {
                Toast.makeText(this, "Le password non coincidono", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSignUp.isEnabled = false
            btnSignUp.text = "Caricamento..."

            db.collection("Utenti").get().addOnSuccessListener { snapshot ->
                val idQuattroCifre = String.format("%04d", snapshot.size() + 1)
                val nomeDoc = "${username.replace(" ", "_")}_$idQuattroCifre"

                val userMap = hashMapOf(
                    "Username" to username,
                    "Telefono" to phone,
                    "Password" to password,
                    "ID_Personale" to idQuattroCifre
                )

                db.collection("Utenti").document(nomeDoc).set(userMap)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Registrato: $nomeDoc", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        btnSignUp.isEnabled = true
                        btnSignUp.text = "REGISTRATI"
                        Toast.makeText(this, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }.addOnFailureListener {
                btnSignUp.isEnabled = true
                Toast.makeText(this, "Errore database", Toast.LENGTH_SHORT).show()
            }
        }
    }
}