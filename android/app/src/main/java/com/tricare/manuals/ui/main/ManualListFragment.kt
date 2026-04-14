package com.tricare.manuals.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tricare.manuals.R
import com.tricare.manuals.data.model.Manual
import com.tricare.manuals.data.repository.ManualRepository
import com.tricare.manuals.databinding.FragmentManualListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupMenu()
        observeViewModel()
        observeDownloadProgress()

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
            },
            onDownloadClick = { manual -> viewModel.enqueueDownload(manual.code) },
            onShareClick = { manual -> shareManual(manual) },
            onExportPdfClick = { manual -> exportAsPdf(manual) }
        )
        binding.recyclerManuals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ManualListFragment.adapter
        }
    }

    private fun observeDownloadProgress() {
        val workManager = androidx.work.WorkManager.getInstance(requireContext())
        ManualRepository.KNOWN_MANUALS.forEach { known ->
            workManager.getWorkInfosForUniqueWorkLiveData("download_${known.code}")
                .observe(viewLifecycleOwner) { workInfos ->
                    adapter.updateProgress(known.code, workInfos?.firstOrNull())
                }
        }
    }

    // ── PDF Export ────────────────────────────────────────────────────────────

    private fun exportAsPdf(manual: Manual) {
        val filePath = manual.filePath ?: return
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(requireContext(), "File not found. Please re-download.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(requireContext(), getString(R.string.export_pdf_loading), Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val html = withContext(Dispatchers.IO) {
                buildHtmlForPdf(manual, file.readText())
            }
            // WebView must be created on the main thread
            val webView = WebView(requireContext())
            webView.settings.javaScriptEnabled = false
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    val jobName = "${manual.name} Change ${manual.downloadedChange}"
                    val printManager = requireContext()
                        .getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val printAdapter = view.createPrintDocumentAdapter(jobName)
                    printManager.print(
                        jobName, printAdapter,
                        PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.NA_LETTER)
                            .build()
                    )
                }
            }
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    }

    private fun buildHtmlForPdf(manual: Manual, md: String): String {
        val sb = StringBuilder()
        var inList = false
        var inTable = false

        lineLoop@ for (rawLine in md.lines()) {
            val line = rawLine.trim()

            if (inList && !line.startsWith("- ") && line.isNotEmpty()) {
                sb.append("</ul>\n"); inList = false
            }
            if (inTable && !line.startsWith("|")) {
                sb.append("</table>\n"); inTable = false
            }

            when {
                line.startsWith("#### ") -> sb.append("<h4>${esc(line.drop(5))}</h4>\n")
                line.startsWith("### ")  -> sb.append("<h3>${esc(line.drop(4))}</h3>\n")
                line.startsWith("## ")   -> sb.append("<h2>${esc(line.drop(3))}</h2>\n")
                line.startsWith("# ")    -> sb.append("<h1>${esc(line.drop(2))}</h1>\n")
                line == "---"            -> sb.append("<hr>\n")
                line.startsWith("- ")    -> {
                    if (!inList) { sb.append("<ul>\n"); inList = true }
                    sb.append("<li>${inline(esc(line.drop(2)))}</li>\n")
                }
                line.startsWith("|") && !line.replace(Regex("\\|[-: ]+"), "").isBlank() -> {
                    if (!inTable) { sb.append("<table>\n"); inTable = true }
                    if (line.replace("|","").replace("-","").replace(" ","").isEmpty()) continue@lineLoop
                    val cells = line.split("|").drop(1).dropLast(1)
                    sb.append("<tr>")
                    cells.forEach { sb.append("<td>${esc(it.trim())}</td>") }
                    sb.append("</tr>\n")
                }
                line.isEmpty() -> sb.append("\n")
                else -> sb.append("<p>${inline(esc(line))}</p>\n")
            }
        }
        if (inList) sb.append("</ul>\n")
        if (inTable) sb.append("</table>\n")

        return """<!DOCTYPE html><html><head><meta charset="UTF-8">
<title>${esc(manual.name)} Change ${manual.downloadedChange}</title>
<style>
body{font-family:Arial,sans-serif;font-size:11pt;line-height:1.6;margin:0.75in}
h1{color:#1A5276;font-size:18pt;border-bottom:2px solid #1A5276;padding-bottom:6px;page-break-after:avoid}
h2{color:#1A5276;font-size:14pt;margin-top:20pt;page-break-after:avoid}
h3{color:#154360;font-size:12pt;page-break-after:avoid}
h4{color:#154360;font-size:11pt;page-break-after:avoid}
hr{border:0;border-top:1px solid #ccc;margin:12px 0}
p{margin:4px 0}
table{border-collapse:collapse;width:100%;margin:8px 0;font-size:10pt}
td,th{border:1px solid #999;padding:4px 8px;vertical-align:top}
th{background:#E8EEF5;font-weight:bold}
ul{margin:4px 0}li{margin:2px 0}
code{background:#f4f4f4;padding:1px 4px;font-size:10pt}
</style></head><body>
${sb.toString()}
</body></html>"""
    }

    private fun esc(s: String) = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun inline(s: String) = s
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "<strong>$1</strong>")
        .replace(Regex("\\*(.+?)\\*"), "<em>$1</em>")
        .replace(Regex("`(.+?)`"), "<code>$1</code>")

    // ── Share ─────────────────────────────────────────────────────────────────

    private fun shareManual(manual: Manual) {
        val filePath = manual.filePath ?: return
        val file = File(filePath)
        if (!file.exists()) return

        val mimeType = if (filePath.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "text/plain"
        val uri: Uri = FileProvider.getUriForFile(
            requireContext(), "com.tricare.manuals.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${manual.name} — Change ${manual.downloadedChange}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share ${manual.name}"))
    }

    // ── Menu & ViewModel ─────────────────────────────────────────────────────

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
                launch { viewModel.manuals.collect { adapter.submitList(it) } }
                launch { viewModel.showBirthdayPrompt.collect { if (it) showBirthdayDialog() } }
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
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.buymeacoffee.com/AppsbyDan")))
            }
            .setNegativeButton("Maybe Later") { _, _ -> viewModel.dismissBirthdayPrompt() }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
