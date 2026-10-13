package com.example.vangtichai

import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on a device or emulator: drives the keypad and checks the amount and change table. */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun digitsAreAddedFromTheRight() {
        tap(R.id.key_2)
        assertAmount("Taka: 2")
        tap(R.id.key_3)
        assertAmount("Taka: 23")
        tap(R.id.key_4)
        assertAmount("Taka: 234")
    }

    @Test
    fun changeTableListsTheNotes() {
        type("6880")
        assertCounts("13", "3", "1", "1", "1", "0", "0", "0")
    }

    @Test
    fun clearResetsAmountAndTable() {
        type("688")
        tap(R.id.key_clear)
        assertAmount("Taka:")
        assertCounts("0", "0", "0", "0", "0", "0", "0", "0")
    }

    @Test
    fun amountAndChangeSurviveRecreation() {
        type("688")
        // The same save/restore path a rotation goes through
        activityRule.scenario.recreate()
        assertAmount("Taka: 688")
        assertCounts("1", "1", "1", "1", "1", "1", "1", "1")
    }

    private fun tap(@IdRes key: Int) {
        onView(withId(key)).perform(click())
    }

    private fun type(digits: String) {
        val keys = intArrayOf(
            R.id.key_0, R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4,
            R.id.key_5, R.id.key_6, R.id.key_7, R.id.key_8, R.id.key_9,
        )
        digits.forEach { tap(keys[it.digitToInt()]) }
    }

    private fun assertAmount(text: String) {
        onView(withId(R.id.amount_text)).check(matches(withText(text)))
    }

    private fun assertCounts(vararg counts: String) {
        val rows = intArrayOf(
            R.id.note_500, R.id.note_100, R.id.note_50, R.id.note_20,
            R.id.note_10, R.id.note_5, R.id.note_2, R.id.note_1,
        )
        rows.forEachIndexed { i, row ->
            onView(allOf(withId(R.id.note_count), isDescendantOfA(withId(row))))
                .check(matches(withText(counts[i])))
        }
    }
}
