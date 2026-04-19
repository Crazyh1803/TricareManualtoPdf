package com.tricare.manuals.data.network

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TricareWebClient @Inject constructor() {

    /** Last network error message — read by the diagnostic UI. */
    @Volatile var lastError: String = ""
        private set

    // Plain OkHttp with NO custom SSLSocketFactory. This makes OkHttp use
    // Android's default trust manager, which DOES respect network_security_config.xml.
    // A custom SSLSocketFactory bypasses the XML config entirely, which is why
    // <certificates src="user"/> had no effect — we're removing it so the config
    // actually applies. The domain-config for health.mil now trusts system + user
    // (covers Bitdefender Net-Defender and corporate proxies) + bundled DoD certs.
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Safari/537.36"

    fun fetchHtml(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                lastError = "HTTP-${response.code}:${url.takeLast(60)}"
                null
            }
        } catch (e: Exception) {
            lastError = "${e.javaClass.simpleName}:${e.message?.take(150)}"
            null
        }
    }

    fun fetchHtmlWithFinalUrl(url: String): Pair<String, String>? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val finalUrl = response.request.url.toString()
                val body = response.body?.string() ?: return null
                Pair(body, finalUrl)
            } else {
                lastError = "HTTP-${response.code}:${url.takeLast(60)}"
                null
            }
        } catch (e: Exception) {
            lastError = "${e.javaClass.simpleName}:${e.message?.take(150)}"
            null
        }
    }

    fun headCheck(url: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", userAgent)
                .build()
            val response = client.newCall(request).execute()
            response.code == 200
        } catch (_: Exception) {
            false
        }
    }
}
