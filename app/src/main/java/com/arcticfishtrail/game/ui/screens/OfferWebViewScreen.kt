package com.arcticfishtrail.game.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.arcticfishtrail.game.integration.OfferWebViewClient
import com.arcticfishtrail.game.ui.components.GameBackground
import com.arcticfishtrail.game.ui.components.GlassPanel
import com.arcticfishtrail.game.ui.components.PlateButton
import com.arcticfishtrail.game.ui.theme.SunGold

/**
 * The offer WebView. The client ([OfferWebViewClient]) decides White/Black from the
 * main-frame response; this screen gives the offer everything it needs to work - third
 * party cookies, cookie flushing, file upload, downloads - and a retry screen when an
 * in-offer navigation fails, so the user is never trapped on a blank page.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OfferWebViewScreen(
    url: String,
    fromCache: Boolean,
    onWhiteVerdict: () -> Unit,
    onOfferCommitted: (String) -> Unit,
    onReroute: () -> Unit,
    onTransientWhite: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    val webViewRef = remember { arrayOfNulls<WebView>(1) }
    // Load-once flag keyed on the URL. Comparing against webView.url would drag the user
    // back to the landing page on every recomposition after they navigate.
    val loaded = remember(url) { booleanArrayOf(false) }
    var showError by remember(url) { mutableStateOf(false) }

    // Outstanding file-chooser callback. It MUST be answered exactly once in every outcome,
    // or the file input dies for the life of the page.
    val filePathCallback = remember { arrayOfNulls<ValueCallback<Array<Uri>>>(1) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = filePathCallback[0]
        filePathCallback[0] = null
        // A null result is a valid "cancelled" answer - the callback still has to receive it.
        callback?.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        )
    }

    // WebView flushes cookies lazily; without this the session and the click cookie do not
    // survive to the next launch and the user returns as a new visitor.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                CookieManager.getInstance().flush()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler {
        val webView = webViewRef[0]
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        } else {
            // No game UI behind the offer to fall back to, so leaving means leaving the app.
            activity?.finish()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        // Offer pages routinely pull assets over http from an https shell.
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    CookieManager.getInstance().setAcceptCookie(true)
                    // Blocked by default; pixels, iframe payment pages and partner redirects
                    // depend on them.
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    // Must precede the first loadUrl, or that request goes out without the header.
                    allowRequestedWithHeader(settings)

                    webChromeClient = object : WebChromeClient() {
                        override fun onShowFileChooser(
                            webView: WebView?,
                            callback: ValueCallback<Array<Uri>>?,
                            params: FileChooserParams?
                        ): Boolean {
                            if (callback == null) return false
                            // A second request before the first returned: answer the stale one.
                            filePathCallback[0]?.onReceiveValue(null)
                            filePathCallback[0] = callback
                            val intent = params?.createIntent()
                            return try {
                                if (intent != null) {
                                    fileChooserLauncher.launch(intent)
                                    true
                                } else {
                                    filePathCallback[0] = null
                                    callback.onReceiveValue(null)
                                    false
                                }
                            } catch (e: Exception) {
                                Log.w("OfferWebView", "No file chooser available", e)
                                filePathCallback[0] = null
                                callback.onReceiveValue(null)
                                false
                            }
                        }
                    }

                    webViewClient = OfferWebViewClient(
                        context = ctx,
                        fromCache = fromCache,
                        onWhiteVerdict = onWhiteVerdict,
                        onOfferCommitted = { committed ->
                            showError = false
                            onOfferCommitted(committed)
                        },
                        onReroute = onReroute,
                        onTransientWhite = onTransientWhite,
                        onInOfferError = { showError = true }
                    )

                    setDownloadListener { downloadUrl, _, contentDisposition, mimeType, _ ->
                        startDownload(ctx, downloadUrl, contentDisposition, mimeType)
                    }

                    webViewRef[0] = this
                }
            },
            update = { webView ->
                webViewRef[0] = webView
                if (!loaded[0]) {
                    loaded[0] = true
                    // Belt and braces for the main frame in case the allow-list below was
                    // rejected; this header applies to this one request only.
                    webView.loadUrl(url, mapOf(REQUESTED_WITH to webView.context.packageName))
                }
            },
            onRelease = { webView ->
                // Never leave the chooser callback unanswered when the view goes away.
                filePathCallback[0]?.onReceiveValue(null)
                filePathCallback[0] = null
                // Offer pages are ad-heavy: without this the Activity leaks on every config
                // change and JS timers and media keep running.
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.destroy()
                webViewRef[0] = null
            }
        )

        if (showError) {
            OfferErrorOverlay(onRetry = {
                showError = false
                webViewRef[0]?.reload()
            })
        }
    }
}

@Composable
private fun OfferErrorOverlay(onRetry: () -> Unit) {
    GameBackground(dim = 0.55f) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            GlassPanel(modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp)) {
                Text(
                    text = "Connection lost",
                    style = MaterialTheme.typography.headlineSmall,
                    color = SunGold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "The page could not load. Check your connection and try again.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                PlateButton(text = "Retry", onClick = onRetry, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private const val REQUESTED_WITH = "X-Requested-With"

/**
 * Hands a download to the system - the WebView cannot fetch files itself. Only http/https:
 * blob: and data: mean nothing outside the WebView that produced them.
 */
private fun startDownload(context: Context, url: String, contentDisposition: String?, mimeType: String?) {
    val lower = url.lowercase()
    if (!lower.startsWith("http://") && !lower.startsWith("https://")) return
    runCatching {
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val request = DownloadManager.Request(Uri.parse(url))
            .setMimeType(mimeType)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }.onFailure {
        Log.w("OfferWebView", "DownloadManager failed, handing the URL to the system", it)
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

/**
 * Restores the `X-Requested-With` header the WebView stopped sending by default in M108.
 *
 * The tracker identifies in-app traffic by that header, so without it the bundle id never
 * appears in its stats. `"*"` is the documented wildcard and covers every origin in a
 * redirect chain as well as sub-requests (XHR, iframes).
 */
private fun allowRequestedWithHeader(settings: WebSettings) {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
        Log.w("OfferWebView", "REQUESTED_WITH_HEADER_ALLOW_LIST unsupported on this WebView")
        return
    }
    runCatching {
        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, setOf("*"))
    }.onFailure {
        Log.w("OfferWebView", "Could not set the X-Requested-With allow list", it)
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
