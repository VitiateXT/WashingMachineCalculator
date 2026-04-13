package de.moritz.waschzeitrechner

import androidx.annotation.StringRes
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private const val DEFAULT_RELATIVE_FINISH_OFFSET_HOURS = 2L
private const val FINISH_TIME_ROUTINE_WINDOW_MINUTES = 90L
private const val QUICK_WINDOW_MINUTES = 50L
private const val TARGET_BUFFER_MINUTES = 15L

enum class WashProgram(
    val storageKey: String,
    @StringRes val labelResId: Int,
    val defaultDurationMinutes: Int
) {
    STANDARD("standard", R.string.program_standard, 30),
    QUICK("quick", R.string.program_quick, 20),
    ECO("eco", R.string.program_eco, 180),
    COTTON("cotton", R.string.program_cotton, 120),
    DELICATES("delicates", R.string.program_delicates, 45);

    val durationPreferenceKey: String
        get() = "duration_${storageKey}"

    companion object {
        fun fromStorageKey(value: String?): WashProgram =
            entries.firstOrNull { it.storageKey == value } ?: STANDARD
    }
}

data class PreferredFinishTime(
    val hour: Int,
    val minute: Int
)

data class WashRecommendation(
    val program: WashProgram,
    val durationMinutes: Int,
    @StringRes val messageResId: Int
)

fun calculateInitialFinishDateTime(
    now: LocalDateTime,
    preferredFinishTime: PreferredFinishTime?
): LocalDateTime {
    if (preferredFinishTime == null) {
        return now.plusHours(DEFAULT_RELATIVE_FINISH_OFFSET_HOURS).withSecond(0).withNano(0)
    }

    var candidate = now
        .withHour(preferredFinishTime.hour)
        .withMinute(preferredFinishTime.minute)
        .withSecond(0)
        .withNano(0)

    if (!candidate.isAfter(now)) {
        candidate = candidate.plusDays(1)
    }

    return candidate
}

fun splitDurationMinutes(totalMinutes: Int): Pair<Int, Int> {
    val safeMinutes = totalMinutes.coerceAtLeast(0)
    return safeMinutes / 60 to safeMinutes % 60
}

fun composeDurationMinutes(hours: Int, minutes: Int): Int =
    hours.coerceAtLeast(0) * 60 + minutes.coerceIn(0, 59)

fun shouldShowSmartDurationRecommendation(
    smartRecommendationsEnabled: Boolean,
    currentProgram: WashProgram,
    currentDurationMinutes: Int,
    recommendation: WashRecommendation?
): Boolean =
    smartRecommendationsEnabled &&
        recommendation != null &&
        (recommendation.program != currentProgram || recommendation.durationMinutes != currentDurationMinutes)

fun buildSmartRecommendation(
    now: LocalDateTime,
    finishDateTime: LocalDateTime,
    currentProgram: WashProgram,
    currentDurationMinutes: Int,
    defaultProgram: WashProgram,
    preferredFinishTime: PreferredFinishTime?,
    programDurations: Map<WashProgram, Int>
): WashRecommendation? {
    val availableMinutes = ChronoUnit.MINUTES.between(now, finishDateTime)
    if (availableMinutes <= 0) {
        return null
    }

    val normalizedDurations = WashProgram.entries.associateWith { program ->
        programDurations[program]?.coerceAtLeast(1) ?: program.defaultDurationMinutes
    }
    val candidates = normalizedDurations
        .filterValues { duration -> duration.toLong() <= availableMinutes }
        .toList()

    if (candidates.isEmpty()) {
        return null
    }

    val defaultDuration = normalizedDurations.getValue(defaultProgram)
    val routineMatches = preferredFinishTime != null &&
        finishDateTime.toLocalTime().let { finishTime ->
            val finishMinutes = finishTime.hour * 60 + finishTime.minute
            val preferredMinutes = preferredFinishTime.hour * 60 + preferredFinishTime.minute
            abs(finishMinutes - preferredMinutes) <= FINISH_TIME_ROUTINE_WINDOW_MINUTES
        }

    val recommendation = when {
        currentDurationMinutes.toLong() > availableMinutes -> {
            candidates.maxByOrNull { (_, duration) -> duration }
                ?.toRecommendation(R.string.smart_recommendation_fits_finish_time)
        }

        availableMinutes <= QUICK_WINDOW_MINUTES -> {
            candidates.minByOrNull { (_, duration) -> duration }
                ?.toRecommendation(R.string.smart_recommendation_time_is_tight)
        }

        routineMatches -> {
            val defaultCandidate = candidates.firstOrNull { (program, _) -> program == defaultProgram }
            (defaultCandidate ?: candidates.closestTo(defaultDuration, defaultProgram))
                ?.toRecommendation(R.string.smart_recommendation_matches_routine)
        }

        else -> {
            val targetDuration = (availableMinutes - TARGET_BUFFER_MINUTES)
                .coerceAtLeast(20)
                .toInt()
            candidates.closestTo(targetDuration, defaultProgram)
                ?.toRecommendation(R.string.smart_recommendation_uses_time_well)
        }
    }

    if (recommendation?.program == currentProgram && recommendation.durationMinutes == currentDurationMinutes) {
        return null
    }

    return recommendation
}

private fun List<Pair<WashProgram, Int>>.closestTo(
    targetDuration: Int,
    defaultProgram: WashProgram
): Pair<WashProgram, Int>? = minWithOrNull(
    compareBy<Pair<WashProgram, Int>> { (_, duration) -> abs(duration - targetDuration) }
        .thenByDescending { (program, _) -> if (program == defaultProgram) 1 else 0 }
        .thenByDescending { (_, duration) -> duration }
)

private fun Pair<WashProgram, Int>.toRecommendation(@StringRes messageResId: Int): WashRecommendation =
    WashRecommendation(
        program = first,
        durationMinutes = second,
        messageResId = messageResId
    )