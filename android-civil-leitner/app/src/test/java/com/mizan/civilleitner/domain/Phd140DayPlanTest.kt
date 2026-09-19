package com.mizan.civilleitner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class Phd140DayPlanTest {
    @Test fun planIsExactly140StudyDaysBeforeExam() {
        assertEquals(140L, ChronoUnit.DAYS.between(Phd140DayPlan.START, Phd140DayPlan.EXAM))
        assertEquals("1405/06/27", PersianDate.fromGregorian(Phd140DayPlan.START).numeric())
        assertEquals("1405/11/16", PersianDate.fromGregorian(Phd140DayPlan.EXAM).numeric())
        assertEquals("1405/11/15", Phd140DayPlan.planFor(140).persianDate)
    }

    @Test fun civilCoverageEndsAt1335OnDay89() {
        val d = Phd140DayPlan.planFor(89)
        assertEquals(1321, d.civilFrom)
        assertEquals(1335, d.civilTo)
    }

    @Test fun currentTradeCorpusEndsAt801OnDay89() {
        val d = Phd140DayPlan.planFor(89)
        assertEquals(793, d.tradeFrom)
        assertEquals(801, d.tradeTo)
    }

    @Test fun twoThousandEnglishAndThousandArabicEndOnDay89() {
        val d = Phd140DayPlan.planFor(89)
        assertTrue(d.englishFrom in 1978..2000)
        assertEquals(2000, d.englishTo)
        assertTrue(d.arabicFrom in 989..1000)
        assertEquals(1000, d.arabicTo)
    }

    @Test fun finalDayIsReviewOnlyAndHasPositiveTimeBudget() {
        val d = Phd140DayPlan.planFor(140)
        assertEquals(0, d.civilFrom)
        assertEquals(0, d.tradeFrom)
        assertEquals(0, d.englishFrom)
        assertEquals(0, d.arabicFrom)
        assertTrue(d.baseStudyMinutes > 0)
        assertEquals(LocalDate.of(2027, 2, 4), d.gregorianDate)
    }

    @Test fun persianRoundTripWorksForPlanner() {
        val g = PersianDate.parseToGregorian("1405/07/01")
        assertEquals("1405/07/01", PersianDate.fromGregorian(g).numeric())
    }
}
