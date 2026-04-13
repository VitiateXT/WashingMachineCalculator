package de.moritz.waschzeitrechner

import java.time.Duration
import java.time.LocalDateTime

data class WashScheduleResult(
    val isValid: Boolean,
    val startDateTime: LocalDateTime?,
    val messageResId: Int
)

object WashScheduleCalculator {
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