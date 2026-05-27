package com.example.omnitext

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddContactActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_contact)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val addContactToolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.addContactToolbar)
        setSupportActionBar(addContactToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        addContactToolbar.setNavigationOnClickListener {
            finish()
        }

        val etPhone = findViewById<TextInputEditText>(R.id.etPhoneToAdd)
        val btnAdd = findViewById<MaterialButton>(R.id.btnAddContact)

        btnAdd.setOnClickListener {
            val phoneInput = etPhone.text.toString().trim()
            if (phoneInput.isNotEmpty()) {
                cercaContattoEAvviaChat(phoneInput)
            } else {
                Toast.makeText(this, "Inserisci un numero valido", Toast.LENGTH_SHORT).show()
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

        // Toolbar: sfondo + icona freccia
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.addContactToolbar)
        toolbar?.setBackgroundColor(colSfondo)
        toolbar?.navigationIcon?.setTint(colScritte)

        // Titoli
        findViewById<TextView>(R.id.tvAddTitle)?.setTextColor(colScritte)
        findViewById<TextView>(R.id.tvAddSub)?.setTextColor(colScritte).also {
            findViewById<TextView>(R.id.tvAddSub)?.alpha = 0.7f
        }

        // Pulsante aggiungi: colore inviati come sfondo, sfondo come testo
        findViewById<MaterialButton>(R.id.btnAddContact)?.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(colInviati)
            setTextColor(colSfondo)
        }
    }

    private fun cercaContattoEAvviaChat(phoneInput: String) {
        val mioUid = auth.currentUser?.uid ?: return

        db.collection("Utenti")
            .whereEqualTo("Telefono", phoneInput)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot == null || snapshot.isEmpty) {
                    Toast.makeText(this, "Contatto non trovato", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val uDoc = snapshot.documents[0]
                val altroUid = uDoc.id
                val nomeAltroUtente = uDoc.getString("Username") ?: "Utente"

                if (altroUid == mioUid) {
                    Toast.makeText(this, "Non puoi aggiungere te stesso", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                creaStanzaChat(mioUid, altroUid, nomeAltroUtente)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Errore nella ricerca", Toast.LENGTH_SHORT).show()
            }
    }

    private fun creaStanzaChat(mioUid: String, altroUid: String, nomeAltroUtente: String) {
        val list = listOf(mioUid, altroUid).sorted()
        val chatRoomId = "${list[0]}_${list[1]}"

        val chatData = hashMapOf(
            "Partecipanti" to list,
            "timestampOrdinamento" to System.currentTimeMillis(),
            "messagges" to hashMapOf(
                "testo" to "Inizia a chattare!",
                "mittente" to ""
            )
        )

        db.collection("ChatRooms").document(chatRoomId)
            .set(chatData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Chat avviata!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, SingleChatActivity::class.java).apply {
                    putExtra("CHAT_ID", chatRoomId)
                    putExtra("OTHER_USER_NAME", nomeAltroUtente)
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Errore nel creare la chat", Toast.LENGTH_SHORT).show()
            }
    }
}