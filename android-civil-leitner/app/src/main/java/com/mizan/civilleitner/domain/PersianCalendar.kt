package com.mizan.civilleitner.domain

import java.time.LocalDate

object PersianCalendar {
    private val months = arrayOf(
        "", "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    data class Parts(val year: Int, val month: Int, val day: Int) {
        fun numeric(): String = "%04d/%02d/%02d".format(year, month, day)
        fun label(): String = "$day ${months[month]} $year"
    }

    fun fromGregorian(date: LocalDate): Parts {
        val gy = date.year
        val gm = date.monthValue
        val gd = date.dayOfMonth
        val gdm = intArrayOf(0,31,59,90,120,151,181,212,243,273,304,334)
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
        return Parts(jy,jm,jd)
    }

    fun toGregorian(jyInput: Int, jm: Int, jd: Int): LocalDate {
        require(jyInput in 1200..1600 && jm in 1..12 && jd in 1..31)
        var jy = jyInput + 1595
        var days = -355668L + 365L * jy + (jy / 33) * 8 + ((jy % 33 + 3) / 4) + jd
        days += if (jm < 7) (jm - 1) * 31L else (jm - 7) * 30L + 186L

        var gy = 400 * (days / 146097).toInt()
        days %= 146097
        if (days > 36524) {
            gy += 100 * (--days / 36524).toInt()
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
        val leap = (gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0
        val salA = intArrayOf(0,31,if(leap)29 else 28,31,30,31,30,31,31,30,31,30,31)
        var gm = 1
        while (gm <= 12 && gd > salA[gm]) {
            gd -= salA[gm]
            gm++
        }
        return LocalDate.of(gy,gm,gd)
    }

    fun parseNumeric(value: String): LocalDate? {
        val normalized = value.trim()
            .replace('۰','0').replace('۱','1').replace('۲','2').replace('۳','3').replace('۴','4')
            .replace('۵','5').replace('۶','6').replace('۷','7').replace('۸','8').replace('۹','9')
            .replace('-', '/')
        val p = normalized.split('/')
        if (p.size != 3) return null
        val y=p[0].toIntOrNull() ?: return null
        val m=p[1].toIntOrNull() ?: return null
        val d=p[2].toIntOrNull() ?: return null
        return runCatching { toGregorian(y,m,d) }.getOrNull()
    }
}
