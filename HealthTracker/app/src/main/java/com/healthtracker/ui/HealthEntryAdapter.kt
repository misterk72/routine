package com.healthtracker.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.healthtracker.data.HealthEntry
import com.healthtracker.databinding.ItemHealthEntryBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class HealthEntryAdapter(private val onEntryClick: (HealthEntry) -> Unit) :
    ListAdapter<HealthEntry, HealthEntryAdapter.HealthEntryViewHolder>(HealthEntryDiffCallback()) {
    
    companion object {
        // French-style date format for list items
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", java.util.Locale.FRENCH)
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH'h'mm", java.util.Locale.FRENCH)
        
        // Custom formatter to capitalize first letter of day name
        private fun formatWithCapitalizedDay(dateTime: LocalDateTime, pattern: String): String {
            val formatted = DateTimeFormatter.ofPattern(pattern, java.util.Locale.FRENCH).format(dateTime)
            // Capitalize first letter of the day name
            return formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.FRENCH) else it.toString() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HealthEntryViewHolder {
        val binding = ItemHealthEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HealthEntryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HealthEntryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HealthEntryViewHolder(private val binding: ItemHealthEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                onEntryClick(getItem(adapterPosition))
            }
        }

        fun bind(entry: HealthEntry) {
            // Format date in French style with capitalized day name
            val formattedDate = formatWithCapitalizedDay(entry.timestamp, "EEEE d MMMM yyyy")
            val formattedTime = entry.timestamp.format(TIME_FORMATTER)
            binding.timestampText.text = "$formattedDate, $formattedTime"
            binding.weightText.text = entry.weight?.let { "Weight: ${it} kg" }
            binding.waistText.text = entry.waistMeasurement?.let { "Waist: ${it} cm" }
            binding.notesText.text = entry.notes
        }
    }

    private class HealthEntryDiffCallback : DiffUtil.ItemCallback<HealthEntry>() {
        override fun areItemsTheSame(oldItem: HealthEntry, newItem: HealthEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HealthEntry, newItem: HealthEntry): Boolean {
            return oldItem == newItem
        }
    }
}
