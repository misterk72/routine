package com.healthtracker.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthtracker.R
import com.healthtracker.data.MetricType
import com.healthtracker.databinding.ActivitySettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var metricTypesAdapter: MetricTypesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        title = getString(R.string.settings)

        setupMetricTypesAdapter()
        setupExportFormatSpinner()
        setupListeners()
        observeViewModel()
    }

    private fun setupMetricTypesAdapter() {
        metricTypesAdapter = MetricTypesAdapter(
            onEditClick = { metricType ->
                // In a real implementation, show a dialog to edit the metric type
                // For demo purposes, we'll just show a toast
                Toast.makeText(
                    this,
                    getString(R.string.edit_metric_type, metricType.name),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDeleteClick = { metricType ->
                viewModel.deleteMetricType(metricType)
            }
        )
        binding.metricTypesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = metricTypesAdapter
        }
    }

    private fun setupExportFormatSpinner() {
        val exportFormats = arrayOf(
            getString(R.string.export_csv),
            getString(R.string.export_json),
            getString(R.string.export_excel)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, exportFormats)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.exportFormatSpinner.adapter = adapter
    }

    private fun setupListeners() {
        // Notifications switch
        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setNotificationsEnabled(isChecked)
            // Update UI to enable/disable reminder time selection
            binding.reminderTimeLabel.isEnabled = isChecked
            binding.reminderTimeValue.isEnabled = isChecked
            binding.reminderTimeButton.isEnabled = isChecked
        }

        // Reminder time button
        binding.reminderTimeButton.setOnClickListener {
            showTimePickerDialog()
        }

        // Export button
        binding.exportButton.setOnClickListener {
            val format = binding.exportFormatSpinner.selectedItem.toString()
            viewModel.setExportFormat(format)
            // In a real app, we would start the export process here
            Toast.makeText(
                this,
                getString(R.string.exporting_data, format),
                Toast.LENGTH_SHORT
            ).show()
        }

        // Add metric type button
        binding.addMetricTypeButton.setOnClickListener {
            // In a real implementation, show a dialog to add a new metric type
            // For demo purposes, we'll add a hardcoded one
            addSampleMetricType()
        }
    }

    private fun observeViewModel() {
        viewModel.notificationsEnabled.observe(this) { enabled ->
            binding.notificationsSwitch.isChecked = enabled
            binding.reminderTimeLabel.isEnabled = enabled
            binding.reminderTimeValue.isEnabled = enabled
            binding.reminderTimeButton.isEnabled = enabled
        }

        viewModel.reminderTime.observe(this) { time ->
            binding.reminderTimeValue.text = time
        }

        viewModel.exportFormat.observe(this) { format ->
            val position = (binding.exportFormatSpinner.adapter as ArrayAdapter<String>)
                .getPosition(format)
            if (position >= 0) {
                binding.exportFormatSpinner.setSelection(position)
            }
        }

        viewModel.metricTypes.observe(this) { metricTypes ->
            metricTypesAdapter.submitList(metricTypes)
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showTimePickerDialog() {
        val currentTime = viewModel.reminderTime.value ?: "08:00"
        val hour = currentTime.split(":")[0].toInt()
        val minute = currentTime.split(":")[1].toInt()

        TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val timeString = String.format("%02d:%02d", selectedHour, selectedMinute)
                viewModel.setReminderTime(timeString)
            },
            hour,
            minute,
            true // 24-hour format
        ).show()
    }

    private fun addSampleMetricType() {
        // This is just a placeholder for demo purposes
        val metricName = "${getString(R.string.sample_metric)} ${System.currentTimeMillis() % 1000}"
        viewModel.addMetricType(
            name = metricName,
            unit = getString(R.string.units),
            description = getString(R.string.sample_metric_description),
            minValue = 0.0,
            maxValue = 100.0,
            stepSize = 1.0
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

/**
 * Adapter for displaying metric types in a RecyclerView
 */
class MetricTypesAdapter(
    private val onEditClick: (MetricType) -> Unit,
    private val onDeleteClick: (MetricType) -> Unit
) : androidx.recyclerview.widget.ListAdapter<MetricType, MetricTypesAdapter.ViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<MetricType>() {
        override fun areItemsTheSame(oldItem: MetricType, newItem: MetricType): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MetricType, newItem: MetricType): Boolean {
            return oldItem == newItem
        }
    }
) {

    inner class ViewHolder(private val binding: android.view.View) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding) {
        
        private val nameTextView: android.widget.TextView = binding.findViewById(android.R.id.text1)
        private val detailsTextView: android.widget.TextView = binding.findViewById(android.R.id.text2)
        private val editButton: android.widget.ImageButton = binding.findViewById(android.R.id.button1)
        private val deleteButton: android.widget.ImageButton = binding.findViewById(android.R.id.button2)

        fun bind(metricType: MetricType) {
            nameTextView.text = metricType.name
            detailsTextView.text = "Unit: ${metricType.unit ?: "None"}, " +
                    "Range: ${metricType.minValue ?: "Min"}-${metricType.maxValue ?: "Max"}"
            
            editButton.setOnClickListener { onEditClick(metricType) }
            deleteButton.setOnClickListener { onDeleteClick(metricType) }
        }
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        // Create a custom layout for our list item
        val context = parent.context
        
        // Create the main horizontal layout
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
        }
        
        // Create the text container
        val textContainer = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        
        // Create the title text view
        val text1 = android.widget.TextView(context).apply {
            id = android.R.id.text1
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 16f
        }
        
        // Create the details text view
        val text2 = android.widget.TextView(context).apply {
            id = android.R.id.text2
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 14f
        }
        
        // Add text views to the text container
        textContainer.addView(text1)
        textContainer.addView(text2)
        
        // Add the text container to the main layout
        layout.addView(textContainer)
        
        // Add edit button
        val editButton = android.widget.ImageButton(context).apply {
            id = android.R.id.button1
            setImageResource(android.R.drawable.ic_menu_edit)
            background = null
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
                marginEnd = 8
            }
        }
        layout.addView(editButton)
        
        // Add delete button
        val deleteButton = android.widget.ImageButton(context).apply {
            id = android.R.id.button2
            setImageResource(android.R.drawable.ic_menu_delete)
            background = null
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
        }
        layout.addView(deleteButton)
        
        return ViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
