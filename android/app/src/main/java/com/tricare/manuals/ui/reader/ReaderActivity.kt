package com.tricare.manuals.ui.reader

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tricare.manuals.R
import com.tricare.manuals.databinding.ActivityReaderBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class ReaderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MANUAL_CODE = "extra_manual_code"
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_FORMAT = "extra_format"
    }

    lateinit var binding: ActivityReaderBinding
    private val viewModel: ReaderViewModel by viewModels()

    private var manualCode: String = ""
    private var filePath: String = ""
    private var format: String = "md"

    private var sectionAdapter: SectionAdapter? = null
    private var pdfPageAdapter: PdfPageAdapter? = null
    private var currentSectionIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        manualCode = intent.getStringExtra(EXTRA_MANUAL_CODE) ?: ""
        filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: ""
        format = intent.getStringExtra(EXTRA_FORMAT) ?: "md"

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)

        viewModel.init(manualCode)

        // Setup nav drawer toggle
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(binding.navView)
        }

        when (format) {
            "pdf" -> setupPdfViewer()
            else -> setupMarkdownViewer()
        }

        setupDrawer()
        observeViewModel()
    }

    private fun setupPdfViewer() {
        binding.recyclerPdfPages.visibility = View.VISIBLE
        binding.recyclerSections.visibility = View.GONE

        val file = File(filePath)
        if (filePath.isNotEmpty() && file.exists()) {
            pdfPageAdapter = PdfPageAdapter(file)
            binding.recyclerPdfPages.apply {
                layoutManager = LinearLayoutManager(this@ReaderActivity)
                adapter = pdfPageAdapter
                setHasFixedSize(false)
            }
        } else {
            Toast.makeText(this, getString(R.string.pdf_not_found), Toast.LENGTH_LONG).show()
        }
    }

    private fun setupMarkdownViewer() {
        binding.recyclerPdfPages.visibility = View.GONE
        binding.recyclerSections.visibility = View.VISIBLE

        sectionAdapter = SectionAdapter(this)
        binding.recyclerSections.apply {
            layoutManager = LinearLayoutManager(this@ReaderActivity)
            adapter = sectionAdapter
        }
    }

    private fun setupDrawer() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            val sectionIndex = menuItem.itemId
            if (format != "pdf") {
                navigateToSection(sectionIndex)
            }
            binding.drawerLayout.closeDrawers()
            true
        }
    }

    private fun saveCurrentBookmark() {
        val sections = viewModel.sections.value
        if (sections.isNotEmpty() && currentSectionIndex < sections.size) {
            val section = sections[currentSectionIndex]
            val scrollY = binding.recyclerSections.computeVerticalScrollOffset()
            viewModel.addBookmark(section.filename, section.title, scrollY)
            Toast.makeText(this, getString(R.string.bookmark_saved), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.bookmark_saved), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBookmarkSheet() {
        BookmarkSheetFragment.newInstance(manualCode)
            .show(supportFragmentManager, BookmarkSheetFragment.TAG)
    }

    private fun navigateToSection(index: Int) {
        val layoutManager = binding.recyclerSections.layoutManager as? LinearLayoutManager
        layoutManager?.scrollToPosition(index)
        currentSectionIndex = index
    }

    private fun populateDrawerMenu(sections: List<com.tricare.manuals.data.model.Section>) {
        val menu = binding.navView.menu
        menu.clear()
        sections.forEachIndexed { index, section ->
            menu.add(Menu.NONE, index, index, section.title)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.sections.collect { sections ->
                sectionAdapter?.submitList(sections)
                populateDrawerMenu(sections)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfPageAdapter?.release()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_reader, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_bookmark -> {
                saveCurrentBookmark()
                true
            }
            R.id.action_bookmarks_list -> {
                showBookmarkSheet()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
