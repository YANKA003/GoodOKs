package com.goodok.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.goodok.app.R
import com.goodok.app.data.Repository
import com.goodok.app.data.model.AppLanguage
import com.goodok.app.data.model.AppTheme
import com.goodok.app.databinding.ActivityAuthBinding
import com.goodok.app.ui.main.MainActivity
import com.goodok.app.util.LanguageHelper
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var repository: Repository
    private var isLoginMode = true

    // Phone validation pattern
    private val phonePattern = Pattern.compile("^\\+?[1-9]\\d{6,14}$")

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeAndLanguage()
        super.onCreate(savedInstanceState)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        // Check if already logged in
        if (repository.isLoggedIn) {
            goToMain()
            return
        }

        setupViews()
        setupSpinners()
    }

    private fun applyThemeAndLanguage() {
        // Apply saved theme
        try {
            val prefs = getSharedPreferences("goodok_prefs", MODE_PRIVATE)
            val theme = prefs.getInt("theme", AppTheme.CLASSIC.id)
            when (theme) {
                AppTheme.MODERN.id -> setTheme(R.style.Theme_GoodOK_Modern)
                AppTheme.NEON.id -> setTheme(R.style.Theme_GoodOK_Neon)
                AppTheme.CHILDISH.id -> setTheme(R.style.Theme_GoodOK_Childish)
                else -> setTheme(R.style.Theme_GoodOK_Classic)
            }

            // Apply saved language
            val lang = prefs.getString("language", "ru") ?: "ru"
            LanguageHelper.setLanguage(this, lang)
        } catch (e: Exception) {
            setTheme(R.style.Theme_GoodOK_Classic)
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

    private fun setupSpinners() {
        // Language spinner
        val languages = AppLanguage.values()
        val languageNames = languages.map { it.displayName }.toTypedArray()
        val languageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languageNames)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguage.adapter = languageAdapter
        binding.spinnerLanguage.setSelection(languages.indexOf(AppLanguage.RUSSIAN))

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
    }

    private fun updateMode() {
        if (isLoginMode) {
            // Login mode
            binding.tvTitle.text = getString(R.string.login)
            binding.btnAuth.text = getString(R.string.login)
            binding.tvSwitchMode.text = getString(R.string.no_account)

            // Hide registration fields
            binding.etUsername.visibility = View.GONE
            binding.layoutPhone.visibility = View.GONE
            binding.etPasswordConfirm.visibility = View.GONE
            binding.layoutLanguage.visibility = View.GONE
            binding.layoutTheme.visibility = View.GONE
        } else {
            // Registration mode
            binding.tvTitle.text = getString(R.string.register)
            binding.btnAuth.text = getString(R.string.register)
            binding.tvSwitchMode.text = getString(R.string.have_account)

            // Show registration fields
            binding.etUsername.visibility = View.VISIBLE
            binding.layoutPhone.visibility = View.VISIBLE
            binding.etPasswordConfirm.visibility = View.VISIBLE
            binding.layoutLanguage.visibility = View.VISIBLE
            binding.layoutTheme.visibility = View.VISIBLE
        }
    }

    private fun authenticate() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val passwordConfirm = binding.etPasswordConfirm.text.toString().trim()

        // Validation
        if (!validateFields(email, password, username, phone, passwordConfirm)) {
            return
        }

        // Show loading
        binding.btnAuth.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = if (isLoginMode) {
                    repository.login(email, password)
                } else {
                    // Get selected language and theme
                    val selectedLanguage = AppLanguage.values()[binding.spinnerLanguage.selectedItemPosition]
                    val selectedTheme = binding.spinnerTheme.selectedItemPosition

                    repository.register(
                        email = email,
                        password = password,
                        username = username,
                        phone = phone,
                        language = selectedLanguage.code,
                        theme = selectedTheme
                    )
                }

                result.fold(
                    onSuccess = {
                        Toast.makeText(this@AuthActivity, R.string.success, Toast.LENGTH_SHORT).show()
                        goToMain()
                    },
                    onFailure = { e ->
                        Log.e("AuthActivity", "Auth failed", e)
                        val errorMessage = e.message ?: getString(R.string.unknown_error)
                        Toast.makeText(this@AuthActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthActivity", "Auth error", e)
                Toast.makeText(this@AuthActivity, "${getString(R.string.error)}: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnAuth.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun validateFields(
        email: String,
        password: String,
        username: String,
        phone: String,
        passwordConfirm: String
    ): Boolean {
        // Check email
        if (email.isEmpty()) {
            binding.etEmail.error = getString(R.string.fill_all_fields)
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Invalid email format"
            return false
        }

        // Check password
        if (password.isEmpty()) {
            binding.etPassword.error = getString(R.string.fill_all_fields)
            return false
        }

        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            return false
        }

        if (!isLoginMode) {
            // Registration validation

            // Check username
            if (username.isEmpty()) {
                binding.etUsername.error = getString(R.string.fill_all_fields)
                return false
            }

            // Check phone (REQUIRED for registration)
            if (phone.isEmpty()) {
                binding.etPhone.error = getString(R.string.fill_all_fields)
                Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show()
                return false
            }

            // Validate phone format
            val cleanPhone = phone.replace("[\\s\\-\\(\\)]".toRegex(), "")
            if (!phonePattern.matcher(cleanPhone).matches()) {
                binding.etPhone.error = getString(R.string.invalid_phone)
                return false
            }

            // Check password confirmation
            if (passwordConfirm.isEmpty()) {
                binding.etPasswordConfirm.error = getString(R.string.fill_all_fields)
                return false
            }

            if (password != passwordConfirm) {
                binding.etPasswordConfirm.error = getString(R.string.passwords_not_match)
                Toast.makeText(this, R.string.passwords_not_match, Toast.LENGTH_SHORT).show()
                return false
            }
        }

        return true
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
