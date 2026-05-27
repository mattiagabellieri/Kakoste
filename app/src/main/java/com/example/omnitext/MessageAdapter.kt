package com.example.omnitext

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.example.omnitext.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(private val messageList: List<Map<String, Any>>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val mioUid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutReceived: LinearLayout = view.findViewById(R.id.layoutReceived)
        val layoutSent: LinearLayout = view.findViewById(R.id.layoutSent)
        val tvReceivedMessage: TextView = view.findViewById(R.id.tvReceivedMessage)
        val tvSentMessage: TextView = view.findViewById(R.id.tvSentMessage)
        val tvReceivedTime: TextView = view.findViewById(R.id.tvReceivedTime)
        val tvSentTime: TextView = view.findViewById(R.id.tvSentTime)
        val tvMessageDate: TextView = view.findViewById(R.id.tvMessageDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val currentMessage = messageList[position]
        val testo = currentMessage["testo"]?.toString() ?: ""
        val mittente = currentMessage["mittente"]?.toString() ?: ""
        val timestamp = currentMessage["timestamp"] as? Long ?: 0L

        val mioUidPulito = mioUid.trim()

        val orarioFormattato = if (timestamp != 0L) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        } else {
            ""
        }

        if (position > 0) {
            val messaggioPrecedente = messageList[position - 1]
            val timestampPrecedente = messaggioPrecedente["timestamp"] as? Long ?: 0L

            val dataCorrente = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(timestamp))
            val dataPrecedente = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(timestampPrecedente))

            if (dataCorrente == dataPrecedente) {
                holder.tvMessageDate.visibility = View.GONE
            } else {
                holder.tvMessageDate.visibility = View.VISIBLE
                holder.tvMessageDate.text = formattaDataElegante(timestamp)
            }
        } else {
            holder.tvMessageDate.visibility = View.VISIBLE
            holder.tvMessageDate.text = formattaDataElegante(timestamp)
        }

        // --- APPLICAZIONE DINAMICA DEL TEMA SCELTO ---
        val context = holder.itemView.context
        val prefs = context.getSharedPreferences("ImpostazioniTema", Context.MODE_PRIVATE)

        val colInviati = prefs.getInt("color_inviati", "#1A3B6D".toColorInt())
        val colTestoInviati = prefs.getInt("color_testo_inviati", android.graphics.Color.WHITE)
        val colRicevuti = prefs.getInt("color_ricevuti", "#E0E0E0".toColorInt())
        val colTestoRicevuti = prefs.getInt("color_testo_ricevuti", android.graphics.Color.BLACK)

        if (mittente.isNotEmpty() && mittente == mioUidPulito) {
            holder.layoutSent.visibility = View.VISIBLE
            holder.layoutReceived.visibility = View.GONE
            holder.tvSentMessage.text = testo
            holder.tvSentTime.text = orarioFormattato

            // Applica la tinta allo sfondo drawable del fumetto e imposta il testo
            holder.layoutSent.background?.setTint(colInviati)
            holder.tvSentMessage.setTextColor(colTestoInviati)
            holder.tvSentTime.setTextColor(colTestoInviati)
        } else {
            holder.layoutReceived.visibility = View.VISIBLE
            holder.layoutSent.visibility = View.GONE
            holder.tvReceivedMessage.text = testo
            holder.tvReceivedTime.text = orarioFormattato

            // Applica la tinta allo sfondo drawable del fumetto e imposta il testo
            holder.layoutReceived.background?.setTint(colRicevuti)
            holder.tvReceivedMessage.setTextColor(colTestoRicevuti)
            holder.tvReceivedTime.setTextColor(colTestoRicevuti)
        }
    }

    override fun getItemCount() = messageList.size

    private fun formattaDataElegante(timestamp: Long): String {
        val formatoGiorno = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val oggi = formatoGiorno.format(Date())
        val giornoMessaggio = formatoGiorno.format(Date(timestamp))

        return when (giornoMessaggio) {
            oggi -> "Oggi"
            formatoGiorno.format(Date(System.currentTimeMillis() - 86400000)) -> "Ieri"
            else -> SimpleDateFormat("dd MMMM", Locale.getDefault()).format(Date(timestamp))
        }
    }
}