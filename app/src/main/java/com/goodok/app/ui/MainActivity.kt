package com.goodok.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.goodok.app.R
import com.goodok.app.data.Repository
import com.goodok.app.data.model.User
import com.goodok.app.databinding.ActivityMainBinding
import com.goodok.app.ui.auth.AuthActivity
import com.goodok.app.ui.calls.CallsActivity
import com.goodok.app.ui.channels.ChannelsActivity
import com.goodok.app.ui.chats.ChatActivity
import com.goodok.app.ui.contacts.ContactsActivity
import com.goodok.app.ui.settings.SettingsActivity
import com.goodok.app.util.LanguageHelper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: Repository
    private lateinit var adapter: UsersAdapter
    private var currentTheme = 0
    private val CONTACTS_PERMISSION_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLanguage()
        applyTheme()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        if (repository.currentUser == null) {
            goToAuth()
            return
        }

        currentTheme = repository.theme

        setupToolbar()
        setupNavigation()
        setupBottomNavigation()
        setupUserList()
        loadUsers()

        // Handle notification intent
        handleNotificationIntent()
    }

    private fun handleNotificationIntent() {
        intent?.let { intent ->
            if (intent.getBooleanExtra("openChat", false)) {
                val senderId = intent.getStringExtra("senderId")
                val senderName = intent.getStringExtra("senderName")
                if (!senderId.isNullOrEmpty()) {
                    openChat(User(
                        uid = senderId,
                        username = senderName ?: "User"
                    ))
                }
            }
        }
    }

    private fun goToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(android.R.drawable.ic_menu_manage)
            title = getString(R.string.app_name)
        }
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupNavigation() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                R.id.nav_premium -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    intent.putExtra("show_premium", true)
                    startActivity(intent)
                }
                R.id.nav_logout -> {
                    repository.logout()
                    goToAuth()
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupBottomNavigation() {
        if (currentTheme >= 2) {
            binding.bottomNavigation.visibility = View.VISIBLE
            binding.bottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_chats -> true
                    R.id.nav_calls -> {
                        startActivity(Intent(this, CallsActivity::class.java))
                        true
                    }
                    R.id.nav_channels -> {
                        startActivity(Intent(this, ChannelsActivity::class.java))
                        true
                    }
                    R.id.nav_contacts -> {
                        startActivity(Intent(this, ContactsActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
        } else {
            binding.bottomNavigation.visibility = View.GONE
        }
    }

    private fun setupUserList() {
        adapter = UsersAdapter { user -> openChat(user) }
        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter
    }

    private fun loadUsers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED) {
            loadUsersFromContacts()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                CONTACTS_PERMISSION_REQUEST
            )
        }
    }

    private fun loadUsersFromContacts() {
        lifecycleScope.launch {
            try {
                val phoneNumbers = getPhoneNumbersFromContacts()

                if (phoneNumbers.isEmpty()) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = getString(R.string.no_contacts)
                    return@launch
                }

                val result = repository.findUsersByPhones(phoneNumbers)
                binding.progressBar.visibility = View.GONE

                result.fold(
                    onSuccess = { users ->
                        val filtered = users.filter { it.uid != repository.currentUserId }
                        adapter.submitList(filtered)
                        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
                    },
                    onFailure = { e ->
                        Log.e("MainActivity", "Error finding users", e)
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.tvEmpty.text = "Error: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading users", e)
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun getPhoneNumbersFromContacts(): List<String> {
        val phones = mutableListOf<String>()
        try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null
            )?.use { cursor ->
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val number = cursor.getString(numberIndex)
                    val normalized = number.filter { it.isDigit() }
                    if (normalized.isNotEmpty()) {
                        phones.add(normalized)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error reading contacts", e)
        }
        return phones.distinct()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CONTACTS_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadUsersFromContacts()
            } else {
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "Grant contacts permission"
            }
        }
    }

    private fun openChat(user: User) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("userId", user.uid)
        intent.putExtra("username", user.username)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        repository.setOnlineStatus(true)
        if (currentTheme != repository.theme) {
            recreate()
        }
    }

    override fun onPause() {
        super.onPause()
        repository.setOnlineStatus(false)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent()
    }
}
