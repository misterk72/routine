package com.healthtracker.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.healthtracker.R
import com.healthtracker.databinding.ActivityWorkoutEntryBinding
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class WorkoutEntryActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_WORKOUT_ID = "workout_id"

        private fun formatWithCapitalizedDay(dateTime: LocalDateTime, pattern: String): String {
            val formatted = DateTimeFormatter.ofPattern(pattern, Locale.FRENCH).format(dateTime)
            return formatted.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString()
            }
        }

        private fun formatDistance(distanceKm: Float): String {
            return String.format(Locale.getDefault(), "%.2f", distanceKm)
        }
    }

    private lateinit var binding: ActivityWorkoutEntryBinding
    private val viewModel: WorkoutEntryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkoutEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val workoutId = intent.getLongExtra(EXTRA_WORKOUT_ID, 0L)
        if (workoutId == 0L) {
            Toast.makeText(this, R.string.workout_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.getWorkout(workoutId).observe(this) { workoutWithUser ->
            if (workoutWithUser == null) {
                Toast.makeText(this, R.string.workout_not_found, Toast.LENGTH_SHORT).show()
                finish()
                return@observe
            }

            val entry = workoutWithUser.entry
            val user = workoutWithUser.user

            val formattedDate = formatWithCapitalizedDay(entry.startTime, "EEEE d MMMM yyyy, HH'h'mm")
            binding.startTimeText.text = getString(R.string.workout_start_display, formattedDate)
            binding.userText.text = getString(R.string.workout_user_display, user.name)

            setTextOrHide(binding.programText, entry.program?.takeIf { it.isNotBlank() }?.let {
                getString(R.string.workout_program_display, it)
            })
            setTextOrHide(binding.durationText, entry.durationMinutes?.let {
                getString(R.string.workout_duration_display, it)
            })
            setTextOrHide(binding.distanceText, entry.distanceKm?.let {
                getString(R.string.workout_distance_display, formatDistance(it))
            })
            setTextOrHide(binding.caloriesText, entry.calories?.let {
                getString(R.string.workout_calories_display, it)
            })
            setTextOrHide(binding.heartRateAvgText, entry.heartRateAvg?.let {
                getString(R.string.workout_heart_rate_avg_display, it)
            })
            setTextOrHide(binding.heartRateMinText, entry.heartRateMin?.let {
                getString(R.string.workout_heart_rate_min_display, it)
            })
            setTextOrHide(binding.heartRateMaxText, entry.heartRateMax?.let {
                getString(R.string.workout_heart_rate_max_display, it)
            })
            setTextOrHide(binding.sleepHeartRateAvgText, entry.sleepHeartRateAvg?.let {
                getString(R.string.workout_sleep_heart_rate_avg_display, it)
            })
            setTextOrHide(binding.vo2MaxText, entry.vo2Max?.let {
                getString(R.string.workout_vo2_max_display, it)
            })
            setTextOrHide(binding.notesText, entry.notes?.takeIf { it.isNotBlank() })
        }
    }

    private fun setTextOrHide(view: android.view.View, text: String?) {
        if (text.isNullOrBlank()) {
            view.visibility = android.view.View.GONE
        } else {
            view.visibility = android.view.View.VISIBLE
            if (view is android.widget.TextView) {
                view.text = text
            }
        }
    }
}
