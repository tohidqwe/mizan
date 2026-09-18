package com.mizan.civilleitner.domain

import java.time.LocalDate

data class StrictReviewDecision(
    val enabled: Boolean,
    val stage: Int,
    val nextReviewEpochDay: Long,
    val explicitMastered: Boolean,
    val requeueToday: Boolean = false,
)

object StrictReviewScheduler {
    // Brutal review cadence requested for exam mode: 1, 2, 3, 7, 14, 28 days,
    // then every 28 days until the learner explicitly marks mastery.
    val intervalsDays = intArrayOf(1, 2, 3, 7, 14, 28)

    fun activate(today: LocalDate = LocalDate.now()): StrictReviewDecision =
        StrictReviewDecision(
            enabled = true,
            stage = 0,
            nextReviewEpochDay = today.plusDays(intervalsDays[0].toLong()).toEpochDay(),
            explicitMastered = false,
        )

    fun grade(
        currentStage: Int,
        result: ReviewResult,
        today: LocalDate = LocalDate.now(),
    ): StrictReviewDecision {
        require(currentStage in intervalsDays.indices)

        return when (result) {
            ReviewResult.DONT_KNOW -> StrictReviewDecision(
                enabled = true,
                stage = 0,
                nextReviewEpochDay = today.toEpochDay(),
                explicitMastered = false,
                requeueToday = true,
            )

            ReviewResult.HARD -> StrictReviewDecision(
                enabled = true,
                stage = currentStage,
                nextReviewEpochDay = today.plusDays(1).toEpochDay(),
                explicitMastered = false,
            )

            ReviewResult.KNEW -> {
                val newStage = (currentStage + 1).coerceAtMost(intervalsDays.lastIndex)
                val interval = if (currentStage == intervalsDays.lastIndex) {
                    intervalsDays.last()
                } else {
                    intervalsDays[newStage]
                }
                StrictReviewDecision(
                    enabled = true,
                    stage = newStage,
                    nextReviewEpochDay = today.plusDays(interval.toLong()).toEpochDay(),
                    explicitMastered = false,
                )
            }
        }
    }

    // Kept for compatibility with older tests/callers; a completed review means "KNEW".
    fun complete(currentStage: Int, today: LocalDate = LocalDate.now()): StrictReviewDecision =
        grade(currentStage, ReviewResult.KNEW, today)

    fun master(): StrictReviewDecision = StrictReviewDecision(
        enabled = false,
        stage = intervalsDays.lastIndex,
        nextReviewEpochDay = Long.MAX_VALUE,
        explicitMastered = true,
    )

    fun reactivate(today: LocalDate = LocalDate.now()): StrictReviewDecision = activate(today)
}
