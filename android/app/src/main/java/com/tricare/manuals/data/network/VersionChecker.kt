package com.tricare.manuals.data.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reports the latest published change number for a manual.
 *
 * This used to work it out by probing manuals.health.mil: fetch the TOC with no
 * Change parameter, scan Jsoup-decoded hrefs for the highest Change=N, then
 * binary-search 1..200 — around eight round trips per manual, plus special
 * handling because the site wrote `&amp;Change=` in raw HTML. All of it existed
 * because the number was never stated anywhere, only implied by which pages
 * rendered.
 *
 * That site is gone, and the replacement cannot be read by a browserless client
 * at all (see [MirrorClient]). The mirror's manifest states the number outright,
 * so this is now a single read of one small JSON file.
 */
@Singleton
class VersionChecker @Inject constructor(
    private val mirror: MirrorClient
) {

    /**
     * Latest published change for [code], or null if the manifest could not be
     * read (offline, or the mirror is unreachable).
     */
    fun checkLatestVersion(code: String): Int? = mirror.fetchLatestChange(code)

    /**
     * Change numbers available to download for [code].
     *
     * The mirror publishes only the current change of each manual — superseded
     * revisions are not kept — so this is the latest one alone. It previously
     * probed the five most recent numbers on the live site and returned
     * whichever responded; offering those now would list versions that cannot
     * actually be downloaded.
     */
    fun discoverAvailableChanges(code: String, latestChange: Int): List<Int> =
        if (latestChange > 0) listOf(latestChange) else emptyList()
}
