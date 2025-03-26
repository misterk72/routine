package com.healthtracker.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
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
        title = "Settings"

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
                    "Edit metric type: ${metricType.name}",
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
        val exportFormats = arrayOf("CSV", "JSON", "Excel")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, exportFormats)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.exportFormatSpinner.adapter = adapter
    }

    private fun setupListeners() {
        // Dark mode switch
        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setDarkMode(isChecked)
        }

        // Units radio group
        binding.unitsRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val useMetric = (checkedId == binding.metricRadioButton.id)
            viewModel.setMetricUnits(useMetric)
        }

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
                "Exporting data in $format format...",
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
        viewModel.isDarkMode.observe(this) { isDarkMode ->
            binding.darkModeSwitch.isChecked = isDarkMode
            // In a real app, we would apply the theme change here
        }

        viewModel.isMetricUnits.observe(this) { isMetric ->
            binding.metricRadioButton.isChecked = isMetric
            binding.imperialRadioButton.isChecked = !isMetric
        }

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
        val metricName = "Sample Metric ${System.currentTimeMillis() % 1000}"
        viewModel.addMetricType(
            name = metricName,
            unit = "units",
            description = "A sample metric type for demonstration",
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
        // Using a standard two-line list item with two buttons
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        
        // In a real app, we would create a custom layout
        // For demo purposes, we're adding buttons programmatically
        val layout = view as android.widget.LinearLayout
        layout.orientation = android.widget.LinearLayout.HORIZONTAL
        
        val textContainer = android.widget.LinearLayout(parent.context)
        textContainer.orientation = android.widget.LinearLayout.VERTICAL
        textContainer.layoutParams = android.widget.LinearLayout.LayoutParams(
            0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        )
        
        val text1 = layout.findViewById<android.widget.TextView>(android.R.id.text1)
        val text2 = layout.findViewById<android.widget.TextView>(android.R.id.text2)
        layout.removeAllViews()
        
        textContainer.addView(text1)
        textContainer.addView(text2)
        layout.addView(textContainer)
        
        // Add edit button
        val editButton = android.widget.ImageButton(parent.context)
        editButton.id = android.R.id.button1
        editButton.setImageResource(android.R.drawable.ic_menu_edit)
        editButton.background = null
        layout.addView(editButton)
        
        // Add delete button
        val deleteButton = android.widget.ImageButton(parent.context)
        deleteButton.id = android.R.id.button2
        deleteButton.setImageResource(android.R.drawable.ic_menu_delete)
        deleteButton.background = null
        layout.addView(deleteButton)
        
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
