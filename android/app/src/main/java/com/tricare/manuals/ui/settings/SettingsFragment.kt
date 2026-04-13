package com.tricare.manuals.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.tricare.manuals.R
import com.tricare.manuals.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsPreferenceFragment())
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

@AndroidEntryPoint
class SettingsPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        setupDarkTheme()
        setupStorageUsed()
        setupDeleteAll()
        setupSupportDeveloper()
        setupVersion()
    }

    private fun setupDarkTheme() {
        findPreference<SwitchPreferenceCompat>("dark_theme")
            ?.setOnPreferenceChangeListener { _, newValue ->
                val isDark = newValue as Boolean
                AppCompatDelegate.setDefaultNightMode(
                    if (isDark) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                )
                true
            }
    }

    private fun setupStorageUsed() {
        val storagePref = findPreference<Preference>("storage_used")
        // Files now live in the public Downloads folder
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val totalBytes = if (downloadsDir.exists()) {
            downloadsDir.listFiles { f ->
                f.isFile && (f.name.startsWith("TOT5_") || f.name.startsWith("TPT5_") ||
                        f.name.startsWith("TRT5_") || f.name.startsWith("TST5_"))
            }?.sumOf { it.length() } ?: 0L
        } else 0L
        val totalMb = totalBytes / (1024.0 * 1024.0)
        storagePref?.summary = if (totalMb >= 0.1) "%.1f MB used".format(totalMb)
        else if (totalBytes > 0) "${totalBytes / 1024} KB used"
        else "No manuals downloaded"
    }

    private fun setupDeleteAll() {
        findPreference<Preference>("delete_all_downloads")?.setOnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_confirm_title))
                .setMessage(getString(R.string.delete_confirm_message))
                .setPositiveButton(getString(R.string.delete_confirm_positive)) { _, _ ->
                    deleteAllDownloads()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            true
        }
    }

    private fun deleteAllDownloads() {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        var deletedCount = 0
        downloadsDir.listFiles { f ->
            f.isFile && (f.name.startsWith("TOT5_") || f.name.startsWith("TPT5_") ||
                    f.name.startsWith("TRT5_") || f.name.startsWith("TST5_"))
        }?.forEach { f ->
            if (f.delete()) deletedCount++
        }
        setupStorageUsed()
        android.widget.Toast.makeText(
            requireContext(),
            if (deletedCount > 0) getString(R.string.delete_done) else "No files found to delete.",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun setupSupportDeveloper() {
        findPreference<Preference>("support_developer")?.setOnPreferenceClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.buymeacoffee.com/AppsbyDan")))
            true
        }
    }

    private fun setupVersion() {
        val versionPref = findPreference<Preference>("app_version")
        try {
            val pInfo = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0)
            versionPref?.summary = pInfo.versionName
        } catch (e: Exception) {
            versionPref?.summary = "1.0"
        }
    }
}
