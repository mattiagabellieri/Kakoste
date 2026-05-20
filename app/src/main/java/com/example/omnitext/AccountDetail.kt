package com.example.omnitext

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountDetail : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var etUsername: TextInputEditText
    private lateinit var etPhone: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_account_detail)

        // 1. TROVA LA TOOLBAR DALL'XML E IMPOSTALA COME ACTION BAR
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // 2. ABILITA LA FRECCIA INDIETRO NATIVA BIANCA
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = ""
        toolbar.navigationIcon?.setTint(android.graphics.Color.WHITE)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etUsername = findViewById(R.id.etSettingsUsername)
        etPhone = findViewById(R.id.etSettingsPhone)
        val btnSave = findViewById<MaterialButton>(R.id.btnSaveProfile)

        recuperaDatiProfilo()

        // Salva il nuovo Username su Firestore
        btnSave.setOnClickListener {
            val nuovoUsername = etUsername.text.toString().trim()
            if (nuovoUsername.isNotEmpty()) {
                aggiornaUsername(nuovoNome = nuovoUsername)
            } else {
                Toast.makeText(this, "Il nome utente non può essere vuoto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // GESTISCE IL CLIC SULLA FRECCIA TORNANDO ALLA CHAT HOMEPAGE
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun recuperaDatiProfilo() {
        val mioUid = auth.currentUser?.uid ?: return

        db.collection("Utenti").document(mioUid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val usernameCorrente = document.getString("Username") ?: ""
                    val telefonoCorrente = document.getString("Telefono") ?: ""

                    etUsername.setText(usernameCorrente)
                    etPhone.setText(telefonoCorrente)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Impossibile caricare i dati del profilo", Toast.LENGTH_SHORT).show()
            }
    }

    private fun aggiornaUsername(nuovoNome: String) {
        val mioUid = auth.currentUser?.uid ?: return

        db.collection("Utenti").document(mioUid)
            .update("Username", nuovoNome)
            .addOnSuccessListener {
                Toast.makeText(this, "Profilo aggiornato con successo!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Errore durante il salvataggio: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }
}