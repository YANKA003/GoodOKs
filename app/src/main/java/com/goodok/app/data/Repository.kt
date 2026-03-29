package com.goodok.app.data

import android.content.Context
import com.goodok.app.data.local.AppDatabase
import com.goodok.app.data.local.PreferencesManager
import com.goodok.app.data.model.*
import com.goodok.app.data.remote.FirebaseService
import com.goodok.app.services.PushManager
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

class Repository(context: Context) {
    private val firebaseService = FirebaseService()
    private val database = AppDatabase.getDatabase(context)
    private val prefsManager = PreferencesManager(context)
    private val pushManager = PushManager.getInstance(context)

    val currentUser: FirebaseUser?
        get() = firebaseService.currentUser

    val currentUserId: String?
        get() = firebaseService.currentUserId

    val theme: Int
        get() = prefsManager.theme

    val language: String
        get() = prefsManager.language

    val isPremium: Boolean
        get() = prefsManager.isPremium

    val premiumType: String
        get() = prefsManager.premiumType

    // Auth
    suspend fun login(email: String, password: String) = firebaseService.login(email, password)

    suspend fun register(email: String, password: String, username: String) =
        firebaseService.register(email, password, username)

    suspend fun registerWithPhone(email: String, password: String, username: String, phone: String) =
        firebaseService.registerWithPhone(email, password, username, phone)

    fun logout() {
        pushManager.deleteToken()
        firebaseService.logout()
        prefsManager.clear()
    }

    // Users
    suspend fun getUser(uid: String) = firebaseService.getUser(uid)

    suspend fun updateUser(user: User) = firebaseService.updateUser(user)

    suspend fun searchUsers(query: String) = firebaseService.searchUsers(query)

    suspend fun findUsersByPhones(phones: List<String>) = firebaseService.findUsersByPhones(phones)

    fun setOnlineStatus(isOnline: Boolean) {
        currentUserId?.let { firebaseService.setOnlineStatus(it, isOnline) }
    }

    fun observeUserStatus(uid: String): Flow<User> = firebaseService.observeUserStatus(uid)

    // Messages
    suspend fun sendMessage(message: Message) = firebaseService.sendMessage(message)

    suspend fun editMessage(messageId: String, receiverId: String, newContent: String) {
        currentUserId?.let { senderId ->
            firebaseService.editMessage(messageId, senderId, receiverId, newContent)
        }
    }

    suspend fun deleteMessage(messageId: String, receiverId: String) {
        currentUserId?.let { senderId ->
            firebaseService.deleteMessage(messageId, senderId, receiverId)
        }
    }

    fun observeMessages(userId: String): Flow<List<Message>> {
        return currentUserId?.let { firebaseService.observeMessages(it, userId) }
            ?: throw IllegalStateException("User not logged in")
    }

    // Calls
    suspend fun saveCall(call: Call) = firebaseService.saveCall(call)

    fun observeCalls(): Flow<List<Call>> {
        return currentUserId?.let { firebaseService.observeCalls(it) }
            ?: throw IllegalStateException("User not logged in")
    }

    // Channels
    suspend fun createChannel(channel: Channel) = firebaseService.createChannel(channel)

    suspend fun searchChannels(query: String) = firebaseService.searchChannels(query)

    suspend fun subscribeToChannel(channelId: String) {
        currentUserId?.let { firebaseService.subscribeToChannel(channelId, it) }
    }

    // Push notifications
    fun getPushToken() = pushManager.getCurrentToken()

    suspend fun updatePushToken() {
        val token = pushManager.getCurrentToken()
        if (token != null && currentUserId != null) {
            firebaseService.updatePushToken(currentUserId!!, token)
        }
    }

    // Local database operations
    suspend fun insertUser(user: User) = database.userDao().insert(user)

    suspend fun getUserById(uid: String) = database.userDao().getById(uid)

    fun getAllUsersFlow() = database.userDao().getAllUsers(currentUserId ?: "")

    suspend fun insertContact(contact: com.goodok.app.data.model.Contact) =
        database.contactDao().insert(contact)

    suspend fun insertContacts(contacts: List<com.goodok.app.data.model.Contact>) =
        database.contactDao().insertAll(contacts)

    fun getAllContactsFlow() = database.contactDao().getAllContactsFlow()

    fun getRegisteredContactsFlow() = database.contactDao().getRegisteredContacts()

    suspend fun searchContacts(query: String) = database.contactDao().searchContacts(query)

    // Settings
    fun setTheme(theme: Int) {
        prefsManager.theme = theme
    }

    fun setLanguage(language: String) {
        prefsManager.language = language
    }

    fun setPremium(isPremium: Boolean, type: String, expiry: Long) {
        prefsManager.isPremium = isPremium
        prefsManager.premiumType = type
        prefsManager.premiumExpiry = expiry
    }
}
