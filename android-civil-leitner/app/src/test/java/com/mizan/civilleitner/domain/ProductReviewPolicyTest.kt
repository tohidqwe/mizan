package com.mizan.civilleitner.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductReviewPolicyTest {
    @Test
    fun englishWrongIsExactly24Hours() {
        val now = 1_800_000_000_000L
        assertEquals(
            now + 24L * 60L * 60L * 1000L,
            ProductReviewPolicy.englishAfterWrong(now),
        )
    }

    @Test
    fun englishCorrectIsExactlyThreeDays() {
        val now = 1_800_000_000_000L
        assertEquals(
            now + 3L * 24L * 60L * 60L * 1000L,
            ProductReviewPolicy.englishAfterCorrect(now),
        )
    }

    @Test
    fun officialManualIntervalsAreStable() {
        val now = 2_000_000L
        assertEquals(now + 12L * 60L * 60L * 1000L, ProductReviewPolicy.dueForInterval(now, "H12"))
        assertEquals(now + 40L * 24L * 60L * 60L * 1000L, ProductReviewPolicy.dueForInterval(now, "D40"))
    }
}
