package com.mizan.civilleitner.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class MotivationSet(
    val study: String,
    val health: String,
    val smoking: String,
    val family: String,
    val growth: String,
)

data class DailyPlan(
    val dayNumber: Int,
    val gregorianDate: LocalDate,
    val persianDate: String,
    val persianLabel: String,
    val phase: String,
    val mandatoryMinutes: Int,
    val tasks: List<String>,
    val civilFrom: Int = 0,
    val civilTo: Int = 0,
    val tradeUnitFrom: Int = 0,
    val tradeUnitTo: Int = 0,
    val vocabFrom: Int = 0,
    val vocabTo: Int = 0,
    val testTarget: Int = 0,
    val motivation: MotivationSet,
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
}

object Phd140DayPlan {
    val START: LocalDate = LocalDate.of(2026, 9, 18) // 1405/06/27
    val EXAM: LocalDate = LocalDate.of(2027, 2, 5) // 1405/11/16
    const val TOTAL_STUDY_DAYS = 140

    private val studyMessages = listOf(
        "امروز منتظر انگیزه نمان؛ اول کار را شروع کن، انگیزه بعد از حرکت می‌آید.",
        "قبولی دکتری با جهش‌های مقطعی ساخته نمی‌شود؛ با روزهای معمولیِ کامل ساخته می‌شود.",
        "کار امروز اگر انجام نشود، فردا سنگین‌تر برمی‌گردد؛ امروز تمامش کن.",
        "مرور عقب‌افتاده بدهی است؛ بدهی علمی را قبل از مبحث جدید صفر کن.",
        "هدفت روشن است: تا شب باید چیزی بلد باشی که صبح بلد نبودی.",
        "امروز قرار نیست عالی باشی؛ قرار است برنامه را کامل اجرا کنی.",
        "هر تست غلط یک نقص قابل تعمیر است؛ علتش را پیدا کن و تکرارش نکن.",
        "سختی برنامه دشمن تو نیست؛ تمرین فشار روز آزمون است.",
    )
    private val healthMessages = listOf(
        "سلامتی پروژه جانبی نیست؛ انرژی مطالعه از خواب، آب، غذا و حرکت منظم می‌آید.",
        "برای بدن قوی‌تر، امروز یک انتخاب کوچک بهتر از دیروز انجام بده و تکرارش کن.",
        "کاهش وزن پایدار از تصمیم‌های کوچک روزانه می‌آید، نه فشار افراطی.",
        "بیست دقیقه حرکت امروز، تمرکز فردای تو را ارزان‌تر و قابل‌دسترس‌تر می‌کند.",
    )
    private val smokingMessages = listOf(
        "هر بار که سیگار را عقب می‌اندازی، داری اختیار را از عادت پس می‌گیری.",
        "وسوسه دستور نیست؛ می‌آید و می‌رود. تصمیم نهایی با توست.",
        "ترک سیگار یک زنجیره تصمیم است؛ امروز فقط حلقه امروز را محکم کن.",
        "اگر لغزشی شد، آن را به شکست تبدیل نکن؛ همان لحظه به مسیر برگرد.",
    )
    private val familyMessages = listOf(
        "موفقیت وقتی ارزش دارد که آدم‌های مهم زندگی‌ات سهمی از آرامش تو داشته باشند.",
        "امروز یک گفت‌وگوی کوتاهِ باکیفیت با خانواده را از برنامه حذف نکن.",
        "خانواده مانع مسیر نیست؛ اگر درست مدیریت شود، پشتوانه مسیر است.",
        "حضور واقعی کنار خانواده یعنی چند دقیقه بدون حواس‌پرتی، نه فقط هم‌مکانی.",
    )
    private val growthMessages = listOf(
        "هر روز یک کار عقب‌افتاده را ببند؛ ذهن آزاد، مطالعه عمیق‌تر می‌سازد.",
        "انضباط یعنی انجام کار درست حتی وقتی حالش را نداری.",
        "پیشرفت حرفه‌ای با قول‌های بزرگ نیست؛ با خروجی قابل اندازه‌گیری امروز است.",
        "نسخه قوی‌تر تو از تکرار رفتارهای کوچک ساخته می‌شود، نه تصمیم‌های هیجانی.",
    )

    init {
        check(ChronoUnit.DAYS.between(START, EXAM) == TOTAL_STUDY_DAYS.toLong())
    }

    fun calendarDay(today: LocalDate = LocalDate.now()): Int {
        val raw = ChronoUnit.DAYS.between(START, today).toInt() + 1
        return raw.coerceIn(1, TOTAL_STUDY_DAYS)
    }

    fun isStudyWindow(today: LocalDate = LocalDate.now()): Boolean =
        !today.isBefore(START) && today.isBefore(EXAM)

    fun planFor(dayNumber: Int): DailyPlan {
        require(dayNumber in 1..TOTAL_STUDY_DAYS)
        val date = START.plusDays((dayNumber - 1).toLong())
        val p = PersianDate.fromGregorian(date)
        val motivation = MotivationSet(
            study = studyMessages[(dayNumber - 1) % studyMessages.size],
            health = healthMessages[(dayNumber - 1) % healthMessages.size],
            smoking = smokingMessages[(dayNumber - 1) % smokingMessages.size],
            family = familyMessages[(dayNumber - 1) % familyMessages.size],
            growth = growthMessages[(dayNumber - 1) % growthMessages.size],
        )

        return when {
            dayNumber <= 89 -> {
                val civilFrom = (dayNumber - 1) * 15 + 1
                val civilTo = dayNumber * 15
                // Cover the current consolidated Qavanin.ir Trade Law numbering (1..600).
                // If the official consolidated source omits/marks an article inactive, no study card
                // is generated for that number; the daily range simply skips it.
                val tradeFrom = ((dayNumber - 1) * 600) / 89 + 1
                val tradeTo = (dayNumber * 600) / 89
                val vocabFrom = (dayNumber - 1) * 8 + 1
                val vocabTo = dayNumber * 8
                val tests = when {
                    dayNumber <= 30 -> 25
                    dayNumber <= 60 -> 35
                    else -> 45
                }
                val minutes = if (dayNumber <= 30) 300 else 330
                DailyPlan(
                    dayNumber, date, p.numeric(), p.label(),
                    phase = "پوشش کامل + فهم مفهومی",
                    mandatoryMinutes = minutes,
                    tasks = listOf(
                        "مرورهای سررسیدشده: تا صفر شدن صف",
                        "حقوق مدنی: مواد جاری Qavanin.ir در بازه $civilFrom تا $civilTo + توضیح ساده + نکته آزمونی",
                        "حقوق تجارت: مواد جاری Qavanin.ir در بازه $tradeFrom تا $tradeTo؛ مواد منسوخ/حذف‌شده رسمی خودکار رد می‌شوند",
                        "متون فقه: ۲ قطعه عربی کوتاه از معاملات + ترجمه فعال + استخراج حکم + دام تستی",
                        "زبان: واژگان $vocabFrom تا $vocabTo + یک متن کوتاه",
                        "تست ترکیبی: $tests سؤال و ثبت همه خطاها",
                    ),
                    civilFrom = civilFrom,
                    civilTo = civilTo,
                    tradeUnitFrom = tradeFrom,
                    tradeUnitTo = tradeTo,
                    vocabFrom = vocabFrom,
                    vocabTo = vocabTo,
                    testTarget = tests,
                    motivation = motivation,
                )
            }
            dayNumber <= 112 -> {
                val vocabFrom = (dayNumber - 1) * 8 + 1
                val vocabTo = (dayNumber * 8).coerceAtMost(1000)
                DailyPlan(
                    dayNumber, date, p.numeric(), p.label(),
                    phase = "دور دوم + اتصال مباحث + تست سنگین",
                    mandatoryMinutes = 360,
                    tasks = listOf(
                        "مرورهای سررسیدشده: قبل از هر چیز تا صفر شدن صف",
                        "مدنی: مرور موضوعی دو خوشه از مواد ضعیف + حل تست",
                        "تجارت: مرور موضوعی دو خوشه از مواد ضعیف + حل تست",
                        "متون فقه: ۳ قطعه عربی زمان‌دار + ترجمه بدون کمک + تحلیل حکم",
                        "زبان: واژگان $vocabFrom تا $vocabTo + کلوزتست/درک مطلب",
                        "۷۰ تست ترکیبی زمان‌دار + دفترچه خطا",
                    ),
                    vocabFrom = vocabFrom,
                    vocabTo = vocabTo,
                    testTarget = 70,
                    motivation = motivation,
                )
            }
            dayNumber <= 126 -> {
                val vocabFrom = if (dayNumber <= 125) (dayNumber - 1) * 8 + 1 else 0
                val vocabTo = if (dayNumber <= 125) (dayNumber * 8).coerceAtMost(1000) else 0
                val mock = dayNumber % 2 == 1
                val tests = if (mock) 100 else 80
                DailyPlan(
                    dayNumber, date, p.numeric(), p.label(),
                    phase = "شبیه‌سازی آزمون + ترمیم ضعف",
                    mandatoryMinutes = 360,
                    tasks = listOf(
                        "مرورهای سررسیدشده: صفر کردن صف",
                        if (mock) "آزمون جامع شبیه‌سازی‌شده و زمان‌دار" else "ترمیم سه ضعف اصلی آزمون قبلی",
                        "تحلیل تک‌تک پاسخ‌های غلط و مشکوک",
                        "متون فقه: ترجمه و تشخیص حکم تحت محدودیت زمان",
                        if (vocabFrom > 0) "زبان: واژگان $vocabFrom تا $vocabTo + متن زمان‌دار" else "زبان: مرور ۱۰۰۰ واژه + متن زمان‌دار",
                        "حداقل $tests تست/سؤال زمان‌دار",
                    ),
                    vocabFrom = vocabFrom,
                    vocabTo = vocabTo,
                    testTarget = tests,
                    motivation = motivation,
                )
            }
            else -> {
                val tests = if (dayNumber <= 136) 60 else 30
                DailyPlan(
                    dayNumber, date, p.numeric(), p.label(),
                    phase = "جمع‌بندی نهایی + تثبیت + آمادگی روز آزمون",
                    mandatoryMinutes = if (dayNumber <= 136) 300 else 210,
                    tasks = listOf(
                        "فقط مرورهای سررسیدشده و کارت‌های ضعیف؛ مبحث تازه ممنوع",
                        "مرور دفترچه خطاهای مدنی و تجارت",
                        "متون فقه: قطعات منتخب دشوار + ترجمه سریع",
                        "زبان: فقط واژگان ضعیف + یک متن کوتاه",
                        "تست زمان‌دار کنترل‌شده: $tests سؤال",
                        "خواب و ساعت بیداری را مطابق روز آزمون تثبیت کن",
                    ),
                    testTarget = tests,
                    motivation = motivation,
                )
            }
        }
    }

    fun all(): List<DailyPlan> = (1..TOTAL_STUDY_DAYS).map(::planFor)
}
