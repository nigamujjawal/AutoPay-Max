package com.uj.appstorysautopaymanager.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoPayRepositoryRecurringTest {

    private val day = 24L * 60L * 60L * 1000L

    @Test
    fun `three occurrences 30 days apart are monthly spaced`() {
        val newest = 3 * 30 * day
        val dates = listOf(newest, newest - 30 * day, newest - 60 * day)
        assertTrue(isMonthlySpaced(dates))
    }

    @Test
    fun `gaps drifting within 20-40 days still count as monthly`() {
        val newest = 100 * day
        val dates = listOf(newest, newest - 25 * day, newest - 65 * day) // 25-day then 40-day gap
        assertTrue(isMonthlySpaced(dates))
    }

    @Test
    fun `two payments a week apart are not monthly spaced`() {
        val newest = 20 * day
        val dates = listOf(newest, newest - 7 * day, newest - 14 * day)
        assertFalse(isMonthlySpaced(dates))
    }

    @Test
    fun `a single wide gap breaks the whole pattern`() {
        val newest = 200 * day
        val dates = listOf(newest, newest - 30 * day, newest - 130 * day) // second gap is 100 days
        assertFalse(isMonthlySpaced(dates))
    }
}
