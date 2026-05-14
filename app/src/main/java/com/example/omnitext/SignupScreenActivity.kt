package com.example.omnitext

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        val etUsername = findViewById<EditText>(R.id.etNewUsername)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etPassword = findViewById<EditText>(R.id.etNewPassword)
        val etConfPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnSignUp = findViewById<Button>(R.id.btnDoSignUp)

        btnSignUp.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // ... (validazione campi)

            btnSignUp.isEnabled = false
            btnSignUp.text = "Creazione account..."

            val fakeEmail = "$phone@omnitext.com"

            auth.createUserWithEmailAndPassword(fakeEmail, password)
                .addOnSuccessListener { authResult ->
                    // PRENDIAMO L'UID UNIVOCO
                    val uid = authResult.user?.uid ?: ""

                    // Generiamo comunque l'ID di 4 cifre per estetica/ricerca,
                    // ma il DOCUMENTO si chiamerà come l'UID
                    db.collection("Utenti").get().addOnSuccessListener { snapshot ->
                        val idQuattroCifre = String.format("%04d", snapshot.size() + 1)

                        val userMap = hashMapOf(
                            "Username" to username,
                            "Telefono" to phone,
                            "ID_Personale" to idQuattroCifre,
                            "UID" to uid // Lo teniamo anche dentro come campo per comodità
                        )

                        // USIAMO .document(uid) INVECE DI NOME_ID
                        db.collection("Utenti").document(uid).set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Registrazione completata!", Toast.LENGTH_LONG).show()
                                finish()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    btnSignUp.isEnabled = true
                    Toast.makeText(this, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}