package com.example.omnitext

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
// Questo import esplicito dice ad Android Studio esattamente dove trovare i tuoi XML
import com.example.omnitext.R

class ChatAdapter(private val chatList: List<ChatModel>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById<TextView>(R.id.tvChatName)
        val tvLastMsg: TextView = view.findViewById<TextView>(R.id.tvLastMessage)
        val tvAvatarLetter: TextView = view.findViewById<TextView>(R.id.textView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chatList[position]

        holder.tvName.text = chat.otherUserName
        holder.tvLastMsg.text = chat.lastMessage
        holder.tvAvatarLetter.text = chat.otherUserName.take(1).uppercase()

        holder.itemView.setOnClickListener { contextView ->
            val context = contextView.context
            val intent = Intent(context, SingleChatActivity::class.java)
            intent.putExtra("CHAT_ID", chat.chatRoomId)
            intent.putExtra("OTHER_USER_NAME", chat.otherUserName)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = chatList.size
}