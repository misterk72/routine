package com.healthtracker.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.healthtracker.data.HealthEntry
import com.healthtracker.databinding.ItemHealthEntryBinding
import java.time.format.DateTimeFormatter

class HealthEntryAdapter(private val onEntryClick: (HealthEntry) -> Unit) :
    ListAdapter<HealthEntry, HealthEntryAdapter.HealthEntryViewHolder>(HealthEntryDiffCallback()) {

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
            binding.timestampText.text = entry.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
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
