package de.moritz.waschzeitrechner

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WashScheduleCalculatorTest {
    @Test
    fun returnsStartTimeWhenEnoughTimeRemains() {
        val now = LocalDateTime.of(2026, 3, 24, 10, 0)
        val finish = LocalDateTime.of(2026, 3, 24, 12, 0)

        val result = WashScheduleCalculator.calculate(now, finish, 30)

        assertTrue(result.isValid)
        assertEquals(LocalDateTime.of(2026, 3, 24, 11, 30), result.startDateTime)
    }

    @Test
    fun rejectsFinishTimesInThePast() {
        val now = LocalDateTime.of(2026, 3, 24, 10, 0)
        val finish = LocalDateTime.of(2026, 3, 24, 9, 45)

        val result = WashScheduleCalculator.calculate(now, finish, 30)

        assertFalse(result.isValid)
        assertEquals(null, result.startDateTime)
        assertEquals(R.string.finish_time_in_past_message, result.messageResId)
    }

    @Test
    fun rejectsZeroMinuteDuration() {
        val now = LocalDateTime.of(2026, 3, 24, 10, 0)
        val finish = LocalDateTime.of(2026, 3, 24, 11, 0)

        val result = WashScheduleCalculator.calculate(now, finish, 0)

        assertFalse(result.isValid)
        assertEquals(R.string.invalid_duration_message, result.messageResId)
    }
}