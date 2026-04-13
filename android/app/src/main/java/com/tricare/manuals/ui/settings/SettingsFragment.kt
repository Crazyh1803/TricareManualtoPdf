package com.tricare.manuals.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
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
        setupStorageUsed()
        setupDeleteAll()
    }

    private fun setupStorageUsed() {
        val storagePref = findPreference<Preference>("storage_used")
        val manualsDir = File(requireContext().filesDir, "manuals")
        val totalBytes = if (manualsDir.exists()) {
            manualsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L
        val totalMb = totalBytes / (1024.0 * 1024.0)
        storagePref?.summary = if (totalMb >= 1.0) {
            "%.1f MB used".format(totalMb)
        } else {
            "${totalBytes / 1024} KB used"
        }
    }

    private fun setupDeleteAll() {
        val deletePref = findPreference<Preference>("delete_all_downloads")
        deletePref?.setOnPreferenceClickListener {
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
        val manualsDir = File(requireContext().filesDir, "manuals")
        if (manualsDir.exists()) {
            manualsDir.deleteRecursively()
        }
        setupStorageUsed()
        android.widget.Toast.makeText(
            requireContext(),
            getString(R.string.delete_done),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}
