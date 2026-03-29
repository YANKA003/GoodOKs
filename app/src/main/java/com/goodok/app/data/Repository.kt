package com.goodok.app.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.goodok.app.data.local.PreferencesManager
import com.goodok.app.data.model.User
import com.goodok.app.data.model.Message
import com.goodok.app.data.model.Chat
import com.goodok.app.data.model.Call
import com.goodok.app.data.model.Contact
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class Repository(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val prefs = PreferencesManager(context)

    val currentUser: User?
        get() = if (auth.currentUser != null) {
            User(
                id = auth.currentUser!!.uid,
                email = prefs.userEmail ?: auth.currentUser!!.email ?: "",
                username = prefs.username ?: "",
                phone = prefs.phone ?: "",
                avatarUrl = prefs.avatarUrl,
                language = prefs.language,
                theme = prefs.theme
            )
        } else null

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    var theme: Int
        get() = prefs.theme
        set(value) {
            prefs.theme = value
        }

    var language: String
        get() = prefs.language
        set(value) {
            prefs.language = value
        }

    var biometricEnabled: Boolean
        get() = prefs.biometricEnabled
        set(value) {
            prefs.biometricEnabled = value
        }

    // ==================== AUTH ====================

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User not found")

            // Load user data from database
            val userSnapshot = database.getReference("users").child(user.uid).get().await()
            val dbUser = userSnapshot.getValue(User::class.java)

            // Update preferences
            prefs.currentUserId = user.uid
            prefs.userEmail = email
            prefs.username = dbUser?.username ?: ""
            prefs.phone = dbUser?.phone ?: ""
            prefs.avatarUrl = dbUser?.avatarUrl
            prefs.language = dbUser?.language ?: "ru"
            prefs.theme = dbUser?.theme ?: 0

            Result.success(currentUser!!)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Неверный email или пароль"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        email: String,
        password: String,
        username: String,
        phone: String,
        language: String,
        theme: Int
    ): Result<User> {
        return try {
            // Create auth user
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("User creation failed")

            // Create user model
            val user = User(
                id = firebaseUser.uid,
                email = email,
                username = username,
                phone = phone,
                language = language,
                theme = theme,
                createdAt = System.currentTimeMillis()
            )

            // Save to database
            database.getReference("users").child(firebaseUser.uid).setValue(user).await()

            // Update preferences
            prefs.currentUserId = firebaseUser.uid
            prefs.userEmail = email
            prefs.username = username
            prefs.phone = phone
            prefs.language = language
            prefs.theme = theme

            Result.success(user)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Email уже используется"))
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("Пароль слишком слабый"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
        prefs.clear()
    }

    // ==================== USERS ====================

    suspend fun getUser(userId: String): User? {
        return try {
            val snapshot = database.getReference("users").child(userId).get().await()
            snapshot.getValue(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            database.getReference("users").child(user.id).setValue(user).await()
            prefs.username = user.username
            prefs.phone = user.phone
            prefs.avatarUrl = user.avatarUrl
            prefs.language = user.language
            prefs.theme = user.theme
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeUsers(): Flow<List<User>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = snapshot.children.mapNotNull { it.getValue(User::class.java) }
                trySend(users)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        database.getReference("users").addValueEventListener(listener)
        awaitClose { database.getReference("users").removeEventListener(listener) }
    }

    // ==================== CHATS ====================

    suspend fun getOrCreateChat(otherUserId: String): Result<Chat> {
        return try {
            val currentId = currentUserId ?: throw Exception("Not logged in")

            // Check for existing chat
            val chatsSnapshot = database.getReference("chats").get().await()
            for (chatSnapshot in chatsSnapshot.children) {
                val chat = chatSnapshot.getValue(Chat::class.java)
                if (chat != null && chat.participants.contains(currentId) && chat.participants.contains(otherUserId)) {
                    return Result.success(chat)
                }
            }

            // Create new chat
            val chatRef = database.getReference("chats").push()
            val chat = Chat(
                id = chatRef.key!!,
                type = com.goodok.app.data.model.ChatType.PRIVATE,
                participants = listOf(currentId, otherUserId),
                createdAt = System.currentTimeMillis()
            )
            chatRef.setValue(chat).await()
            Result.success(chat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeChats(): Flow<List<Chat>> = callbackFlow {
        val currentId = currentUserId
        if (currentId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chats = snapshot.children
                    .mapNotNull { it.getValue(Chat::class.java) }
                    .filter { it.participants.contains(currentId) }
                    .sortedByDescending { it.updatedAt }
                trySend(chats)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        database.getReference("chats").addValueEventListener(listener)
        awaitClose { database.getReference("chats").removeEventListener(listener) }
    }

    // ==================== MESSAGES ====================

    suspend fun sendMessage(chatId: String, text: String): Result<Message> {
        return try {
            val currentId = currentUserId ?: throw Exception("Not logged in")
            val messageRef = database.getReference("messages").child(chatId).push()

            val message = Message(
                id = messageRef.key!!,
                chatId = chatId,
                senderId = currentId,
                senderName = prefs.username ?: "",
                text = text,
                timestamp = System.currentTimeMillis()
            )

            messageRef.setValue(message).await()

            // Update chat's last message
            database.getReference("chats").child(chatId).child("lastMessage").setValue(message).await()
            database.getReference("chats").child(chatId).child("updatedAt").setValue(System.currentTimeMillis()).await()

            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMediaMessage(chatId: String, mediaUrl: String, mediaType: String): Result<Message> {
        return try {
            val currentId = currentUserId ?: throw Exception("Not logged in")
            val messageRef = database.getReference("messages").child(chatId).push()

            val message = Message(
                id = messageRef.key!!,
                chatId = chatId,
                senderId = currentId,
                senderName = prefs.username ?: "",
                type = when (mediaType) {
                    "image" -> com.goodok.app.data.model.MessageType.IMAGE
                    "video" -> com.goodok.app.data.model.MessageType.VIDEO
                    "audio" -> com.goodok.app.data.model.MessageType.AUDIO
                    else -> com.goodok.app.data.model.MessageType.DOCUMENT
                },
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                timestamp = System.currentTimeMillis()
            )

            messageRef.setValue(message).await()
            database.getReference("chats").child(chatId).child("lastMessage").setValue(message).await()
            database.getReference("chats").child(chatId).child("updatedAt").setValue(System.currentTimeMillis()).await()

            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children
                    .mapNotNull { it.getValue(Message::class.java) }
                    .filter { !it.deleted }
                    .sortedBy { it.timestamp }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        database.getReference("messages").child(chatId).addValueEventListener(listener)
        awaitClose { database.getReference("messages").child(chatId).removeEventListener(listener) }
    }

    // ==================== CALLS ====================

    suspend fun createCall(receiverId: String, isVideo: Boolean): Result<Call> {
        return try {
            val currentId = currentUserId ?: throw Exception("Not logged in")
            val callRef = database.getReference("calls").push()

            val call = Call(
                id = callRef.key!!,
                callerId = currentId,
                callerName = prefs.username ?: "",
                receiverId = receiverId,
                type = if (isVideo) com.goodok.app.data.model.CallType.VIDEO else com.goodok.app.data.model.CallType.VOICE,
                status = com.goodok.app.data.model.CallStatus.RINGING
            )

            callRef.setValue(call).await()
            Result.success(call)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeCalls(): Flow<List<Call>> = callbackFlow {
        val currentId = currentUserId
        if (currentId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val calls = snapshot.children
                    .mapNotNull { it.getValue(Call::class.java) }
                    .filter { it.callerId == currentId || it.receiverId == currentId }
                    .sortedByDescending { it.startTime ?: System.currentTimeMillis() }
                trySend(calls)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        database.getReference("calls").addValueEventListener(listener)
        awaitClose { database.getReference("calls").removeEventListener(listener) }
    }

    // ==================== PUSH TOKEN ====================

    suspend fun updatePushToken() {
        val token = prefs.pushToken ?: return
        val userId = currentUserId ?: return
        try {
            database.getReference("users").child(userId).child("pushToken").setValue(token).await()
        } catch (e: Exception) {
            Log.e("Repository", "Failed to update push token", e)
        }
    }

    fun savePushToken(token: String) {
        prefs.pushToken = token
    }
}
