package com.arcticfishtrail.game.integration

import java.util.Locale

/** sub13: "<first>_<last>", each reading rendered as "x,y,z" with three decimals. */
object MotionSample {

    const val MISSING = "na"

    fun format(values: FloatArray?): String {
        if (values == null || values.size < 3) return MISSING
        val x = values[0]
        val y = values[1]
        val z = values[2]
        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return MISSING
        // Locale.US is load-bearing. Without it "%.3f" renders 0,123 on a ru or de device;
        // those commas merge with the axis separator and the offer receives six fields
        // where it expects three. It only breaks on devices you do not own.
        return String.format(Locale.US, "%.3f,%.3f,%.3f", x, y, z)
    }

    fun describe(first: FloatArray?, last: FloatArray?): String =
        "${format(first)}_${format(last)}"
}
