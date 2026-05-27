package com.example.omnitext

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
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

        val mainView = findViewById<android.view.View>(R.id.mainChatLayout)

        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                if (imeInsets.bottom > 0) imeInsets.bottom else systemBars.bottom
            )
            WindowInsetsCompat.CONSUMED
        }

        chatRoomId = intent.getStringArrayExtra("CHAT_ID")?.joinToString(",")
            ?: intent.getStringExtra("CHAT_ID")
                    ?: ""
        otherUserName = intent.getStringExtra("OTHER_USER_NAME") ?: "Utente"

        val tvChatUserTitle = findViewById<TextView>(R.id.tvChatUserTitle)
        tvChatUserTitle.text = otherUserName

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.chatToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false) // nasconde "Omnitext" di default
        toolbar.setNavigationOnClickListener { finish() }

        val rvMessages = findViewById<RecyclerView>(R.id.rvMessages)
        rvMessages.layoutManager = LinearLayoutManager(this)
        messageAdapter = MessageAdapter(messageList)
        rvMessages.adapter = messageAdapter

        applicaTemaDinamico()

        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSendMessage = findViewById<ImageButton>(R.id.btnSendMessage)

        btnSendMessage.setOnClickListener {
            val testo = etMessage.text.toString().trim()
            if (testo.isNotEmpty()) {
                inviaMessaggioSuFirestore(testo)
                etMessage.text.clear()
            }
        }

        ascoltaMessaggiInTempoReale()
    }

    private fun applicaTemaDinamico() {
        val prefs = getSharedPreferences("ImpostazioniTema", Context.MODE_PRIVATE)
        val colSfondoApp = prefs.getInt("color_sfondo", "#1D4682".toColorInt())
        val colScritte = prefs.getInt("color_scritte", Color.WHITE)
        val colInviati = prefs.getInt("color_inviati", "#FFD400".toColorInt())

        // 1. Sfondo globale
        findViewById<android.view.View>(R.id.mainChatLayout)?.setBackgroundColor(colSfondoApp)

        // 2. Toolbar: sfondo + freccia indietro + titolo utente
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.chatToolbar)
        toolbar?.setBackgroundColor(colSfondoApp)
        toolbar?.navigationIcon?.setTint(colScritte)

        val tvChatUserTitle = findViewById<TextView>(R.id.tvChatUserTitle)
        tvChatUserTitle?.setTextColor(colScritte)

        // 3. Pulsante invia: usa colore inviati per risaltare
        val btnSendMessage = findViewById<ImageButton>(R.id.btnSendMessage)
        btnSendMessage?.setColorFilter(colInviati)

        // 4. Campo testo: colore sfondo come testo (sarà su card chiara) + hint coordinato
        val etMessage = findViewById<EditText>(R.id.etMessage)
        etMessage?.setTextColor(colSfondoApp)
        etMessage?.setHintTextColor(Color.argb(150,
            Color.red(colSfondoApp), Color.green(colSfondoApp), Color.blue(colSfondoApp)))

        // 5. InputCard (MaterialCardView contenitore della barra di testo):
        //    sfondo bianco semi-trasparente coordinato, bordo del colore inviati
        val inputCard = findViewById<MaterialCardView>(R.id.inputCard)
        inputCard?.apply {
            setCardBackgroundColor(Color.WHITE)
            strokeColor = colInviati
            strokeWidth = 2
        }
    }

    private fun cifraTesto(testoChiaro: String): String {
        return testoChiaro.map { (it.code + 3).toChar() }.joinToString("")
    }

    private fun decifraTesto(testoCifrato: String): String {
        return testoCifrato.map { (it.code - 3).toChar() }.joinToString("")
    }

    private fun inviaMessaggioSuFirestore(testoChiaro: String) {
        if (chatRoomId.isEmpty()) return

        val mioUid = auth.currentUser?.uid ?: return
        val testoCriptato = cifraTesto(testoChiaro)

        val messageData = hashMapOf(
            "testo" to testoCriptato,
            "mittente" to mioUid,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("ChatRooms")
            .document(chatRoomId)
            .collection("Messages")
            .add(messageData)
            .addOnSuccessListener {
                val updateUltimoMsg = hashMapOf<String, Any>(
                    "timestampOrdinamento" to System.currentTimeMillis(),
                    "messagges" to hashMapOf(
                        "testo" to testoCriptato,
                        "mittente" to mioUid
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
                    val data = doc.data?.toMutableMap()
                    if (data != null) {
                        val testoCriptatoSalvato = data["testo"]?.toString() ?: ""
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