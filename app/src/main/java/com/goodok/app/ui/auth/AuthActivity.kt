package com.goodok.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.goodok.app.R
import com.goodok.app.data.Repository
import com.goodok.app.databinding.ActivityAuthBinding
import com.goodok.app.ui.MainActivity
import com.goodok.app.util.LanguageHelper
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private lateinit var repository: Repository
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme()
        applyLanguage()
        super.onCreate(savedInstanceState)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        // Check if already logged in
        if (repository.currentUser != null) {
            goToMain()
            return
        }

        setupViews()
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

    private fun applyLanguage() {
        try {
            val repo = Repository(applicationContext)
            LanguageHelper.setLanguage(this, repo.language)
        } catch (e: Exception) {
            LanguageHelper.setLanguage(this, "ru")
        }
    }

    private fun setupViews() {
        updateMode()

        binding.tvSwitchMode.setOnClickListener {
            isLoginMode = !isLoginMode
            updateMode()
        }

        binding.btnAuth.setOnClickListener {
            authenticate()
        }
    }

    private fun updateMode() {
        if (isLoginMode) {
            binding.tvTitle.text = getString(R.string.login)
            binding.btnAuth.text = getString(R.string.login)
            binding.tvSwitchMode.text = getString(R.string.no_account)
            binding.etPhone.visibility = android.view.View.GONE
        } else {
            binding.tvTitle.text = getString(R.string.register)
            binding.btnAuth.text = getString(R.string.register)
            binding.tvSwitchMode.text = getString(R.string.have_account)
            binding.etPhone.visibility = android.view.View.VISIBLE
        }
    }

    private fun authenticate() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show()
            return
        }

        if (!isLoginMode && username.isEmpty()) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnAuth.isEnabled = false
        binding.progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = if (isLoginMode) {
                    repository.login(email, password)
                } else {
                    repository.registerWithPhone(email, password, username, phone)
                }

                result.fold(
                    onSuccess = {
                        // Update push token on server
                        repository.updatePushToken()
                        goToMain()
                    },
                    onFailure = { e ->
                        Log.e("AuthActivity", "Auth failed", e)
                        Toast.makeText(
                            this@AuthActivity,
                            "${getString(R.string.error)}: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthActivity", "Auth error", e)
                Toast.makeText(this@AuthActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnAuth.isEnabled = true
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
