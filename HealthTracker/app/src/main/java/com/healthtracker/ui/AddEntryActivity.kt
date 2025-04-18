package com.healthtracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.healthtracker.R
import com.healthtracker.ui.HealthTrackerViewModel
import com.healthtracker.data.HealthEntry
import com.healthtracker.data.User
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
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
    private val viewModel: HealthTrackerViewModel by viewModels()
    private var selectedUserId: Long = 0
    private val userList = mutableListOf<User>()
    
    // Vues
    private lateinit var toolbar: Toolbar
    private lateinit var timestampEditText: TextInputEditText
    private lateinit var userSpinner: Spinner
    private lateinit var weightEditText: TextInputEditText
    private lateinit var waistEditText: TextInputEditText
    private lateinit var bodyFatEditText: TextInputEditText
    private lateinit var notesEditText: TextInputEditText
    private lateinit var saveButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_entry_multi_user)
        
        // Initialisation des vues
        toolbar = findViewById(R.id.toolbar)
        timestampEditText = findViewById(R.id.timestampEditText)
        userSpinner = findViewById(R.id.userSpinner)
        weightEditText = findViewById(R.id.weightEditText)
        waistEditText = findViewById(R.id.waistEditText)
        bodyFatEditText = findViewById(R.id.bodyFatEditText)
        notesEditText = findViewById(R.id.notesEditText)
        saveButton = findViewById(R.id.saveButton)

        setSupportActionBar(toolbar)

        setupDatePicker()
        setupUserDropdown()
        setupSaveButton()
    }

    private fun setupDatePicker() {
        timestampEditText.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.date_and_time))
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val date = LocalDateTime.ofInstant(
                    java.util.Date(selection).toInstant(),
                    ZoneId.systemDefault()
                )
                // Display date in user-friendly format with capitalized day name
                timestampEditText.setText(formatWithCapitalizedDay(date, "EEEE d MMMM yyyy, HH'h'mm"))
            }

            datePicker.show(supportFragmentManager, "datePicker")
        }
    }

    private fun setupUserDropdown() {
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, mutableListOf<String>())
        userSpinner.adapter = adapter
        
        // Observer pour la liste des utilisateurs
        viewModel.users.observe(this) { users ->
            userList.clear()
            userList.addAll(users)
            
            val userNames = users.map { it.name }.toMutableList()
            userNames.add(getString(R.string.add_new_user))
            
            adapter.clear()
            adapter.addAll(userNames)
            adapter.notifyDataSetChanged()
            
            // Sélectionner l'utilisateur par défaut
            viewModel.defaultUser.observe(this) { defaultUser ->
                if (defaultUser != null) {
                    selectedUserId = defaultUser.id
                    val defaultPosition = userNames.indexOf(defaultUser.name)
                    if (defaultPosition >= 0) {
                        userSpinner.setSelection(defaultPosition)
                    }
                }
            }
        }
        
        userSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position < userList.size) {
                    // Un utilisateur existant a été sélectionné
                    selectedUserId = userList[position].id
                } else {
                    // "Nouveau..." a été sélectionné
                    showAddUserDialog()
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Ne rien faire
            }
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
                    viewModel.addUser(name)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            if (selectedUserId > 0) {
                val entry = HealthEntry(
                    userId = selectedUserId,
                    timestamp = LocalDateTime.now(),
                    weight = weightEditText.text.toString().toFloatOrNull(),
                    waistMeasurement = waistEditText.text.toString().toFloatOrNull(),
                    bodyFat = bodyFatEditText.text.toString().toFloatOrNull(),
                    notes = notesEditText.text.toString()
                )
                viewModel.addEntry(entry)
                finish()
            }
        }
    }
}
