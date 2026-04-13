package com.tricare.manuals.data.network

import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TocParser @Inject constructor() {

    companion object {
        // Matches chapter TOC filenames: C1TOC.html, C12TOC.html, etc.
        private val CHAPTER_TOC_REGEX = Regex("C\\d+TOC\\.html?", RegexOption.IGNORE_CASE)

        // Matches section/content URLs in the TRICARE site. Three known formats:
        //   1. Static file path:   /DisplayManualHtmlFile/TPT5/C48/C1S1_1.html
        //   2. ASP.NET handler:    DisplayManualHtml.aspx?...Section=C1S1_1
        //   3. Any .html file with a chapter/section naming convention: C1S1_1.html
        // Keeping the match intentionally broad so that unexpected URL structures are
        // captured. The chapter-TOC filter below still separates TOC pages from sections.
        private val SECTION_URL_PATTERN = Regex(
            "(DisplayManualHtml)|(C\\d+[A-Z0-9_]*\\.html?)",
            RegexOption.IGNORE_CASE
        )
        private val NATURAL_SORT_REGEX = Regex("(\\d+)")
    }

    /**
     * Parses a TOC HTML page and returns all hrefs that look like chapter or section HTML files.
     * [baseUrl] is used to resolve relative paths.
     */
    fun parseChapterTocUrls(html: String, baseUrl: String): List<String> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            doc.select("a[href]")
                .map { it.attr("abs:href") }
                .filter { href ->
                    SECTION_URL_PATTERN.containsMatchIn(href)
                }
                .distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Returns true if [filename] looks like a chapter TOC file (e.g. C1TOC.html, C12TOC.html).
     */
    fun isChapterToc(filename: String): Boolean {
        return CHAPTER_TOC_REGEX.matches(filename)
    }

    /**
     * Sorts a list of section URLs using natural (numeric) sort order based on filename components.
     */
    fun naturalSort(urls: List<String>): List<String> {
        return urls.sortedWith(Comparator { a, b ->
            val aParts = splitNatural(filenameFromUrl(a))
            val bParts = splitNatural(filenameFromUrl(b))
            compareNaturalParts(aParts, bParts)
        })
    }

    private fun filenameFromUrl(url: String): String {
        return url.substringAfterLast('/').substringBefore('?').substringBefore('#')
    }

    private fun splitNatural(s: String): List<Any> {
        val parts = mutableListOf<Any>()
        var lastEnd = 0
        NATURAL_SORT_REGEX.findAll(s).forEach { match ->
            if (match.range.first > lastEnd) {
                parts.add(s.substring(lastEnd, match.range.first))
            }
            parts.add(match.value.toLong())
            lastEnd = match.range.last + 1
        }
        if (lastEnd < s.length) {
            parts.add(s.substring(lastEnd))
        }
        return parts
    }

    private fun compareNaturalParts(a: List<Any>, b: List<Any>): Int {
        val len = minOf(a.size, b.size)
        for (i in 0 until len) {
            val ai = a[i]
            val bi = b[i]
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

    /**
     * Extracts the section title from an HTML page.
     */
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
     * Converts HTML content to a simplified Markdown string.
     */
    fun htmlToMarkdown(html: String): String {
        return try {
            val doc = Jsoup.parse(html)
            // Remove scripts and styles
            doc.select("script, style, nav, header, footer").remove()
            val body = doc.body()
            elementToMarkdown(body).trim()
        } catch (e: Exception) {
            ""
        }
    }

    private fun elementToMarkdown(element: org.jsoup.nodes.Element): String {
        val sb = StringBuilder()
        for (child in element.childNodes()) {
            when (child) {
                is org.jsoup.nodes.TextNode -> {
                    val text = child.text()
                    if (text.isNotBlank()) sb.append(text)
                }
                is org.jsoup.nodes.Element -> {
                    when (child.tagName().lowercase()) {
                        "h1" -> sb.append("\n# ${child.text()}\n\n")
                        "h2" -> sb.append("\n## ${child.text()}\n\n")
                        "h3" -> sb.append("\n### ${child.text()}\n\n")
                        "h4" -> sb.append("\n#### ${child.text()}\n\n")
                        "h5" -> sb.append("\n##### ${child.text()}\n\n")
                        "h6" -> sb.append("\n###### ${child.text()}\n\n")
                        "p" -> sb.append("\n${elementToMarkdown(child)}\n\n")
                        "br" -> sb.append("\n")
                        "strong", "b" -> sb.append("**${child.text()}**")
                        "em", "i" -> sb.append("*${child.text()}*")
                        "ul" -> {
                            sb.append("\n")
                            for (li in child.select("> li")) {
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
                            for (row in child.select("tr")) {
                                val cells = row.select("td, th")
                                sb.append("| ${cells.joinToString(" | ") { it.text() }} |\n")
                            }
                            sb.append("\n")
                        }
                        "a" -> {
                            val href = child.attr("href")
                            val text = child.text()
                            if (href.isNotBlank() && text.isNotBlank()) {
                                sb.append("[$text]($href)")
                            } else {
                                sb.append(text)
                            }
                        }
                        "img" -> {
                            val src = child.attr("src")
                            val alt = child.attr("alt")
                            if (src.isNotBlank()) sb.append("![$alt]($src)")
                        }
                        "blockquote" -> sb.append("\n> ${child.text()}\n\n")
                        "code" -> sb.append("`${child.text()}`")
                        "pre" -> sb.append("\n```\n${child.text()}\n```\n\n")
                        "hr" -> sb.append("\n---\n\n")
                        "div", "section", "article", "main" -> sb.append(elementToMarkdown(child))
                        else -> sb.append(elementToMarkdown(child))
                    }
                }
            }
        }
        return sb.toString()
    }
}
