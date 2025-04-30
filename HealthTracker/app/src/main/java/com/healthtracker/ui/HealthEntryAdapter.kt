package com.healthtracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.healthtracker.R
import com.healthtracker.data.HealthEntry
import com.healthtracker.data.HealthEntryWithUser
import com.healthtracker.databinding.ItemHealthEntryBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class HealthEntryAdapter(private val onEntryClick: (HealthEntry) -> Unit) :
    ListAdapter<HealthEntryWithUser, HealthEntryAdapter.HealthEntryViewHolder>(HealthEntryDiffCallback()) {
    
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
                onEntryClick(getItem(adapterPosition).entry)
            }
        }

        fun bind(entryWithUser: HealthEntryWithUser) {
            val entry = entryWithUser.entry
            val user = entryWithUser.user
            
            // Format date in French style with capitalized day name
            val formattedDate = formatWithCapitalizedDay(entry.timestamp, "EEEE d MMMM yyyy")
            val formattedTime = entry.timestamp.format(TIME_FORMATTER)
            binding.timestampText.text = "$formattedDate, $formattedTime"
            
            // Afficher le nom de l'utilisateur
            binding.userNameText.text = user.name
            
            // Afficher la masse si elle existe
            if (entry.weight != null) {
                binding.weightText.text = binding.root.context.getString(R.string.weight_display, entry.weight.toString())
                binding.weightText.visibility = View.VISIBLE
            } else {
                binding.weightText.visibility = View.GONE
            }
            
            // Afficher la masse graisseuse si elle existe
            if (entry.bodyFat != null) {
                binding.bodyFatText.text = binding.root.context.getString(R.string.body_fat_display, entry.bodyFat.toString())
                binding.bodyFatText.visibility = View.VISIBLE
            } else {
                binding.bodyFatText.visibility = View.GONE
            }
            
            // Afficher le tour de taille s'il existe
            if (entry.waistMeasurement != null) {
                binding.waistText.text = binding.root.context.getString(R.string.waist_display, entry.waistMeasurement.toString())
                binding.waistText.visibility = View.VISIBLE
            } else {
                binding.waistText.visibility = View.GONE
            }
            
            // Afficher les notes si elles existent
            if (!entry.notes.isNullOrBlank()) {
                binding.notesText.text = entry.notes
                binding.notesText.visibility = View.VISIBLE
            } else {
                binding.notesText.visibility = View.GONE
            }
        }
    }

    private class HealthEntryDiffCallback : DiffUtil.ItemCallback<HealthEntryWithUser>() {
        override fun areItemsTheSame(oldItem: HealthEntryWithUser, newItem: HealthEntryWithUser): Boolean {
            return oldItem.entry.id == newItem.entry.id
        }

        override fun areContentsTheSame(oldItem: HealthEntryWithUser, newItem: HealthEntryWithUser): Boolean {
            return oldItem.entry == newItem.entry && oldItem.user == newItem.user
        }
    }
}
