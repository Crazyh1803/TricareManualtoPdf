package com.tricare.manuals.data.network

import android.content.Context
import com.tricare.manuals.R
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.KeyStore
import java.security.cert.CertificateException
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
    /** Last network/SSL error message — read by the diagnostic UI. */
    @Volatile var lastError: String = ""
        private set

    private val client: OkHttpClient = buildClient()

    /**
     * Builds an OkHttpClient with a composite SSLSocketFactory that:
     * 1. Tries the Android system CA store first (covers DigiCert, Let's Encrypt, etc.)
     * 2. Falls back to our bundled DoD Root CA 3 if system trust fails
     *
     * This is done in code (not network_security_config.xml) so it's immune to
     * resource shrinking and XML parse errors.
     */
    private fun buildClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        try {
            // ── System trust manager ──────────────────────────────────────────
            val systemTmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )
            systemTmf.init(null as KeyStore?)
            val systemTm = systemTmf.trustManagers.first() as X509TrustManager

            // ── DoD Root CA 3 / 5 / 6 trust manager ──────────────────────────
            // Bundle all three current DoD roots so we cover CA3 (RSA, valid to 2029),
            // CA5 (EC, valid to 2041), and CA6 (RSA-4096, valid to 2053).
            val cf = CertificateFactory.getInstance("X.509")
            val dodKs = KeyStore.getInstance(KeyStore.getDefaultType())
            dodKs.load(null, null)
            listOf(
                "dod_root_ca3" to R.raw.dod_root_ca3,
                "dod_root_ca5" to R.raw.dod_root_ca5,
                "dod_root_ca6" to R.raw.dod_root_ca6
            ).forEach { (alias, resId) ->
                try {
                    val cert = context.resources.openRawResource(resId).use {
                        cf.generateCertificate(it) as X509Certificate
                    }
                    dodKs.setCertificateEntry(alias, cert)
                } catch (_: Exception) { /* skip if a cert can't be loaded */ }
            }
            val dodTmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )
            dodTmf.init(dodKs)
            val dodTm = dodTmf.trustManagers.first() as X509TrustManager

            // ── Composite: system first, DoD cert fallback ────────────────────
            val compositeTm = object : X509TrustManager {
                override fun checkClientTrusted(
                    chain: Array<out X509Certificate>, authType: String
                ) = Unit

                override fun checkServerTrusted(
                    chain: Array<out X509Certificate>, authType: String
                ) {
                    try {
                        systemTm.checkServerTrusted(chain, authType)
                    } catch (_: CertificateException) {
                        // System trust failed — try DoD Root CA 3.
                        // Throws CertificateException if also untrusted.
                        dodTm.checkServerTrusted(chain, authType)
                    }
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> =
                    systemTm.acceptedIssuers + dodTm.acceptedIssuers
            }

            val sslCtx = SSLContext.getInstance("TLS")
            sslCtx.init(null, arrayOf(compositeTm), null)
            builder.sslSocketFactory(sslCtx.socketFactory, compositeTm)
            lastError = "SSL-setup:OK"  // overwritten by any later network error
        } catch (e: Exception) {
            lastError = "SSL-setup-FAILED:${e.javaClass.simpleName}:${e.message}"
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
                lastError = "HTTP-${response.code}:${url.takeLast(50)}"
                null
            }
        } catch (e: Exception) {
            lastError = "${e.javaClass.simpleName}:${e.message?.take(120)}"
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
                lastError = "HTTP-${response.code}:${url.takeLast(50)}"
                null
            }
        } catch (e: Exception) {
            lastError = "${e.javaClass.simpleName}:${e.message?.take(120)}"
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
