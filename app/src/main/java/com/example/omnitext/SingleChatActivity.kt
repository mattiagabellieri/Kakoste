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

        // VISTA PRINCIPALE PER IL CONTROLLO DELLA TASTIERA
        val mainView = findViewById<android.view.View>(R.id.mainChatLayout)

        // CORREZIONE EDGE-TO-EDGE + COMPORTAMENTO TASTIERA (IME)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeBars = insets.getInsets(WindowInsetsCompat.Type.ime()) // Gestisce la tastiera

            // Se la tastiera è aperta usa il suo ingombro, altrimenti usa la barra di sistema in basso
            val bottomPadding = if (imeBars.bottom > 0) imeBars.bottom else systemBars.bottom

            // Impostiamo il padding dinamico alla vista principale
            v.setPadding(0, systemBars.top, 0, bottomPadding)
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

        // Configura il tasto "Indietro" nella Toolbar
        setSupportActionBar(chatToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        chatToolbar.setNavigationOnClickListener {
            finish()
        }

        tvChatUserTitle.text = otherUserName

        // 3. Configura la RecyclerView dei messaggi
        messageAdapter = MessageAdapter(messageList)
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // Mantiene i messaggi ancorati al fondo
        }
        rvMessages.layoutManager = layoutManager
        rvMessages.adapter = messageAdapter

        // SCROLL AUTOMATICO QUANDO IL LAYOUT SI STRINGE (Apertura della tastiera)
        rvMessages.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                if (messageList.isNotEmpty()) {
                    rvMessages.postDelayed({
                        rvMessages.smoothScrollToPosition(messageList.size - 1)
                    }, 100)
                }
            }
        }

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