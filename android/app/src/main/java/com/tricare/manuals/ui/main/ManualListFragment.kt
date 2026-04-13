package com.tricare.manuals.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tricare.manuals.R
import com.tricare.manuals.databinding.FragmentManualListBinding
import com.tricare.manuals.ui.download.DownloadDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManualListFragment : Fragment() {

    private var _binding: FragmentManualListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ManualListViewModel by viewModels()
    private lateinit var adapter: ManualAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManualListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set the fragment's toolbar as the activity's support action bar
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupFab()
        setupBmcButton()
        setupMenu()
        observeViewModel()

        viewModel.checkBirthdayPrompt()
        viewModel.checkAllVersions()
    }

    private fun setupRecyclerView() {
        adapter = ManualAdapter(
            onOpenClick = { manual ->
                manual.filePath?.let { path ->
                    val intent = Intent(
                        requireContext(),
                        com.tricare.manuals.ui.reader.ReaderActivity::class.java
                    ).apply {
                        putExtra(com.tricare.manuals.ui.reader.ReaderActivity.EXTRA_MANUAL_CODE, manual.code)
                        putExtra(com.tricare.manuals.ui.reader.ReaderActivity.EXTRA_FILE_PATH, path)
                        putExtra(
                            com.tricare.manuals.ui.reader.ReaderActivity.EXTRA_FORMAT,
                            manual.downloadedFormat ?: "md"
                        )
                    }
                    startActivity(intent)
                }
            }
        )
        binding.recyclerManuals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ManualListFragment.adapter
        }
    }

    private fun setupFab() {
        binding.fabDownload.setOnClickListener {
            DownloadDialogFragment.newInstance()
                .show(childFragmentManager, DownloadDialogFragment.TAG)
        }
    }

    private fun setupBmcButton() {
        binding.btnBuyMeACoffee.setOnClickListener {
            openUrl("https://www.buymeacoffee.com/AppsbyDan")
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_settings -> {
                        findNavController().navigate(R.id.action_manualListFragment_to_settingsFragment)
                        true
                    }
                    R.id.action_refresh -> {
                        viewModel.checkAllVersions()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.manuals.collect { manuals ->
                        adapter.submitList(manuals)
                    }
                }
                launch {
                    viewModel.showBirthdayPrompt.collect { show ->
                        if (show) showBirthdayDialog()
                    }
                }
            }
        }
    }

    private fun showBirthdayDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("\uD83C\uDF82 Happy June 17th!")
            .setMessage(
                "If TRICARE Manuals has been useful to you, consider buying the developer " +
                "a drink — it helps keep the app updated!"
            )
            .setPositiveButton("Buy a Drink \uD83C\uDF7A") { _, _ ->
                viewModel.dismissBirthdayPrompt()
                openUrl("https://www.buymeacoffee.com/AppsbyDan")
            }
            .setNegativeButton("Maybe Later") { _, _ ->
                viewModel.dismissBirthdayPrompt()
            }
            .setCancelable(false)
            .show()
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
