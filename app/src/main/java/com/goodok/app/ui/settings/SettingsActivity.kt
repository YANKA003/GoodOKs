package com.goodok.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.goodok.app.BuildConfig
import com.goodok.app.R
import com.goodok.app.data.Repository
import com.goodok.app.data.model.AppLanguage
import com.goodok.app.data.model.AppTheme
import com.goodok.app.databinding.ActivitySettingsBinding
import com.goodok.app.ui.auth.AuthActivity
import com.goodok.app.util.LanguageHelper
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repository: Repository
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var executor: Executor

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeAndLanguage()
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)
        executor = ContextCompat.getMainExecutor(this)

        setupBiometric()
        setupViews()
        loadUserData()
    }

    private fun applyThemeAndLanguage() {
        try {
            val prefs = getSharedPreferences("goodok_prefs", MODE_PRIVATE)
            val theme = prefs.getInt("theme", AppTheme.CLASSIC.id)
            when (theme) {
                AppTheme.MODERN.id -> setTheme(R.style.Theme_GoodOK_Modern)
                AppTheme.NEON.id -> setTheme(R.style.Theme_GoodOK_Neon)
                AppTheme.CHILDISH.id -> setTheme(R.style.Theme_GoodOK_Childish)
            }

            val lang = prefs.getString("language", "ru") ?: "ru"
            LanguageHelper.applyLanguage(this, lang)
        } catch (e: Exception) {
            setTheme(R.style.Theme_GoodOK_Classic)
        }
    }

    private fun setupBiometric() {
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // Biometric auth succeeded, enable the feature
                runOnUiThread {
                    repository.biometricEnabled = true
                    binding.switchBiometric.isChecked = true
                    Toast.makeText(this@SettingsActivity, R.string.biometric_enabled, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                runOnUiThread {
                    binding.switchBiometric.isChecked = false
                    Toast.makeText(this@SettingsActivity, R.string.error, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun setupViews() {
        // Back button
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Edit profile
        binding.btnEditProfile.setOnClickListener {
            // TODO: Open edit profile dialog
            Toast.makeText(this, R.string.edit_profile, Toast.LENGTH_SHORT).show()
        }

        // Biometric switch
        binding.switchBiometric.isChecked = repository.biometricEnabled
        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkBiometricAndEnable()
            } else {
                repository.biometricEnabled = false
                Toast.makeText(this, R.string.biometric_disabled, Toast.LENGTH_SHORT).show()
            }
        }

        // Language spinner
        val languages = AppLanguage.values().map { it.displayName }
        val languageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguage.adapter = languageAdapter

        // Set current language
        val currentLang = AppLanguage.values().find { it.code == repository.language }
        currentLang?.let { binding.spinnerLanguage.setSelection(AppLanguage.values().indexOf(it)) }

        binding.spinnerLanguage.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selected = AppLanguage.values()[position]
                if (selected.code != repository.language) {
                    repository.language = selected.code
                    LanguageHelper.applyLanguage(this@SettingsActivity, selected.code)
                    recreate()
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Theme spinner
        val themes = listOf(
            getString(R.string.theme_classic),
            getString(R.string.theme_modern),
            getString(R.string.theme_neon),
            getString(R.string.theme_childish)
        )
        val themeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themes)
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTheme.adapter = themeAdapter
        binding.spinnerTheme.setSelection(repository.theme)

        binding.spinnerTheme.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position != repository.theme) {
                    repository.theme = position
                    recreate()
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Notifications
        binding.switchNotifications.isChecked = true
        binding.switchSound.isChecked = true
        binding.switchVibration.isChecked = true

        // Version
        binding.tvVersion.text = getString(R.string.version, BuildConfig.VERSION_NAME)

        // Logout
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadUserData() {
        repository.currentUser?.let { user ->
            binding.tvUsername.text = user.username
            binding.tvEmail.text = user.email

            user.avatarUrl?.let { url ->
                Glide.with(this)
                    .load(url)
                    .circleCrop()
                    .into(binding.ivAvatar)
            }
        }
    }

    private fun checkBiometricAndEnable() {
        val biometricManager = BiometricManager.from(this)

        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(getString(R.string.biometric_title))
                    .setSubtitle(getString(R.string.biometric_subtitle))
                    .setNegativeButtonText(getString(R.string.cancel))
                    .setAuthenticationEngagementType(BiometricPrompt.AUTHENTICATION_ENGINE_UNKNOWN)
                    .build()

                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                binding.switchBiometric.isChecked = false
                Toast.makeText(this, "Отпечаток пальца не поддерживается", Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                binding.switchBiometric.isChecked = false
                Toast.makeText(this, "Добавьте отпечаток пальца в настройках устройства", Toast.LENGTH_SHORT).show()
            }
            else -> {
                binding.switchBiometric.isChecked = false
                Toast.makeText(this, "Ошибка биометрии", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirm)
            .setPositiveButton(R.string.yes) { _, _ ->
                performLogout()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun performLogout() {
        repository.logout()
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
