package com.mizan.civilleitner.domain

object ProductReviewPolicy {
    const val HOUR_MS = 60L * 60L * 1000L
    const val DAY_MS = 24L * HOUR_MS

    val intervalsMillis: LinkedHashMap<String, Long> = linkedMapOf(
        "H12" to 12L * HOUR_MS,
        "H24" to 24L * HOUR_MS,
        "H48" to 48L * HOUR_MS,
        "D3" to 3L * DAY_MS,
        "D7" to 7L * DAY_MS,
        "D14" to 14L * DAY_MS,
        "D20" to 20L * DAY_MS,
        "D40" to 40L * DAY_MS,
    )

    fun dueForInterval(nowMillis: Long, intervalCode: String): Long =
        nowMillis + requireNotNull(intervalsMillis[intervalCode]) {
            "Unsupported interval: " + intervalCode
        }

    fun englishAfterWrong(nowMillis: Long): Long = nowMillis + DAY_MS
    fun englishAfterCorrect(nowMillis: Long): Long = nowMillis + 3L * DAY_MS

    fun arabicAfterWrong(nowMillis: Long): Long = englishAfterWrong(nowMillis)
    fun arabicAfterCorrect(nowMillis: Long): Long = englishAfterCorrect(nowMillis)
}
