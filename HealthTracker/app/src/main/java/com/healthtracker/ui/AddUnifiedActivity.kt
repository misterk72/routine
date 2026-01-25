package com.healthtracker.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.healthtracker.R
import com.healthtracker.data.HealthEntry
import com.healthtracker.data.Location
import com.healthtracker.data.User
import com.healthtracker.data.WorkoutEntry
import com.healthtracker.databinding.ActivityAddUnifiedBinding
import com.healthtracker.gadgetbridge.GadgetbridgeImportConfig
import com.healthtracker.gadgetbridge.GadgetbridgeImporter
import com.healthtracker.gadgetbridge.GadgetbridgeIntents
import com.healthtracker.gadgetbridge.GadgetbridgeImportResult
import com.healthtracker.location.LocationService
import com.healthtracker.sync.SyncManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AddUnifiedActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ENTRY_TYPE = "entry_type"
        const val EXTRA_WORKOUT_ID = "workout_id"
        const val ENTRY_TYPE_HEALTH = "health"
        const val ENTRY_TYPE_WORKOUT = "workout"
        private const val TAG = "AddUnifiedActivity"

        private fun formatWithCapitalizedDay(dateTime: LocalDateTime, pattern: String): String {
            val formatted = DateTimeFormatter.ofPattern(pattern, Locale.FRENCH).format(dateTime)
            return formatted.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString()
            }
        }
    }

    private lateinit var binding: ActivityAddUnifiedBinding
    private val healthViewModel: HealthTrackerViewModel by viewModels()
    private val workoutViewModel: WorkoutViewModel by viewModels()

    private var selectedUserId: Long = 0
    private var selectedLocationId: Long? = null
    private val userList = mutableListOf<User>()
    private val locationList = mutableListOf<Location>()

    @Inject
    lateinit var locationService: LocationService

    @Inject
    lateinit var syncManager: SyncManager

    private lateinit var gadgetbridgeConfig: GadgetbridgeImportConfig
    private var gadgetbridgeReceiverRegistered = false
    private var autoImportTriggered = false
    private val gadgetbridgeExportReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                GadgetbridgeIntents.ACTION_DATABASE_EXPORT_SUCCESS -> {
                    Log.d(TAG, "Gadgetbridge export success broadcast received")
                    importFromGadgetbridge()
                }
                GadgetbridgeIntents.ACTION_DATABASE_EXPORT_FAIL -> {
                    Log.w(TAG, "Gadgetbridge export failed broadcast received")
                    Toast.makeText(this@AddUnifiedActivity, R.string.gadgetbridge_export_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private var currentType = ENTRY_TYPE_HEALTH
    private var editingWorkoutId: Long? = null
    private var pendingUserId: Long? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (locationGranted) {
            detectCurrentLocation()
        } else {
            Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddUnifiedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        gadgetbridgeConfig = GadgetbridgeImportConfig(this)

        val currentDateTime = LocalDateTime.now()
        binding.healthTimestampEditText.setText(formatWithCapitalizedDay(currentDateTime, "EEEE d MMMM yyyy, HH'h'mm"))
        binding.workoutStartTimeEditText.setText(formatWithCapitalizedDay(currentDateTime, "EEEE d MMMM yyyy, HH'h'mm"))

        setupTypeToggle()
        setupDatePickers()
        setupUserDropdowns()
        setupLocationDropdown()
        checkLocationPermission()
        setupWorkoutComputedFields()
        setupGadgetbridgeImport()
        setupSaveButton()

        val initialType = intent.getStringExtra(EXTRA_ENTRY_TYPE)
        editingWorkoutId = intent.getLongExtra(EXTRA_WORKOUT_ID, 0L).takeIf { it > 0L }
        if (editingWorkoutId != null) {
            binding.typeToggleGroup.visibility = View.GONE
            binding.workoutSection.visibility = View.VISIBLE
            binding.healthSection.visibility = View.GONE
            currentType = ENTRY_TYPE_WORKOUT
            binding.workoutForceImportGadgetbridgeButton.visibility = View.GONE
            loadWorkoutForEdit(editingWorkoutId!!)
        }
        if (initialType == ENTRY_TYPE_WORKOUT) {
            binding.typeToggleGroup.check(binding.toggleWorkout.id)
        } else {
            binding.typeToggleGroup.check(binding.toggleHealth.id)
        }
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
        maybeTriggerInitialImport()
    }

    override fun onStop() {
        if (gadgetbridgeReceiverRegistered) {
            unregisterReceiver(gadgetbridgeExportReceiver)
            gadgetbridgeReceiverRegistered = false
        }
        super.onStop()
    }

    private fun setupTypeToggle() {
        binding.typeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            if (checkedId == binding.toggleWorkout.id) {
                currentType = ENTRY_TYPE_WORKOUT
                binding.workoutSection.visibility = View.VISIBLE
                binding.healthSection.visibility = View.GONE
                maybeTriggerInitialImport()
            } else {
                currentType = ENTRY_TYPE_HEALTH
                binding.healthSection.visibility = View.VISIBLE
                binding.workoutSection.visibility = View.GONE
            }
        }
    }

    private fun setupDatePickers() {
        binding.healthTimestampEditText.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.date_and_time))
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
                    binding.healthTimestampEditText.setText(
                        formatWithCapitalizedDay(finalDateTime, "EEEE d MMMM yyyy, HH'h'mm")
                    )
                }
                timePicker.show(supportFragmentManager, "healthTimePicker")
            }

            datePicker.show(supportFragmentManager, "healthDatePicker")
        }

        binding.workoutStartTimeEditText.setOnClickListener {
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
                    binding.workoutStartTimeEditText.setText(
                        formatWithCapitalizedDay(finalDateTime, "EEEE d MMMM yyyy, HH'h'mm")
                    )
                }
                timePicker.show(supportFragmentManager, "workoutTimePicker")
            }

            datePicker.show(supportFragmentManager, "workoutDatePicker")
        }
    }

    private fun setupUserDropdowns() {
        val healthAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, mutableListOf())
        val workoutAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, mutableListOf())
        binding.healthUserDropdown.setAdapter(healthAdapter)
        binding.workoutUserDropdown.setAdapter(workoutAdapter)

        healthViewModel.users.observe(this) { users ->
            userList.clear()
            userList.addAll(users)

            val userNames = users.map { it.name }.toMutableList()
            userNames.add(getString(R.string.add_new_user))

            healthAdapter.clear()
            healthAdapter.addAll(userNames)
            healthAdapter.notifyDataSetChanged()

            workoutAdapter.clear()
            workoutAdapter.addAll(userNames)
            workoutAdapter.notifyDataSetChanged()

            healthViewModel.defaultUser.observe(this) { defaultUser ->
                if (defaultUser != null) {
                    selectedUserId = defaultUser.id
                    binding.healthUserDropdown.setText(defaultUser.name, false)
                    binding.workoutUserDropdown.setText(defaultUser.name, false)
                }
            }

            pendingUserId?.let { userId ->
                val selectedUser = userList.firstOrNull { it.id == userId }
                if (selectedUser != null) {
                    selectedUserId = selectedUser.id
                    binding.healthUserDropdown.setText(selectedUser.name, false)
                    binding.workoutUserDropdown.setText(selectedUser.name, false)
                    pendingUserId = null
                }
            }
        }

        binding.healthUserDropdown.setOnItemClickListener { parent, _, position, _ ->
            handleUserSelection(parent, position)
        }
        binding.workoutUserDropdown.setOnItemClickListener { parent, _, position, _ ->
            handleUserSelection(parent, position)
        }
    }

    private fun handleUserSelection(parent: AdapterView<*>, position: Int) {
        val selectedItem = parent.getItemAtPosition(position).toString()
        if (position < userList.size) {
            selectedUserId = userList[position].id
            val name = userList[position].name
            binding.healthUserDropdown.setText(name, false)
            binding.workoutUserDropdown.setText(name, false)
        } else if (selectedItem == getString(R.string.add_new_user)) {
            showAddUserDialog()
        }
    }

    private fun showAddUserDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_user, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_new_user)
            .setView(dialogView)
            .setPositiveButton(R.string.add) { _, _ ->
                val name = nameEditText.text.toString()
                if (name.isNotEmpty()) {
                    healthViewModel.addUser(name)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupLocationDropdown() {
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, mutableListOf())
        binding.healthLocationDropdown.setAdapter(adapter)

        healthViewModel.locations.observe(this) { locations ->
            locationList.clear()
            locationList.addAll(locations)

            val locationNames = locations.map { it.name }.toMutableList()
            locationNames.add(getString(R.string.add_location))

            adapter.clear()
            adapter.addAll(locationNames)
            adapter.notifyDataSetChanged()
        }

        binding.healthLocationDropdown.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position).toString()
            if (position < locationList.size) {
                selectedLocationId = locationList[position].id
            } else if (selectedItem == getString(R.string.add_location)) {
                showAddLocationDialog()
            }
        }
    }

    private fun showAddLocationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_location, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.locationNameEditText)
        val latitudeEditText = dialogView.findViewById<EditText>(R.id.latitudeEditText)
        val longitudeEditText = dialogView.findViewById<EditText>(R.id.longitudeEditText)
        val radiusEditText = dialogView.findViewById<EditText>(R.id.radiusEditText)

        if (locationService.hasLocationPermissions()) {
            locationService.getLastLocation { location ->
                if (location != null) {
                    latitudeEditText.setText(location.latitude.toString())
                    longitudeEditText.setText(location.longitude.toString())
                }
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_location)
            .setView(dialogView)
            .setPositiveButton(R.string.add) { _, _ ->
                val name = nameEditText.text.toString()
                val latitude = latitudeEditText.text.toString().toDoubleOrNull()
                val longitude = longitudeEditText.text.toString().toDoubleOrNull()
                val radius = radiusEditText.text.toString().toFloatOrNull() ?: 100f

                if (name.isNotEmpty() && latitude != null && longitude != null) {
                    val newLocation = Location(
                        name = name,
                        latitude = latitude,
                        longitude = longitude,
                        radius = radius
                    )
                    healthViewModel.addLocation(newLocation)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                detectCurrentLocation()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.location)
                    .setMessage(R.string.location_permission_required)
                    .setPositiveButton(R.string.change) { _, _ ->
                        requestLocationPermission()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            else -> {
                requestLocationPermission()
            }
        }
    }

    private fun requestLocationPermission() {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun detectCurrentLocation() {
        locationService.getLastLocation { location ->
            if (location != null) {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.location_detected, location.name), Toast.LENGTH_SHORT).show()
                    selectedLocationId = location.id
                    binding.healthLocationDropdown.setText(location.name, false)
                }
            }
        }
    }

    private fun setupWorkoutComputedFields() {
        val watcher = {
            updateComputedFields()
        }
        binding.workoutDurationEditText.doAfterTextChanged { watcher() }
        binding.workoutDistanceEditText.doAfterTextChanged { watcher() }
        binding.workoutCaloriesEditText.doAfterTextChanged { watcher() }
        binding.workoutHeartRateMaxEditText.doAfterTextChanged { watcher() }
        binding.workoutSleepHeartRateAvgEditText.doAfterTextChanged { watcher() }
    }

    private fun updateComputedFields() {
        val durationMinutes = binding.workoutDurationEditText.text?.toString()?.toFloatOrNull()
        val distanceKm = binding.workoutDistanceEditText.text?.toString()?.toFloatOrNull()
        val calories = binding.workoutCaloriesEditText.text?.toString()?.toFloatOrNull()
        val heartRateMax = binding.workoutHeartRateMaxEditText.text?.toString()?.toFloatOrNull()
        val sleepHr = binding.workoutSleepHeartRateAvgEditText.text?.toString()?.toFloatOrNull()

        val avgSpeed = if (durationMinutes != null && durationMinutes > 0 && distanceKm != null) {
            (distanceKm / durationMinutes) * 60f
        } else {
            null
        }
        binding.workoutAverageSpeedEditText.setText(avgSpeed?.let { String.format(Locale.US, "%.2f", it) }.orEmpty())

        val caloriesPerKm = if (distanceKm != null && distanceKm > 0 && calories != null) {
            calories / distanceKm
        } else {
            null
        }
        binding.workoutCaloriesPerKmEditText.setText(caloriesPerKm?.let { String.format(Locale.US, "%.1f", it) }.orEmpty())

        val vo2Max = if (heartRateMax != null && sleepHr != null && sleepHr > 0f) {
            15f * heartRateMax / sleepHr
        } else {
            null
        }
        binding.workoutVo2MaxEditText.setText(vo2Max?.let { String.format(Locale.US, "%.1f", it) }.orEmpty())
    }

    private fun setupGadgetbridgeImport() {
        binding.workoutForceImportGadgetbridgeButton.setOnClickListener {
            triggerGadgetbridgeExport(resetImportState = true, showToast = true)
        }
    }

    private fun maybeTriggerInitialImport() {
        if (autoImportTriggered || currentType != ENTRY_TYPE_WORKOUT || editingWorkoutId != null) {
            return
        }
        val exportUri = gadgetbridgeConfig.getExportUri() ?: return
        autoImportTriggered = true
        triggerGadgetbridgeExport(resetImportState = false, showToast = false, exportUri = exportUri)
    }

    private fun triggerGadgetbridgeExport(
        resetImportState: Boolean,
        showToast: Boolean,
        exportUri: android.net.Uri? = null
    ) {
        val resolvedUri = exportUri ?: gadgetbridgeConfig.getExportUri()
        if (resolvedUri == null) {
            if (showToast) {
                Toast.makeText(this, R.string.gadgetbridge_missing_export, Toast.LENGTH_LONG).show()
            }
            return
        }
        if (resetImportState) {
            gadgetbridgeConfig.setLastImportedStartTime(0L)
            Log.d(TAG, "Reset Gadgetbridge import state before export")
        }
        val intent = Intent(GadgetbridgeIntents.ACTION_TRIGGER_DB_EXPORT)
        intent.setPackage("nodomain.freeyourgadget.gadgetbridge")
        sendBroadcast(intent)
        Log.d(TAG, "Triggered Gadgetbridge export for uri=$resolvedUri")
        if (showToast) {
            Toast.makeText(this, R.string.gadgetbridge_export_triggered, Toast.LENGTH_SHORT).show()
        }
    }

    private fun importFromGadgetbridge() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                GadgetbridgeImporter(this@AddUnifiedActivity, gadgetbridgeConfig).importLatestWorkout()
            }
            if (result == null) {
                Log.w(TAG, "No workout imported from Gadgetbridge")
                Toast.makeText(this@AddUnifiedActivity, R.string.gadgetbridge_import_empty, Toast.LENGTH_LONG).show()
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
        binding.workoutStartTimeEditText.setText(formatWithCapitalizedDay(result.startTime, "EEEE d MMMM yyyy, HH'h'mm"))
        binding.workoutDurationEditText.setText(result.durationMinutes?.toString().orEmpty())
        binding.workoutHeartRateAvgEditText.setText(result.heartRateAvg?.toString().orEmpty())
        binding.workoutHeartRateMinEditText.setText(result.heartRateMin?.toString().orEmpty())
        binding.workoutHeartRateMaxEditText.setText(result.heartRateMax?.toString().orEmpty())
        binding.workoutSleepHeartRateAvgEditText.setText(result.sleepHeartRateAvg?.toString().orEmpty())
        binding.workoutVo2MaxEditText.setText(result.vo2Max?.let { String.format(Locale.US, "%.1f", it) }.orEmpty())
        updateComputedFields()
        Log.d(TAG, "Applied Gadgetbridge import result")
        Toast.makeText(this, R.string.gadgetbridge_imported, Toast.LENGTH_SHORT).show()
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            if (selectedUserId == 0L) {
                Toast.makeText(this, R.string.select_user, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentType == ENTRY_TYPE_WORKOUT) {
                saveWorkoutEntry()
            } else {
                saveHealthEntry()
            }
        }
    }

    private fun loadWorkoutForEdit(workoutId: Long) {
        lifecycleScope.launch {
            val workout = withContext(Dispatchers.IO) {
                workoutViewModel.getWorkoutById(workoutId)
            }
            if (workout == null) {
                Toast.makeText(this@AddUnifiedActivity, R.string.workout_not_found, Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            pendingUserId = workout.userId
            binding.workoutStartTimeEditText.setText(
                formatWithCapitalizedDay(workout.startTime, "EEEE d MMMM yyyy, HH'h'mm")
            )
            binding.workoutProgramEditText.setText(workout.program.orEmpty())
            binding.workoutDurationEditText.setText(workout.durationMinutes?.toString().orEmpty())
            binding.workoutDistanceEditText.setText(workout.distanceKm?.toString().orEmpty())
            binding.workoutCaloriesEditText.setText(workout.calories?.toString().orEmpty())
            binding.workoutHeartRateAvgEditText.setText(workout.heartRateAvg?.toString().orEmpty())
            binding.workoutHeartRateMinEditText.setText(workout.heartRateMin?.toString().orEmpty())
            binding.workoutHeartRateMaxEditText.setText(workout.heartRateMax?.toString().orEmpty())
            binding.workoutSleepHeartRateAvgEditText.setText(workout.sleepHeartRateAvg?.toString().orEmpty())
            binding.workoutVo2MaxEditText.setText(workout.vo2Max?.toString().orEmpty())
            binding.workoutNotesEditText.setText(workout.notes.orEmpty())
            updateComputedFields()
        }
    }

    private fun saveHealthEntry() {
        val timestampText = binding.healthTimestampEditText.text?.toString().orEmpty()
        val timestamp = parseDateTime(timestampText)
        val entry = HealthEntry(
            userId = selectedUserId,
            timestamp = timestamp,
            weight = binding.healthWeightEditText.text?.toString()?.toFloatOrNull(),
            waistMeasurement = binding.healthWaistEditText.text?.toString()?.toFloatOrNull(),
            bodyFat = binding.healthBodyFatEditText.text?.toString()?.toFloatOrNull(),
            notes = binding.healthNotesEditText.text?.toString().orEmpty(),
            locationId = selectedLocationId
        )
        healthViewModel.addEntry(entry)
        syncManager.syncNow()
        finish()
    }

    private fun saveWorkoutEntry() {
        val startTimeText = binding.workoutStartTimeEditText.text?.toString().orEmpty()
        val startTime = parseDateTime(startTimeText)
        val entry = WorkoutEntry(
            id = editingWorkoutId ?: 0,
            userId = selectedUserId,
            startTime = startTime,
            durationMinutes = binding.workoutDurationEditText.text?.toString()?.toIntOrNull(),
            distanceKm = binding.workoutDistanceEditText.text?.toString()?.toFloatOrNull(),
            calories = binding.workoutCaloriesEditText.text?.toString()?.toIntOrNull(),
            heartRateAvg = binding.workoutHeartRateAvgEditText.text?.toString()?.toIntOrNull(),
            heartRateMin = binding.workoutHeartRateMinEditText.text?.toString()?.toIntOrNull(),
            heartRateMax = binding.workoutHeartRateMaxEditText.text?.toString()?.toIntOrNull(),
            sleepHeartRateAvg = binding.workoutSleepHeartRateAvgEditText.text?.toString()?.toIntOrNull(),
            vo2Max = binding.workoutVo2MaxEditText.text?.toString()?.toFloatOrNull(),
            program = binding.workoutProgramEditText.text?.toString()?.takeIf { it.isNotBlank() },
            notes = binding.workoutNotesEditText.text?.toString()?.takeIf { it.isNotBlank() }
        )
        if (editingWorkoutId != null) {
            workoutViewModel.updateWorkout(entry)
        } else {
            workoutViewModel.addWorkout(entry)
        }
        syncManager.syncNow()
        Toast.makeText(this, R.string.workout_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun parseDateTime(value: String): LocalDateTime {
        if (value.isBlank()) {
            return LocalDateTime.now()
        }
        return try {
            val formatter = DateTimeFormatter.ofPattern(
                "EEEE d MMMM yyyy, HH'h'mm",
                Locale.FRENCH
            )
            LocalDateTime.parse(value.lowercase(Locale.FRENCH), formatter)
        } catch (_: Exception) {
            LocalDateTime.now()
        }
    }
}
