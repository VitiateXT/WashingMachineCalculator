package de.moritz.waschzeitrechner

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WashPreferencesTest {

    @Test
    fun fromStorageKeyFallsBackToStandardForUnknownValue() {
        assertEquals(WashProgram.STANDARD, WashProgram.fromStorageKey("unknown"))
    }

    @Test
    fun calculateInitialFinishDateTimeUsesRelativeFallbackWhenNoPreferenceExists() {
        val now = LocalDateTime.of(2026, 4, 9, 9, 15)

        val finishDateTime = calculateInitialFinishDateTime(now, preferredFinishTime = null)

        assertEquals(LocalDateTime.of(2026, 4, 9, 11, 15), finishDateTime)
    }

    @Test
    fun calculateInitialFinishDateTimeUsesSameDayWhenPreferredTimeIsStillAhead() {
        val now = LocalDateTime.of(2026, 4, 9, 9, 15)

        val finishDateTime = calculateInitialFinishDateTime(
            now = now,
            preferredFinishTime = PreferredFinishTime(hour = 18, minute = 30)
        )

        assertEquals(LocalDateTime.of(2026, 4, 9, 18, 30), finishDateTime)
    }

    @Test
    fun calculateInitialFinishDateTimeRollsToNextDayWhenPreferredTimeHasPassed() {
        val now = LocalDateTime.of(2026, 4, 9, 21, 15)

        val finishDateTime = calculateInitialFinishDateTime(
            now = now,
            preferredFinishTime = PreferredFinishTime(hour = 7, minute = 0)
        )

        assertEquals(LocalDateTime.of(2026, 4, 10, 7, 0), finishDateTime)
    }

    @Test
    fun splitAndComposeDurationMinutesRoundTrip() {
        val durationMinutes = composeDurationMinutes(hours = 2, minutes = 35)

        assertEquals(155, durationMinutes)
        assertEquals(Pair(2, 35), splitDurationMinutes(durationMinutes))
    }

    @Test
    fun smartRecommendationIsShownOnlyWhenEnabledAndChanged() {
        val recommendation = WashRecommendation(
            program = WashProgram.QUICK,
            durationMinutes = 20,
            messageResId = R.string.smart_recommendation_time_is_tight
        )

        assertTrue(
            shouldShowSmartDurationRecommendation(
                smartRecommendationsEnabled = true,
                currentProgram = WashProgram.STANDARD,
                currentDurationMinutes = 75,
                recommendation = recommendation
            )
        )

        assertFalse(
            shouldShowSmartDurationRecommendation(
                smartRecommendationsEnabled = false,
                currentProgram = WashProgram.STANDARD,
                currentDurationMinutes = 75,
                recommendation = recommendation
            )
        )

        assertFalse(
            shouldShowSmartDurationRecommendation(
                smartRecommendationsEnabled = true,
                currentProgram = WashProgram.QUICK,
                currentDurationMinutes = 20,
                recommendation = recommendation
            )
        )
    }

    @Test
    fun buildSmartRecommendationReturnsNullWhenNoTimeRemains() {
        val recommendation = buildSmartRecommendation(
            now = LocalDateTime.of(2026, 4, 9, 10, 0),
            finishDateTime = LocalDateTime.of(2026, 4, 9, 10, 0),
            currentProgram = WashProgram.STANDARD,
            currentDurationMinutes = 30,
            defaultProgram = WashProgram.STANDARD,
            preferredFinishTime = PreferredFinishTime(18, 0),
            programDurations = WashProgram.entries.associateWith { it.defaultDurationMinutes }
        )

        assertNull(recommendation)
    }

    @Test
    fun buildSmartRecommendationPrefersQuickestProgramWhenTimeIsTight() {
        val recommendation = buildSmartRecommendation(
            now = LocalDateTime.of(2026, 4, 9, 10, 0),
            finishDateTime = LocalDateTime.of(2026, 4, 9, 10, 35),
            currentProgram = WashProgram.STANDARD,
            currentDurationMinutes = 30,
            defaultProgram = WashProgram.STANDARD,
            preferredFinishTime = PreferredFinishTime(18, 0),
            programDurations = WashProgram.entries.associateWith { it.defaultDurationMinutes }
        )

        assertNotNull(recommendation)
        assertEquals(WashProgram.QUICK, recommendation.program)
        assertEquals(R.string.smart_recommendation_time_is_tight, recommendation.messageResId)
    }

    @Test
    fun buildSmartRecommendationFallsBackToProgramThatFitsWhenCurrentDurationIsTooLong() {
        val recommendation = buildSmartRecommendation(
            now = LocalDateTime.of(2026, 4, 9, 10, 0),
            finishDateTime = LocalDateTime.of(2026, 4, 9, 10, 50),
            currentProgram = WashProgram.COTTON,
            currentDurationMinutes = 120,
            defaultProgram = WashProgram.COTTON,
            preferredFinishTime = PreferredFinishTime(18, 0),
            programDurations = WashProgram.entries.associateWith { it.defaultDurationMinutes }
        )

        assertNotNull(recommendation)
        assertEquals(WashProgram.DELICATES, recommendation.program)
        assertEquals(R.string.smart_recommendation_fits_finish_time, recommendation.messageResId)
    }

    @Test
    fun buildSmartRecommendationPrefersDefaultProgramWhenFinishTimeMatchesRoutine() {
        val durations = WashProgram.entries.associateWith { it.defaultDurationMinutes }

        val recommendation = buildSmartRecommendation(
            now = LocalDateTime.of(2026, 4, 9, 14, 0),
            finishDateTime = LocalDateTime.of(2026, 4, 9, 18, 15),
            currentProgram = WashProgram.QUICK,
            currentDurationMinutes = 20,
            defaultProgram = WashProgram.COTTON,
            preferredFinishTime = PreferredFinishTime(18, 0),
            programDurations = durations
        )

        assertNotNull(recommendation)
        assertEquals(WashProgram.COTTON, recommendation.program)
        assertEquals(R.string.smart_recommendation_matches_routine, recommendation.messageResId)
    }

    @Test
    fun buildSmartRecommendationTargetsAvailableWindowWhenThereIsPlentyOfTime() {
        val recommendation = buildSmartRecommendation(
            now = LocalDateTime.of(2026, 4, 9, 8, 0),
            finishDateTime = LocalDateTime.of(2026, 4, 9, 12, 0),
            currentProgram = WashProgram.QUICK,
            currentDurationMinutes = 20,
            defaultProgram = WashProgram.STANDARD,
            preferredFinishTime = PreferredFinishTime(18, 0),
            programDurations = WashProgram.entries.associateWith { it.defaultDurationMinutes }
        )

        assertNotNull(recommendation)
        assertEquals(WashProgram.ECO, recommendation.program)
        assertEquals(R.string.smart_recommendation_uses_time_well, recommendation.messageResId)
    }
}