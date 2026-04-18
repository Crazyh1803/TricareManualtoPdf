package com.tricare.manuals.data.network

import android.content.Context
import com.tricare.manuals.R
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

@Singleton
class TricareWebClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Stores the last network error so the diagnostic Toast can surface it.
     * Empty string means no error yet.
     */
    @Volatile var lastError: String = ""
        private set

    private val client: OkHttpClient = buildClient()

    /**
     * Builds an OkHttpClient whose SSLSocketFactory trusts both the Android system
     * CA store and our bundled DoD Root CA 3 cert.
     *
     * Why do this in code rather than network_security_config.xml?
     * - resource shrinking can silently strip raw PEM files if the shrinker doesn't
     *   recognise the @raw/ reference inside <certificates src="@raw/...">
     * - config parsing errors fall back to a restrictive default with no indication
     * Loading the cert programmatically is explicit, auditable, and immune to both.
     */
    private fun buildClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        try {
            // 1. Load DoD Root CA 3 from raw resources.
            val cf = CertificateFactory.getInstance("X.509")
            val dodCert = context.resources.openRawResource(R.raw.dod_root_ca3).use {
                cf.generateCertificate(it) as X509Certificate
            }

            // 2. Build a KeyStore that holds all system-trusted CAs plus the DoD cert.
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)

            val systemTmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )
            systemTmf.init(null as KeyStore?)
            (systemTmf.trustManagers.firstOrNull() as? X509TrustManager)
                ?.acceptedIssuers
                ?.forEachIndexed { i, cert -> keyStore.setCertificateEntry("sys_$i", cert) }

            keyStore.setCertificateEntry("dod_root_ca3", dodCert)

            // 3. Create a TrustManager and SSLContext from the combined store.
            val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            tmf.init(keyStore)
            val trustManager = tmf.trustManagers.first() as X509TrustManager

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(trustManager), null)

            builder.sslSocketFactory(sslContext.socketFactory, trustManager)
        } catch (e: Exception) {
            // If the custom SSL setup fails for any reason, fall back to the system
            // default (system-trust only). Record the error so the diagnostic can show it.
            lastError = "SSL setup: ${e.javaClass.simpleName}: ${e.message}"
        }

        return builder.build()
    }

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
                lastError = "HTTP ${response.code} for $url"
                null
            }
        } catch (e: Exception) {
            lastError = "${e.javaClass.simpleName}: ${e.message}"
            null
        }
    }

    /**
     * Fetches [url], follows redirects, and returns Pair(html, finalUrl).
     * The finalUrl reflects the URL after any server-side redirects.
     */
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
                lastError = "HTTP ${response.code} for $url"
                null
            }
        } catch (e: Exception) {
            lastError = "${e.javaClass.simpleName}: ${e.message}"
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
