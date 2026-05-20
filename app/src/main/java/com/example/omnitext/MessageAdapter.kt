package com.example.omnitext

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.example.omnitext.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(private val messageList: List<Map<String, Any>>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    // Ottiene l'UID dinamicamente ogni volta che serve, evitando stringhe vuote
    private val mioUid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutReceived: LinearLayout = view.findViewById(R.id.layoutReceived)
        val layoutSent: LinearLayout = view.findViewById(R.id.layoutSent)
        val tvReceivedMessage: TextView = view.findViewById(R.id.tvReceivedMessage)
        val tvSentMessage: TextView = view.findViewById(R.id.tvSentMessage)

        val tvReceivedTime: TextView = view.findViewById(R.id.tvReceivedTime)
        val tvSentTime: TextView = view.findViewById(R.id.tvSentTime)

        // NUOVO RIFERIMENTI PER LA DATA IN CIMA
        val tvMessageDate: TextView = view.findViewById(R.id.tvMessageDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]

        val testo = message["testo"]?.toString()?.trim() ?: ""
        val mittente = message["mittente"]?.toString()?.trim() ?: ""
        val mioUidPulito = mioUid.trim()

        // 1. FORMATTAZIONE DELL'ORARIO (es. "15:30")
        val timestamp = message["timestamp"] as? Long
        val orarioFormattato = if (timestamp != null) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        } else {
            ""
        }

        // 2. GESTIONE DELLA DATA CENTRATA STILE WHATSAPP
        if (timestamp != null) {
            val dataCorrente = formattaDataElegante(timestamp)

            // Se è il primo messaggio, mostra sempre la data.
            // Altrimenti confrontalo con il messaggio precedente: se la data cambia, mostrala.
            if (position == 0) {
                holder.tvMessageDate.visibility = View.VISIBLE
                holder.tvMessageDate.text = dataCorrente
            } else {
                val messaggioPrecedente = messageList[position - 1]
                val timestampPrecedente = messaggioPrecedente["timestamp"] as? Long

                if (timestampPrecedente != null) {
                    val dataPrecedente = formattaDataElegante(timestampPrecedente)
                    if (dataCorrente != dataPrecedente) {
                        holder.tvMessageDate.visibility = View.VISIBLE
                        holder.tvMessageDate.text = dataCorrente
                    } else {
                        holder.tvMessageDate.visibility = View.GONE
                    }
                } else {
                    holder.tvMessageDate.visibility = View.GONE
                }
            }
        } else {
            holder.tvMessageDate.visibility = View.GONE
        }

        // 3. LOGICA DI VISUALIZZAZIONE DELLE BOLLE
        if (mittente.isNotEmpty() && mittente == mioUidPulito) {
            // Messaggio inviato da me -> Mostra a destra, nascondi a sinistra
            holder.layoutSent.visibility = View.VISIBLE
            holder.layoutReceived.visibility = View.GONE
            holder.tvSentMessage.text = testo
            holder.tvSentTime.text = orarioFormattato
        } else {
            // Messaggio ricevuto dall'altro -> Mostra a sinistra, nascondi a destra
            holder.layoutReceived.visibility = View.VISIBLE
            holder.layoutSent.visibility = View.GONE
            holder.tvReceivedMessage.text = testo
            holder.tvReceivedTime.text = orarioFormattato
        }
    }

    override fun getItemCount() = messageList.size

    // Funzione di supporto per convertire il timestamp in "Oggi", "Ieri" o nella data completa (es. "24 maggio")
    private fun formattaDataElegante(timestamp: Long): String {
        val formatoGiorno = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val oggi = formatoGiorno.format(Date())
        val giornoMessaggio = formatoGiorno.format(Date(timestamp))

        return when (giornoMessaggio) {
            oggi -> "Oggi"
            formatoGiorno.format(Date(System.currentTimeMillis() - 86400000)) -> "Ieri"
            else -> SimpleDateFormat("d MMMM yyyy", Locale.ITALIAN).format(Date(timestamp))
        }
    }
}