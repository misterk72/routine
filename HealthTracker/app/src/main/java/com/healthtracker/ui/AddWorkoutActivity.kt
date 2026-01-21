package com.healthtracker.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.android.material.textfield.TextInputEditText
import com.healthtracker.R
import com.healthtracker.data.User
import com.healthtracker.data.WorkoutEntry
import com.healthtracker.gadgetbridge.GadgetbridgeImportConfig
import com.healthtracker.gadgetbridge.GadgetbridgeImporter
import com.healthtracker.gadgetbridge.GadgetbridgeIntents
import com.healthtracker.gadgetbridge.GadgetbridgeImportResult
import com.healthtracker.sync.SyncManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class AddWorkoutActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "AddWorkoutActivity"
        private fun formatWithCapitalizedDay(dateTime: LocalDateTime, pattern: String): String {
            val formatted = DateTimeFormatter.ofPattern(pattern, java.util.Locale.FRENCH).format(dateTime)
            return formatted.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(java.util.Locale.FRENCH) else it.toString()
            }
        }
    }

    private val viewModel: WorkoutViewModel by viewModels()
    private var selectedUserId: Long = 0
    private val userList = mutableListOf<User>()

    private lateinit var toolbar: Toolbar
    private lateinit var startTimeEditText: TextInputEditText
    private lateinit var userSpinner: AutoCompleteTextView
    private lateinit var programEditText: TextInputEditText
    private lateinit var durationEditText: TextInputEditText
    private lateinit var distanceEditText: TextInputEditText
    private lateinit var caloriesEditText: TextInputEditText
    private lateinit var heartRateAvgEditText: TextInputEditText
    private lateinit var heartRateMinEditText: TextInputEditText
    private lateinit var heartRateMaxEditText: TextInputEditText
    private lateinit var sleepHeartRateAvgEditText: TextInputEditText
    private lateinit var vo2MaxEditText: TextInputEditText
    private lateinit var averageSpeedEditText: TextInputEditText
    private lateinit var caloriesPerKmEditText: TextInputEditText
    private lateinit var notesEditText: TextInputEditText
    private lateinit var importGadgetbridgeButton: com.google.android.material.button.MaterialButton
    private lateinit var saveButton: FloatingActionButton

    @Inject
    lateinit var syncManager: SyncManager

    private lateinit var gadgetbridgeConfig: GadgetbridgeImportConfig
    private var gadgetbridgeReceiverRegistered = false
    private val gadgetbridgeExportReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                GadgetbridgeIntents.ACTION_DATABASE_EXPORT_SUCCESS -> {
                    Log.d(TAG, "Gadgetbridge export success broadcast received")
                    importFromGadgetbridge()
                }
                GadgetbridgeIntents.ACTION_DATABASE_EXPORT_FAIL -> {
                    Log.w(TAG, "Gadgetbridge export failed broadcast received")
                    Toast.makeText(this@AddWorkoutActivity, R.string.gadgetbridge_export_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_workout)

        toolbar = findViewById(R.id.toolbar)
        startTimeEditText = findViewById(R.id.startTimeEditText)
        userSpinner = findViewById(R.id.userDropdown)
        programEditText = findViewById(R.id.programEditText)
        durationEditText = findViewById(R.id.durationEditText)
        distanceEditText = findViewById(R.id.distanceEditText)
        caloriesEditText = findViewById(R.id.caloriesEditText)
        heartRateAvgEditText = findViewById(R.id.heartRateAvgEditText)
        heartRateMinEditText = findViewById(R.id.heartRateMinEditText)
        heartRateMaxEditText = findViewById(R.id.heartRateMaxEditText)
        sleepHeartRateAvgEditText = findViewById(R.id.sleepHeartRateAvgEditText)
        vo2MaxEditText = findViewById(R.id.vo2MaxEditText)
        averageSpeedEditText = findViewById(R.id.averageSpeedEditText)
        caloriesPerKmEditText = findViewById(R.id.caloriesPerKmEditText)
        notesEditText = findViewById(R.id.notesEditText)
        importGadgetbridgeButton = findViewById(R.id.importGadgetbridgeButton)
        saveButton = findViewById(R.id.saveButton)

        setSupportActionBar(toolbar)

        gadgetbridgeConfig = GadgetbridgeImportConfig(this)

        val currentDateTime = LocalDateTime.now()
        startTimeEditText.setText(formatWithCapitalizedDay(currentDateTime, "EEEE d MMMM yyyy, HH'h'mm"))

        setupDatePicker()
        setupUserDropdown()
        setupSaveButton()
        setupComputedFields()
        setupGadgetbridgeImport()
    }

    override fun onStart() {
        super.onStart()
        if (!gadgetbridgeReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(GadgetbridgeIntents.ACTION_DATABASE_EXPORT_SUCCESS)
                addAction(GadgetbridgeIntents.ACTION_DATABASE_EXPORT_FAIL)
            }
            ContextCompat.registerReceiver(this, gadgetbridgeExportReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
            gadgetbridgeReceiverRegistered = true
        }
    }

    override fun onStop() {
        if (gadgetbridgeReceiverRegistered) {
            unregisterReceiver(gadgetbridgeExportReceiver)
            gadgetbridgeReceiverRegistered = false
        }
        super.onStop()
    }

    private fun setupDatePicker() {
        startTimeEditText.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.workout_start_time))
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val selectedDate = LocalDateTime.ofInstant(
                    java.util.Date(selection).toInstant(),
                    ZoneId.systemDefault()
                )
                val timePicker = MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(selectedDate.hour)
                    .setMinute(selectedDate.minute)
                    .setTitleText(getString(R.string.select_time))
                    .build()

                timePicker.addOnPositiveButtonClickListener {
                    val finalDateTime = selectedDate
                        .withHour(timePicker.hour)
                        .withMinute(timePicker.minute)
                    startTimeEditText.setText(
                        formatWithCapitalizedDay(finalDateTime, "EEEE d MMMM yyyy, HH'h'mm")
                    )
                }
                timePicker.show(supportFragmentManager, "timePicker")
            }

            datePicker.show(supportFragmentManager, "datePicker")
        }
    }

    private fun setupUserDropdown() {
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, mutableListOf())
        userSpinner.setAdapter(adapter)

        viewModel.users.observe(this) { users ->
            userList.clear()
            userList.addAll(users)
            val userNames = users.map { it.name }
            adapter.clear()
            adapter.addAll(userNames)
            adapter.notifyDataSetChanged()

            viewModel.defaultUser.observe(this) { defaultUser ->
                if (defaultUser != null) {
                    selectedUserId = defaultUser.id
                    userSpinner.setText(defaultUser.name, false)
                }
            }
        }

        userSpinner.setOnItemClickListener { parent, _, position, _ ->
            if (position < userList.size) {
                selectedUserId = userList[position].id
            }
        }
    }

    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            val startTimeText = startTimeEditText.text?.toString().orEmpty()
            val startTime = parseStartTime(startTimeText)
            if (selectedUserId == 0L) {
                Toast.makeText(this, R.string.select_user, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val entry = WorkoutEntry(
                userId = selectedUserId,
                startTime = startTime,
                durationMinutes = durationEditText.text?.toString()?.toIntOrNull(),
                distanceKm = distanceEditText.text?.toString()?.toFloatOrNull(),
                calories = caloriesEditText.text?.toString()?.toIntOrNull(),
                heartRateAvg = heartRateAvgEditText.text?.toString()?.toIntOrNull(),
                heartRateMin = heartRateMinEditText.text?.toString()?.toIntOrNull(),
                heartRateMax = heartRateMaxEditText.text?.toString()?.toIntOrNull(),
                sleepHeartRateAvg = sleepHeartRateAvgEditText.text?.toString()?.toIntOrNull(),
                vo2Max = vo2MaxEditText.text?.toString()?.toFloatOrNull(),
                program = programEditText.text?.toString()?.takeIf { it.isNotBlank() },
                notes = notesEditText.text?.toString()?.takeIf { it.isNotBlank() }
            )

            viewModel.addWorkout(entry)
            syncManager.syncNow()
            Toast.makeText(this, R.string.workout_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupComputedFields() {
        val watcher = {
            updateComputedFields()
        }
        durationEditText.doAfterTextChanged { watcher() }
        distanceEditText.doAfterTextChanged { watcher() }
        caloriesEditText.doAfterTextChanged { watcher() }
        heartRateMaxEditText.doAfterTextChanged { watcher() }
        sleepHeartRateAvgEditText.doAfterTextChanged { watcher() }
    }

    private fun updateComputedFields() {
        val durationMinutes = durationEditText.text?.toString()?.toFloatOrNull()
        val distanceKm = distanceEditText.text?.toString()?.toFloatOrNull()
        val calories = caloriesEditText.text?.toString()?.toFloatOrNull()
        val heartRateMax = heartRateMaxEditText.text?.toString()?.toFloatOrNull()
        val sleepHr = sleepHeartRateAvgEditText.text?.toString()?.toFloatOrNull()

        val avgSpeed = if (durationMinutes != null && durationMinutes > 0 && distanceKm != null) {
            (distanceKm / durationMinutes) * 60f
        } else {
            null
        }
        averageSpeedEditText.setText(avgSpeed?.let { String.format(java.util.Locale.US, "%.2f", it) }.orEmpty())

        val caloriesPerKm = if (distanceKm != null && distanceKm > 0 && calories != null) {
            calories / distanceKm
        } else {
            null
        }
        caloriesPerKmEditText.setText(caloriesPerKm?.let { String.format(java.util.Locale.US, "%.1f", it) }.orEmpty())

        val vo2Max = if (heartRateMax != null && sleepHr != null && sleepHr > 0f) {
            15f * heartRateMax / sleepHr
        } else {
            null
        }
        vo2MaxEditText.setText(vo2Max?.let { String.format(java.util.Locale.US, "%.1f", it) }.orEmpty())
    }

    private fun setupGadgetbridgeImport() {
        importGadgetbridgeButton.setOnClickListener {
            val exportUri = gadgetbridgeConfig.getExportUri()
            if (exportUri == null) {
                Toast.makeText(this, R.string.gadgetbridge_missing_export, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val intent = Intent(GadgetbridgeIntents.ACTION_TRIGGER_DB_EXPORT)
            intent.setPackage("nodomain.freeyourgadget.gadgetbridge")
            sendBroadcast(intent)
            Log.d(TAG, "Triggered Gadgetbridge export for uri=$exportUri")
            Toast.makeText(this, R.string.gadgetbridge_export_triggered, Toast.LENGTH_SHORT).show()
        }
    }

    private fun importFromGadgetbridge() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                GadgetbridgeImporter(this@AddWorkoutActivity, gadgetbridgeConfig).importLatestWorkout()
            }
            if (result == null) {
                Log.w(TAG, "No workout imported from Gadgetbridge")
                Toast.makeText(this@AddWorkoutActivity, R.string.gadgetbridge_import_empty, Toast.LENGTH_LONG).show()
                return@launch
            }
            applyImportResult(result)
        }
    }

    private fun applyImportResult(result: GadgetbridgeImportResult) {
        Log.d(
            TAG,
            "Import values start=${result.startTime} durationMin=${result.durationMinutes} " +
                "distanceKm=${result.distanceKm} calories=${result.calories} " +
                "hrAvg=${result.heartRateAvg} hrMin=${result.heartRateMin} hrMax=${result.heartRateMax} " +
                "sleepHrAvg=${result.sleepHeartRateAvg} vo2=${result.vo2Max}"
        )
        startTimeEditText.setText(formatWithCapitalizedDay(result.startTime, "EEEE d MMMM yyyy, HH'h'mm"))
        durationEditText.setText(result.durationMinutes?.toString().orEmpty())
        // Distance and calories are entered manually, do not overwrite on import.
        heartRateAvgEditText.setText(result.heartRateAvg?.toString().orEmpty())
        heartRateMinEditText.setText(result.heartRateMin?.toString().orEmpty())
        heartRateMaxEditText.setText(result.heartRateMax?.toString().orEmpty())
        sleepHeartRateAvgEditText.setText(result.sleepHeartRateAvg?.toString().orEmpty())
        vo2MaxEditText.setText(result.vo2Max?.let { String.format(java.util.Locale.US, "%.1f", it) }.orEmpty())
        updateComputedFields()
        Log.d(TAG, "Applied Gadgetbridge import result")
        Toast.makeText(this, R.string.gadgetbridge_imported, Toast.LENGTH_SHORT).show()
    }

    private fun parseStartTime(value: String): LocalDateTime {
        if (value.isBlank()) {
            return LocalDateTime.now()
        }
        return try {
            val formatter = DateTimeFormatter.ofPattern(
                "EEEE d MMMM yyyy, HH'h'mm",
                java.util.Locale.FRENCH
            )
            LocalDateTime.parse(value.lowercase(java.util.Locale.FRENCH), formatter)
        } catch (_: Exception) {
            LocalDateTime.now()
        }
    }
}
