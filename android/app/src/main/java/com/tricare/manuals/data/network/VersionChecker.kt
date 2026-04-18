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
        private val CHANGE_PARAM = Regex("[?&]Change=(\\d+)", RegexOption.IGNORE_CASE)
    }

    /**
     * Finds the highest change number available for [code].
     *
     * **Why raw-HTML regex failed:** The TRICARE site encodes ampersands as `&amp;` in
     * HTML attributes, so a link looks like `...&amp;Change=53`. A raw-HTML regex of
     * `[?&]Change=(\d+)` never matches because the literal `&` character isn't in the
     * raw HTML string. Jsoup decodes `&amp;` → `&` when reading the `href` attribute,
     * so we now use Jsoup for all Change= detection.
     *
     * Algorithm:
     * 1. Fetch the TOC page with no Change param. If the server redirects to ?Change=N,
     *    return N immediately (1 round-trip).
     * 2. Scan the returned HTML with Jsoup for the highest Change=N in any decoded href
     *    or <option> value.
     * 3. Binary-search [1, 200] using Jsoup-decoded hrefs to distinguish valid from
     *    invalid change numbers (~8 fetches, log₂200).
     *
     * Returns null only if the server is completely unreachable.
     */
    fun checkLatestVersion(code: String): Int? {

        // ── Step 1: no-Change fetch — server may redirect to the latest ──────────
        val noChangeResult = webClient.fetchHtmlWithFinalUrl("$TOC_BASE$code")
        if (noChangeResult != null) {
            val (html, finalUrl) = noChangeResult

            // If the server redirected to e.g. ManualToc.aspx?Manual=TOT5&Change=53
            val changeFromRedirect = CHANGE_PARAM.find(finalUrl)
                ?.groupValues?.get(1)?.toIntOrNull()
            if (changeFromRedirect != null && changeFromRedirect > 0) return changeFromRedirect

            // Scan the HTML for the max Change=N in any decoded href / option value
            val changeFromLinks = maxChangeInDecodedHrefs(html)
            if (changeFromLinks != null && changeFromLinks > 1) return changeFromLinks
        }

        // ── Step 2: binary search with Jsoup-decoded hrefs ───────────────────────
        // Re-use the already-fetched HTML if we have it, otherwise fetch Change=1.
        if (webClient.fetchHtml("${TOC_BASE}$code&Change=1") == null) return null

        var lo = 1      // highest known-valid change
        var hi = 200    // safe ceiling (all four TRICARE manuals are well below 200)

        while (lo < hi - 1) {
            val mid = (lo + hi) / 2
            val html = webClient.fetchHtml("${TOC_BASE}$code&Change=$mid")
                ?: return lo    // network hiccup — return best known value
            if (pageServesChange(html, mid)) lo = mid else hi = mid
        }
        return lo
    }

    /**
     * Returns the maximum Change=N value found in any Jsoup-decoded href on the page,
     * or in any <select name="Change"> <option> value elements.
     *
     * Jsoup decodes HTML entities when reading attribute values, so `&amp;Change=53`
     * in the raw HTML becomes `&Change=53` in the attribute string — which our regex
     * then matches correctly.
     */
    private fun maxChangeInDecodedHrefs(html: String): Int? {
        return try {
            val doc = Jsoup.parse(html)

            val fromLinks = doc.select("a[href]").flatMap { el ->
                CHANGE_PARAM.findAll(el.attr("href"))
                    .mapNotNull { it.groupValues[1].toIntOrNull() }
            }

            // Also check <select name="Change"> or <option> elements in case the site
            // has a change-selector dropdown
            val fromSelect = doc
                .select("[name=Change] option, [name=change] option")
                .mapNotNull { it.attr("value").toIntOrNull() }

            (fromLinks + fromSelect).maxOrNull()
        } catch (_: Exception) { null }
    }

    /**
     * Returns true if the fetched page genuinely corresponds to change [n].
     *
     * Strategy 1 (preferred): Use Jsoup to read decoded hrefs. A valid Change=N page
     * has links like `DisplayManualHtml.aspx?...&Change=N`; an invalid one falls back
     * to Change=1 (or the latest) and its links reflect that other number.
     *
     * Strategy 2 (fallback): Scan visible text for "Change N" — handles pages where
     * the change appears in a heading/title but not in link query parameters.
     */
    private fun pageServesChange(html: String, n: Int): Boolean {
        return try {
            val doc = Jsoup.parse(html)
            val linkPattern = Regex("[?&]Change=$n(?:[^0-9]|$)", RegexOption.IGNORE_CASE)

            // Strategy 1: decoded hrefs (fixes &amp;Change=N missing from raw regex)
            if (doc.select("a[href]").any { linkPattern.containsMatchIn(it.attr("href")) }) {
                return true
            }

            // Strategy 2: visible text
            val text = (doc.title() + " " + doc.body().text()).replace('\u00A0', ' ')
            Regex("""change\W{0,4}$n(?:\D|$)""", RegexOption.IGNORE_CASE).containsMatchIn(text)
        } catch (_: Exception) { false }
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
