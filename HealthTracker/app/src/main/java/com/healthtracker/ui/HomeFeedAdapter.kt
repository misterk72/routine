package com.healthtracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.healthtracker.R
import com.healthtracker.data.HealthEntry
import com.healthtracker.databinding.ItemHealthEntryBinding
import com.healthtracker.databinding.ItemWorkoutEntryBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeFeedAdapter(
    private val onHealthEntryClick: (HealthEntry) -> Unit,
    private val onWorkoutEntryClick: (Long) -> Unit
) : ListAdapter<HomeFeedItem, RecyclerView.ViewHolder>(HomeFeedDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEALTH = 1
        private const val VIEW_TYPE_WORKOUT = 2

        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH'h'mm", Locale.FRENCH)

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

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HomeFeedItem.Health -> VIEW_TYPE_HEALTH
            is HomeFeedItem.Workout -> VIEW_TYPE_WORKOUT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEALTH -> {
                val binding = ItemHealthEntryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HealthEntryViewHolder(binding)
            }
            VIEW_TYPE_WORKOUT -> {
                val binding = ItemWorkoutEntryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                WorkoutEntryViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown viewType=$viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HomeFeedItem.Health -> (holder as HealthEntryViewHolder).bind(item)
            is HomeFeedItem.Workout -> (holder as WorkoutEntryViewHolder).bind(item)
        }
    }

    inner class HealthEntryViewHolder(private val binding: ItemHealthEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val item = getItem(adapterPosition) as? HomeFeedItem.Health ?: return@setOnClickListener
                onHealthEntryClick(item.entryWithUser.entry)
            }
        }

        fun bind(item: HomeFeedItem.Health) {
            val entry = item.entryWithUser.entry
            val user = item.entryWithUser.user

            val formattedDate = formatWithCapitalizedDay(entry.timestamp, "EEEE d MMMM yyyy")
            val formattedTime = entry.timestamp.format(TIME_FORMATTER)
            binding.timestampText.text = "$formattedDate, $formattedTime"

            binding.userNameText.text = user.name

            if (entry.weight != null) {
                binding.weightText.text = binding.root.context.getString(
                    R.string.weight_display,
                    entry.weight.toString()
                )
                binding.weightText.visibility = View.VISIBLE
            } else {
                binding.weightText.visibility = View.GONE
            }

            if (entry.bodyFat != null) {
                binding.bodyFatText.text = binding.root.context.getString(
                    R.string.body_fat_display,
                    entry.bodyFat.toString()
                )
                binding.bodyFatText.visibility = View.VISIBLE
            } else {
                binding.bodyFatText.visibility = View.GONE
            }

            if (entry.waistMeasurement != null) {
                binding.waistText.text = binding.root.context.getString(
                    R.string.waist_display,
                    entry.waistMeasurement.toString()
                )
                binding.waistText.visibility = View.VISIBLE
            } else {
                binding.waistText.visibility = View.GONE
            }

            if (!entry.notes.isNullOrBlank()) {
                binding.notesText.text = entry.notes
                binding.notesText.visibility = View.VISIBLE
            } else {
                binding.notesText.visibility = View.GONE
            }
        }
    }

    inner class WorkoutEntryViewHolder(private val binding: ItemWorkoutEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HomeFeedItem.Workout) {
            binding.root.setOnClickListener {
                onWorkoutEntryClick(item.entryWithUser.entry.id)
            }
            val entry = item.entryWithUser.entry
            val user = item.entryWithUser.user

            val formattedDate = formatWithCapitalizedDay(entry.startTime, "EEEE d MMMM yyyy")
            val formattedTime = entry.startTime.format(TIME_FORMATTER)
            binding.timestampText.text = "$formattedDate, $formattedTime"
            binding.userNameText.text = user.name

            if (!entry.program.isNullOrBlank()) {
                binding.programText.text = binding.root.context.getString(
                    R.string.workout_program_display,
                    entry.program
                )
                binding.programText.visibility = View.VISIBLE
            } else {
                binding.programText.visibility = View.GONE
            }

            if (entry.durationMinutes != null) {
                binding.durationText.text = binding.root.context.getString(
                    R.string.workout_duration_display,
                    entry.durationMinutes
                )
                binding.durationText.visibility = View.VISIBLE
            } else {
                binding.durationText.visibility = View.GONE
            }

            if (entry.distanceKm != null) {
                binding.distanceText.text = binding.root.context.getString(
                    R.string.workout_distance_display,
                    formatDistance(entry.distanceKm)
                )
                binding.distanceText.visibility = View.VISIBLE
            } else {
                binding.distanceText.visibility = View.GONE
            }

            if (entry.calories != null) {
                binding.caloriesText.text = binding.root.context.getString(
                    R.string.workout_calories_display,
                    entry.calories
                )
                binding.caloriesText.visibility = View.VISIBLE
            } else {
                binding.caloriesText.visibility = View.GONE
            }

            when {
                entry.heartRateAvg != null -> {
                    binding.heartRateText.text = binding.root.context.getString(
                        R.string.workout_heart_rate_avg_display,
                        entry.heartRateAvg
                    )
                    binding.heartRateText.visibility = View.VISIBLE
                }
                entry.heartRateMin != null && entry.heartRateMax != null -> {
                    binding.heartRateText.text = binding.root.context.getString(
                        R.string.workout_heart_rate_range_display,
                        entry.heartRateMin,
                        entry.heartRateMax
                    )
                    binding.heartRateText.visibility = View.VISIBLE
                }
                else -> {
                    binding.heartRateText.visibility = View.GONE
                }
            }

            if (!entry.notes.isNullOrBlank()) {
                binding.notesText.text = entry.notes
                binding.notesText.visibility = View.VISIBLE
            } else {
                binding.notesText.visibility = View.GONE
            }
        }
    }
}

private class HomeFeedDiffCallback : DiffUtil.ItemCallback<HomeFeedItem>() {
    override fun areItemsTheSame(oldItem: HomeFeedItem, newItem: HomeFeedItem): Boolean {
        return oldItem::class == newItem::class && oldItem.stableId == newItem.stableId
    }

    override fun areContentsTheSame(oldItem: HomeFeedItem, newItem: HomeFeedItem): Boolean {
        return oldItem == newItem
    }
}
