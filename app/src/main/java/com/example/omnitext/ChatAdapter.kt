package com.example.omnitext

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val chatList: List<ChatModel>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Colleghiamo i componenti di item_chat.xml
        val tvName: TextView = view.findViewById(R.id.tvChatName)
        val tvLastMsg: TextView = view.findViewById(R.id.tvLastMessage)

        // Qui risolviamo l'errore dell'avatar:
        // Cerchiamo il TextView direttamente usando il suo ID globale
        val tvAvatarLetter: TextView = view.findViewById(R.id.textView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        // Qui carichiamo il layout item_chat.xml
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chatList[position]

        holder.tvName.text = chat.otherUserName
        holder.tvLastMsg.text = chat.lastMessage

        // Prende la prima lettera del nome
        holder.tvAvatarLetter.text = chat.otherUserName.take(1).uppercase()

        // Click sulla riga per aprire la chat singola
        holder.itemView.setOnClickListener {
            val intent = Intent(it.context, SingleChatActivity::class.java)
            intent.putExtra("CHAT_ID", chat.chatRoomId)
            intent.putExtra("OTHER_USER_NAME", chat.otherUserName)
            it.context.startActivity(intent)
        }
    }

    override fun getItemCount() = chatList.size
}