package com.goodok.app.ui.calls

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.goodok.app.databinding.ActivityIncomingCallBinding
import com.goodok.app.data.Repository
import com.goodok.app.data.model.CallStatus
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

class IncomingCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncomingCallBinding
    private lateinit var repository: Repository

    private var callId: String? = null
    private var callerName: String = ""
    private var isVideo: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        callId = intent.getStringExtra("call_id")
        callerName = intent.getStringExtra("caller_name") ?: ""
        isVideo = intent.getBooleanExtra("is_video", false)

        setupViews()
    }

    private fun setupViews() {
        binding.tvCallerName.text = callerName
        binding.tvCallType.text = if (isVideo) {
            com.goodok.app.R.string.video_call
        } else {
            com.goodok.app.R.string.voice_call
        }

        binding.btnAccept.setOnClickListener {
            acceptCall()
        }

        binding.btnDecline.setOnClickListener {
            declineCall()
        }
    }

    private fun acceptCall() {
        lifecycleScope.launch {
            // Update call status
            callId?.let { id ->
                FirebaseDatabase.getInstance()
                    .getReference("calls")
                    .child(id)
                    .child("status")
                    .setValue(CallStatus.CONNECTED)
            }

            // Open CallActivity
            val intent = android.content.Intent(this@IncomingCallActivity, CallActivity::class.java)
            intent.putExtra("call_id", callId)
            intent.putExtra("is_caller", false)
            intent.putExtra("is_video", isVideo)
            startActivity(intent)
            finish()
        }
    }

    private fun declineCall() {
        lifecycleScope.launch {
            callId?.let { id ->
                FirebaseDatabase.getInstance()
                    .getReference("calls")
                    .child(id)
                    .child("status")
                    .setValue(CallStatus.DECLINED)
            }
            finish()
        }
    }
}
