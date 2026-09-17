package com.mizan.civilleitner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StrictReviewSchedulerTest {
    private val today = LocalDate.of(2026, 9, 18)

    @Test fun activationStartsAt24Hours() {
        val d = StrictReviewScheduler.activate(today)
        assertTrue(d.enabled)
        assertEquals(today.plusDays(1).toEpochDay(), d.nextReviewEpochDay)
        assertEquals(0, d.stage)
    }

    @Test fun cadenceIs1_2_3_7_14_28AndThen28() {
        var stage = 0
        val expected = listOf(2, 3, 7, 14, 28, 28, 28)
        expected.forEach { days ->
            val d = StrictReviewScheduler.complete(stage, today)
            assertEquals(today.plusDays(days.toLong()).toEpochDay(), d.nextReviewEpochDay)
            stage = d.stage
        }
    }

    @Test fun explicitMasteryRemovesFromReview() {
        val d = StrictReviewScheduler.master()
        assertFalse(d.enabled)
        assertTrue(d.explicitMastered)
        assertEquals(Long.MAX_VALUE, d.nextReviewEpochDay)
    }
}
