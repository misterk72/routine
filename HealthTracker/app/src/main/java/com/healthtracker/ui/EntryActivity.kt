package com.healthtracker.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthtracker.R
import com.healthtracker.data.HealthEntry
import com.healthtracker.data.MetricValue
import com.healthtracker.databinding.ActivityEntryBinding
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class EntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntryBinding
    private val viewModel: EntryViewModel by viewModels()
    private lateinit var metricValuesAdapter: MetricValuesAdapter

    companion object {
        const val EXTRA_ENTRY_ID = "entry_id"
        
        // Custom formatter to capitalize first letter of day name
        private fun formatWithCapitalizedDay(dateTime: LocalDateTime, pattern: String): String {
            val formatted = DateTimeFormatter.ofPattern(pattern, java.util.Locale.FRENCH).format(dateTime)
            // Capitalize first letter of the day name
            return formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.FRENCH) else it.toString() }
        }
        
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", java.util.Locale.FRENCH)
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH'h'mm", java.util.Locale.FRENCH)
        private val DATETIME_FORMATTER = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy, HH'h'mm", java.util.Locale.FRENCH)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        setupMetricValuesAdapter()
        setupListeners()
        observeViewModel()
        
        // Check if we're editing an existing entry or creating a new one
        val entryId = intent.getLongExtra(EXTRA_ENTRY_ID, 0L)
        if (entryId > 0) {
            viewModel.loadEntry(entryId)
            title = getString(R.string.edit_health_entry)
        } else {
            viewModel.createNewEntry()
            title = getString(R.string.new_health_entry)
        }
    }

    private fun setupMetricValuesAdapter() {
        metricValuesAdapter = MetricValuesAdapter { metricValue ->
            viewModel.removeMetricValue(metricValue)
        }
        binding.additionalMetricsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@EntryActivity)
            adapter = metricValuesAdapter
        }
    }

    private fun setupListeners() {
        // Date/Time edit button
        binding.editDateTimeButton.setOnClickListener {
            val currentDateTime = viewModel.currentEntry.value?.timestamp ?: LocalDateTime.now()
            showDateTimePicker(currentDateTime)
        }

        // Save button
        binding.fabSaveEntry.setOnClickListener {
            saveEntry()
        }

        // Add metric button
        binding.addMetricButton.setOnClickListener {
            // In a real implementation, this would show a dialog to add a new metric
            // For now, we'll just simulate adding a random metric
            simulateAddMetric()
        }
    }

    private fun observeViewModel() {
        viewModel.currentEntry.observe(this) { entry ->
            updateUI(entry)
        }

        viewModel.metricValues.observe(this) { metricValues ->
            metricValuesAdapter.submitList(metricValues)
        }

        viewModel.isSaving.observe(this) { isSaving ->
            // Could show a progress indicator here
            binding.fabSaveEntry.isEnabled = !isSaving
        }

        viewModel.saveComplete.observe(this) { isComplete ->
            if (isComplete) {
                finish() // Return to previous screen after saving
            }
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateUI(entry: HealthEntry?) {
        entry?.let { e ->
            // Update date/time display
            // Format with capitalized day name
            binding.dateTimeValue.text = formatWithCapitalizedDay(e.timestamp, "EEEE d MMMM yyyy, HH'h'mm")
            
            // Update weight, body fat and waist inputs
            e.weight?.let { binding.weightInput.setText(it.toString()) }
            e.bodyFat?.let { binding.bodyFatInput.setText(it.toString()) }
            e.waistMeasurement?.let { binding.waistInput.setText(it.toString()) }
            
            // Update notes
            binding.notesInput.setText(e.notes)
        }
    }

    private fun saveEntry() {
        val weightText = binding.weightInput.text.toString()
        val bodyFatText = binding.bodyFatInput.text.toString()
        val waistText = binding.waistInput.text.toString()
        val notes = binding.notesInput.text.toString().ifEmpty { null }

        val weight = if (weightText.isNotEmpty()) weightText.toFloatOrNull() else null
        val bodyFat = if (bodyFatText.isNotEmpty()) bodyFatText.toFloatOrNull() else null
        val waist = if (waistText.isNotEmpty()) waistText.toFloatOrNull() else null

        // Validate inputs (basic validation)
        if (weightText.isNotEmpty() && weight == null) {
            binding.weightInputLayout.error = getString(R.string.invalid_weight)
            return
        }
        
        if (bodyFatText.isNotEmpty() && bodyFat == null) {
            binding.bodyFatInputLayout.error = getString(R.string.invalid_body_fat)
            return
        }
        
        if (waistText.isNotEmpty() && waist == null) {
            binding.waistInputLayout.error = getString(R.string.invalid_waist)
            return
        }
        
        // Clear any error messages
        binding.weightInputLayout.error = null
        binding.bodyFatInputLayout.error = null
        binding.waistInputLayout.error = null

        // Update entry with form values
        viewModel.updateEntryValues(weight, waist, bodyFat, notes)
        
        // Save entry
        viewModel.saveEntry()
    }

    private fun showDateTimePicker(initialDateTime: LocalDateTime) {
        // Show date picker first
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                // After date is selected, show time picker
                val timePickerDialog = TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        // Create new datetime with selected values
                        val newDateTime = LocalDateTime.of(
                            year, month + 1, dayOfMonth,
                            hourOfDay, minute
                        )
                        updateEntryDateTime(newDateTime)
                    },
                    initialDateTime.hour,
                    initialDateTime.minute,
                    false // 24-hour format
                )
                timePickerDialog.show()
            },
            initialDateTime.year,
            initialDateTime.monthValue - 1, // DatePickerDialog uses 0-based months
            initialDateTime.dayOfMonth
        )
        datePickerDialog.show()
    }

    private fun updateEntryDateTime(newDateTime: LocalDateTime) {
        viewModel.updateEntryDateTime(newDateTime)
        binding.dateTimeValue.text = formatWithCapitalizedDay(newDateTime, "EEEE d MMMM yyyy, HH'h'mm")
    }

    private fun simulateAddMetric() {
        // This is just a placeholder for demo purposes
        // In a real app, we would show a dialog to select metric type and value
        val metricTypes = listOf(
            getString(R.string.blood_pressure),
            getString(R.string.heart_rate),
            getString(R.string.mood),
            getString(R.string.sleep_hours),
            getString(R.string.steps)
        )
        val randomMetric = metricTypes.random()
        val randomValue = when (randomMetric) {
            getString(R.string.blood_pressure) -> 120.0 + (Math.random() * 40 - 20)
            getString(R.string.heart_rate) -> 60.0 + (Math.random() * 40)
            getString(R.string.mood) -> (1.0 + (Math.random() * 4)).toInt().toDouble()
            getString(R.string.sleep_hours) -> 5.0 + (Math.random() * 4)
            getString(R.string.steps) -> (2000.0 + (Math.random() * 8000)).toInt().toDouble()
            else -> 0.0
        }
        
        val unit = when (randomMetric) {
            getString(R.string.blood_pressure) -> getString(R.string.mmHg)
            getString(R.string.heart_rate) -> getString(R.string.bpm)
            getString(R.string.mood) -> getString(R.string.mood_scale)
            getString(R.string.sleep_hours) -> getString(R.string.hours)
            getString(R.string.steps) -> getString(R.string.steps_unit)
            else -> ""
        }
        
        viewModel.addMetricValue(randomMetric, randomValue, unit)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

/**
 * Adapter for displaying metric values in a RecyclerView
 */
class MetricValuesAdapter(
    private val onDeleteClick: (MetricValue) -> Unit
) : androidx.recyclerview.widget.ListAdapter<MetricValue, MetricValuesAdapter.ViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<MetricValue>() {
        override fun areItemsTheSame(oldItem: MetricValue, newItem: MetricValue): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MetricValue, newItem: MetricValue): Boolean {
            return oldItem == newItem
        }
    }
) {

    inner class ViewHolder(private val binding: com.healthtracker.databinding.ItemMetricValueBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(metricValue: MetricValue) {
            binding.metricTypeText.text = metricValue.metricType
            binding.metricValueText.text = binding.root.context.getString(R.string.metric_value_display, metricValue.value.toString(), metricValue.unit ?: "")
            binding.deleteMetricButton.setOnClickListener {
                onDeleteClick(metricValue)
            }
        }
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val binding = com.healthtracker.databinding.ItemMetricValueBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
