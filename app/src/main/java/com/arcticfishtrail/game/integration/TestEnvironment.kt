package com.arcticfishtrail.game.integration

/** sub14: is this install running somewhere that is not a real user's phone. */
object TestEnvironment {

    data class Snapshot(
        val firebaseTestLab: String? = null,
        val instrumentationPresent: Boolean = false,
        val debuggerConnected: Boolean = false,
        val debuggable: Boolean = false,
        val adbEnabled: String? = null,
        val hardware: String = "",
        val product: String = "",
        val model: String = "",
        val manufacturer: String = "",
        val brand: String = "",
        val device: String = "",
        val fingerprint: String = "",
    )

    private val HARDWARE_MARKERS = listOf(
        "goldfish", "ranchu", "vbox", "android_x86", "gce_x86", "cutf", "cuttlefish",
    )

    // Without a trailing underscore - the "<marker>_" prefix is applied at the comparison.
    // "sdk" is load-bearing: the current Android Studio AVD reports
    // product = "sdk_gphone64_arm64", which matches only through "sdk" + the "_" rule.
    private val PRODUCT_MARKERS = listOf(
        "sdk", "google_sdk", "vbox86p", "emulator", "simulator",
    )

    private val MODEL_MARKERS = listOf(
        "google_sdk", "emulator", "android sdk built for", "sdk_gphone",
    )

    fun isTestEnvironment(s: Snapshot): Boolean =
        s.firebaseTestLab == "true" ||
            s.instrumentationPresent ||
            s.debuggerConnected ||
            s.debuggable ||
            // Fires on any user who left developer options on, and there are more of them
            // than you would guess. Kept on its own line so it can be dropped alone.
            s.adbEnabled == "1" ||
            isEmulator(s)

    fun isEmulator(s: Snapshot): Boolean {
        val hardware = s.hardware.lowercase()
        val product = s.product.lowercase()
        val model = s.model.lowercase()
        val fingerprint = s.fingerprint.lowercase()

        if (HARDWARE_MARKERS.any { hardware.contains(it) }) return true

        // Equality or a "<marker>_" prefix, never a bare prefix: a real device whose
        // product is "sdkfusion" would otherwise be written off as an emulator, and every
        // install from it treated as fake traffic.
        if (PRODUCT_MARKERS.any { product == it || product.startsWith("${it}_") }) return true

        if (MODEL_MARKERS.any { model.contains(it) }) return true
        if (s.manufacturer.lowercase().contains("genymotion")) return true

        if (fingerprint.startsWith("generic") || fingerprint.contains("/sdk_")) return true

        // Noisy in the same way ADB_ENABLED is: every AOSP-derived custom ROM and a long
        // tail of grey-market retail devices ship test-keys builds, and those are real
        // users. On its own line so it can be dropped alone.
        if (fingerprint.endsWith("test-keys")) return true

        // Both, not either - a single "generic" is common enough on real hardware.
        if (s.brand.lowercase().startsWith("generic") &&
            s.device.lowercase().startsWith("generic")
        ) return true

        return false
    }
}
