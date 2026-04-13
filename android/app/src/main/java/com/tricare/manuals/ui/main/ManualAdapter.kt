package com.tricare.manuals.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tricare.manuals.data.model.Manual
import com.tricare.manuals.databinding.ItemManualBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ManualAdapter(
    private val onOpenClick: (Manual) -> Unit
) : ListAdapter<Manual, ManualAdapter.ManualViewHolder>(ManualDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManualViewHolder {
        val binding = ItemManualBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ManualViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ManualViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ManualViewHolder(
        private val binding: ItemManualBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentManual: Manual? = null

        fun bind(manual: Manual) {
            currentManual = manual
            binding.tvManualName.text = manual.name

            // Status text
            if (manual.downloadedChange != null) {
                binding.tvStatus.text = "Change ${manual.downloadedChange} downloaded"
                    .let { if (manual.downloadedFormat != null) "$it (${manual.downloadedFormat.uppercase()})" else it }
            } else {
                binding.tvStatus.text = "Not downloaded"
            }

            // Update badge
            val hasUpdate = manual.downloadedChange != null &&
                    manual.latestChange > manual.downloadedChange
            binding.tvUpdateBadge.visibility = if (hasUpdate) View.VISIBLE else View.GONE
            if (hasUpdate) {
                binding.tvUpdateBadge.text = "Update available: Change ${manual.latestChange}"
            }

            // Version spinner - show last 5 change numbers up to latestChange
            val changes = if (manual.latestChange > 0) {
                (maxOf(1, manual.latestChange - 4)..manual.latestChange)
                    .toList()
                    .sortedDescending()
            } else {
                listOf(1)
            }
            val changeLabels = changes.map { "Change $it" }
            val spinnerAdapter = ArrayAdapter(
                binding.root.context,
                android.R.layout.simple_spinner_item,
                changeLabels
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.spinnerVersion.adapter = spinnerAdapter
            // Select the downloaded change if available
            val selectedIndex = if (manual.downloadedChange != null) {
                changes.indexOf(manual.downloadedChange).takeIf { it >= 0 } ?: 0
            } else {
                0
            }
            binding.spinnerVersion.setSelection(selectedIndex)

            // Open button - only shown if downloaded
            if (manual.filePath != null && manual.downloadedChange != null) {
                binding.btnOpen.visibility = View.VISIBLE
                binding.btnOpen.setOnClickListener {
                    onOpenClick(manual)
                }
            } else {
                binding.btnOpen.visibility = View.GONE
            }

            // Downloaded timestamp
            if (manual.downloadedAt != null) {
                val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                binding.tvDownloadedAt.visibility = View.VISIBLE
                binding.tvDownloadedAt.text = "Downloaded: ${sdf.format(Date(manual.downloadedAt))}"
            } else {
                binding.tvDownloadedAt.visibility = View.GONE
            }
        }
    }

    class ManualDiffCallback : DiffUtil.ItemCallback<Manual>() {
        override fun areItemsTheSame(oldItem: Manual, newItem: Manual) = oldItem.code == newItem.code
        override fun areContentsTheSame(oldItem: Manual, newItem: Manual) = oldItem == newItem
    }
}
