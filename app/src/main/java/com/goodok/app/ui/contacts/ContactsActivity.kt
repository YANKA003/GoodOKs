package com.goodok.app.ui.contacts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.goodok.app.R
import com.goodok.app.data.Repository
import com.goodok.app.databinding.ActivityContactsBinding
import com.goodok.app.util.LanguageHelper
import kotlinx.coroutines.launch

class ContactsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContactsBinding
    private lateinit var repository: Repository
    private lateinit var adapter: ContactsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLanguage()
        applyTheme()
        super.onCreate(savedInstanceState)

        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        setupToolbar()
        setupRecyclerView()
        loadContacts()
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
        supportActionBar?.title = getString(R.string.contacts)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = ContactsAdapter { contact ->
            // Open contact
        }
        binding.rvContacts.layoutManager = LinearLayoutManager(this)
        binding.rvContacts.adapter = adapter
    }

    private fun loadContacts() {
        binding.progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            repository.getAllContactsFlow().collect { contacts ->
                binding.progressBar.visibility = android.view.View.GONE
                adapter.submitList(contacts)
                binding.tvEmpty.visibility = if (contacts.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }
}
