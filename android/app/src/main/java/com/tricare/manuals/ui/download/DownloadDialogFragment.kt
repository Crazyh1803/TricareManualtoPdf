package com.tricare.manuals.ui.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tricare.manuals.data.repository.ManualRepository
import com.tricare.manuals.databinding.FragmentDownloadDialogBinding
import com.tricare.manuals.util.appDataStore
import com.tricare.manuals.worker.DownloadWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadDialogFragment : BottomSheetDialogFragment() {

    @Inject lateinit var repository: ManualRepository

    companion object {
        const val TAG = "DownloadDialogFragment"
        private val WIFI_ONLY_KEY = booleanPreferencesKey("wifi_only_downloads")
        private val FORMAT_KEY = stringPreferencesKey("download_format")

        fun newInstance() = DownloadDialogFragment()
    }

    private var _binding: FragmentDownloadDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCheckboxes()
        setupDownloadButton()
    }

    private fun setupCheckboxes() {
        binding.cbManual1.text = ManualRepository.KNOWN_MANUALS[0].name
        binding.cbManual2.text = ManualRepository.KNOWN_MANUALS[1].name
        binding.cbManual3.text = ManualRepository.KNOWN_MANUALS[2].name
        binding.cbManual4.text = ManualRepository.KNOWN_MANUALS[3].name

        // Pre-check all by default
        binding.cbManual1.isChecked = true
        binding.cbManual2.isChecked = true
        binding.cbManual3.isChecked = true
        binding.cbManual4.isChecked = true
    }

    private fun setupDownloadButton() {
        binding.btnDownloadSelected.setOnClickListener {
            lifecycleScope.launch {
                val prefs = requireContext().appDataStore.data.first()
                val wifiOnly = prefs[WIFI_ONLY_KEY] ?: false
                val format = prefs[FORMAT_KEY] ?: "md"

                if (wifiOnly && !isOnWifi()) {
                    Toast.makeText(
                        requireContext(),
                        "WiFi-only mode is enabled. Connect to WiFi to download.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val selectedCodes = mutableListOf<String>()
                if (binding.cbManual1.isChecked) selectedCodes.add(ManualRepository.KNOWN_MANUALS[0].code)
                if (binding.cbManual2.isChecked) selectedCodes.add(ManualRepository.KNOWN_MANUALS[1].code)
                if (binding.cbManual3.isChecked) selectedCodes.add(ManualRepository.KNOWN_MANUALS[2].code)
                if (binding.cbManual4.isChecked) selectedCodes.add(ManualRepository.KNOWN_MANUALS[3].code)

                if (selectedCodes.isEmpty()) {
                    Toast.makeText(requireContext(), "Please select at least one manual.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                enqueueDownloads(selectedCodes, format)
                showProgressSection()
            }
        }
    }

    private suspend fun enqueueDownloads(codes: List<String>, format: String) {
        val workManager = WorkManager.getInstance(requireContext())

        for (code in codes) {
            // Fetch the live Manual from the database so we get the real latestChange value.
            // KNOWN_MANUALS are static defaults with latestChange=0 and must NOT be used here.
            val dbManual = repository.getManual(code)
            val changeNum = dbManual?.latestChange?.takeIf { it > 0 } ?: 1

            val inputData = workDataOf(
                DownloadWorker.KEY_MANUAL_CODE to code,
                DownloadWorker.KEY_FORMAT to format,
                DownloadWorker.KEY_CHANGE_NUM to changeNum
            )

            val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(inputData)
                .addTag(code)
                .build()

            workManager.enqueue(workRequest)

            workManager.getWorkInfoByIdLiveData(workRequest.id)
                .observe(viewLifecycleOwner) { workInfo ->
                    workInfo ?: return@observe
                    updateProgressForManual(code, workInfo)
                }
        }
    }

    private fun updateProgressForManual(code: String, workInfo: WorkInfo) {
        val (progressBar, textView) = when (code) {
            ManualRepository.KNOWN_MANUALS[0].code -> Pair(binding.progressManual1, binding.tvProgressManual1)
            ManualRepository.KNOWN_MANUALS[1].code -> Pair(binding.progressManual2, binding.tvProgressManual2)
            ManualRepository.KNOWN_MANUALS[2].code -> Pair(binding.progressManual3, binding.tvProgressManual3)
            ManualRepository.KNOWN_MANUALS[3].code -> Pair(binding.progressManual4, binding.tvProgressManual4)
            else -> return
        }

        progressBar.visibility = View.VISIBLE
        textView.visibility = View.VISIBLE

        when (workInfo.state) {
            WorkInfo.State.RUNNING -> {
                val current = workInfo.progress.getInt(DownloadWorker.KEY_PROGRESS_CURRENT, 0)
                val total = workInfo.progress.getInt(DownloadWorker.KEY_PROGRESS_TOTAL, 0)
                val etaSeconds = workInfo.progress.getLong(DownloadWorker.KEY_ETA_SECONDS, -1L)
                if (total > 0) {
                    progressBar.isIndeterminate = false
                    progressBar.progress = (current * 100 / total)
                    val etaText = if (etaSeconds >= 0) " · ${formatEta(etaSeconds)}" else ""
                    textView.text = "$current/$total sections$etaText"
                } else {
                    progressBar.isIndeterminate = true
                    textView.text = "Fetching table of contents…"
                }
            }
            WorkInfo.State.SUCCEEDED -> {
                progressBar.isIndeterminate = false
                progressBar.progress = 100
                textView.text = "Done!"
            }
            WorkInfo.State.FAILED -> {
                progressBar.isIndeterminate = false
                val reason = workInfo.outputData.getString(DownloadWorker.KEY_ERROR_REASON)
                    ?: "Unknown error"
                textView.text = "Failed: $reason"
            }
            WorkInfo.State.CANCELLED -> {
                textView.text = "Cancelled"
            }
            else -> {}
        }
    }

    private fun showProgressSection() {
        binding.layoutProgress.visibility = View.VISIBLE
        binding.btnDownloadSelected.isEnabled = false
        binding.btnDownloadSelected.text = "Downloading…"
    }

    private fun formatEta(seconds: Long): String = when {
        seconds >= 3600 -> "~${seconds / 3600} hr remaining"
        seconds >= 60   -> "~${seconds / 60} min remaining"
        else            -> "< 1 min remaining"
    }

    private fun isOnWifi(): Boolean {
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
