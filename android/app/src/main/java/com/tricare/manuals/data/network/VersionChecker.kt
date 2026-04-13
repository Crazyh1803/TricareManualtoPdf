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
    }

    /**
     * Finds the latest change number for [code] by binary-searching with full HTML verification.
     *
     * The TRICARE server returns HTTP 200 for EVERY change value (valid or not), so HEAD-based
     * probing cannot distinguish valid from invalid. Instead we fetch the actual HTML for each
     * probe and check whether the page content explicitly references the change number we
     * requested:
     *
     *  - A valid page (e.g. Change=48) will contain "Change 48" in its heading or body.
     *  - An invalid page (e.g. Change=49) falls back to showing Change 1 content, which will
     *    never contain "Change 49".
     *
     * This converges in ≈7 fetches (log₂100) regardless of the actual latest change value.
     * Returns null only if the network is unreachable.
     */
    fun checkLatestVersion(code: String): Int? {
        var lo = 1
        var hi = 100   // safe ceiling — all four TRICARE manuals are well below 100

        while (lo < hi - 1) {
            val mid = (lo + hi) / 2
            val html = webClient.fetchHtml("$TOC_BASE$code&Change=$mid") ?: return null
            if (pageContainsChange(html, mid)) lo = mid else hi = mid
        }
        return lo
    }

    /**
     * Returns true if [html] explicitly mentions "Change [n]" in its title or body text.
     * A page served by the TRICARE site will contain this label when the server actually
     * served that change's content; it will be absent when the server silently fell back
     * to serving a different change (typically Change 1).
     */
    private fun pageContainsChange(html: String, n: Int): Boolean {
        return try {
            val doc = Jsoup.parse(html)
            val searchable = doc.title() + " " + doc.body().text()
            Regex("\\bChange\\s+$n\\b", RegexOption.IGNORE_CASE).containsMatchIn(searchable)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Probes HEAD requests on [ManualToc.aspx] for each of the last five change numbers
     * up to [latestChange]. Returns those that return HTTP 200, sorted descending.
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
