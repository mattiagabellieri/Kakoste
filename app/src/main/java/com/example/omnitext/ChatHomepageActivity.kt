package com.example.omnitext

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatHomepageActivity : AppCompatActivity() {

    private lateinit var adapter: ChatAdapter
    private val chatList = mutableListOf<ChatModel>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat_homepage)

        val rvChatList = findViewById<RecyclerView>(R.id.rvChatList)
        val fabNewChat = findViewById<FloatingActionButton>(R.id.fabNewChat)
        val ibSettings = findViewById<ImageButton>(R.id.ibSettings)

        // Configura RecyclerView
        rvChatList.layoutManager = LinearLayoutManager(this)
        adapter = ChatAdapter(chatList)
        rvChatList.adapter = adapter

        fabNewChat.setOnClickListener {
            startActivity(Intent(this, AddContactActivity::class.java))
        }

        // AGGIUNTO: Click per andare alla modifica profilo
        ibSettings.setOnClickListener {
            startActivity(Intent(this, AccountDetail::class.java))
        }

        caricaChatDalloUsername()
    }

    // --- FUNZIONE DI SUPPORTO PER IL CIFRARIO DI CESARE ---
    private val CHIAVE_CIFRARIO = 3

    private fun decifraTesto(testoCifrato: String): String {
        return testoCifrato.map { carattere ->
            when {
                carattere.isUpperCase() -> {
                    var nuovoCodice = (carattere.code - 'A'.code - CHIAVE_CIFRARIO) % 26
                    if (nuovoCodice < 0) nuovoCodice += 26
                    (nuovoCodice + 'A'.code).toChar()
                }
                carattere.isLowerCase() -> {
                    var nuovoCodice = (carattere.code - 'a'.code - CHIAVE_CIFRARIO) % 26
                    if (nuovoCodice < 0) nuovoCodice += 26
                    (nuovoCodice + 'a'.code).toChar()
                }
                else -> carattere // Mantiene inalterati spazi, numeri o punteggiatura
            }
        }.joinToString("")
    }
    // -----------------------------------------------------

    private fun caricaChatDalloUsername() {
        val mioUid = auth.currentUser?.uid ?: return

        db.collection("ChatRooms")
            .whereArrayContains("Partecipanti", mioUid)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                chatList.clear()
                value?.documents?.forEach { doc ->
                    val partecipanti = doc.get("Partecipanti") as? List<String>
                    val altroUid = partecipanti?.find { it != mioUid } ?: ""

                    val lastMsgMap = doc.get("messagges") as? Map<String, Any>
                    val testoLastMsgCifrato = lastMsgMap?.get("testo")?.toString() ?: "Nessun messaggio"

                    // DECIFRAZIONE DELL'ANTEPRIMA
                    // Se c'è un messaggio reale lo decifriamo, altrimenti manteniamo la scritta standard "Nessun messaggio"
                    val testoLastMsgDecifrato = if (testoLastMsgCifrato != "Nessun messaggio") {
                        decifraTesto(testoLastMsgCifrato)
                    } else {
                        testoLastMsgCifrato
                    }

                    db.collection("Utenti").document(altroUid).get().addOnSuccessListener { uDoc ->
                        val nome = uDoc.getString("Username") ?: "Utente"

                        // Passiamo il testo decifrato al modello della lista
                        chatList.add(ChatModel(doc.id, testoLastMsgDecifrato, altroUid, nome))
                        adapter.notifyDataSetChanged()
                    }
                }
            }
    }
}