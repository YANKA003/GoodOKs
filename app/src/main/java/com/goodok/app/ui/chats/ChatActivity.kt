package com.goodok.app.ui.chats

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.goodok.app.R
import com.goodok.app.data.Repository
import com.goodok.app.data.model.Message
import com.goodok.app.databinding.ActivityChatBinding
import com.goodok.app.ui.calls.CallActivity
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.io.File

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var repository: Repository
    private lateinit var adapter: MessagesAdapter

    private var chatId: String? = null
    private var otherUserId: String? = null
    private var otherUserName: String = ""

    private val storage = FirebaseStorage.getInstance()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadAndSendMedia(it, "image") }
    }

    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadAndSendMedia(it, "video") }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            uploadAndSendMedia(photoUri!!, "image")
        }
    }

    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        chatId = intent.getStringExtra("chat_id")
        otherUserId = intent.getStringExtra("other_user_id")

        setupToolbar()
        setupRecyclerView()
        setupInput()
        setupMediaButtons()
        loadChat()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnVoiceCall.setOnClickListener {
            startCall(isVideo = false)
        }

        binding.btnVideoCall.setOnClickListener {
            startCall(isVideo = true)
        }
    }

    private fun setupRecyclerView() {
        adapter = MessagesAdapter(repository.currentUserId)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter
    }

    private fun setupInput() {
        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        binding.btnAttach.setOnClickListener {
            toggleMediaButtons()
        }
    }

    private fun setupMediaButtons() {
        binding.btnTakePhoto.setOnClickListener {
            takePhoto()
        }

        binding.btnGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnVideo.setOnClickListener {
            pickVideoLauncher.launch("video/*")
        }
    }

    private fun toggleMediaButtons() {
        if (binding.layoutMediaButtons.visibility == View.VISIBLE) {
            binding.layoutMediaButtons.visibility = View.GONE
        } else {
            binding.layoutMediaButtons.visibility = View.VISIBLE
        }
    }

    private fun loadChat() {
        lifecycleScope.launch {
            // Get or create chat
            val otherId = otherUserId ?: return@launch

            if (chatId == null) {
                val result = repository.getOrCreateChat(otherId)
                result.fold(
                    onSuccess = { chat ->
                        chatId = chat.id
                        loadMessages()
                        loadUserInfo(otherId)
                    },
                    onFailure = {
                        Toast.makeText(this@ChatActivity, "Error loading chat", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            } else {
                loadMessages()
                loadUserInfo(otherId)
            }
        }
    }

    private fun loadUserInfo(userId: String) {
        lifecycleScope.launch {
            val user = repository.getUser(userId)
            user?.let {
                otherUserName = it.username
                binding.tvChatName.text = it.username
                binding.tvChatStatus.text = if (it.isOnline()) {
                    getString(R.string.online)
                } else {
                    getString(R.string.offline)
                }
            }
        }
    }

    private fun loadMessages() {
        val id = chatId ?: return
        lifecycleScope.launch {
            repository.observeMessages(id).collect { messages ->
                adapter.submitList(messages)
                binding.rvMessages.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) return

        val id = chatId ?: return

        binding.etMessage.text?.clear()

        lifecycleScope.launch {
            repository.sendMessage(id, text).fold(
                onSuccess = {
                    // Message sent
                },
                onFailure = { e ->
                    Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun takePhoto() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
            return
        }

        val photoFile = File.createTempFile("photo", ".jpg", cacheDir)
        photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
        takePhotoLauncher.launch(photoUri)
    }

    private fun uploadAndSendMedia(uri: Uri, mediaType: String) {
        val id = chatId ?: return

        Toast.makeText(this, R.string.loading, Toast.LENGTH_SHORT).show()

        val ref = storage.reference.child("media/${System.currentTimeMillis()}_${uri.lastPathSegment}")
        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                    lifecycleScope.launch {
                        repository.sendMediaMessage(id, downloadUrl.toString(), mediaType).fold(
                            onSuccess = {
                                Toast.makeText(this@ChatActivity, R.string.success, Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { e ->
                                Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startCall(isVideo: Boolean) {
        val otherId = otherUserId ?: return

        lifecycleScope.launch {
            repository.createCall(otherId, isVideo).fold(
                onSuccess = { call ->
                    val intent = Intent(this@ChatActivity, CallActivity::class.java)
                    intent.putExtra("call_id", call.id)
                    intent.putExtra("is_caller", true)
                    intent.putExtra("is_video", isVideo)
                    intent.putExtra("other_user_id", otherId)
                    intent.putExtra("other_user_name", otherUserName)
                    startActivity(intent)
                },
                onFailure = { e ->
                    Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    companion object {
        private const val REQUEST_CAMERA = 1001
    }
}
