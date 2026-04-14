package com.tricare.manuals.data.network

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TocParser @Inject constructor() {

    companion object {
        // Matches chapter TOC filenames: C1TOC.html, C12TOC.html, etc.
        private val CHAPTER_TOC_REGEX = Regex("C\\d+TOC\\.html?", RegexOption.IGNORE_CASE)

        // Matches section/content URLs in the TRICARE site.
        private val SECTION_URL_PATTERN = Regex(
            "(DisplayManualHtml)|(C\\d+[A-Z0-9_]*\\.html?)",
            RegexOption.IGNORE_CASE
        )
        private val NATURAL_SORT_REGEX = Regex("(\\d+)")

        // CSS selectors for US-gov / MHS site chrome to remove before extraction
        private val BOILERPLATE_SELECTORS = listOf(
            ".usa-banner", ".usa-header", ".usa-nav", ".usa-footer", ".usa-identifier",
            ".usa-skipnav", ".usa-skip-link",
            "[class*='skipnav']", "[id*='skipnav']",
            "[class*='skip-nav']", "[id*='skip-nav']",
            "[class*='site-header']", "[class*='site-footer']",
            "[class*='social']", "[class*='govdelivery']", "[id*='GovDelivery']",
            ".breadcrumb", ".breadcrumbs", "[class*='breadcrumb']", "[aria-label='Breadcrumb']",
            ".disclaimer-modal",
            ".mil-header", ".mil-footer",
            "[role='banner']", "[role='contentinfo']", "[role='navigation']",
            // Previous / Next page navigation
            ".pager", ".pagination", "[class*='prev-next']", "[class*='previous-next']"
        )

        // Regex patterns applied to the final markdown text

        // Strips the per-section footer starting with "tricare.mil is the official…"
        private val TRICARE_FOOTER_REGEX = Regex(
            """(?s)- tricare\.mil is the official website.*""",
            RegexOption.IGNORE_CASE
        )
        // Strips '- END -' marker and any attached DHA Logo image
        private val END_MARKER_REGEX = Regex(
            """- END -!\[DHA Logo\][^\n]*""",
            RegexOption.IGNORE_CASE
        )
        // Strips standalone DHA Logo image line
        private val DHA_LOGO_REGEX = Regex(
            """(?m)^!\[DHA Logo\][^\n]*\n?""",
            RegexOption.IGNORE_CASE
        )
        // Strips the "official .mil website" banner block
        private val GOV_BANNER_REGEX = Regex(
            """(?s)\[Skip (main navigation|to main content)\][^\n]*\n.*?Share sensitive information only on official, secure websites\.\s*""",
            RegexOption.IGNORE_CASE
        )
        // Strips the "Need larger text?" link
        private val LARGER_TEXT_REGEX = Regex(
            """\[Need larger text\?\]\([^)]*\)\s*""",
            RegexOption.IGNORE_CASE
        )
        // Strips Previous/Next nav links that survived HTML removal
        private val PREV_NEXT_REGEX = Regex(
            """\[(Previous|Next)\]\([^)]*\)\s*""",
            RegexOption.IGNORE_CASE
        )
        // Strips "You are leaving Health.mil" footer block to end of string
        private val GOV_FOOTER_REGEX = Regex(
            """(?s)You are leaving Health\.mil.*""",
            RegexOption.IGNORE_CASE
        )
        // Replaces base64-encoded images with a human-readable placeholder
        private val BASE64_IMAGE_REGEX = Regex(
            """!\[([^\]]*)\]\(data:image/[^;]+;base64,[A-Za-z0-9+/=\s]+\)"""
        )
        // Inserts a blank line before numbered subsection patterns (e.g. "1.0 ", "2.1.3 ")
        // but NOT inside table rows (lines starting with |)
        private val SECTION_NUMBER_REGEX = Regex(
            """(?m)(?<!\n)(?<!\|)(\d+\.\d+(?:\.\d+)*\s+[A-Z])"""
        )
        // Inserts a blank line before Note:, Example:, Figure references
        private val NOTE_EXAMPLE_REGEX = Regex(
            """(?m)(?<!\n)(Note:|Example:|Figure\s+\d)""",
            RegexOption.IGNORE_CASE
        )
        // Collapses 3+ consecutive blank lines to 2
        private val EXCESS_BLANK_REGEX = Regex("""\n{3,}""")
    }

    fun parseChapterTocUrls(html: String, baseUrl: String): List<String> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            doc.select("a[href]")
                .map { it.attr("abs:href") }
                .filter { href -> SECTION_URL_PATTERN.containsMatchIn(href) }
                .distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isChapterToc(filename: String): Boolean = CHAPTER_TOC_REGEX.matches(filename)

    fun naturalSort(urls: List<String>): List<String> {
        return urls.sortedWith(Comparator { a, b ->
            val aParts = splitNatural(filenameFromUrl(a))
            val bParts = splitNatural(filenameFromUrl(b))
            compareNaturalParts(aParts, bParts)
        })
    }

    private fun filenameFromUrl(url: String) =
        url.substringAfterLast('/').substringBefore('?').substringBefore('#')

    private fun splitNatural(s: String): List<Any> {
        val parts = mutableListOf<Any>()
        var lastEnd = 0
        NATURAL_SORT_REGEX.findAll(s).forEach { match ->
            if (match.range.first > lastEnd) parts.add(s.substring(lastEnd, match.range.first))
            parts.add(match.value.toLong())
            lastEnd = match.range.last + 1
        }
        if (lastEnd < s.length) parts.add(s.substring(lastEnd))
        return parts
    }

    private fun compareNaturalParts(a: List<Any>, b: List<Any>): Int {
        for (i in 0 until minOf(a.size, b.size)) {
            val ai = a[i]; val bi = b[i]
            val cmp = when {
                ai is Long && bi is Long -> ai.compareTo(bi)
                ai is String && bi is String -> ai.compareTo(bi, ignoreCase = true)
                ai is Long -> -1
                else -> 1
            }
            if (cmp != 0) return cmp
        }
        return a.size.compareTo(b.size)
    }

    fun extractTitle(html: String): String {
        return try {
            val doc = Jsoup.parse(html)
            doc.title().takeIf { it.isNotBlank() }
                ?: doc.select("h1, h2, .section-title").firstOrNull()?.text()
                ?: "Untitled Section"
        } catch (e: Exception) {
            "Untitled Section"
        }
    }

    /**
     * Converts a full TRICARE section HTML page to clean Markdown.
     *
     * Applies all cleanup rules:
     * 1. Strip .mil site chrome (banners, nav, footer, breadcrumbs) via Jsoup
     * 2. Strip Previous/Next navigation links
     * 3. Remove base64 images (replace with placeholder)
     * 4. Strip remaining boilerplate text patterns
     * 5. Add line breaks before numbered subsections, Note:/Example: blocks
     */
    fun htmlToMarkdown(html: String): String {
        return try {
            val doc = Jsoup.parse(html)

            // Remove structural junk
            doc.select("script, style").remove()

            // Remove US-gov / MHS site chrome
            for (sel in BOILERPLATE_SELECTORS) {
                doc.select(sel).remove()
            }

            // Remove Previous / Next links by their text content
            doc.select("a").forEach { a ->
                val text = a.text().trim()
                if (text.equals("Previous", ignoreCase = true) ||
                    text.equals("Next", ignoreCase = true)) {
                    a.remove()
                }
            }

            // Convert the cleaned body to markdown
            val rawMd = elementToMarkdown(doc.body()).trim()

            // Apply text-level post-processing
            postProcess(rawMd)
        } catch (e: Exception) {
            ""
        }
    }

    private fun postProcess(md: String): String {
        var result = md

        // Remove "tricare.mil is the official website…" footer block
        result = TRICARE_FOOTER_REGEX.replace(result, "")

        // Remove "- END -" marker and DHA Logo
        result = END_MARKER_REGEX.replace(result, "")
        result = DHA_LOGO_REGEX.replace(result, "")

        // Remove gov banner block ("Skip main navigation … secure websites.")
        result = GOV_BANNER_REGEX.replace(result, "")

        // Remove "Need larger text?" link
        result = LARGER_TEXT_REGEX.replace(result, "")

        // Remove any surviving Previous/Next markdown links
        result = PREV_NEXT_REGEX.replace(result, "")

        // Remove gov footer block ("You are leaving Health.mil …")
        result = GOV_FOOTER_REGEX.replace(result, "")

        // Replace base64 images with descriptive placeholder
        result = BASE64_IMAGE_REGEX.replace(result) { match ->
            val altText = match.groupValues[1].trim()
            if (altText.isNotBlank())
                "\n\n_[Figure: $altText — see Health.mil for original figure]_\n\n"
            else
                "\n\n_[Figure removed — see Health.mil for original]_\n\n"
        }

        // Add blank lines before numbered subsection headings (but not in tables)
        result = result.lines().joinToString("\n") { line ->
            if (line.startsWith("|")) line  // don't touch table rows
            else SECTION_NUMBER_REGEX.replace(line, "\n\n$1")
        }

        // Add blank lines before Note:, Example:, Figure references
        result = NOTE_EXAMPLE_REGEX.replace(result, "\n\n$1")

        // Collapse excessive blank lines
        result = EXCESS_BLANK_REGEX.replace(result, "\n\n")

        return result.trim()
    }

    private fun elementToMarkdown(element: Element): String {
        val sb = StringBuilder()
        for (child in element.childNodes()) {
            when (child) {
                is TextNode -> {
                    val text = child.text()
                    if (text.isNotBlank()) sb.append(text)
                }
                is Element -> {
                    when (child.tagName().lowercase()) {
                        "h1" -> sb.append("\n## ${child.text()}\n\n")
                        "h2" -> sb.append("\n### ${child.text()}\n\n")
                        "h3" -> sb.append("\n#### ${child.text()}\n\n")
                        "h4" -> sb.append("\n##### ${child.text()}\n\n")
                        "h5", "h6" -> sb.append("\n###### ${child.text()}\n\n")
                        "p" -> {
                            val inner = elementToMarkdown(child)
                            if (inner.isNotBlank()) sb.append("\n$inner\n\n")
                        }
                        "br" -> sb.append("\n")
                        "strong", "b" -> sb.append("**${child.text()}**")
                        "em", "i" -> sb.append("*${child.text()}*")
                        "ul" -> {
                            sb.append("\n")
                            child.select("> li").forEach { li ->
                                sb.append("- ${li.text()}\n")
                            }
                            sb.append("\n")
                        }
                        "ol" -> {
                            sb.append("\n")
                            child.select("> li").forEachIndexed { idx, li ->
                                sb.append("${idx + 1}. ${li.text()}\n")
                            }
                            sb.append("\n")
                        }
                        "table" -> {
                            sb.append("\n")
                            val rows = child.select("tr")
                            rows.forEachIndexed { rowIdx, row ->
                                val cells = row.select("td, th")
                                sb.append("| ${cells.joinToString(" | ") { it.text() }} |\n")
                                if (rowIdx == 0) {
                                    sb.append("|${cells.joinToString("|") { "---" }}|\n")
                                }
                            }
                            sb.append("\n")
                        }
                        "a" -> {
                            val href = child.attr("href")
                            val text = child.text()
                            if (href.isNotBlank() && text.isNotBlank()) sb.append("[$text]($href)")
                            else sb.append(text)
                        }
                        "img" -> {
                            val src = child.attr("src")
                            val alt = child.attr("alt")
                            // Skip base64 images here; postProcess() will handle
                            // any that leak through as markdown
                            if (src.isNotBlank() && !src.startsWith("data:")) {
                                sb.append("![$alt]($src)")
                            }
                        }
                        "blockquote" -> sb.append("\n> ${child.text()}\n\n")
                        "code" -> sb.append("`${child.text()}`")
                        "pre" -> sb.append("\n```\n${child.text()}\n```\n\n")
                        "hr" -> sb.append("\n---\n\n")
                        else -> sb.append(elementToMarkdown(child))
                    }
                }
            }
        }
        return sb.toString()
    }
}
