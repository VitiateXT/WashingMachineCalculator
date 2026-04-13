package de.moritz.waschzeitrechner

import java.time.Duration
import java.time.LocalDateTime

/**
 * Outcome of a [WashScheduleCalculator.calculate] call.
 *
 * @property isValid `true` when a start time could be computed, `false` otherwise.
 * @property startDateTime The time the user should start the washing cycle so it
 * finishes at the requested finish time, or `null` when [isValid] is `false`.
 * @property messageResId A string resource id describing the result — either the
 * "start at" label on success, or an error explanation (duration invalid, finish
 * time in the past, not enough time remaining).
 */
data class WashScheduleResult(
    val isValid: Boolean,
    val startDateTime: LocalDateTime?,
    val messageResId: Int
)

/**
 * Pure scheduling logic for the washing calculator.
 *
 * Kept framework-free so it can be unit-tested without Android dependencies.
 */
object WashScheduleCalculator {
    /**
     * Computes the delay-start time that makes the wash cycle end at [finishDateTime].
     *
     * @param now The reference "current" time (injected for testability).
     * @param finishDateTime The time the user wants the cycle to finish.
     * @param washDurationMinutes The length of the selected wash program in minutes.
     * @return A [WashScheduleResult] describing the outcome.
     */
    fun calculate(
        now: LocalDateTime,
        finishDateTime: LocalDateTime,
        washDurationMinutes: Int
    ): WashScheduleResult {
        if (washDurationMinutes <= 0) {
            return WashScheduleResult(
                isValid = false,
                startDateTime = null,
                messageResId = R.string.invalid_duration_message
            )
        }

        val minutesUntilFinish = Duration.between(now, finishDateTime).toMinutes()
        val fillerMinutes = minutesUntilFinish - washDurationMinutes

        if (fillerMinutes < 0) {
            val messageResId = if (finishDateTime.isBefore(now)) {
                R.string.finish_time_in_past_message
            } else {
                R.string.insufficient_time_message
            }

            return WashScheduleResult(
                isValid = false,
                startDateTime = null,
                messageResId = messageResId
            )
        }

        return WashScheduleResult(
            isValid = true,
            startDateTime = now.plusMinutes(fillerMinutes),
            messageResId = R.string.start_time_label
        )
    }
}