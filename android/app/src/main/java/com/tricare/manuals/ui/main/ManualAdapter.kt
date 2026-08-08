package com.tricare.manuals.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import com.tricare.manuals.data.model.Manual
import com.tricare.manuals.databinding.ItemManualBinding
import com.tricare.manuals.worker.DownloadWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PAYLOAD_PROGRESS = "progress"

class ManualAdapter(
    private val onOpenClick: (Manual) -> Unit,
    private val onDownloadClick: (Manual) -> Unit,
    private val onShareClick: (Manual) -> Unit,
    private val onExportPdfClick: (Manual) -> Unit,
    private val onSourceClick: (Manual) -> Unit
) : ListAdapter<Manual, ManualAdapter.ManualViewHolder>(ManualDiffCallback()) {

    private val progressMap = mutableMapOf<String, WorkInfo?>()

    /** Called by the Fragment whenever WorkManager reports a state change for a manual. */
    fun updateProgress(code: String, workInfo: WorkInfo?) {
        progressMap[code] = workInfo
        val pos = currentList.indexOfFirst { it.code == code }
        if (pos < 0) return

        // Use a full rebind for terminal states so the status text, Open button,
        // and Download label all refresh from the updated database values.
        // Use a partial (progress-only) rebind while actively running to avoid
        // resetting the spinner selection mid-download.
        val isActiveState = workInfo?.state == WorkInfo.State.RUNNING ||
                workInfo?.state == WorkInfo.State.ENQUEUED
        if (isActiveState) {
            notifyItemChanged(pos, PAYLOAD_PROGRESS)
        } else {
            notifyItemChanged(pos)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManualViewHolder {
        val binding = ItemManualBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ManualViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ManualViewHolder, position: Int) {
        holder.bind(getItem(position), progressMap[getItem(position).code])
    }

    // Partial rebind for progress-only updates so the spinner doesn't reset.
    override fun onBindViewHolder(
        holder: ManualViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_PROGRESS)) {
            holder.bindProgress(progressMap[getItem(position).code])
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class ManualViewHolder(
        private val binding: ItemManualBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(manual: Manual, workInfo: WorkInfo?) {
            binding.tvManualName.text = manual.name
            binding.tvSourceLink.setOnClickListener { onSourceClick(manual) }

            // Status text
            val downloadFailed = workInfo?.state == WorkInfo.State.FAILED
            if (manual.downloadedChange != null) {
                binding.tvStatus.text = "Change ${manual.downloadedChange} downloaded"
                    .let { if (manual.downloadedFormat != null) "$it (${manual.downloadedFormat.uppercase()})" else it }
            } else if (downloadFailed) {
                val reason = workInfo?.outputData?.getString(
                    com.tricare.manuals.worker.DownloadWorker.KEY_ERROR_REASON
                ) ?: "Unknown error"
                binding.tvStatus.text = "Download failed — $reason"
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

            // Version spinner
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
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            binding.spinnerVersion.adapter = spinnerAdapter
            val selectedIndex = if (manual.downloadedChange != null) {
                changes.indexOf(manual.downloadedChange).takeIf { it >= 0 } ?: 0
            } else {
                0
            }
            binding.spinnerVersion.setSelection(selectedIndex)

            // Open button — only when a file is present
            if (manual.filePath != null && manual.downloadedChange != null) {
                binding.btnOpen.visibility = View.VISIBLE
                binding.btnOpen.setOnClickListener { onOpenClick(manual) }
            } else {
                binding.btnOpen.visibility = View.GONE
            }

            // Share + Export PDF buttons — only when a file is present
            if (manual.filePath != null) {
                binding.btnShare.visibility = View.VISIBLE
                binding.btnShare.setOnClickListener { onShareClick(manual) }
                binding.btnExportPdf.visibility = View.VISIBLE
                binding.btnExportPdf.setOnClickListener { onExportPdfClick(manual) }
            } else {
                binding.btnShare.visibility = View.GONE
                binding.btnExportPdf.visibility = View.GONE
            }

            // Download button — always visible; label reflects state
            binding.btnDownload.text = if (manual.downloadedChange != null) "Re-download" else "Download"
            binding.btnDownload.setOnClickListener { onDownloadClick(manual) }

            // Downloaded timestamp
            if (manual.downloadedAt != null) {
                val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                binding.tvDownloadedAt.visibility = View.VISIBLE
                binding.tvDownloadedAt.text = "Downloaded: ${sdf.format(Date(manual.downloadedAt))}"
            } else {
                binding.tvDownloadedAt.visibility = View.GONE
            }

            bindProgress(workInfo)
        }

        /** Updates only the progress row — called both from full bind and partial payloads. */
        fun bindProgress(workInfo: WorkInfo?) {
            val isActive = workInfo?.state == WorkInfo.State.RUNNING ||
                    workInfo?.state == WorkInfo.State.ENQUEUED

            binding.layoutProgress.visibility = if (isActive) View.VISIBLE else View.GONE
            // Disable download button while in progress to avoid duplicate enqueues
            binding.btnDownload.isEnabled = !isActive

            if (!isActive) return

            if (workInfo?.state == WorkInfo.State.RUNNING) {
                val current = workInfo.progress.getInt(DownloadWorker.KEY_PROGRESS_CURRENT, 0)
                val total = workInfo.progress.getInt(DownloadWorker.KEY_PROGRESS_TOTAL, 0)
                val etaSeconds = workInfo.progress.getLong(DownloadWorker.KEY_ETA_SECONDS, -1L)

                if (total > 0) {
                    binding.progressBar.isIndeterminate = false
                    binding.progressBar.progress = current * 100 / total
                    val etaText = if (etaSeconds >= 0) " · ${formatEta(etaSeconds)}" else ""
                    binding.tvProgress.text = "$current/$total sections$etaText"
                } else {
                    binding.progressBar.isIndeterminate = true
                    binding.tvProgress.text = "Fetching table of contents…"
                }
            } else {
                // ENQUEUED — waiting to start
                binding.progressBar.isIndeterminate = true
                binding.tvProgress.text = "Queued…"
            }
        }

        private fun formatEta(seconds: Long): String = when {
            seconds >= 3600 -> "~${seconds / 3600} hr remaining"
            seconds >= 60   -> "~${seconds / 60} min remaining"
            else            -> "< 1 min remaining"
        }
    }

    class ManualDiffCallback : DiffUtil.ItemCallback<Manual>() {
        override fun areItemsTheSame(oldItem: Manual, newItem: Manual) = oldItem.code == newItem.code
        override fun areContentsTheSame(oldItem: Manual, newItem: Manual) = oldItem == newItem
    }
}
