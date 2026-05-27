package com.example.omnitext

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.example.omnitext.R

class ChatAdapter(private val chatList: List<ChatModel>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvChatName)
        val tvLastMsg: TextView = view.findViewById(R.id.tvLastMessage)
        val tvAvatarLetter: TextView = view.findViewById(R.id.textView)
        val avatarContainer: MaterialCardView = view.findViewById(R.id.avatarContainer)
        // BUG FIX: non castiamo più parent come MaterialCardView prima che la view sia attached
        // L'item root viene acceduto direttamente tramite itemView nel bind
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chatList[position]
        val context = holder.itemView.context

        holder.tvName.text = chat.otherUserName
        holder.tvLastMsg.text = chat.lastMessage
        holder.tvAvatarLetter.text = chat.otherUserName.take(1).uppercase()

        // Recupero del tema dinamico
        val prefs = context.getSharedPreferences("ImpostazioniTema", Context.MODE_PRIVATE)
        val colInviati = prefs.getInt("color_inviati", "#FFD400".toColorInt())
        val colTestoInviati = prefs.getInt("color_testo_inviati", Color.BLACK)

        // Nome e ultimo messaggio: stesso colore testo dei messaggi inviati
        holder.tvName.setTextColor(colTestoInviati)
        holder.tvLastMsg.setTextColor(colTestoInviati)
        holder.tvLastMsg.alpha = 0.7f

        // Avatar: sfondo = colore fumetto inviati, lettera = testo inviati
        holder.avatarContainer.setCardBackgroundColor(colInviati)
        holder.tvAvatarLetter.setTextColor(colTestoInviati)

        // Card dell'item: sfondo = colore fumetto messaggi inviati
        (holder.itemView as? MaterialCardView)?.setCardBackgroundColor(colInviati)

        holder.itemView.setOnClickListener { contextView ->
            val currentContext = contextView.context
            val intent = Intent(currentContext, SingleChatActivity::class.java)
            intent.putExtra("CHAT_ID", chat.chatRoomId)
            intent.putExtra("OTHER_USER_NAME", chat.otherUserName)
            currentContext.startActivity(intent)
        }
    }

    override fun getItemCount() = chatList.size
}