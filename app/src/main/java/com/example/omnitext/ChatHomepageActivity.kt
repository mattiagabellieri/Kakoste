package com.example.omnitext

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class ChatHomepageActivity : AppCompatActivity() {

    private lateinit var adapter: ChatAdapter
    private val chatList = mutableListOf<ChatModel>()
    private val filteredChatList = mutableListOf<ChatModel>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat_homepage)

        val rvChatList = findViewById<RecyclerView>(R.id.rvChatList)
        val fabNewChat = findViewById<FloatingActionButton>(R.id.fabNewChat)
        val ibSettings = findViewById<ImageButton>(R.id.ibSettings)
        searchView = findViewById(R.id.searchView)

        rvChatList.layoutManager = LinearLayoutManager(this)
        adapter = ChatAdapter(filteredChatList)
        rvChatList.adapter = adapter

        fabNewChat.setOnClickListener {
            startActivity(Intent(this, AddContactActivity::class.java))
        }

        ibSettings.setOnClickListener {
            startActivity(Intent(this, AccountDetail::class.java))
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filtraChat(newText)
                return true
            }
        })

        ascoltaChatInTempoReale()
    }

    private fun filtraChat(testo: String?) {
        val query = testo?.lowercase(Locale.getDefault())?.trim() ?: ""
        filteredChatList.clear()

        if (query.isEmpty()) {
            filteredChatList.addAll(chatList)
        } else {
            for (chat in chatList) {
                if (chat.otherUserName.lowercase(Locale.getDefault()).contains(query) ||
                    chat.lastMessage.lowercase(Locale.getDefault()).contains(query)) {
                    filteredChatList.add(chat)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("ImpostazioniTema", Context.MODE_PRIVATE)
        val colSfondo = prefs.getInt("color_sfondo", "#1D4682".toColorInt())
        val colScritte = prefs.getInt("color_scritte", Color.WHITE)
        val colInviati = prefs.getInt("color_inviati", "#FFD400".toColorInt())

        // RECUPERO DEL COLORE DEI MESSAGGI RICEVUTI
        val colTestoRicevuti = prefs.getInt("color_testo_ricevuti", Color.BLACK)

        // Sfondo schermata
        findViewById<View>(R.id.main)?.setBackgroundColor(colSfondo)

        // Titolo "Chat"
        findViewById<android.widget.TextView>(R.id.tvTitle)?.setTextColor(colScritte)

        // Icona impostazioni
        findViewById<ImageButton>(R.id.ibSettings)?.apply {
            setColorFilter(colScritte)
        }

        // --- STILE E COLORAZIONE PERSONALIZZATA SEARCHVIEW ---
        searchView.findViewById<androidx.appcompat.widget.SearchView.SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)?.apply {
            setTextColor(colTestoRicevuti)
            setHintTextColor(Color.argb(130, Color.red(colTestoRicevuti), Color.green(colTestoRicevuti), Color.blue(colTestoRicevuti)))
            textSize = 15f
        }

        // Colora tutte le icone interne con lo stesso colore del testo ricevuto
        searchView.findViewById<android.widget.ImageView>(androidx.appcompat.R.id.search_button)?.setColorFilter(colTestoRicevuti)
        searchView.findViewById<android.widget.ImageView>(androidx.appcompat.R.id.search_mag_icon)?.setColorFilter(colTestoRicevuti)
        searchView.findViewById<android.widget.ImageView>(androidx.appcompat.R.id.search_close_btn)?.setColorFilter(colTestoRicevuti)

        // FAB
        findViewById<FloatingActionButton>(R.id.fabNewChat)?.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(colInviati)
            colorFilter = android.graphics.PorterDuffColorFilter(colSfondo, android.graphics.PorterDuff.Mode.SRC_IN)
        }

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
                val totaleDocumenti = value?.documents?.size ?: 0
                if (totaleDocumenti == 0) {
                    filteredChatList.clear()
                    adapter.notifyDataSetChanged()
                    return@addSnapshotListener
                }

                var documentiProcessati = 0

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

                    val isGruppo = doc.getBoolean("isGruppo") ?: false

                    val alTermineDellaQuery = { nomeOttenuto: String ->
                        chatList.add(ChatModel(doc.id, testoLastMsgDecifrato, altroUid, nomeOttenuto))
                        documentiProcessati++

                        if (documentiProcessati == totaleDocumenti) {
                            filtraChat(searchView.query?.toString())
                        }
                    }

                    if (isGruppo) {
                        val nomeGruppo = doc.getString("NomeGruppo") ?: "Gruppo"
                        alTermineDellaQuery(nomeGruppo)
                    } else {
                        db.collection("Utenti").document(altroUid).get().addOnSuccessListener { uDoc ->
                            val nome = uDoc.getString("Username") ?: "Utente"
                            alTermineDellaQuery(nome)
                        }.addOnFailureListener {
                            alTermineDellaQuery("Utente")
                        }
                    }
                }
            }
    }
}