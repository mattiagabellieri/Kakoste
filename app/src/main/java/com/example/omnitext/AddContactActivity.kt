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

        // Configura la Toolbar e attiva la freccia indietro
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

    private fun cercaContattoEAvviaChat(phoneInput: String) {
        val mioUid = auth.currentUser?.uid ?: return

        // IMPORTANTE: Controlla sulla console Firebase se la collezione si chiama "Utenti" o "Users"
        // e se il campo del telefono si chiama "Telefono", "phone" o "telefono".
        db.collection("Utenti") // <-- Sostituito "Users" con "Utenti" (allineato con il resto del progetto)
            .whereEqualTo("Telefono", phoneInput) // <-- Spesso salvato con la T maiuscola nel SignUp
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot == null || snapshot.isEmpty) {
                    Toast.makeText(this, "Contatto non trovato", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val uDoc = snapshot.documents[0]
                val altroUid = uDoc.id

                // Recuperiamo lo Username dell'altro utente per passarlo alla schermata di chat
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
        // Ordiniamo gli UID e creiamo l'ID unendoli con l'underscore '_' anziché con la virgola
        val list = listOf(mioUid, altroUid).sorted()
        val chatRoomId = "${list[0]}_${list[1]}"

        val chatData = hashMapOf(
            "Partecipanti" to list,
            "timestampOrdinamento" to System.currentTimeMillis(), // Utile per ordinare le chat in Home
            "messagges" to hashMapOf(
                "testo" to "Inizia a chattare!",
                "mittente" to ""
            )
        )

        db.collection("ChatRooms").document(chatRoomId)
            .set(chatData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Chat avviata!", Toast.LENGTH_SHORT).show()

                // Novità: Apriamo direttamente la chat appena creata passando i dati corretti
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