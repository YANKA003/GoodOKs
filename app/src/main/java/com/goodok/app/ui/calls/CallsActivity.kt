package com.goodok.app.ui.calls

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.goodok.app.R
import com.goodok.app.data.Repository
import com.goodok.app.databinding.ActivityCallsBinding
import com.goodok.app.util.LanguageHelper
import kotlinx.coroutines.launch

class CallsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCallsBinding
    private lateinit var repository: Repository
    private lateinit var adapter: CallsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLanguage()
        applyTheme()
        super.onCreate(savedInstanceState)

        binding = ActivityCallsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        setupToolbar()
        setupRecyclerView()
        observeCalls()
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
        supportActionBar?.title = getString(R.string.calls)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = CallsAdapter()
        binding.rvCalls.layoutManager = LinearLayoutManager(this)
        binding.rvCalls.adapter = adapter
    }

    private fun observeCalls() {
        lifecycleScope.launch {
            repository.observeCalls().collect { calls ->
                adapter.submitList(calls)
            }
        }
    }
}
