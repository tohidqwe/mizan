package com.mizan.civilleitner.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

data class DailyPlan(
    val dayNumber: Int,
    val gregorianDate: LocalDate,
    val persianDate: String,
    val persianLabel: String,
    val phase: String,
    val civilFrom: Int,
    val civilTo: Int,
    val tradeFrom: Int,
    val tradeTo: Int,
    val englishFrom: Int,
    val englishTo: Int,
    val arabicFrom: Int,
    val arabicTo: Int,
    val fiqhFrom: Int,
    val fiqhTo: Int,
    val baseStudyMinutes: Int,
)

object PersianDate {
    private val monthNames = arrayOf(
        "", "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    data class Parts(val year: Int, val month: Int, val day: Int) {
        fun numeric(): String = "%04d/%02d/%02d".format(year, month, day)
        fun label(): String = "$day ${monthNames[month]} $year"
    }

    fun fromGregorian(date: LocalDate): Parts {
        val gy = date.year
        val gm = date.monthValue
        val gd = date.dayOfMonth
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = gy + if (gm > 2) 1 else 0
        var days = 355666L + 365L * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 +
            (gy2 + 399) / 400 + gd + gdm[gm - 1]
        var jy = -1595 + 33 * (days / 12053).toInt()
        days %= 12053
        jy += 4 * (days / 1461).toInt()
        days %= 1461
        if (days > 365) {
            jy += ((days - 1) / 365).toInt()
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31).toInt()
            jd = 1 + (days % 31).toInt()
        } else {
            jm = 7 + ((days - 186) / 30).toInt()
            jd = 1 + ((days - 186) % 30).toInt()
        }
        return Parts(jy, jm, jd)
    }

    fun toGregorian(jyInput: Int, jm: Int, jd: Int): LocalDate {
        require(jm in 1..12 && jd in 1..31)
        var jy = jyInput + 1595
        var days = -355668L + 365L * jy + (jy / 33) * 8L + ((jy % 33 + 3) / 4) +
            jd + if (jm < 7) (jm - 1) * 31L else (jm - 7) * 30L + 186L

        var gy = 400 * (days / 146097).toInt()
        days %= 146097
        if (days > 36524) {
            gy += 100 * ((--days) / 36524).toInt()
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461).toInt()
        days %= 1461
        if (days > 365) {
            gy += ((days - 1) / 365).toInt()
            days = (days - 1) % 365
        }
        var gd = days.toInt() + 1
        val leap = gy % 4 == 0 && (gy % 100 != 0 || gy % 400 == 0)
        val monthDays = intArrayOf(0, 31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 1
        while (gm <= 12 && gd > monthDays[gm]) {
            gd -= monthDays[gm]
            gm++
        }
        return LocalDate.of(gy, gm, gd)
    }

    fun parseToGregorian(value: String): LocalDate {
        val latin = value
            .replace('۰','0').replace('۱','1').replace('۲','2').replace('۳','3').replace('۴','4')
            .replace('۵','5').replace('۶','6').replace('۷','7').replace('۸','8').replace('۹','9')
        val p = latin.split('/', '-', '.').map { it.trim() }
        require(p.size == 3)
        return toGregorian(p[0].toInt(), p[1].toInt(), p[2].toInt())
    }
}

object Phd140DayPlan {
    val START: LocalDate = LocalDate.of(2026, 9, 18)
    val EXAM: LocalDate = LocalDate.of(2027, 2, 5)
    const val TOTAL_STUDY_DAYS = 140

    fun daysUntilExam(today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, EXAM).coerceAtLeast(0)

    fun calendarDay(today: LocalDate = LocalDate.now()): Int {
        val raw = ChronoUnit.DAYS.between(START, today).toInt() + 1
        return raw.coerceIn(1, TOTAL_STUDY_DAYS)
    }

    private fun spread(day: Int, total: Int, days: Int): IntRange {
        if (day > days) return IntRange.EMPTY
        val from = ((day - 1) * total) / days + 1
        val to = (day * total) / days
        return from..to
    }

    fun planFor(dayNumber: Int): DailyPlan {
        require(dayNumber in 1..TOTAL_STUDY_DAYS)
        val date = START.plusDays((dayNumber - 1).toLong())
        val p = PersianDate.fromGregorian(date)
        val phase = when {
            dayNumber <= 89 -> "پوشش منظم منابع + مرور سررسیدشده"
            dayNumber <= 115 -> "دور دوم و تثبیت"
            dayNumber <= 130 -> "مرور فشرده و بازیابی"
            else -> "جمع‌بندی نهایی"
        }

        val civil = spread(dayNumber, 1335, 89)
        val trade = spread(dayNumber, 900, 89)
        val english = spread(dayNumber, 2000, 89)
        val arabic = spread(dayNumber, 1000, 89)
        val fiqh = spread(dayNumber, 120, 60)

        val base = when {
            dayNumber <= 89 -> 210
            dayNumber <= 115 -> 240
            dayNumber <= 130 -> 210
            else -> 150
        }

        return DailyPlan(
            dayNumber = dayNumber,
            gregorianDate = date,
            persianDate = p.numeric(),
            persianLabel = p.label(),
            phase = phase,
            civilFrom = civil.first.takeIf { !civil.isEmpty() } ?: 0,
            civilTo = civil.last.takeIf { !civil.isEmpty() } ?: 0,
            tradeFrom = trade.first.takeIf { !trade.isEmpty() } ?: 0,
            tradeTo = trade.last.takeIf { !trade.isEmpty() } ?: 0,
            englishFrom = english.first.takeIf { !english.isEmpty() } ?: 0,
            englishTo = english.last.takeIf { !english.isEmpty() } ?: 0,
            arabicFrom = arabic.first.takeIf { !arabic.isEmpty() } ?: 0,
            arabicTo = arabic.last.takeIf { !arabic.isEmpty() } ?: 0,
            fiqhFrom = fiqh.first.takeIf { !fiqh.isEmpty() } ?: 0,
            fiqhTo = fiqh.last.takeIf { !fiqh.isEmpty() } ?: 0,
            baseStudyMinutes = base,
        )
    }

    fun estimateReviewMinutes(
        lawCount: Int,
        englishCount: Int,
        arabicCount: Int,
        fiqhCount: Int,
        plannerCount: Int = 0,
    ): Int = ceil(
        lawCount * 2.0 + englishCount * 0.55 + arabicCount * 0.6 + fiqhCount * 3.0 + plannerCount * 0.3
    ).toInt()
}
