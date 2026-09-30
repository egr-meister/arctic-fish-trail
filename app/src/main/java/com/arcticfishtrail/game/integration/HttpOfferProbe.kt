package com.arcticfishtrail.game.integration

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.WebSettings
import com.arcticfishtrail.game.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class HttpOfferProbe(
    private val timeoutMillis: Int = 15_000,
    private val maxRedirects: Int = 10
) : OfferProbe {

    override fun probe(url: String): ProbeResult {
        var current = url
        var redirects = 0

        while (true) {
            val connection = URL(current).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            // Followed by hand so the final URL can be handed to the WebView.
            connection.instanceFollowRedirects = false
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            // Offer servers answer 404 to "Java/..." - that would pin White forever, so the
            // probe must look exactly like the WebView that follows it.
            connection.setRequestProperty("User-Agent", UserAgentProvider.userAgent)
            connection.setRequestProperty(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
            )
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            // This probe - not the WebView - is the request the tracker registers as the click
            // (it walks the redirect chain itself and the WebView only gets the final URL), so
            // the in-app marker must be here, on every hop. Set on each new connection.
            connection.setRequestProperty("X-Requested-With", BuildConfig.APPLICATION_ID)
            if (connection is HttpsURLConnection) relaxTls(connection)

            try {
                val code = connection.responseCode
                if (code in 300..399 && redirects < maxRedirects) {
                    val location = connection.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        // market:// or intent:// targets cannot be opened here, but they ARE
                        // the answer: Black, and OfferWebViewClient hands them to the system.
                        val resolved = resolve(current, location)
                            ?: return ProbeResult(code, location)
                        current = resolved
                        redirects++
                        continue
                    }
                }
                return ProbeResult(code, current)
            } finally {
                connection.disconnect()
            }
        }
    }

    /** Resolves a (possibly relative) Location; null when it is not http(s). */
    private fun resolve(current: String, location: String): String? = runCatching {
        val url = URL(URL(current), location)
        if (url.protocol.equals("http", true) || url.protocol.equals("https", true)) url.toString() else null
    }.getOrNull()

    /**
     * Accepts any certificate for THIS connection only. Offer domains often ship an
     * incomplete fullchain, which would fail the probe. The WebView and every other
     * connection keep standard validation - never setDefaultSSLSocketFactory here.
     * The real fix is the server's chain.
     */
    @SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager", "BadHostnameVerifier")
    private fun relaxTls(connection: HttpsURLConnection) {
        runCatching {
            val trustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) = Unit
                override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) = Unit
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
            val context = SSLContext.getInstance("TLS")
            context.init(null, arrayOf(trustManager), SecureRandom())
            connection.sslSocketFactory = context.socketFactory
            connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
        }.onFailure { Log.w("HttpOfferProbe", "Could not relax TLS for probe", it) }
    }
}

/**
 * Caches the WebView User-Agent at startup: getDefaultUserAgent() initialises the WebView
 * provider, which is slow and not something the probe thread should do.
 */
object UserAgentProvider {

    private const val FALLBACK =
        "Mozilla/5.0 (Linux; Android 13; K) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36"

    @Volatile
    private var cached: String? = null

    fun init(context: Context) {
        if (cached != null) return
        cached = runCatching { WebSettings.getDefaultUserAgent(context) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    val userAgent: String get() = cached ?: FALLBACK
}
