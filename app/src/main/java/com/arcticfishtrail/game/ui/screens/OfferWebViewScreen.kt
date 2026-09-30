package com.arcticfishtrail.game.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.arcticfishtrail.game.integration.OfferWebViewClient

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OfferWebViewScreen(url: String) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    // Live WebView, so back navigation can consult its history.
    val webViewRef = remember { arrayOfNulls<WebView>(1) }
    // Load-once flag keyed on the URL. Comparing against webView.url instead would drag the
    // user back to the landing page on every recomposition after they navigate.
    val loaded = remember(url) { booleanArrayOf(false) }

    BackHandler {
        val webView = webViewRef[0]
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        } else {
            // No game UI behind the offer to fall back to - leave instead of trapping the user.
            activity?.finish()
        }
    }

    AndroidView(
        // The activity is edge-to-edge; keep the page out from under the system bars.
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        factory = { ctx ->
            WebView(ctx).apply {
                with(settings) {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    @Suppress("DEPRECATION")
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    javaScriptCanOpenWindowsAutomatically = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                // Must happen before the first loadUrl, or that request leaves without it.
                enableRequestedWithHeader(settings)
                webChromeClient = WebChromeClient()
                webViewClient = OfferWebViewClient(ctx)
                webViewRef[0] = this
            }
        },
        update = { webView ->
            webViewRef[0] = webView
            if (!loaded[0]) {
                loaded[0] = true
                // Belt and braces: covers the main frame even where the allow-list API is gone
                // (it is marked temporary). Does not survive redirects or reach subresources.
                webView.loadUrl(url, mapOf(REQUESTED_WITH_HEADER to webView.context.packageName))
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
            webViewRef[0] = null
        }
    )
}

private const val REQUESTED_WITH_HEADER = "X-Requested-With"

/**
 * Since WebView M108 X-Requested-With (the app's package name) is no longer sent by default.
 * The origin allow-list from androidx.webkit brings it back; "*" covers every domain in the
 * redirect chain plus XHR/iframe subrequests. Both exceptions are possible on odd WebView
 * builds (unsupported feature, rejected rule) and must never break the offer screen.
 */
private fun enableRequestedWithHeader(settings: WebSettings) {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) return
    try {
        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, setOf("*"))
    } catch (e: IllegalArgumentException) {
        Log.w("OfferWebViewScreen", "Requested-With allow-list rule rejected", e)
    } catch (e: UnsupportedOperationException) {
        Log.w("OfferWebViewScreen", "Requested-With allow-list not supported", e)
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
