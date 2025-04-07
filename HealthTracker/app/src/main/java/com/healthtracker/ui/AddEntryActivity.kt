package com.healthtracker.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.datepicker.MaterialDatePicker
import com.healthtracker.R
import com.healthtracker.databinding.ActivityAddEntryBinding
import com.healthtracker.ui.HealthTrackerViewModel
import com.healthtracker.data.HealthEntry
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AddEntryActivity : AppCompatActivity() {
    companion object {
        // French-style date format
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy, HH'h'mm", java.util.Locale.FRENCH)
        
        // Custom formatter to capitalize first letter of day name
        private fun formatWithCapitalizedDay(dateTime: LocalDateTime, pattern: String): String {
            val formatted = DateTimeFormatter.ofPattern(pattern, java.util.Locale.FRENCH).format(dateTime)
            // Capitalize first letter of the day name
            return formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.FRENCH) else it.toString() }
        }
    }
    private lateinit var binding: ActivityAddEntryBinding
    private lateinit var viewModel: HealthTrackerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        viewModel = ViewModelProvider(this)[HealthTrackerViewModel::class.java]

        setupDatePicker()
        setupSaveButton()
    }

    private fun setupDatePicker() {
        binding.timestampEditText.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.date_and_time))
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val date = LocalDateTime.ofInstant(
                    java.util.Date(selection).toInstant(),
                    ZoneId.systemDefault()
                )
                // Display date in user-friendly format with capitalized day name
                binding.timestampEditText.setText(formatWithCapitalizedDay(date, "EEEE d MMMM yyyy, HH'h'mm"))
            }

            datePicker.show(supportFragmentManager, "datePicker")
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            val entry = HealthEntry(
                timestamp = LocalDateTime.now(),
                weight = binding.weightEditText.text.toString().toFloatOrNull(),
                waistMeasurement = binding.waistEditText.text.toString().toFloatOrNull(),
                bodyFat = binding.bodyFatEditText.text.toString().toFloatOrNull(),
                notes = binding.notesEditText.text.toString()
            )
            viewModel.addEntry(entry)
            finish()
        }
    }
}
