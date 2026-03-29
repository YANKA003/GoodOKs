package com.goodok.app.ui.calls

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.goodok.app.R
import com.goodok.app.data.Repository
import com.goodok.app.data.model.CallStatus
import com.goodok.app.databinding.ActivityCallBinding
import com.goodok.app.webrtc.WebRTCManager
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

class CallActivity : androidx.appcompat.app.AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding
    private lateinit var repository: Repository
    private lateinit var webRTCManager: WebRTCManager

    private var callId: String? = null
    private var isCaller: Boolean = false
    private var isVideo: Boolean = false
    private var otherUserId: String? = null
    private var otherUserName: String = ""

    private var isMuted = false
    private var isSpeakerOn = false
    private var callStartTime: Long = 0

    companion object {
        private const val TAG = "CallActivity"
        private const val REQUEST_PERMISSIONS = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        // Get intent extras
        callId = intent.getStringExtra("call_id")
        isCaller = intent.getBooleanExtra("is_caller", false)
        isVideo = intent.getBooleanExtra("is_video", false)
        otherUserId = intent.getStringExtra("other_user_id")
        otherUserName = intent.getStringExtra("other_user_name") ?: ""

        setupViews()
        checkPermissions()
    }

    private fun setupViews() {
        binding.tvCallerName.text = otherUserName
        binding.tvCallStatus.text = if (isCaller) getString(R.string.calling) else getString(R.string.ringing)

        // Show/hide video views
        if (isVideo) {
            binding.surfaceRemote.visibility = View.VISIBLE
            binding.surfaceLocal.visibility = View.VISIBLE
            binding.btnSwitchCamera.visibility = View.VISIBLE
        } else {
            binding.surfaceRemote.visibility = View.GONE
            binding.surfaceLocal.visibility = View.GONE
            binding.btnSwitchCamera.visibility = View.GONE
        }

        // Buttons
        binding.btnMute.setOnClickListener {
            isMuted = !isMuted
            webRTCManager.setMute(isMuted)
            updateMuteButton()
        }

        binding.btnSpeaker.setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            webRTCManager.setSpeaker(isSpeakerOn)
            updateSpeakerButton()
        }

        binding.btnEndCall.setOnClickListener {
            endCall()
        }

        binding.btnSwitchCamera.setOnClickListener {
            webRTCManager.switchCamera()
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (isVideo && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (permissions.isEmpty()) {
            initWebRTC()
        } else {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                initWebRTC()
            } else {
                Toast.makeText(this, R.string.permission_microphone, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun initWebRTC() {
        webRTCManager = WebRTCManager(this, callId ?: return, isCaller)

        // Initialize surface views
        if (isVideo) {
            binding.surfaceLocal.init(webRTCManager.rootEglBase.eglBaseContext, null)
            binding.surfaceRemote.init(webRTCManager.rootEglBase.eglBaseContext, null)

            webRTCManager.setLocalVideoTarget(binding.surfaceLocal)
        }

        // Set up signaling callbacks
        webRTCManager.onRemoteVideoReceived = { videoTrack ->
            runOnUiThread {
                if (isVideo) {
                    videoTrack.addSink(binding.surfaceRemote)
                }
            }
        }

        webRTCManager.onConnected = {
            runOnUiThread {
                onCallConnected()
            }
        }

        webRTCManager.onDisconnected = {
            runOnUiThread {
                onCallDisconnected()
            }
        }

        webRTCManager.onError = { error ->
            runOnUiThread {
                Toast.makeText(this, "Call error: $error", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Start call
        webRTCManager.start(isVideo)
    }

    private fun onCallConnected() {
        callStartTime = System.currentTimeMillis()
        binding.tvCallStatus.text = getString(R.string.connected)
        binding.tvDuration.visibility = View.VISIBLE
        startDurationTimer()
    }

    private fun onCallDisconnected() {
        binding.tvCallStatus.text = getString(R.string.call_ended)
        finish()
    }

    private fun startDurationTimer() {
        Thread {
            while (!isFinishing) {
                Thread.sleep(1000)
                val duration = (System.currentTimeMillis() - callStartTime) / 1000
                runOnUiThread {
                    binding.tvDuration.text = formatDuration(duration)
                }
            }
        }.start()
    }

    private fun formatDuration(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }

    private fun updateMuteButton() {
        val icon = if (isMuted) {
            android.R.drawable.ic_btn_speak_now
        } else {
            android.R.drawable.ic_btn_speak_now
        }
        binding.btnMute.setImageResource(icon)
        binding.btnMute.alpha = if (isMuted) 0.5f else 1f
    }

    private fun updateSpeakerButton() {
        binding.btnSpeaker.alpha = if (isSpeakerOn) 1f else 0.5f
    }

    private fun endCall() {
        // Update call status in database
        callId?.let { id ->
            val updates = mapOf(
                "status" to CallStatus.ENDED.name,
                "endTime" to System.currentTimeMillis(),
                "duration" to (System.currentTimeMillis() - callStartTime) / 1000
            )
            FirebaseDatabase.getInstance().getReference("calls").child(id).updateChildren(updates)
        }

        webRTCManager.endCall()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::webRTCManager.isInitialized) {
            webRTCManager.endCall()
        }
    }
}
