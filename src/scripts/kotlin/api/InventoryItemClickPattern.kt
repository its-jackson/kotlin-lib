package scripts.kotlin.api

import org.tribot.script.sdk.interfaces.Item

/**
 * Returns an array of 28 Items based on the indexes of the given items. Array indexes without a corresponding Item
 * will have null values.
 */
fun getAsInventory(items: List<Item?>): Array<Item?> {
    val out: Array<Item?> = arrayOfNulls(28)
    for (item in items) {
        val i: Int = item?.index ?: -1
        if (i in 0..27) out[i] = item
    }
    return out
}

/**
 * A pattern, or order, to clicks items in
 */
enum class ClickPattern(val clickList: IntArray) {
    LEFT_TO_RIGHT(
        intArrayOf(
            0,
            1,
            2,
            3,
            4,
            5,
            6,
            7,
            8,
            9,
            10,
            11,
            12,
            13,
            14,
            15,
            16,
            17,
            18,
            19,
            20,
            21,
            22,
            23,
            24,
            25,
            26,
            27
        )
    ),
    TOP_TO_BOTTOM(
        intArrayOf(
            0,
            4,
            8,
            12,
            16,
            20,
            24,
            1,
            5,
            9,
            13,
            17,
            21,
            25,
            2,
            6,
            10,
            14,
            18,
            22,
            26,
            3,
            7,
            11,
            15,
            19,
            23,
            27
        )
    ),
    ZIGZAG(
        intArrayOf(
            0,
            1,
            2,
            3,
            7,
            6,
            5,
            4,
            8,
            9,
            10,
            11,
            15,
            14,
            13,
            12,
            16,
            17,
            18,
            19,
            20,
            21,
            22,
            23,
            27,
            26,
            25,
            24
        )
    ),
    TOP_TO_BOTTOM_ZIGZAG(
        intArrayOf(
            0,
            4,
            8,
            12,
            16,
            20,
            24,
            25,
            21,
            17,
            13,
            9,
            5,
            1,
            2,
            6,
            10,
            14,
            18,
            22,
            26,
            27,
            23,
            19,
            15,
            11,
            7,
            3
        )
    );
}