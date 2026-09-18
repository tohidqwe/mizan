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

    @Test fun tradeCoverageSpansCurrentQavaninNumberingThrough600OnDay89() {
        val d = Phd140DayPlan.planFor(89)
        assertEquals(594, d.tradeUnitFrom)
        assertEquals(600, d.tradeUnitTo)
    }

    @Test fun thousandVocabularyTargetEndsOnDay125() {
        val d = Phd140DayPlan.planFor(125)
        assertEquals(993, d.vocabFrom)
        assertEquals(1000, d.vocabTo)
    }

    @Test fun allDaysHavePersianDatesAndMandatoryTasks() {
        val all = Phd140DayPlan.all()
        assertEquals(140, all.size)
        assertTrue(all.all { it.persianDate.startsWith("1405/") && it.tasks.isNotEmpty() && it.mandatoryMinutes > 0 })
        assertEquals(LocalDate.of(2027, 2, 4), all.last().gregorianDate)
    }
}
