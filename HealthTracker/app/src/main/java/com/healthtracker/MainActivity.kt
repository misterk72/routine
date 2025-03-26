package com.healthtracker

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.healthtracker.databinding.ActivityMainBinding
import com.healthtracker.ui.HealthEntryAdapter
import com.healthtracker.ui.HealthTrackerViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDateTime
import java.time.ZoneId

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
        binding.toolbar.title = "Health Tracker"

        setupRecyclerView()
        setupFab()
        observeEntries()
    }

    private fun setupRecyclerView() {
        adapter = HealthEntryAdapter { _ ->
            // Handle entry click
        }
        binding.entriesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.entriesRecyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddEntry.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val date = LocalDateTime.ofInstant(
                java.util.Date(selection).toInstant(),
                ZoneId.systemDefault()
            )
            // Handle date selection with a placeholder entry
            val entry = com.healthtracker.data.HealthEntry(
                timestamp = date,
                weight = 0f,
                waistMeasurement = 0f,
                notes = "New entry"
            )
            viewModel.addEntry(entry)
        }

        datePicker.show(supportFragmentManager, "datePicker")
    }

    private fun observeEntries() {
        viewModel.entries.observe(this) { entries ->
            adapter.submitList(entries)
        }
    }
}
