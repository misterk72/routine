package com.healthtracker

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthtracker.data.HealthEntry
import com.healthtracker.databinding.ActivityMainBinding
import com.healthtracker.ui.AddEntryActivity
import com.healthtracker.ui.EntryActivity
import com.healthtracker.ui.HealthEntryAdapter
import com.healthtracker.ui.HealthTrackerViewModel
import com.healthtracker.ui.SettingsActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: HealthTrackerViewModel by viewModels()
    private lateinit var adapter: HealthEntryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the Toolbar
        binding.toolbar.title = getString(R.string.app_name)
        binding.toolbar.inflateMenu(R.menu.menu_main)
        
        // Set up toolbar menu item clicks
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = HealthEntryAdapter { entry ->
            // Launch EntryActivity to edit the selected entry
            val intent = Intent(this, EntryActivity::class.java).apply {
                putExtra(EntryActivity.EXTRA_ENTRY_ID, entry.id)
            }
            startActivity(intent)
        }
        binding.entriesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.entriesRecyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddEntry.setOnClickListener {
            // Launch AddEntryActivity to create a new entry
            startActivity(Intent(this, AddEntryActivity::class.java))
        }
    }

    private fun observeViewModel() {
        // Observe entries with user information
        viewModel.entriesWithUser.observe(this) { entriesWithUser ->
            adapter.submitList(entriesWithUser)
            
            // Show empty state if no entries
            binding.entriesRecyclerView.visibility = if (entriesWithUser.isEmpty()) View.GONE else View.VISIBLE
            // TODO: Add empty state view and show it when entries.isEmpty()
        }
        
        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            // TODO: Show loading indicator when isLoading is true
            // For now we'll just disable the FAB during loading
            binding.fabAddEntry.isEnabled = !isLoading
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh entries when returning to this screen
        viewModel.loadEntriesWithUser()
    }
}
