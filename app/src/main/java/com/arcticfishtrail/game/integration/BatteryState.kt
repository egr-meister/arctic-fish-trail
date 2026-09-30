package com.arcticfishtrail.game.integration

/**
 * sub12: "<percent>_<onPower>", e.g. "85_true" or "-1_false".
 *
 * percentOf and isOnPower are public so the format has exactly one definition - a caller
 * that needs the percent on its own must not re-derive it from level and scale.
 */
object BatteryState {

    // Mirrors android.os.BatteryManager.BATTERY_STATUS_*, duplicated so this file stays
    // free of android.os and can be read on its own.
    const val STATUS_UNKNOWN = 1
    const val STATUS_CHARGING = 2
    const val STATUS_FULL = 5

    /** Not 0: an empty battery and a missing reading are different answers. */
    const val UNKNOWN_PERCENT = -1

    fun describe(plugged: Int, status: Int, level: Int, scale: Int): String =
        "${percentOf(level, scale)}_${isOnPower(plugged, status)}"

    fun percentOf(level: Int, scale: Int): Int {
        if (level < 0 || scale <= 0) return UNKNOWN_PERCENT
        // scale is not always 100 - a fair number of devices report 255, and dividing by
        // a hardcoded 100 would send a third of the real charge.
        return (level * 100 / scale).coerceIn(0, 100)
    }

    /**
     * FULL counts as on-power alongside CHARGING: at 100% many ROMs stop reporting
     * CHARGING and switch to FULL while the cable is still in, so plugged alone would
     * miss every device sitting on a charger overnight.
     */
    fun isOnPower(plugged: Int, status: Int): Boolean =
        plugged != 0 || status == STATUS_CHARGING || status == STATUS_FULL
}
