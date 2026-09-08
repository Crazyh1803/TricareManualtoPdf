package com.tricare.manuals.data.network

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads manual content from the project's published mirror.
 *
 * **Why not straight from manuals.dha.mil.** The manuals site was rebuilt as a
 * Blazor app behind bot defence. Its publication page returns a ~6 KB shell to
 * any client that cannot run JavaScript, so a browserless client cannot even
 * learn which sections exist; and the endpoint serving section content drops
 * the connection on a client with no established session — measured at 5 of 25
 * sequential requests succeeding. Establishing that session needs a real
 * browser, which an OkHttp client is not.
 *
 * The scraper already visits the site in headless Chromium once a week and
 * publishes the result as static files. Reading those is reliable, needs no
 * session, and means one scrape serves every user instead of every device
 * scraping a government server for itself.
 *
 * What this costs: content is as fresh as the last scrape rather than live.
 * [SOURCE_BASE] links every section back to the authoritative page so a reader
 * can always check the original — see [sourceUrl].
 */
@Singleton
class MirrorClient @Inject constructor() {

    companion object {
        /** Static mirror published by .github/workflows/update-manuals.yml. */
        const val MIRROR_BASE = "https://crazyh1803.github.io/TricareManualtoPdf/data"

        /** The authoritative site, for "view the official page" links. */
        const val SOURCE_BASE = "https://manuals.dha.mil"

        /** Official page for a manual. */
        fun sourceUrl(code: String): String = "$SOURCE_BASE/View-Publication/$code"

        /**
         * Official page for one section. [name] is the source file name the
         * scraper records for each section (e.g. "C7S18_2"), which is exactly
         * the identifier the site uses in its own URLs.
         */
        fun sourceUrl(code: String, revision: Int, name: String): String =
            "$SOURCE_BASE/View-Publication/$code/Revision/$revision/FileName/$name"
    }

    /** Last network error message — read by the diagnostic UI. */
    @Volatile
    var lastError: String = ""
        private set

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun fetch(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "*/*")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    lastError = "HTTP-${response.code}:${url.takeLast(60)}"
                    null
                }
            }
        } catch (e: Exception) {
            lastError = "${e.javaClass.simpleName}:${e.message?.take(150)}"
            null
        }
    }

    /** One manual as listed in the mirror's manifest. */
    data class MirrorManual(
        val code: String,
        val name: String,
        val latestChange: Int,
        val hasContent: Boolean
    )

    /** One section as listed in a manual's table of contents. */
    data class MirrorSection(
        val id: String,
        /** Source file name on the official site — used to build [sourceUrl]. */
        val name: String,
        val title: String,
        val chapter: Int,
        val section: String,
        val isChapterToc: Boolean
    )

    /**
     * The manual list, or null if the manifest could not be read.
     *
     * Returning the list rather than reading a hardcoded set means a manual
     * added to the scraper appears in the app without an app release.
     */
    fun fetchManuals(): List<MirrorManual>? {
        val body = fetch("$MIRROR_BASE/manuals.json") ?: return null
        return try {
            val arr = JSONObject(body).getJSONArray("manuals")
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                MirrorManual(
                    code = o.getString("code"),
                    name = o.getString("name"),
                    latestChange = o.optInt("latestChange", 0),
                    hasContent = o.optBoolean("hasContent", false)
                )
            }
        } catch (e: Exception) {
            lastError = "manifest-parse:${e.message?.take(120)}"
            null
        }
    }

    /** Latest published change number for [code], or null if unknown. */
    fun fetchLatestChange(code: String): Int? =
        fetchManuals()?.firstOrNull { it.code.equals(code, ignoreCase = true) }?.latestChange

    /** Table of contents for [code], or null if it could not be read. */
    fun fetchToc(code: String): Pair<Int, List<MirrorSection>>? {
        val body = fetch("$MIRROR_BASE/$code/toc.json") ?: return null
        return try {
            val root = JSONObject(body)
            val change = root.optInt("change", 0)
            val arr = root.getJSONArray("sections")
            val sections = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                MirrorSection(
                    id = o.getString("id"),
                    // Pre-migration TOCs have no "name"; fall back to the id so
                    // a source link is still built, just from the older value.
                    name = o.optString("name", o.getString("id")),
                    title = o.optString("title", ""),
                    chapter = o.optInt("chapter", 0),
                    section = o.optString("section", ""),
                    isChapterToc = o.optBoolean("isChapterToc", false)
                )
            }
            change to sections
        } catch (e: Exception) {
            lastError = "toc-parse:${e.message?.take(120)}"
            null
        }
    }

    /** Stored HTML for one section, or null if it could not be read. */
    fun fetchSectionHtml(code: String, sectionId: String): String? =
        fetch("$MIRROR_BASE/$code/s/$sectionId.html")
}
