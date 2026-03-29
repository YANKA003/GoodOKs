package com.goodok.app.ui.calls

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.goodok.app.R
import com.goodok.app.databinding.ActivityCallBinding
import com.goodok.app.util.LanguageHelper

class CallActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCallBinding
    private var callerName: String? = null
    private var callType: String = "VOICE"

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLanguage()
        applyTheme()
        super.onCreate(savedInstanceState)

        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        callerName = intent.getStringExtra("callerName")
        callType = intent.getStringExtra("type") ?: "VOICE"

        setupViews()
    }

    private fun applyLanguage() {
        try {
            val repo = com.goodok.app.data.Repository(applicationContext)
            LanguageHelper.setLanguage(this, repo.language)
        } catch (e: Exception) {
            LanguageHelper.setLanguage(this, "ru")
        }
    }

    private fun applyTheme() {
        try {
            val repo = com.goodok.app.data.Repository(applicationContext)
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

    private fun setupViews() {
        binding.tvCallerName.text = callerName ?: "Unknown"
        binding.tvCallType.text = if (callType == "VIDEO") "Video Call" else "Voice Call"

        binding.btnEndCall.setOnClickListener {
            finish()
        }

        binding.btnAnswer.setOnClickListener {
            // Answer call logic
        }

        binding.btnMute.setOnClickListener {
            // Mute logic
        }

        binding.btnSpeaker.setOnClickListener {
            // Speaker logic
        }
    }
}
