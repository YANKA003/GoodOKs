package com.goodok.app.data.model

import com.google.gson.annotations.SerializedName
import java.util.*

// User model
data class User(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("email")
    val email: String = "",
    @SerializedName("username")
    val username: String = "",
    @SerializedName("phone")
    val phone: String = "",
    @SerializedName("avatarUrl")
    val avatarUrl: String? = null,
    @SerializedName("status")
    val status: String = "offline",
    @SerializedName("lastSeen")
    val lastSeen: Long = System.currentTimeMillis(),
    @SerializedName("language")
    val language: String = "ru",
    @SerializedName("theme")
    val theme: Int = 0,
    @SerializedName("pushToken")
    val pushToken: String? = null,
    @SerializedName("biometricEnabled")
    val biometricEnabled: Boolean = false,
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isOnline(): Boolean = status == "online" || System.currentTimeMillis() - lastSeen < 300000
}

// Message model
data class Message(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    @SerializedName("chatId")
    val chatId: String = "",
    @SerializedName("senderId")
    val senderId: String = "",
    @SerializedName("senderName")
    val senderName: String = "",
    @SerializedName("senderAvatar")
    val senderAvatar: String? = null,
    @SerializedName("text")
    val text: String = "",
    @SerializedName("type")
    val type: MessageType = MessageType.TEXT,
    @SerializedName("mediaUrl")
    val mediaUrl: String? = null,
    @SerializedName("mediaType")
    val mediaType: String? = null, // "image", "video", "audio", "document"
    @SerializedName("mediaName")
    val mediaName: String? = null,
    @SerializedName("mediaSize")
    val mediaSize: Long = 0,
    @SerializedName("replyTo")
    val replyTo: String? = null,
    @SerializedName("forwardedFrom")
    val forwardedFrom: String? = null,
    @SerializedName("edited")
    val edited: Boolean = false,
    @SerializedName("read")
    val read: Boolean = false,
    @SerializedName("readBy")
    val readBy: List<String> = emptyList(),
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("deleted")
    val deleted: Boolean = false
)

enum class MessageType {
    TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT, VOICE, LOCATION, CONTACT, STICKER
}

// Chat model
data class Chat(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    @SerializedName("type")
    val type: ChatType = ChatType.PRIVATE,
    @SerializedName("name")
    val name: String = "",
    @SerializedName("avatarUrl")
    val avatarUrl: String? = null,
    @SerializedName("participants")
    val participants: List<String> = emptyList(),
    @SerializedName("admins")
    val admins: List<String> = emptyList(),
    @SerializedName("ownerId")
    val ownerId: String = "",
    @SerializedName("lastMessage")
    val lastMessage: Message? = null,
    @SerializedName("unreadCount")
    val unreadCount: Int = 0,
    @SerializedName("pinned")
    val pinned: Boolean = false,
    @SerializedName("muted")
    val muted: Boolean = false,
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ChatType {
    PRIVATE, GROUP, CHANNEL
}

// Channel model
data class Channel(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    @SerializedName("name")
    val name: String = "",
    @SerializedName("description")
    val description: String = "",
    @SerializedName("avatarUrl")
    val avatarUrl: String? = null,
    @SerializedName("ownerId")
    val ownerId: String = "",
    @SerializedName("subscribers")
    val subscribers: List<String> = emptyList(),
    @SerializedName("subscriberCount")
    val subscriberCount: Int = 0,
    @SerializedName("verified")
    val verified: Boolean = false,
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis()
)

// Call model
data class Call(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    @SerializedName("callerId")
    val callerId: String = "",
    @SerializedName("callerName")
    val callerName: String = "",
    @SerializedName("callerAvatar")
    val callerAvatar: String? = null,
    @SerializedName("receiverId")
    val receiverId: String = "",
    @SerializedName("receiverName")
    val receiverName: String = "",
    @SerializedName("receiverAvatar")
    val receiverAvatar: String? = null,
    @SerializedName("type")
    val type: CallType = CallType.VOICE,
    @SerializedName("status")
    val status: CallStatus = CallStatus.RINGING,
    @SerializedName("startTime")
    val startTime: Long? = null,
    @SerializedName("endTime")
    val endTime: Long? = null,
    @SerializedName("duration")
    val duration: Long = 0
)

enum class CallType {
    VOICE, VIDEO
}

enum class CallStatus {
    RINGING, CONNECTING, CONNECTED, ENDED, MISSED, DECLINED
}

// Contact model
data class Contact(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("phone")
    val phone: String = "",
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("avatarUrl")
    val avatarUrl: String? = null,
    @SerializedName("registered")
    val registered: Boolean = false,
    @SerializedName("userId")
    val userId: String? = null
)

// Push notification data
data class PushData(
    @SerializedName("title")
    val title: String = "",
    @SerializedName("body")
    val body: String = "",
    @SerializedName("type")
    val type: String = "",
    @SerializedName("chatId")
    val chatId: String? = null,
    @SerializedName("senderId")
    val senderId: String? = null,
    @SerializedName("callId")
    val callId: String? = null,
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

// Theme options
enum class AppTheme(val id: Int) {
    CLASSIC(0),
    MODERN(1),
    NEON(2),
    CHILDISH(3)
}

// Language options
enum class AppLanguage(val code: String, val displayName: String) {
    RUSSIAN("ru", "Русский"),
    ENGLISH("en", "English"),
    BELARUSIAN("be", "Беларуская"),
    UKRAINIAN("uk", "Українська"),
    GERMAN("de", "Deutsch"),
    POLISH("pl", "Polski"),
    FRENCH("fr", "Français")
}
