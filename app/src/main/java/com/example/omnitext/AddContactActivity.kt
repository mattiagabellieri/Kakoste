package com.example.omnitext

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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

        // Gestione margini di sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // AGGIUNTA: Configura la Toolbar e attiva la freccia indietro
        val addContactToolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.addContactToolbar)
        setSupportActionBar(addContactToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false) // Nasconde il titolo dell'app

        // Al click sulla freccia indietro, chiude l'attività e torna alla Home
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

    private fun cercaContattoEAvviaChat(phoneInput: String) {
        val mioUid = auth.currentUser?.uid ?: return

        db.collection("Users")
            .whereEqualTo("telefono", phoneInput)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Toast.makeText(this, "Contatto non trovato", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val altroUid = snapshot.documents[0].id
                if (altroUid == mioUid) {
                    Toast.makeText(this, "Non puoi aggiungere te stesso", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                creaStanzaChat(mioUid, altroUid)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Errore nella ricerca", Toast.LENGTH_SHORT).show()
            }
    }

    private fun creaStanzaChat(mioUid: String, altroUid: String) {
        val list = listOf(mioUid, altroUid).sorted()
        val chatRoomId = "${list[0]}, ${list[1]}"

        val chatData = hashMapOf(
            "Partecipanti" to list,
            "messagges" to hashMapOf(
                "testo" to "Inizia a chattare!",
                "mittenteID" to "",
                "ora" to 0,
                "minuto" to 0,
                "giorno" to 0,
                "mese" to 0,
                "anno" to 0
            )
        )

        db.collection("ChatRooms").document(chatRoomId)
            .set(chatData)
            .addOnSuccessListener {
                Toast.makeText(this, "Chat avviata!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Errore nel creare la chat", Toast.LENGTH_SHORT).show()
            }
    }
}