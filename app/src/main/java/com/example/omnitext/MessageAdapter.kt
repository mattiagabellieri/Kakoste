package com.example.omnitext

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.example.omnitext.R

class MessageAdapter(private val messageList: List<Map<String, Any>>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    // MODIFICATO: Ottiene l'UID dinamicamente ogni volta che serve, evitando stringhe vuote
    private val mioUid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutReceived: LinearLayout = view.findViewById(R.id.layoutReceived)
        val layoutSent: LinearLayout = view.findViewById(R.id.layoutSent)
        val tvReceivedMessage: TextView = view.findViewById(R.id.tvReceivedMessage)
        val tvSentMessage: TextView = view.findViewById(R.id.tvSentMessage)
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

        // Controlliamo l'ID mittente per decidere quale fumetto mostrare
        if (mittente.isNotEmpty() && mittente == mioUidPulito) {
            // Messaggio inviato da me -> Mostra a destra (giallo), nascondi a sinistra (bianco)
            holder.layoutSent.visibility = View.VISIBLE
            holder.layoutReceived.visibility = View.GONE
            holder.tvSentMessage.text = testo
        } else {
            // Messaggio ricevuto dall'altro -> Mostra a sinistra (bianco), nascondi a destra (giallo)
            holder.layoutReceived.visibility = View.VISIBLE
            holder.layoutSent.visibility = View.GONE
            holder.tvReceivedMessage.text = testo
        }
    }

    override fun getItemCount() = messageList.size
}