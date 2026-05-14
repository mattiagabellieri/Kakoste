package com.example.omnitext

import android.content.Intent
import android.os.Bundle
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

        // Configura RecyclerView
        rvChatList.layoutManager = LinearLayoutManager(this)
        adapter = ChatAdapter(chatList)
        rvChatList.adapter = adapter

        fabNewChat.setOnClickListener {
            startActivity(Intent(this, AddContactActivity::class.java))
        }

        caricaChatDalloUsername()
    }

    private fun caricaChatDalloUsername() {
        val mioUid = auth.currentUser?.uid ?: return

        // Ascolta in tempo reale le chat dove l'utente è partecipante
        db.collection("ChatRooms")
            .whereArrayContains("Partecipanti", mioUid)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                chatList.clear()
                value?.documents?.forEach { doc ->
                    val partecipanti = doc.get("Partecipanti") as? List<String>
                    val altroUid = partecipanti?.find { it != mioUid } ?: ""

                    // Recuperiamo l'ultimo messaggio dal campo 'messagges' (con due 's' come nel tuo DB)
                    val lastMsgMap = doc.get("messagges") as? Map<String, Any>
                    val testoLastMsg = lastMsgMap?.get("testo")?.toString() ?: "Nessun messaggio"

                    // Qui dovremmo recuperare il nome dell'altro utente dalla collezione "Utenti"
                    db.collection("Utenti").document(altroUid).get().addOnSuccessListener { uDoc ->
                        val nome = uDoc.getString("Username") ?: "Utente"
                        chatList.add(ChatModel(doc.id, testoLastMsg, altroUid, nome))
                        adapter.notifyDataSetChanged()
                    }
                }
            }
    }
}