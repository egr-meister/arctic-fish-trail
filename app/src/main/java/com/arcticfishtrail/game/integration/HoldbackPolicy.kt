package com.arcticfishtrail.game.integration

import kotlin.random.Random

/**
 * Attribution holdback: a fixed slice of installs deliberately never initialise AppsFlyer,
 * so the tracker can tell an unattributed control apart from normal traffic and measure
 * attribution's own lift.
 *
 * Pure so the odds can be read and unit-tested without Android. The decision is rolled once
 * and persisted by the caller - it must not be re-rolled per launch, or a user would drift
 * in and out of the holdback between openings.
 */
object HoldbackPolicy {

    /** 1 in this many installs is held back - every 5th, i.e. ~20%. */
    const val RATE = 5

    /** Query parameter and value that mark a held-back install in the offer URL. */
    const val PARAM = "sub_id_15"
    const val MARKER = "holdback"

    /** True for a held-back install. Random injected so tests are deterministic. */
    fun roll(random: Random = Random.Default): Boolean = random.nextInt(RATE) == 0
}
