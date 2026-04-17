package com.tricare.manuals.data.network

import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VersionChecker @Inject constructor(
    private val webClient: TricareWebClient
) {

    companion object {
        private const val TOC_BASE = "https://manuals.health.mil/pages/ManualToc.aspx?Manual="

        // Matches &Change=N or ?Change=N where N is not followed by another digit.
        // The TRICARE TOC page for Change=N contains DisplayManualHtml links with &Change=N
        // in their query strings. Invalid change numbers cause the server to fall back to
        // Change=1 content, so those links will say &Change=1, not the requested number.
        private val CHANGE_LINK_PATTERN = Regex(
            """[?&]Change=(\d+)(?:[^0-9]|$)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
        )
    }

    /**
     * Finds the highest change number available for [code] by binary-searching
     * the TOC page HTML.
     *
     * The TRICARE server returns HTTP 200 for every Change value, so a HEAD
     * request cannot distinguish valid from invalid.  Instead we fetch each
     * probe page and check whether it truly references the requested change
     * number in its own links (e.g. DisplayManualHtml.aspx?...&Change=53).
     * A page served for an invalid change number falls back to Change=1 content
     * and its internal links will say &Change=1, not the number we requested.
     *
     * Converges in ≈8 fetches (log₂200).
     * Returns null only if the server is completely unreachable.
     */
    fun checkLatestVersion(code: String): Int? {
        val base = "$TOC_BASE$code&Change="

        // Confirm the server is reachable at all.
        val firstHtml = webClient.fetchHtml("${base}1") ?: return null

        // If Change=1 itself already contains a higher change reference in its links,
        // that means the no-Change redirect gave us the latest directly.
        val latestFromFirstPage = maxChangeInLinks(firstHtml)
        if (latestFromFirstPage != null && latestFromFirstPage > 1) {
            return latestFromFirstPage
        }

        // Binary search: find highest N where the TOC page genuinely serves Change=N.
        var lo = 1
        var hi = 200      // safe ceiling — all four manuals are well below 200

        while (lo < hi - 1) {
            val mid = (lo + hi) / 2
            val html = webClient.fetchHtml("${base}$mid")
                ?: return lo  // network hiccup mid-search — return best known value
            if (pageServesChange(html, mid)) lo = mid else hi = mid
        }
        return lo
    }

    /**
     * Returns the maximum Change=N value found in any link on the page,
     * or null if no such links exist.
     */
    private fun maxChangeInLinks(html: String): Int? {
        return try {
            CHANGE_LINK_PATTERN.findAll(html)
                .mapNotNull { it.groupValues[1].toIntOrNull() }
                .maxOrNull()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Returns true if the fetched page genuinely corresponds to [n].
     *
     * Strategy 1 (preferred): The page's raw HTML should contain &Change=[n] in at
     * least one internal link. This is reliable because the TRICARE section links
     * (DisplayManualHtml.aspx?...&Change=N) always embed the change number.
     *
     * Strategy 2 (fallback): Search the visible page text for "Change N" with any
     * separator (handles non-breaking spaces and varied formatting).
     */
    private fun pageServesChange(html: String, n: Int): Boolean {
        return try {
            // Strategy 1: raw HTML link check — fast, no parsing needed
            val linkPattern = Regex(
                """[?&]Change=$n(?:[^0-9]|$)""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
            )
            if (linkPattern.containsMatchIn(html)) return true

            // Strategy 2: visible text check — handles pages where the change number
            // appears in headings/titles but not in link params.
            // Replace non-breaking spaces so \s matches them.
            val doc = Jsoup.parse(html)
            val text = (doc.title() + " " + doc.body().text())
                .replace('\u00A0', ' ')
            Regex("""change\W{0,4}$n(?:\D|$)""", RegexOption.IGNORE_CASE)
                .containsMatchIn(text)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Probes the five most recent change numbers up to [latestChange] to confirm
     * they are reachable, then returns them sorted descending for the spinner.
     */
    fun discoverAvailableChanges(code: String, latestChange: Int): List<Int> {
        val start = maxOf(1, latestChange - 4)
        return (start..latestChange)
            .filter { change ->
                webClient.headCheck("$TOC_BASE$code&Change=$change")
            }
            .sortedDescending()
    }
}
