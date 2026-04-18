package com.tricare.manuals.ui.main

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tricare.manuals.data.model.Manual
import com.tricare.manuals.data.network.NtpClient
import com.tricare.manuals.data.repository.ManualRepository
import com.tricare.manuals.util.appDataStore
import com.tricare.manuals.worker.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

private val LAST_BIRTHDAY_YEAR_KEY = intPreferencesKey("last_birthday_year")

@HiltViewModel
class ManualListViewModel @Inject constructor(
    private val repository: ManualRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _manuals = MutableStateFlow<List<Manual>>(emptyList())
    val manuals: StateFlow<List<Manual>> = _manuals.asStateFlow()

    private val _showBirthdayPrompt = MutableStateFlow(false)
    val showBirthdayPrompt: StateFlow<Boolean> = _showBirthdayPrompt.asStateFlow()

    // Temporary diagnostic — shows raw checkLatestVersion() results so we can see
    // whether the problem is a network failure (null) or a wrong value.
    private val _versionDebug = MutableStateFlow("")
    val versionDebug: StateFlow<String> = _versionDebug.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultManualsExist()
            repository.getAllManuals().collect { list ->
                _manuals.value = list
            }
        }
    }

    fun checkAllVersions() {
        // Use KNOWN_MANUALS directly instead of _manuals.value — the StateFlow may still be
        // empty at the moment this is called (the init coroutine hasn't finished yet), which
        // would silently skip all version checks and leave every card showing "Change 1".
        viewModelScope.launch(Dispatchers.IO) {
            val results = StringBuilder()
            for (manual in ManualRepository.KNOWN_MANUALS) {
                val result = repository.checkLatestVersion(manual.code)
                results.append("${manual.code}=$result ")
            }
            val err = repository.getNetworkError()
            if (err.isNotEmpty()) results.append("\n$err")
            _versionDebug.value = results.toString().trim()
        }
    }

    fun checkBirthdayPrompt() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = NtpClient.getTime()
                val cal = Calendar.getInstance().apply { time = now }
                val month = cal.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
                val day = cal.get(Calendar.DAY_OF_MONTH)
                val year = cal.get(Calendar.YEAR)

                if (month == 6 && day == 17) {
                    val prefs = context.appDataStore.data.first()
                    val lastYear = prefs[LAST_BIRTHDAY_YEAR_KEY] ?: 0
                    if (lastYear < year) {
                        _showBirthdayPrompt.value = true
                    }
                }
            } catch (_: Exception) {
                // Silently ignore NTP errors
            }
        }
    }

    fun enqueueDownload(code: String) {
        viewModelScope.launch {
            val manual = repository.getManual(code) ?: return@launch
            val format = "md" // Markdown is the download format; PDF export is a separate action
            val changeNum = manual.latestChange.takeIf { it > 0 } ?: 1
            val inputData = workDataOf(
                DownloadWorker.KEY_MANUAL_CODE to code,
                DownloadWorker.KEY_FORMAT to format,
                DownloadWorker.KEY_CHANGE_NUM to changeNum
            )
            val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(inputData)
                .addTag(code)
                .build()
            // REPLACE ensures tapping Download twice cancels the first and restarts cleanly
            WorkManager.getInstance(context).enqueueUniqueWork(
                "download_$code",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    fun dismissBirthdayPrompt() {
        viewModelScope.launch {
            _showBirthdayPrompt.value = false
            val cal = Calendar.getInstance()
            val currentYear = cal.get(Calendar.YEAR)
            context.appDataStore.edit { prefs ->
                prefs[LAST_BIRTHDAY_YEAR_KEY] = currentYear
            }
        }
    }
}
