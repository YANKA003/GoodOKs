package com.goodok.app.data.local

import androidx.room.*
import com.goodok.app.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Query("SELECT * FROM users WHERE uid != :currentUserId")
    fun getAllUsers(currentUserId: String): Flow<List<User>>

    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getById(uid: String): User?

    @Query("UPDATE users SET isOnline = :online, lastSeen = :lastSeen WHERE uid = :uid")
    suspend fun updateOnlineStatus(uid: String, online: Boolean, lastSeen: Long)
}

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message)

    @Query("SELECT * FROM messages WHERE (senderId = :userId AND receiverId = :currentUserId) OR (senderId = :currentUserId AND receiverId = :userId) ORDER BY timestamp ASC")
    fun getMessagesWithUser(userId: String, currentUserId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: String): Message?

    @Update
    suspend fun update(message: Message)

    @Query("UPDATE messages SET isEdited = 1, content = :content WHERE id = :id")
    suspend fun updateContent(id: String, content: String)

    @Query("UPDATE messages SET isDeleted = 1 WHERE id = :id")
    suspend fun markAsDeleted(id: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface CallDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(call: Call)

    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    fun getAllCallsFlow(): Flow<List<Call>>

    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    suspend fun getAllCalls(): List<Call>

    @Query("SELECT * FROM calls WHERE id = :id")
    suspend fun getById(id: String): Call?
}

@Dao
interface ChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(channel: Channel)

    @Query("SELECT * FROM channels ORDER BY name ASC")
    fun getAllChannelsFlow(): Flow<List<Channel>>

    @Query("SELECT * FROM channels ORDER BY name ASC")
    suspend fun getAllChannels(): List<Channel>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getById(id: String): Channel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChannelMessage)

    @Query("SELECT * FROM channel_messages WHERE channelId = :channelId ORDER BY timestamp ASC")
    fun getChannelMessages(channelId: String): Flow<List<ChannelMessage>>
}

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: Contact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<Contact>)

    @Query("SELECT * FROM contacts ORDER BY displayName ASC")
    fun getAllContactsFlow(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts ORDER BY displayName ASC")
    suspend fun getAllContacts(): List<Contact>

    @Query("SELECT * FROM contacts WHERE displayName LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%'")
    suspend fun searchContacts(query: String): List<Contact>

    @Query("SELECT * FROM contacts WHERE isRegistered = 1")
    fun getRegisteredContacts(): Flow<List<Contact>>

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: String)
}
