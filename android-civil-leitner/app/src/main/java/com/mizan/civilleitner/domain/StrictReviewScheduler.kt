package com.mizan.civilleitner.domain

import java.time.LocalDate

data class StrictReviewDecision(
    val enabled: Boolean,
    val stage: Int,
    val nextReviewEpochDay: Long,
    val explicitMastered: Boolean,
)

object StrictReviewScheduler {
    // User-requested cadence: 24h, 48h, 3d, 1w, 14d, 28d; then every 28d until explicit mastery.
    val intervalsDays = intArrayOf(1, 2, 3, 7, 14, 28)

    fun activate(today: LocalDate = LocalDate.now()): StrictReviewDecision =
        StrictReviewDecision(
            enabled = true,
            stage = 0,
            nextReviewEpochDay = today.plusDays(intervalsDays[0].toLong()).toEpochDay(),
            explicitMastered = false,
        )

    fun complete(currentStage: Int, today: LocalDate = LocalDate.now()): StrictReviewDecision {
        require(currentStage >= 0)
        val newStage = (currentStage + 1).coerceAtMost(intervalsDays.lastIndex)
        val interval = intervalsDays[newStage]
        return StrictReviewDecision(
            enabled = true,
            stage = newStage,
            nextReviewEpochDay = today.plusDays(interval.toLong()).toEpochDay(),
            explicitMastered = false,
        )
    }

    fun master(): StrictReviewDecision = StrictReviewDecision(
        enabled = false,
        stage = intervalsDays.lastIndex,
        nextReviewEpochDay = Long.MAX_VALUE,
        explicitMastered = true,
    )

    fun reactivate(today: LocalDate = LocalDate.now()): StrictReviewDecision = activate(today)
}
