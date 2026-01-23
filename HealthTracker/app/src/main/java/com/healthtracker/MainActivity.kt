package com.healthtracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.healthtracker.databinding.ActivityMainBinding
import com.healthtracker.sync.SyncManager
import com.healthtracker.ui.AddUnifiedActivity
import com.healthtracker.ui.EntryActivity
import com.healthtracker.ui.HomeFeedAdapter
import com.healthtracker.ui.HealthTrackerViewModel
import com.healthtracker.ui.SettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: HealthTrackerViewModel by viewModels()
    private lateinit var adapter: HomeFeedAdapter
    
    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the Toolbar
        var title = getString(R.string.app_name)
        if (com.healthtracker.BuildConfig.FLAVOR == "dev") {
            title += " [DEV]"
        }
        binding.toolbar.title = title
        binding.toolbar.inflateMenu(R.menu.menu_main)
        
        // Set up toolbar menu item clicks
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.action_add_workout -> {
                    val intent = Intent(this, AddUnifiedActivity::class.java).apply {
                        putExtra(AddUnifiedActivity.EXTRA_ENTRY_TYPE, AddUnifiedActivity.ENTRY_TYPE_WORKOUT)
                    }
                    startActivity(intent)
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
        adapter = HomeFeedAdapter(
            onHealthEntryClick = { entry ->
                val intent = Intent(this, EntryActivity::class.java).apply {
                    putExtra(EntryActivity.EXTRA_ENTRY_ID, entry.id)
                }
                startActivity(intent)
            },
            onWorkoutEntryClick = { workoutId ->
                val intent = Intent(this, AddUnifiedActivity::class.java).apply {
                    putExtra(AddUnifiedActivity.EXTRA_ENTRY_TYPE, AddUnifiedActivity.ENTRY_TYPE_WORKOUT)
                    putExtra(AddUnifiedActivity.EXTRA_WORKOUT_ID, workoutId)
                }
                startActivity(intent)
            }
        )
        binding.entriesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.entriesRecyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddEntry.setOnClickListener {
            startActivity(Intent(this, AddUnifiedActivity::class.java))
        }

        // Temporary button
        val resyncButton: View = binding.inspectServerButton
        resyncButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    Log.d("MainActivity", "Bouton de resynchronisation cliqué. Lancement de la resynchronisation complète...")
                    Toast.makeText(this@MainActivity, "Lancement de la resynchronisation complète...", Toast.LENGTH_SHORT).show()
                    syncManager.performFullResynchronization()
                    Toast.makeText(this@MainActivity, "Resynchronisation complète terminée.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erreur lors de la resynchronisation complète", e)
                    Toast.makeText(this@MainActivity, "Erreur de resync: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.homeItems.observe(this) { items ->
            adapter.submitList(items)
            binding.entriesRecyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
            // TODO: Add empty state view and show it when items.isEmpty()
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
        viewModel.loadHomeItems()
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
