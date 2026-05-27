package com.example.omnitext

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

// Modello di supporto locale per visualizzare i contatti selezionabili
data class ContattoSelezionabile(
    val uid: String,
    val username: String,
    var isChecked: Boolean = false
)

class AddContactActivity : AppCompatActivity() {

    private lateinit var db: com.google.firebase.firestore.FirebaseFirestore
    private lateinit var auth: com.google.firebase.auth.FirebaseAuth

    private val listaContatti = mutableListOf<ContattoSelezionabile>()
    private lateinit var contattiAdapter: SelezionaContattiAdapter
    private lateinit var rvContatti: RecyclerView
    private lateinit var etNomeGruppo: TextInputEditText
    private lateinit var tilNomeGruppo: TextInputLayout
    private lateinit var tvSelezionaTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_contact)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        auth = com.google.firebase.auth.FirebaseAuth.getInstance()

        val addContactToolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.addContactToolbar)
        setSupportActionBar(addContactToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        addContactToolbar.setNavigationOnClickListener { finish() }

        val etPhone = findViewById<TextInputEditText>(R.id.etPhoneToAdd)
        val btnAdd = findViewById<MaterialButton>(R.id.btnAddContact)

        rvContatti = findViewById(R.id.rvContattiSelezionabili)
        etNomeGruppo = findViewById(R.id.etNomeGruppo)
        tilNomeGruppo = findViewById(R.id.tilNomeGruppo)
        tvSelezionaTitle = findViewById(R.id.tvSelezionaTitle)

        // Configura la lista dei contatti selezionabili
        rvContatti.layoutManager = LinearLayoutManager(this)
        contattiAdapter = SelezionaContattiAdapter(listaContatti) { controllaStatoSelezione() }
        rvContatti.adapter = contattiAdapter

        caricaContattiEsistenti()

        btnAdd.setOnClickListener {
            val utentiSelezionati = listaContatti.filter { it.isChecked }
            val phoneInput = etPhone.text.toString().trim()

            if (utentiSelezionati.isEmpty()) {
                if (phoneInput.isNotEmpty()) {
                    cercaContattoEAvviaChat(phoneInput)
                } else {
                    Toast.makeText(this, "Inserisci un numero o seleziona dei contatti per il gruppo", Toast.LENGTH_SHORT).show()
                }
            } else {
                val nomeGruppo = etNomeGruppo.text.toString().trim()
                if (nomeGruppo.isEmpty()) {
                    Toast.makeText(this, "Inserisci un nome per il gruppo", Toast.LENGTH_SHORT).show()
                } else {
                    creaGruppoMulticontatto(nomeGruppo, utentiSelezionati)
                }
            }
        }
    }

    private fun controllaStatoSelezione() {
        val quantiSelezionati = listaContatti.count { it.isChecked }
        if (quantiSelezionati > 0) {
            tilNomeGruppo.visibility = View.VISIBLE
            findViewById<MaterialButton>(R.id.btnAddContact)?.text = "CREA GRUPPO"
        } else {
            tilNomeGruppo.visibility = View.GONE
            findViewById<MaterialButton>(R.id.btnAddContact)?.text = "AGGIUNGI CONTATTO"
        }
    }

    private fun caricaContattiEsistenti() {
        val mioUid = auth.currentUser?.uid ?: return

        db.collection("ChatRooms")
            .whereArrayContains("Partecipanti", mioUid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot == null || snapshot.isEmpty) return@addOnSuccessListener

                val uidsDaCercare = mutableSetOf<String>()
                snapshot.documents.forEach { doc ->
                    val partecipanti = doc.get("Partecipanti") as? List<String>
                    if (partecipanti != null && partecipanti.size == 2) {
                        val altroUid = partecipanti.find { it != mioUid }
                        if (altroUid != null) uidsDaCercare.add(altroUid)
                    }
                }

                listaContatti.clear()
                uidsDaCercare.forEach { uid ->
                    db.collection("Utenti").document(uid).get().addOnSuccessListener { uDoc ->
                        val username = uDoc.getString("Username") ?: "Utente"
                        listaContatti.add(ContattoSelezionabile(uid, username))
                        contattiAdapter.notifyDataSetChanged()
                    }
                }
            }
    }

    private fun creaGruppoMulticontatto(nomeGruppo: String, contatti: List<ContattoSelezionabile>) {
        val mioUid = auth.currentUser?.uid ?: return

        val partecipantiList = mutableListOf<String>()
        partecipantiList.add(mioUid)
        contatti.forEach { partecipantiList.add(it.uid) }

        val partecipantiOrdinati = partecipantiList.sorted()
        val chatRoomId = "group_${System.currentTimeMillis()}"

        val chatData = hashMapOf(
            "Partecipanti" to partecipantiOrdinati,
            "isGruppo" to true,
            "NomeGruppo" to nomeGruppo,
            "timestampOrdinamento" to System.currentTimeMillis(),
            "messagges" to hashMapOf(
                "testo" to "Gruppo creato! Benvenuti.",
                "mittente" to ""
            )
        )

        db.collection("ChatRooms").document(chatRoomId)
            .set(chatData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Gruppo $nomeGruppo avviato!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, SingleChatActivity::class.java).apply {
                    putExtra("CHAT_ID", chatRoomId)
                    putExtra("OTHER_USER_NAME", nomeGruppo)
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Errore nella creazione del gruppo", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("ImpostazioniTema", Context.MODE_PRIVATE)
        val colSfondo = prefs.getInt("color_sfondo", "#1D4682".toColorInt())
        val colScritte = prefs.getInt("color_scritte", Color.WHITE)
        val colInviati = prefs.getInt("color_inviati", "#FFD400".toColorInt())

        // RECUPERIAMO IL COLORE DEL TESTO RICEVUTO (Base dei fumetti chiari)
        val colTestoRicevuti = prefs.getInt("color_testo_ricevuti", Color.BLACK)

        findViewById<View>(R.id.main)?.setBackgroundColor(colSfondo)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.addContactToolbar)
        toolbar?.setBackgroundColor(colSfondo)
        toolbar?.navigationIcon?.setTint(colScritte)

        findViewById<TextView>(R.id.tvAddTitle)?.setTextColor(colScritte)
        findViewById<TextView>(R.id.tvAddSub)?.setTextColor(colScritte).also {
            findViewById<TextView>(R.id.tvAddSub)?.alpha = 0.7f
        }

        tvSelezionaTitle.setTextColor(colScritte)

        tilNomeGruppo.boxStrokeColor = colScritte
        tilNomeGruppo.hintTextColor = ColorStateList.valueOf(colScritte)
        etNomeGruppo.setTextColor(colScritte)

        findViewById<MaterialButton>(R.id.btnAddContact)?.apply {
            backgroundTintList = ColorStateList.valueOf(colInviati)
            setTextColor(colSfondo)
        }

        // AGGIORNATO: Passiamo colTestoRicevuti invece di colScritte per dare il contrasto corretto sulla card bianca
        contattiAdapter.aggiornaColoriTema(colTestoRicevuti, colInviati, colScritte)
    }

    private fun cercaContattoEAvviaChat(phoneInput: String) {
        val mioUid = auth.currentUser?.uid ?: return

        db.collection("Utenti")
            .whereEqualTo("Telefono", phoneInput)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot == null || snapshot.isEmpty) {
                    Toast.makeText(this, "Contatto non trovato", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val uDoc = snapshot.documents[0]
                val altroUid = uDoc.id
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
        val list = listOf(mioUid, altroUid).sorted()
        val chatRoomId = "${list[0]}_${list[1]}"

        val chatData = hashMapOf(
            "Partecipanti" to list,
            "timestampOrdinamento" to System.currentTimeMillis(),
            "messagges" to hashMapOf(
                "testo" to "Inizia a chattare!",
                "mittente" to ""
            )
        )

        db.collection("ChatRooms").document(chatRoomId)
            .set(chatData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Chat avviata!", Toast.LENGTH_SHORT).show()
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

    // --- ADAPTER INTERNO ADATTATO ALLA TAVOLOZZA DEI COLORI ---
    class SelezionaContattiAdapter(
        private val list: List<ContattoSelezionabile>,
        private val onSelectionChanged: () -> Unit
    ) : RecyclerView.Adapter<SelezionaContattiAdapter.ViewHolder>() {

        private var colorScritte: Int = Color.BLACK
        private var colorSpunta: Int = Color.YELLOW
        private var colorBordoCard: Int = Color.WHITE

        // Metodo aggiornato per ricevere anche il colore del bordo coerente con lo sfondo globale
        fun aggiornaColoriTema(scritte: Int, spunta: Int, bordoCard: Int) {
            this.colorScritte = scritte
            this.colorSpunta = spunta
            this.colorBordoCard = bordoCard
            notifyDataSetChanged()
        }

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(android.R.id.text1)
            val checkBox: CheckBox = v.findViewById(android.R.id.checkbox)
            val container: MaterialCardView? = v as? MaterialCardView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val frame = MaterialCardView(parent.context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(12, 6, 12, 6) }
                radius = 16f
                cardElevation = 0f
                // Impostiamo lo sfondo della card esplicitamente bianco (o molto chiaro) come nell'immagine originale
                setCardBackgroundColor(Color.WHITE)
                strokeWidth = 2
            }

            val layoutRiga = android.widget.RelativeLayout(parent.context).apply {
                setPadding(32, 24, 32, 24)
            }
            val txt = TextView(parent.context).apply {
                id = android.R.id.text1
                textSize = 16f
                android.graphics.Typeface.DEFAULT_BOLD
            }
            val chk = CheckBox(parent.context).apply {
                id = android.R.id.checkbox
                val params = android.widget.RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { addRule(android.widget.RelativeLayout.ALIGN_PARENT_END) }
                layoutParams = params
            }
            layoutRiga.addView(txt)
            layoutRiga.addView(chk)
            frame.addView(layoutRiga)

            return ViewHolder(frame)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvName.text = item.username

            holder.checkBox.setOnCheckedChangeListener(null)
            holder.checkBox.isChecked = item.isChecked

            // Assegnazione dei colori dinamici corretti per il contrasto ottimale
            holder.tvName.setTextColor(colorScritte)
            holder.checkBox.buttonTintList = ColorStateList.valueOf(colorSpunta)
            holder.container?.strokeColor = colorBordoCard

            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                item.isChecked = isChecked
                onSelectionChanged()
            }
            holder.itemView.setOnClickListener {
                holder.checkBox.performClick()
            }
        }

        override fun getItemCount() = list.size
    }
}