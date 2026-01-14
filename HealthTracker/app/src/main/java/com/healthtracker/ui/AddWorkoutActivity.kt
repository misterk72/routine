package com.healthtracker.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.android.material.textfield.TextInputEditText
import com.healthtracker.R
import com.healthtracker.data.User
import com.healthtracker.data.WorkoutEntry
import com.healthtracker.sync.SyncManager
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class AddWorkoutActivity : AppCompatActivity() {
    companion object {
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
    private lateinit var notesEditText: TextInputEditText
    private lateinit var saveButton: FloatingActionButton

    @Inject
    lateinit var syncManager: SyncManager

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
        notesEditText = findViewById(R.id.notesEditText)
        saveButton = findViewById(R.id.saveButton)

        setSupportActionBar(toolbar)

        val currentDateTime = LocalDateTime.now()
        startTimeEditText.setText(formatWithCapitalizedDay(currentDateTime, "EEEE d MMMM yyyy, HH'h'mm"))

        setupDatePicker()
        setupUserDropdown()
        setupSaveButton()
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
                program = programEditText.text?.toString()?.takeIf { it.isNotBlank() },
                notes = notesEditText.text?.toString()?.takeIf { it.isNotBlank() }
            )

            viewModel.addWorkout(entry)
            syncManager.syncNow()
            Toast.makeText(this, R.string.workout_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
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
