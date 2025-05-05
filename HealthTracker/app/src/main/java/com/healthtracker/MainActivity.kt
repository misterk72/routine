package com.healthtracker

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.healthtracker.data.HealthEntry
import com.healthtracker.databinding.ActivityMainBinding
import com.healthtracker.sync.SyncManager
import com.healthtracker.ui.AddEntryActivity
import com.healthtracker.ui.EntryActivity
import com.healthtracker.ui.HealthEntryAdapter
import com.healthtracker.ui.HealthTrackerViewModel
import com.healthtracker.ui.SettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: HealthTrackerViewModel by viewModels()
    private lateinit var adapter: HealthEntryAdapter
    
    @Inject
    lateinit var syncManager: SyncManager

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
                R.id.action_sync -> {
                    synchronizeData()
                    true
                }
                else -> false
            }
        }
        
        // Planifier la synchronisation périodique
        syncManager.scheduleSyncWork()

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
    
    /**
     * Déclenche la synchronisation des données avec le serveur via HTTP
     */
    private fun synchronizeData() {
        Toast.makeText(this, R.string.sync_in_progress, Toast.LENGTH_SHORT).show()
        
        // Déclencher la synchronisation immédiate et récupérer l'UUID
        val syncWorkId = syncManager.syncNow()
        
        // Observer le statut de la synchronisation spécifique
        WorkManager.getInstance(this)
            .getWorkInfoByIdLiveData(syncWorkId)
            .observe(this) { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        Toast.makeText(this, R.string.sync_success, Toast.LENGTH_SHORT).show()
                        // Rafraîchir les données après synchronisation
                        viewModel.loadEntriesWithUser()
                    }
                    WorkInfo.State.FAILED -> {
                        Toast.makeText(this, R.string.sync_error, Toast.LENGTH_SHORT).show()
                    }
                    else -> { /* États intermédiaires, ne rien faire */ }
                }
            }
    }
}
