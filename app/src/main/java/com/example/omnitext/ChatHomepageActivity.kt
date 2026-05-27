package com.example.omnitext

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
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

        rvChatList.layoutManager = LinearLayoutManager(this)
        adapter = ChatAdapter(chatList)
        rvChatList.adapter = adapter

        fabNewChat.setOnClickListener {
            startActivity(Intent(this, AddContactActivity::class.java))
        }

        ibSettings.setOnClickListener {
            startActivity(Intent(this, AccountDetail::class.java))
        }

        ascoltaChatInTempoReale()
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("ImpostazioniTema", Context.MODE_PRIVATE)
        val colSfondo = prefs.getInt("color_sfondo", "#1D4682".toColorInt())
        val colScritte = prefs.getInt("color_scritte", Color.WHITE)
        val colInviati = prefs.getInt("color_inviati", "#FFD400".toColorInt())

        // Sfondo schermata
        findViewById<View>(R.id.main)?.setBackgroundColor(colSfondo)

        // Titolo "Chat"
        findViewById<android.widget.TextView>(R.id.tvTitle)?.setTextColor(colScritte)

        // Icona impostazioni (ingranaggio)
        findViewById<ImageButton>(R.id.ibSettings)?.apply {
            setColorFilter(colScritte)
        }

        // FAB: sfondo = colore inviati, icona = sfondo per contrasto
        findViewById<FloatingActionButton>(R.id.fabNewChat)?.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(colInviati)
            colorFilter = android.graphics.PorterDuffColorFilter(colSfondo, android.graphics.PorterDuff.Mode.SRC_IN)
        }

        // Logo tinted con le scritte (opzionale, rimane visibile)
        // Non tocchiamo l'ImageView del logo per non alterare il branding

        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }

    private fun decifraTesto(testoCifrato: String): String {
        return try {
            CesareCipher.decifra(testoCifrato)
        } catch (e: Exception) {
            testoCifrato
        }
    }

    private fun ascoltaChatInTempoReale() {
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

                    val testoLastMsgDecifrato = if (testoLastMsgCifrato != "Nessun messaggio") {
                        decifraTesto(testoLastMsgCifrato)
                    } else {
                        testoLastMsgCifrato
                    }

                    db.collection("Utenti").document(altroUid).get().addOnSuccessListener { uDoc ->
                        val nome = uDoc.getString("Username") ?: "Utente"
                        chatList.add(ChatModel(doc.id, testoLastMsgDecifrato, altroUid, nome))
                        adapter.notifyDataSetChanged()
                    }
                }
            }
    }
}