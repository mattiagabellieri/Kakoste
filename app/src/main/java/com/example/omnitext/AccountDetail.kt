package com.example.omnitext

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import yuku.ambilwarna.AmbilWarnaDialog

class AccountDetail : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var etUsername: TextInputEditText? = null
    private var etPhone: TextInputEditText? = null

    private var viewColorSfondo: View? = null
    private var viewColorScritte: View? = null
    private var viewColorInviati: View? = null
    private var viewColorTestoInviati: View? = null
    private var viewColorRicevuti: View? = null
    private var viewColorTestoRicevuti: View? = null

    private var colorSfondo: Int = 0
    private var colorScritte: Int = 0
    private var colorInviati: Int = 0
    private var colorTestoInviati: Int = 0
    private var colorRicevuti: Int = 0
    private var colorTestoRicevuti: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_account_detail)

        val mainView = findViewById<View>(R.id.main)
        mainView?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationOnClickListener { finish() }

        etUsername = findViewById(R.id.etSettingsUsername)
        etPhone = findViewById(R.id.etSettingsPhone)

        viewColorSfondo = findViewById(R.id.viewColorSfondo)
        viewColorScritte = findViewById(R.id.viewColorScritte)
        viewColorInviati = findViewById(R.id.viewColorInviati)
        viewColorTestoInviati = findViewById(R.id.viewColorTestoInviati)
        viewColorRicevuti = findViewById(R.id.viewColorRicevuti)
        viewColorTestoRicevuti = findViewById(R.id.viewColorTestoRicevuti)

        caricaImpostazioniSalvate()

        viewColorSfondo?.setOnClickListener { apriColorPicker(colorSfondo) { colorSfondo = it; impostaCerchioColore(viewColorSfondo, it) } }
        viewColorScritte?.setOnClickListener { apriColorPicker(colorScritte) { colorScritte = it; impostaCerchioColore(viewColorScritte, it) } }
        viewColorInviati?.setOnClickListener { apriColorPicker(colorInviati) { colorInviati = it; impostaCerchioColore(viewColorInviati, it) } }
        viewColorTestoInviati?.setOnClickListener { apriColorPicker(colorTestoInviati) { colorTestoInviati = it; impostaCerchioColore(viewColorTestoInviati, it) } }
        viewColorRicevuti?.setOnClickListener { apriColorPicker(colorRicevuti) { colorRicevuti = it; impostaCerchioColore(viewColorRicevuti, it) } }
        viewColorTestoRicevuti?.setOnClickListener { apriColorPicker(colorTestoRicevuti) { colorTestoRicevuti = it; impostaCerchioColore(viewColorTestoRicevuti, it) } }

        findViewById<MaterialButton>(R.id.btnSaveProfile)?.setOnClickListener {
            val nuovoNome = etUsername?.text?.toString()?.trim() ?: ""
            if (nuovoNome.isNotEmpty()) {
                aggiornaUsername(nuovoNome)
            }
            salvaColoriNellePreferenze()
            onResume()
            Toast.makeText(this, "Impostazioni e Tema salvati!", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnResetPalette)?.setOnClickListener {
            ripristinaColoriOriginali()
        }

        findViewById<MaterialButton>(R.id.btnLogout)?.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        recuperaDatiUtente()
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("ImpostazioniTema", Context.MODE_PRIVATE)

        val colSfondo = prefs.getInt("color_sfondo", "#1D4682".toColorInt())
        val colScritte = prefs.getInt("color_scritte", Color.WHITE)
        val colInviati = prefs.getInt("color_inviati", "#FFD400".toColorInt())

        // Sfondo principale
        findViewById<View>(R.id.main)?.setBackgroundColor(colSfondo)

        // Toolbar: sfondo + icona navigazione
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar?.setBackgroundColor(colSfondo)
        toolbar?.navigationIcon?.setTint(colScritte)

        // Titolo schermata
        findViewById<TextView>(R.id.tvSettingsTitle)?.setTextColor(colScritte)

        // Pulsante SALVA: sfondo = colore inviati, testo = colore sfondo (contrasto garantito)
        findViewById<MaterialButton>(R.id.btnSaveProfile)?.apply {
            setBackgroundColor(colInviati)
            setTextColor(colSfondo)
        }

        // Pulsante RESET TAVOLOZZA: bordo e testo del colore scritte
        findViewById<MaterialButton>(R.id.btnResetPalette)?.apply {
            setTextColor(colScritte)
            strokeColor = android.content.res.ColorStateList.valueOf(colScritte)
        }

        // Pulsante DISCONNETTI: rimane rosso fisso (segnale di pericolo universale)
        // Non viene toccato intenzionalmente

        // Label fisse della tavolozza: le coloriamo con colScritte per leggibilità sopra lo sfondo
        // Nota: le label sono dentro una MaterialCardView bianca, quindi rimangono scure — OK by design
    }

    private fun apriColorPicker(coloreIniziale: Int, onColorSelected: (Int) -> Unit) {
        val dialog = AmbilWarnaDialog(this, coloreIniziale, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog?) {}
            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                onColorSelected(color)
            }
        })
        dialog.show()
    }

    private fun impostaCerchioColore(view: View?, colore: Int) {
        if (view == null) return
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(colore)
            setStroke(2, Color.parseColor("#CCCCCC"))
        }
        view.background = drawable
    }

    private fun caricaImpostazioniSalvate() {
        val prefs = getSharedPreferences("ImpostazioniTema", Context.MODE_PRIVATE)

        colorSfondo = prefs.getInt("color_sfondo", "#1D4682".toColorInt())
        colorScritte = prefs.getInt("color_scritte", Color.WHITE)
        colorInviati = prefs.getInt("color_inviati", "#FFD400".toColorInt())
        colorTestoInviati = prefs.getInt("color_testo_inviati", Color.BLACK)
        colorRicevuti = prefs.getInt("color_ricevuti", Color.WHITE)
        colorTestoRicevuti = prefs.getInt("color_testo_ricevuti", Color.BLACK)

        impostaCerchioColore(viewColorSfondo, colorSfondo)
        impostaCerchioColore(viewColorScritte, colorScritte)
        impostaCerchioColore(viewColorInviati, colorInviati)
        impostaCerchioColore(viewColorTestoInviati, colorTestoInviati)
        impostaCerchioColore(viewColorRicevuti, colorRicevuti)
        impostaCerchioColore(viewColorTestoRicevuti, colorTestoRicevuti)
    }

    private fun salvaColoriNellePreferenze() {
        getSharedPreferences("ImpostazioniTema", Context.MODE_PRIVATE).edit {
            putInt("color_sfondo", colorSfondo)
            putInt("color_scritte", colorScritte)
            putInt("color_inviati", colorInviati)
            putInt("color_testo_inviati", colorTestoInviati)
            putInt("color_ricevuti", colorRicevuti)
            putInt("color_testo_ricevuti", colorTestoRicevuti)
        }
    }

    private fun ripristinaColoriOriginali() {
        colorSfondo = "#1D4682".toColorInt()
        colorScritte = Color.WHITE
        colorInviati = "#FFD400".toColorInt()
        colorTestoInviati = Color.BLACK
        colorRicevuti = Color.WHITE
        colorTestoRicevuti = Color.BLACK

        salvaColoriNellePreferenze()

        impostaCerchioColore(viewColorSfondo, colorSfondo)
        impostaCerchioColore(viewColorScritte, colorScritte)
        impostaCerchioColore(viewColorInviati, colorInviati)
        impostaCerchioColore(viewColorTestoInviati, colorTestoInviati)
        impostaCerchioColore(viewColorRicevuti, colorRicevuti)
        impostaCerchioColore(viewColorTestoRicevuti, colorTestoRicevuti)

        onResume()
        Toast.makeText(this, "Colori ripristinati di default!", Toast.LENGTH_SHORT).show()
    }

    private fun aggiornaUsername(nuovoNome: String) {
        val mioUid = auth.currentUser?.uid ?: return
        db.collection("Utenti").document(mioUid).update("Username", nuovoNome)
    }

    private fun recuperaDatiUtente() {
        val mioUid = auth.currentUser?.uid ?: return
        db.collection("Utenti").document(mioUid).get().addOnSuccessListener { doc ->
            if (doc != null) {
                etUsername?.setText(doc.getString("Username") ?: "")
                etPhone?.setText(doc.getString("Telefono") ?: "")
            }
        }
    }
}