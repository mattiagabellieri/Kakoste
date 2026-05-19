package com.example.omnitext

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SingleChatActivity : AppCompatActivity() {

    private lateinit var chatRoomId: String
    private lateinit var otherUserName: String
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val messageList = mutableListOf<Map<String, Any>>()
    private lateinit var messageAdapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_single_chat)

        // Margini Edge-to-Edge applicati alla vista principale
        val mainView = findViewById<android.view.View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            findViewById<androidx.appcompat.widget.Toolbar>(R.id.chatToolbar)
                .setPadding(0, systemBars.top, 0, 0)
            insets
        }

        // 1. Recuperiamo i dati passati dall'Adapter delle Chat
        chatRoomId = intent.getStringExtra("CHAT_ID") ?: ""
        otherUserName = intent.getStringExtra("OTHER_USER_NAME") ?: "Chat"

        // 2. Colleghiamo i componenti dell'XML
        val chatToolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.chatToolbar)
        val tvChatUserTitle = findViewById<TextView>(R.id.tvChatUserTitle)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSendMessage = findViewById<ImageButton>(R.id.btnSendMessage)
        val rvMessages = findViewById<RecyclerView>(R.id.rvMessages)

        // AGGIUNTA: Configura il tasto "Indietro" nella Toolbar
        setSupportActionBar(chatToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false) // Nasconde il titolo di default dell'app

        // Quando premi la freccia indietro, chiude questa Activity e torna alla Home
        chatToolbar.setNavigationOnClickListener {
            finish()
        }

        // Impostiamo il nome dell'utente in cima alla Toolbar
        tvChatUserTitle.text = otherUserName

        // 3. Configura la RecyclerView dei messaggi e collega l'Adapter funzionante
        messageAdapter = MessageAdapter(messageList)
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = messageAdapter

        // 4. Gestione del tasto INVIA messaggio
        btnSendMessage.setOnClickListener {
            val testoMessaggio = etMessage.text.toString().trim()
            if (testoMessaggio.isNotEmpty()) {
                inviaMessaggioSuFirestore(testoMessaggio)
                etMessage.setText("")
            }
        }

        ascoltaMessaggiInTempoReale()
    }

    private fun inviaMessaggioSuFirestore(testo: String) {
        val mioUid = auth.currentUser?.uid ?: return

        val infoMessaggio = hashMapOf(
            "mittente" to mioUid,
            "testo" to testo,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("ChatRooms")
            .document(chatRoomId)
            .collection("Messages")
            .add(infoMessaggio)
            .addOnSuccessListener {
                val updateUltimoMsg = mapOf(
                    "messagges" to mapOf(
                        "mittente" to mioUid,
                        "testo" to testo
                    )
                )
                db.collection("ChatRooms").document(chatRoomId).update(updateUltimoMsg)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Errore nell'invio", Toast.LENGTH_SHORT).show()
            }
    }

    private fun ascoltaMessaggiInTempoReale() {
        if (chatRoomId.isEmpty()) return

        db.collection("ChatRooms")
            .document(chatRoomId)
            .collection("Messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                messageList.clear()
                snapshot?.documents?.forEach { doc ->
                    doc.data?.let { messageList.add(it) }
                }

                if (::messageAdapter.isInitialized) {
                    messageAdapter.notifyDataSetChanged()
                    if (messageList.isNotEmpty()) {
                        findViewById<RecyclerView>(R.id.rvMessages).smoothScrollToPosition(messageList.size - 1)
                    }
                }
            }
    }
}