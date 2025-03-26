package com.healthtracker.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.datepicker.MaterialDatePicker
import com.healthtracker.databinding.ActivityAddEntryBinding
import com.healthtracker.ui.HealthTrackerViewModel
import com.healthtracker.data.HealthEntry
import java.time.LocalDateTime
import java.time.ZoneId

class AddEntryActivity : AppCompatActivity() {
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
                .setTitleText("Select date")
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val date = LocalDateTime.ofInstant(
                    java.util.Date(selection).toInstant(),
                    ZoneId.systemDefault()
                )
                binding.timestampEditText.setText(date.toString())
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
                notes = binding.notesEditText.text.toString()
            )
            viewModel.addEntry(entry)
            finish()
        }
    }
}
