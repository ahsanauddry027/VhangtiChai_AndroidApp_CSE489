package com.example.vangtichai

/**
 * Pure (Android-free) logic behind VangtiChai, kept separate from the UI so it can be unit tested.
 */
object ChangeCalculator {

    /** Taka notes the change is given in, largest first. */
    val NOTES = intArrayOf(500, 100, 50, 20, 10, 5, 2, 1)

    /**
     * Longest amount (in digits) the keypad accepts: up to 9,999,999 Taka. That keeps the largest
     * count (19,999 five-hundreds) narrow enough to show in full even on small phones.
     */
    const val MAX_DIGITS = 7

    /**
     * Returns how many of each note in [NOTES] make up [amount], using as few notes as possible.
     * The Taka note system is canonical, so taking the largest note that fits each time is optimal.
     */
    fun breakdown(amount: Long): LongArray {
        require(amount >= 0) { "amount must not be negative: $amount" }
        var remaining = amount
        return LongArray(NOTES.size) { i ->
            val count = remaining / NOTES[i]
            remaining %= NOTES[i]
            count
        }
    }

    /**
     * Adds [digit] to the right of the typed [amount] ("23" + 4 -> "234").
     * A lone leading zero is replaced ("0" + 5 -> "5"). Returns null when [MAX_DIGITS] is reached.
     */
    fun appendDigit(amount: String, digit: Int): String? {
        require(digit in 0..9) { "not a digit: $digit" }
        return when {
            amount == "0" -> digit.toString()
            amount.length >= MAX_DIGITS -> null
            else -> amount + digit
        }
    }

    /** Numeric value of the typed digits; nothing typed counts as zero. */
    fun valueOf(amount: String): Long = amount.toLongOrNull() ?: 0L
}
