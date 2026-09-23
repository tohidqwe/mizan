package com.mizan.civilleitner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LeitnerSchedulerTest {
    private val today = LocalDate.of(2026, 9, 17)

    @Test
    fun knewFromBox1MovesToBox2InTwoDays() {
        val result = LeitnerScheduler.schedule(1, ReviewResult.KNEW, today)
        assertEquals(2, result.newBox)
        assertEquals(LocalDate.of(2026, 9, 19).toEpochDay(), result.nextReviewEpochDay)
        assertFalse(result.requeueToday)
    }

    @Test
    fun dontKnowReturnsToBox1AndRequeuesToday() {
        val result = LeitnerScheduler.schedule(3, ReviewResult.DONT_KNOW, today)
        assertEquals(1, result.newBox)
        assertEquals(today.toEpochDay(), result.nextReviewEpochDay)
        assertTrue(result.requeueToday)
    }

    @Test
    fun hardStaysInSameBoxWithHalfInterval() {
        val result = LeitnerScheduler.schedule(5, ReviewResult.HARD, today)
        assertEquals(5, result.newBox)
        assertEquals(today.plusDays(8).toEpochDay(), result.nextReviewEpochDay)
        assertFalse(result.requeueToday)
    }

    @Test
    fun masteredBoxUsesLongTermMaintenanceInterval() {
        val result = LeitnerScheduler.schedule(6, ReviewResult.KNEW, today)
        assertEquals(6, result.newBox)
        assertEquals(today.plusDays(60).toEpochDay(), result.nextReviewEpochDay)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidBoxIsRejected() {
        LeitnerScheduler.schedule(0, ReviewResult.KNEW, today)
    }
}
