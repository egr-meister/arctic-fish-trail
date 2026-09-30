package com.arcticfishtrail.game.integration

/**
 * Single entry point for URLs that must open in the offer WebView (push clicks today).
 * A push tapped on a cold start arrives before any composable has subscribed, so links
 * are queued until a handler is registered and then delivered exactly once.
 */
object DeepLinkRouter {

    private val lock = Any()
    private var handler: ((String) -> Unit)? = null
    private val pending = ArrayDeque<String>()

    fun setHandler(newHandler: (String) -> Unit) {
        val drained: List<String>
        synchronized(lock) {
            handler = newHandler
            drained = pending.toList()
            pending.clear()
        }
        // Delivered outside the lock: the handler touches Compose state and may re-enter.
        drained.forEach(newHandler)
    }

    fun clearHandler() {
        synchronized(lock) { handler = null }
    }

    fun handle(url: String?) {
        if (url.isNullOrBlank()) return
        val current: ((String) -> Unit)?
        synchronized(lock) {
            current = handler
            if (current == null) pending.addLast(url)
        }
        current?.invoke(url)
    }
}
