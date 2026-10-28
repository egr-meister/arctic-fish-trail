package com.arcticfishtrail.game.integration

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Turns the tracker's answer into the White/Black verdict, by reading the response to the
 * main frame - the campaign link the WebView opened itself. No HTTP probe: that single
 * load is the install's one click.
 *
 * Callbacks, all delivered on the main thread (WebView callbacks run there):
 *   onWhiteVerdict   - a fresh campaign link returned 404: pin White, forever.
 *   onOfferCommitted - the first real page rendered: cache it, stop the splash.
 *   onReroute        - a cached address is dead (any main-frame error): drop it, rebuild.
 *   onTransientWhite - a fresh link hit a network error (not an answer): White this launch,
 *                      pin nothing, so the next launch asks again.
 *
 * One verdict per WebView: once decided (verdict or first render), later main-frame 404s -
 * a dead link inside the offer cabinet - never flip an install already in the funnel.
 */
class OfferWebViewClient(
    private val context: Context,
    private val fromCache: Boolean,
    private val onWhiteVerdict: () -> Unit,
    private val onOfferCommitted: (String) -> Unit,
    private val onReroute: () -> Unit,
    private val onTransientWhite: () -> Unit,
    // Fired for a main-frame network error AFTER the verdict is settled - an in-offer
    // navigation that failed. Drives the screen's retry overlay; changes no verdict.
    private val onInOfferError: () -> Unit = {}
) : WebViewClient() {

    private var decided = false

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        return handleExternal(url)
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        // Landers 404 on images and scripts all the time; only the page itself decides.
        if (request?.isForMainFrame != true || decided) return

        if (fromCache) {
            // Any HTTP error on a cached address means the page is dead: drop the cache and
            // rebuild the campaign link. The branch is NOT changed - Black stays Black.
            decided = true
            onReroute()
            return
        }
        if (RoutingRules.isWhite(errorResponse?.statusCode ?: 0)) {
            // The tracker's answer about this install: White, permanently.
            decided = true
            onWhiteVerdict()
        }
        // fresh + non-404: the offer server is simply erroring; let the WebView retry.
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame != true) return

        val url = request.url?.toString().orEmpty()
        // market:// / intent:// are handled in shouldOverrideUrlLoading, and about:blank
        // errors fire routinely. Neither is an answer.
        if (url == "about:blank") return
        if (error?.errorCode == ERROR_UNSUPPORTED_SCHEME) return

        if (decided) {
            // The verdict already stands; this is a failed navigation inside the offer.
            onInOfferError()
            return
        }

        decided = true
        if (fromCache) {
            // A cached address that will not open: drop it and rebuild.
            onReroute()
        } else {
            // DNS/TLS/no connectivity on a fresh link is not the tracker's answer. Show
            // White for this launch only and pin nothing - the next launch asks again.
            onTransientWhite()
        }
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        super.onPageCommitVisible(view, url)
        if (decided) return
        // Pin the first page that actually renders, not every onPageStarted (about:blank,
        // intermediate redirects, the payment page). From here on the verdict is settled:
        // a 404 the user navigates into must not read as "the tracker said White".
        val scheme = url?.let { UrlSchemes.schemeOf(it) }
        if (scheme == "http" || scheme == "https") {
            decided = true
            onOfferCommitted(url)
        }
    }

    private fun handleExternal(url: String): Boolean {
        if (!UrlSchemes.isExternal(url)) return false
        return try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (e: ActivityNotFoundException) {
            // Nothing handles this scheme. Swallowing it keeps the offer usable; letting the
            // WebView try would show an ugly error page instead.
            Log.w("OfferWebViewClient", "No activity for $url", e)
            true
        }
    }
}
