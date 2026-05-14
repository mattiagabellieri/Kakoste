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

        // Gestione margini di sistema (evita il crash dell'ID 'main')
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val etPhone = findViewById<TextInputEditText>(R.id.etPhoneToAdd)
        val btnAdd = findViewById<MaterialButton>(R.id.btnAddContact)

        btnAdd.setOnClickListener {
            val phoneInput = etPhone.text.toString().trim()

            if (phoneInput.isEmpty()) {
                Toast.makeText(this, "Inserisci un numero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            cercaUtente(phoneInput)
        }
    }

    private fun cercaUtente(telefono: String) {
        val mioUid = auth.currentUser?.uid ?: return

        // 1. Cerchiamo l'utente con quel numero nella collezione "Utenti"
        db.collection("Utenti")
            .whereEqualTo("Telefono", telefono)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Utente non registrato su Omnitext", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // 2. Utente trovato! Prendiamo il suo UID
                val altroUid = documents.documents[0].id

                // Evitiamo di auto-aggiungerci
                if (mioUid == altroUid) {
                    Toast.makeText(this, "Non puoi chattare con te stesso!", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                creaStanzaChat(mioUid, altroUid)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Errore nella ricerca", Toast.LENGTH_SHORT).show()
            }
    }

    private fun creaStanzaChat(mioUid: String, altroUid: String) {
        // Generiamo un ID unico per la stanza (es: uid1_uid2 ordinati alfabeticamente)
        val list = listOf(mioUid, altroUid).sorted()
        val chatRoomId = "${list[0]}, ${list[1]}" // Usiamo la virgola come nel tuo database

        val chatData = hashMapOf(
            "Partecipanti" to list, // Lista dei due UID
            "messagges" to hashMapOf( // Nota: ho usato 'messagges' come nel tuo DB
                "testo" to "Inizia a chattare!",
                "mittenteID" to "",
                "ora" to 0,
                "minuto" to 0,
                "giorno" to 0,
                "mese" to 0,
                "anno" to 0
            )
        )

        // 3. Salviamo la stanza su Firestore nella collezione "ChatRooms"
        db.collection("ChatRooms").document(chatRoomId)
            .set(chatData)
            .addOnSuccessListener {
                Toast.makeText(this, "Chat avviata!", Toast.LENGTH_SHORT).show()
                finish() // Torna alla Homepage
            }
            .addOnFailureListener {
                Toast.makeText(this, "Errore nel creare la chat", Toast.LENGTH_SHORT).show()
            }
    }
}