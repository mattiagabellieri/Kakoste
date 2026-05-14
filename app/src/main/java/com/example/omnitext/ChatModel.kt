package com.example.omnitext

data class ChatModel(
    val chatRoomId: String = "",
    val lastMessage: String = "",
    val otherUserId: String = "",
    val otherUserName: String = "Utente"
)