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

    // --- FUNZIONI DI SUPPORTO PER IL CIFRARIO DI CESARE ---
    private val CHIAVE_CIFRARIO = 3

    private fun cifraTesto(testo: String): String {
        return testo.map { carattere ->
            when {
                carattere.isUpperCase() -> {
                    ((carattere.code - 'A'.code + CHIAVE_CIFRARIO) % 26 + 'A'.code).toChar()
                }
                carattere.isLowerCase() -> {
                    ((carattere.code - 'a'.code + CHIAVE_CIFRARIO) % 26 + 'a'.code).toChar()
                }
                else -> carattere // Mantiene inalterati spazi, numeri o punteggiatura
            }
        }.joinToString("")
    }

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
                else -> carattere
            }
        }.joinToString("")
    }
    // -----------------------------------------------------

    private fun inviaMessaggioSuFirestore(testo: String) {
        val mioUid = auth.currentUser?.uid ?: return

        // 1. CIFRIAMO IL TESTO prima di inviarlo a Firestore
        val testoCifrato = cifraTesto(testo)

        val infoMessaggio = hashMapOf(
            "mittente" to mioUid,
            "testo" to testoCifrato, // Invia la stringa criptata
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
                        "testo" to testoCifrato // Mantiene criptata anche l'anteprima nel database
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
                    val data = doc.data?.toMutableMap() // Trasformiamo in mappa modificabile
                    if (data != null) {
                        val testoCriptatoSalvato = data["testo"]?.toString() ?: ""

                        // 2. DECIFRIAMO IL TESTO per renderlo visibile nella RecyclerView dell'app
                        data["testo"] = decifraTesto(testoCriptatoSalvato)
                        messageList.add(data)
                    }
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