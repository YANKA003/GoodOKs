package com.goodok.app.ui.channels

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.goodok.app.R
import com.goodok.app.data.Repository
import com.goodok.app.databinding.ActivityChannelsBinding
import com.goodok.app.util.LanguageHelper
import kotlinx.coroutines.launch

class ChannelsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChannelsBinding
    private lateinit var repository: Repository
    private lateinit var adapter: ChannelsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLanguage()
        applyTheme()
        super.onCreate(savedInstanceState)

        binding = ActivityChannelsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        setupToolbar()
        setupRecyclerView()
        loadChannels()
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
        supportActionBar?.title = getString(R.string.channels)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = ChannelsAdapter { channel ->
            // Open channel
        }
        binding.rvChannels.layoutManager = LinearLayoutManager(this)
        binding.rvChannels.adapter = adapter
    }

    private fun loadChannels() {
        binding.progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = repository.searchChannels("")
                binding.progressBar.visibility = android.view.View.GONE

                result.fold(
                    onSuccess = { channels ->
                        adapter.submitList(channels)
                    },
                    onFailure = { e ->
                        binding.tvEmpty.visibility = android.view.View.VISIBLE
                    }
                )
            } catch (e: Exception) {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }
}
