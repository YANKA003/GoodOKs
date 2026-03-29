package com.goodok.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.goodok.app.R
import com.goodok.app.data.Repository
import com.goodok.app.databinding.ActivityMainBinding
import com.goodok.app.ui.calls.CallsFragment
import com.goodok.app.ui.channels.ChannelsFragment
import com.goodok.app.ui.contacts.ContactsFragment
import com.goodok.app.ui.chats.ChatsFragment
import com.goodok.app.ui.settings.SettingsActivity
import com.goodok.app.util.LanguageHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeAndLanguage()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        // Check login
        if (!repository.isLoggedIn) {
            goToAuth()
            return
        }

        setupNavigation()

        // Show chats by default
        if (savedInstanceState == null) {
            showFragment(ChatsFragment.newInstance())
        }
    }

    private fun applyThemeAndLanguage() {
        try {
            val prefs = getSharedPreferences("goodok_prefs", MODE_PRIVATE)
            val theme = prefs.getInt("theme", 0)
            when (theme) {
                1 -> setTheme(R.style.Theme_GoodOK_Modern)
                2 -> setTheme(R.style.Theme_GoodOK_Neon)
                3 -> setTheme(R.style.Theme_GoodOK_Childish)
            }

            val lang = prefs.getString("language", "ru") ?: "ru"
            LanguageHelper.applyLanguage(this, lang)
        } catch (e: Exception) {
            setTheme(R.style.Theme_GoodOK_Classic)
        }
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chats -> {
                    showFragment(ChatsFragment.newInstance())
                    true
                }
                R.id.nav_calls -> {
                    showFragment(CallsFragment.newInstance())
                    true
                }
                R.id.nav_contacts -> {
                    showFragment(ContactsFragment.newInstance())
                    true
                }
                R.id.nav_channels -> {
                    showFragment(ChannelsFragment.newInstance())
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun goToAuth() {
        // Will be handled by AuthActivity
    }
}
