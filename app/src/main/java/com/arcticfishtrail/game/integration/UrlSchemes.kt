package com.arcticfishtrail.game.integration

/**
 * Decides which URLs leave the WebView. Offer pages link out to payment apps, messengers and
 * `intent://` handlers; everything the page itself needs to render stays inside.
 */
object UrlSchemes {

    private val ALLOWED = setOf(
        "http", "https", "about", "srcdoc", "blob", "data", "javascript", "file"
    )

    /**
     * The scheme in lower case, or null when the URL has none.
     *
     * RFC 3986: a scheme starts with a letter and continues with letters, digits, `+`, `-`
     * or `.`. Checking that - rather than just looking for a colon - keeps
     * "/path:with:colons" and "2go:" from being mistaken for schemes.
     */
    fun schemeOf(url: String): String? {
        val trimmed = url.trim()
        val colon = trimmed.indexOf(':')
        if (colon <= 0) return null
        val candidate = trimmed.substring(0, colon)
        if (!isAlpha(candidate[0])) return null
        for (c in candidate) {
            if (!isAlpha(c) && !isDigit(c) && c != '+' && c != '-' && c != '.') return null
        }
        return candidate.lowercase()
    }

    /** Relative URLs have no scheme and stay inside the WebView. */
    fun isExternal(url: String): Boolean {
        val scheme = schemeOf(url) ?: return false
        return scheme !in ALLOWED
    }

    private fun isAlpha(c: Char) = c in 'a'..'z' || c in 'A'..'Z'
    private fun isDigit(c: Char) = c in '0'..'9'
}
