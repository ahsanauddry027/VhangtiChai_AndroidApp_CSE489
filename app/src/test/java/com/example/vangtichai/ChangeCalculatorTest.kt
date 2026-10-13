package com.example.vangtichai

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChangeCalculatorTest {

    @Test
    fun breakdownMatchesTheAssignmentScreenshots() {
        //                         500 100  50  20  10   5   2   1
        assertArrayEquals(longArrayOf(0, 0, 0, 0, 0, 1, 0, 1), ChangeCalculator.breakdown(6))
        assertArrayEquals(longArrayOf(0, 0, 1, 0, 1, 1, 1, 1), ChangeCalculator.breakdown(68))
        assertArrayEquals(longArrayOf(1, 1, 1, 1, 1, 1, 1, 1), ChangeCalculator.breakdown(688))
        assertArrayEquals(longArrayOf(13, 3, 1, 1, 1, 0, 0, 0), ChangeCalculator.breakdown(6880))
    }

    @Test
    fun zeroNeedsNoNotes() {
        assertArrayEquals(LongArray(ChangeCalculator.NOTES.size), ChangeCalculator.breakdown(0))
    }

    @Test
    fun largestAcceptedAmount() {
        assertArrayEquals(longArrayOf(19_999, 4, 1, 2, 0, 1, 2, 0), ChangeCalculator.breakdown(9_999_999))
    }

    @Test
    fun notesAlwaysAddUpToTheAmount() {
        for (amount in 0L..5_000L) {
            val counts = ChangeCalculator.breakdown(amount)
            val total = counts.indices.sumOf { counts[it] * ChangeCalculator.NOTES[it] }
            assertEquals("amount $amount", amount, total)
        }
    }

    @Test
    fun usesTheFewestNotesPossible() {
        // Fewest notes for every amount, worked out the slow way (dynamic programming)
        val limit = 2_000
        val fewest = IntArray(limit + 1) { Int.MAX_VALUE }
        fewest[0] = 0
        for (amount in 1..limit) {
            for (note in ChangeCalculator.NOTES) {
                if (note <= amount) fewest[amount] = minOf(fewest[amount], fewest[amount - note] + 1)
            }
        }
        for (amount in 0..limit) {
            assertEquals("amount $amount", fewest[amount].toLong(), ChangeCalculator.breakdown(amount.toLong()).sum())
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeAmountIsRejected() {
        ChangeCalculator.breakdown(-1)
    }

    @Test
    fun digitsAreAddedFromTheRight() {
        val two = ChangeCalculator.appendDigit("", 2)
        val twentyThree = ChangeCalculator.appendDigit(two!!, 3)
        val twoThirtyFour = ChangeCalculator.appendDigit(twentyThree!!, 4)
        assertEquals("2", two)
        assertEquals("23", twentyThree)
        assertEquals("234", twoThirtyFour)
    }

    @Test
    fun leadingZeroIsReplaced() {
        assertEquals("0", ChangeCalculator.appendDigit("", 0))
        assertEquals("0", ChangeCalculator.appendDigit("0", 0))
        assertEquals("5", ChangeCalculator.appendDigit("0", 5))
        assertEquals("50", ChangeCalculator.appendDigit("5", 0))
    }

    @Test
    fun inputStopsAtMaxDigits() {
        val full = "1".repeat(ChangeCalculator.MAX_DIGITS)
        assertNull(ChangeCalculator.appendDigit(full, 1))
        assertEquals(full, ChangeCalculator.appendDigit(full.dropLast(1), 1))
    }

    @Test
    fun valueOfTypedDigits() {
        assertEquals(0L, ChangeCalculator.valueOf(""))
        assertEquals(688L, ChangeCalculator.valueOf("688"))
    }
}
