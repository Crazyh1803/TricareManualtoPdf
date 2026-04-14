package com.tricare.manuals.ui.settings

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
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
        val totalBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaStoreManualBytes()
        } else {
            legacyDownloadsDir()?.listFiles { f ->
                f.isFile && isManualFile(f.name)
            }?.sumOf { it.length() } ?: 0L
        }
        val totalMb = totalBytes / (1024.0 * 1024.0)
        storagePref?.summary = when {
            totalMb >= 0.1  -> "%.1f MB used".format(totalMb)
            totalBytes > 0  -> "${totalBytes / 1024} KB used"
            else            -> "No manuals downloaded"
        }
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
        val deleted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            deleteMediaStoreManuals()
        } else {
            var count = 0
            legacyDownloadsDir()?.listFiles { f -> f.isFile && isManualFile(f.name) }
                ?.forEach { if (it.delete()) count++ }
            count
        }
        setupStorageUsed()
        android.widget.Toast.makeText(
            requireContext(),
            if (deleted > 0) getString(R.string.delete_done) else "No files found to delete.",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    // ── Storage helpers ───────────────────────────────────────────────────────

    private fun isManualFile(name: String) =
        name.startsWith("TOT5_") || name.startsWith("TPT5_") ||
        name.startsWith("TRT5_") || name.startsWith("TST5_")

    private fun legacyDownloadsDir(): File? =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            .takeIf { it.exists() }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun mediaStoreManualBytes(): Long {
        var total = 0L
        val resolver = requireContext().contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        resolver.query(
            collection,
            arrayOf(MediaStore.Downloads.DISPLAY_NAME, MediaStore.Downloads.SIZE),
            null, null, null
        )?.use { cursor ->
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
            while (cursor.moveToNext()) {
                if (isManualFile(cursor.getString(nameCol) ?: continue)) {
                    total += cursor.getLong(sizeCol)
                }
            }
        }
        return total
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deleteMediaStoreManuals(): Int {
        val resolver = requireContext().contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val toDelete = mutableListOf<Uri>()
        resolver.query(
            collection,
            arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            val idCol   = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                if (isManualFile(cursor.getString(nameCol) ?: continue)) {
                    toDelete.add(ContentUris.withAppendedId(collection, cursor.getLong(idCol)))
                }
            }
        }
        return toDelete.count { resolver.delete(it, null, null) > 0 }
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
