package com.goodok.app.ui.chats

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.goodok.app.R
import com.goodok.app.data.Repository
import com.goodok.app.data.model.Message
import com.goodok.app.databinding.ActivityChatBinding
import com.goodok.app.util.LanguageHelper
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.ktx.database
import kotlinx.coroutines.launch
import java.util.*

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var repository: Repository
    private lateinit var adapter: MessagesAdapter
    private var userId: String? = null
    private var username: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLanguage()
        applyTheme()
        super.onCreate(savedInstanceState)

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        userId = intent.getStringExtra("userId")
        username = intent.getStringExtra("username")

        if (userId == null) {
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        observeMessages()

        binding.btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun applyLanguage() {
        try {
            val repo = Repository(applicationContext)
            LanguageHelper.setLanguage(this, repo.language)
        } catch (e: Exception) {
            LanguageHelper.setLanguage(this, "ru")
        }
    }

    private fun applyTheme() {
        try {
            val repo = Repository(applicationContext)
            when (repo.theme) {
                0 -> setTheme(R.style.Theme_GoodOK_Classic)
                1 -> setTheme(R.style.Theme_GoodOK_Modern)
                2 -> setTheme(R.style.Theme_GoodOK_Neon)
                3 -> setTheme(R.style.Theme_GoodOK_Childish)
            }
        } catch (e: Exception) {
            setTheme(R.style.Theme_GoodOK_Classic)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = username ?: "Chat"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = MessagesAdapter(repository.currentUserId ?: "")
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter
    }

    private fun observeMessages() {
        lifecycleScope.launch {
            repository.observeMessages(userId!!).collect { messages ->
                adapter.submitList(messages) {
                    binding.rvMessages.scrollToPosition(messages.size - 1)
                }
            }
        }
    }

    private fun sendMessage() {
        val content = binding.etMessage.text.toString().trim()
        if (content.isEmpty()) return

        val message = Message(
            id = UUID.randomUUID().toString(),
            senderId = repository.currentUserId ?: return,
            receiverId = userId!!,
            content = content,
            timestamp = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            try {
                repository.sendMessage(message)
                binding.etMessage.text?.clear()

                // Send push notification via Firebase Database
                sendPushNotification(message)
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error sending message", e)
            }
        }
    }

    private fun sendPushNotification(message: Message) {
        // Write to notifications node for push processing
        val db = Firebase.database.reference
        val notification = mapOf(
            "toUserId" to userId,
            "fromUserId" to repository.currentUserId,
            "fromUsername" to username,
            "message" to message.content.take(100),
            "timestamp" to System.currentTimeMillis(),
            "type" to "message"
        )
        db.child("notifications").push().setValue(notification)
    }
}
