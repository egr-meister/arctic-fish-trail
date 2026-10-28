package com.arcticfishtrail.game.integration

/**
 * The parts of an FCM data message the app acts on. Parsed in one place so the service and
 * the Activity agree on the keys.
 *
 * A push with a `url` opens that link (with push_token and push_id appended); a push
 * without one is just a return - the normal routing decision already made stands.
 */
data class PushPayload(
    val title: String?,
    val body: String?,
    val url: String?
) {
    companion object {
        const val KEY_URL = "url"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"

        fun from(data: Map<String, String>): PushPayload = PushPayload(
            title = data[KEY_TITLE]?.takeIf { it.isNotBlank() },
            body = data[KEY_BODY]?.takeIf { it.isNotBlank() },
            url = data[KEY_URL]?.takeIf { it.isNotBlank() }
        )
    }
}
