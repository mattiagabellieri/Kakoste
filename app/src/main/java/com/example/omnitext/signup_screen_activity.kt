package com.example.omnitext

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class signup_screen_activity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup_screen)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etUsername = findViewById<EditText>(R.id.etNewUsername)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etPassword = findViewById<EditText>(R.id.etNewPassword)
        val etConfPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnSignUp = findViewById<Button>(R.id.btnDoSignUp)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnSignUp.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confPassword = etConfPassword.text.toString().trim()

            if (username.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Tutti i campi sono obbligatori!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. Controlliamo quanti utenti esistono già per generare l'ID
            db.collection("Utenti").get().addOnSuccessListener { snapshot ->
                val numeroUtenti = snapshot.size() // Conta i documenti esistenti
                val prossimoNumero = numeroUtenti + 1

                // Formatta il numero a 4 cifre (es: 1 diventa 0001)
                val idQuattroCifre = String.format("%04d", prossimoNumero)

                // 2. Prepariamo i dati con il nuovo ID
                val userMap = hashMapOf(
                    "Username" to username,
                    "Numero" to phone,
                    "Password" to password,
                    "ID" to idQuattroCifre, // Il tuo ID 0001, 0002...
                )

                // 3. Salvataggio su Firestore
                db.collection("Utenti")
                    .document(phone) // Usiamo il telefono come chiave del documento
                    .set(userMap)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Registrato! Il tuo ID è $idQuattroCifre", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}