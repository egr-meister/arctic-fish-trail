package com.arcticfishtrail.game.ui

import androidx.annotation.DrawableRes
import com.arcticfishtrail.game.R

/**
 * Single entry point for every image in the game.
 *
 * All files live in `res/drawable-nodpi/` under FIXED names. To swap art, drop new files with
 * the same names (PNG with transparency; `bg_main` as an opaque JPG) — no code change needed.
 * If a file's proportions change, update its aspect constant below.
 */
object Assets {
    @DrawableRes val bgMain: Int = R.drawable.bg_main
    @DrawableRes val logo: Int = R.drawable.logo

    /** Aya, the ice-fishing heroine (portrait). */
    @DrawableRes val mascotA: Int = R.drawable.mascot_a

    /** Red salmon mascot (landscape). */
    @DrawableRes val mascotB: Int = R.drawable.mascot_b

    /** Blue arctic fish mascot (landscape). */
    @DrawableRes val mascotC: Int = R.drawable.mascot_c

    /** Ornate ice-framed plate for main-menu CTAs. */
    @DrawableRes val btnMenu: Int = R.drawable.btn_menu

    /** Simple ice plate reused by every other button (drawn as a horizontal 3-slice). */
    @DrawableRes val btnPlate: Int = R.drawable.btn_plate

    /** Round ice plate: level bubbles, home trail stops, card backs. */
    @DrawableRes val plateRound: Int = R.drawable.plate_round
    @DrawableRes val iconBack: Int = R.drawable.icon_back
    @DrawableRes val iconPause: Int = R.drawable.icon_pause

    const val LOGO_ASPECT: Float = 760f / 859f
    const val MASCOT_A_ASPECT: Float = 445f / 900f
    const val MASCOT_B_ASPECT: Float = 820f / 591f
    const val MASCOT_C_ASPECT: Float = 820f / 535f
    const val BTN_MENU_ASPECT: Float = 1200f / 316f
    const val BTN_PLATE_ASPECT: Float = 1000f / 351f

    /** Fraction of `btn_plate` width kept unstretched at each end (the rounded ice corners). */
    const val BTN_PLATE_CAP_FRACTION: Float = 0.12f

    /** 18 match icons, `item_01`..`item_18` (Pairs builds each deck from a random subset). */
    val items: List<Int> = listOf(
        R.drawable.item_01, R.drawable.item_02, R.drawable.item_03, R.drawable.item_04,
        R.drawable.item_05, R.drawable.item_06, R.drawable.item_07, R.drawable.item_08,
        R.drawable.item_09, R.drawable.item_10, R.drawable.item_11, R.drawable.item_12,
        R.drawable.item_13, R.drawable.item_14, R.drawable.item_15, R.drawable.item_16,
        R.drawable.item_17, R.drawable.item_18,
    )

    /** Text alternatives (content descriptions), same order as [items]. */
    val itemNames: List<String> = listOf(
        "Ice bucket", "Fishing rod", "Bobber", "Spoon lure", "Ice auger", "Tackle box",
        "Lantern", "Thermos", "Compass", "Fish finder", "Ice sled", "Ice skimmer",
        "Bait jar", "Landing net", "Mitten", "Camp heater", "Snow boot", "Ice tent",
    )

    // Handy named indices for decorative use across screens.
    const val ITEM_BOBBER = 2
    const val ITEM_LURE = 3
    const val ITEM_AUGER = 4
    const val ITEM_TACKLE_BOX = 5
    const val ITEM_LANTERN = 6
    const val ITEM_COMPASS = 8
    const val ITEM_FISH_FINDER = 9
    const val ITEM_NET = 13
    const val ITEM_TENT = 17

    @DrawableRes
    fun item(index: Int): Int = items[index.mod(items.size)]

    fun itemName(index: Int): String = itemNames[index.mod(itemNames.size)]
}
