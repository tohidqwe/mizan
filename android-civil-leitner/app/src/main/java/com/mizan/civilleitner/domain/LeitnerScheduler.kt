package com.mizan.civilleitner.domain

import java.time.LocalDate

enum class ReviewResult { DONT_KNOW, HARD, KNEW }

data class ScheduleDecision(
    val newBox: Int,
    val nextReviewEpochDay: Long,
    val requeueToday: Boolean
)

object LeitnerScheduler {
    private val intervals = mapOf(
        1 to 1L,
        2 to 2L,
        3 to 4L,
        4 to 8L,
        5 to 16L,
        6 to 32L,
    )

    fun schedule(
        currentBox: Int,
        result: ReviewResult,
        today: LocalDate = LocalDate.now(),
    ): ScheduleDecision {
        require(currentBox in 1..6) { "reviewBox must be in 1..6" }

        return when (result) {
            ReviewResult.DONT_KNOW -> ScheduleDecision(
                newBox = 1,
                nextReviewEpochDay = today.toEpochDay(),
                requeueToday = true,
            )

            ReviewResult.HARD -> {
                val interval = maxOf(1L, (intervals.getValue(currentBox) / 2L))
                ScheduleDecision(
                    newBox = currentBox,
                    nextReviewEpochDay = today.plusDays(interval).toEpochDay(),
                    requeueToday = false,
                )
            }

            ReviewResult.KNEW -> {
                val newBox = minOf(6, currentBox + 1)
                val interval = if (currentBox == 6) 60L else intervals.getValue(newBox)
                ScheduleDecision(
                    newBox = newBox,
                    nextReviewEpochDay = today.plusDays(interval).toEpochDay(),
                    requeueToday = false,
                )
            }
        }
    }
}
