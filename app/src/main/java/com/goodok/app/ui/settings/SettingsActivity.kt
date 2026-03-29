package com.goodok.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.goodok.app.R
import com.goodok.app.billing.BillingManager
import com.goodok.app.data.Repository
import com.goodok.app.databinding.ActivitySettingsBinding
import com.goodok.app.ui.auth.AuthActivity
import com.goodok.app.ui.premium.PremiumActivity
import com.goodok.app.util.IconHelper
import com.goodok.app.util.LanguageHelper
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repository: Repository
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLanguage()
        applyTheme()
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)
        billingManager = BillingManager(this)

        setupToolbar()
        setupSettings()
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
        supportActionBar?.title = getString(R.string.settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupSettings() {
        // Theme selection
        val themes = arrayOf("Classic", "Modern", "Neon", "Childish")
        binding.tvThemeValue.text = themes[repository.theme]

        binding.llTheme.setOnClickListener {
            showThemeDialog()
        }

        // Language selection
        val languages = LanguageHelper.getSupportedLanguages()
        binding.tvLanguageValue.text = LanguageHelper.getLanguageDisplayName(repository.language)

        binding.llLanguage.setOnClickListener {
            showLanguageDialog()
        }

        // Premium
        binding.llPremium.setOnClickListener {
            startActivity(Intent(this, PremiumActivity::class.java))
        }

        if (repository.isPremium) {
            binding.tvPremiumValue.text = repository.premiumType
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            repository.logout()
            val intent = Intent(this, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showThemeDialog() {
        val themes = arrayOf("Classic", "Modern", "Neon", "Childish")
        android.app.AlertDialog.Builder(this)
            .setTitle("Select Theme")
            .setSingleChoiceItems(themes, repository.theme) { dialog, which ->
                repository.setTheme(which)
                IconHelper.setAppIcon(this, which + 1)
                dialog.dismiss()
                recreate()
            }
            .show()
    }

    private fun showLanguageDialog() {
        val languages = LanguageHelper.getSupportedLanguages()
        val names = languages.map { LanguageHelper.getLanguageDisplayName(it) }.toTypedArray()

        android.app.AlertDialog.Builder(this)
            .setTitle("Select Language")
            .setSingleChoiceItems(names, languages.indexOf(repository.language)) { dialog, which ->
                repository.setLanguage(languages[which])
                dialog.dismiss()
                recreate()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.endConnection()
    }
}
