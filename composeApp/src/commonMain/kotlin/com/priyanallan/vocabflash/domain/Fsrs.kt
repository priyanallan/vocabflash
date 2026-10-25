package com.priyanallan.vocabflash.domain

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * FSRS-6 scheduler — hand-rolled from the specification (see ADR-0001).
 *
 * Pure and deterministic: no clock, no I/O. The caller sets [ScheduleState.due]
 * from [ScheduleState.scheduledDays]. Default weights are the official FSRS-6 set.
 *
 * References: open-spaced-repetition ts-fsrs `algorithm.ts` / `constant.ts`.
 */
object Fsrs {

    /** Official FSRS-6 default parameters (21 weights). */
    val DEFAULT_W: DoubleArray = doubleArrayOf(
        0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666,
        0.796, 1.4835, 0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658,
        0.1542,
    )

    const val REQUEST_RETENTION = 0.9
    const val MIN_INTERVAL = 1
    const val MAX_INTERVAL = 36500
    private const val S_MIN = 0.001
    private const val S_MAX = 36500.0

    private fun decay(w: DoubleArray): Double = -w[20]

    private fun factor(w: DoubleArray): Double = exp(ln(0.9) / decay(w)) - 1.0

    private fun clamp(v: Double, lo: Double, hi: Double): Double = v.coerceIn(lo, hi)

    /** R(t, S) = (1 + FACTOR·t/S)^DECAY — probability of recall after `elapsedDays`. */
    fun forgettingCurve(elapsedDays: Double, stability: Double, w: DoubleArray = DEFAULT_W): Double {
        if (stability <= 0.0) return 0.0
        return (1.0 + factor(w) * elapsedDays / stability).pow(decay(w))
    }

    /** S_0(G) = max(w[G-1], 0.1). */
    fun initStability(g: Rating, w: DoubleArray = DEFAULT_W): Double = max(w[g.value - 1], 0.1)

    /** D_0(G) = clamp(w4 - e^(w5·(G-1)) + 1, 1, 10). */
    fun initDifficulty(g: Rating, w: DoubleArray = DEFAULT_W): Double =
        clamp(w[4] - exp((g.value - 1) * w[5]) + 1.0, 1.0, 10.0)

    private fun linearDamping(delta: Double, oldD: Double): Double = delta * (10.0 - oldD) / 9.0

    private fun meanReversion(init: Double, current: Double, w: DoubleArray): Double =
        w[7] * init + (1.0 - w[7]) * current

    fun nextDifficulty(d: Double, g: Rating, w: DoubleArray = DEFAULT_W): Double {
        val delta = -w[6] * (g.value - 3)
        val next = d + linearDamping(delta, d)
        return clamp(meanReversion(initDifficulty(Rating.Easy, w), next, w), 1.0, 10.0)
    }

    /** Stability after a successful recall. */
    fun nextRecallStability(d: Double, s: Double, r: Double, g: Rating, w: DoubleArray = DEFAULT_W): Double {
        val hardPenalty = if (g == Rating.Hard) w[15] else 1.0
        val easyBonus = if (g == Rating.Easy) w[16] else 1.0
        val value = s * (
            1.0 + exp(w[8]) * (11.0 - d) * s.pow(-w[9]) *
                (exp((1.0 - r) * w[10]) - 1.0) * hardPenalty * easyBonus
            )
        return clamp(value, S_MIN, S_MAX)
    }

    /** Stability after forgetting (Again). */
    fun nextForgetStability(d: Double, s: Double, r: Double, w: DoubleArray = DEFAULT_W): Double {
        val value = w[11] * d.pow(-w[12]) * ((s + 1.0).pow(w[13]) - 1.0) * exp((1.0 - r) * w[14])
        return clamp(value, S_MIN, S_MAX)
    }

    /** Stability update for same-day (short-term) reviews. */
    fun nextShortTermStability(s: Double, g: Rating, w: DoubleArray = DEFAULT_W): Double {
        val sinc = s.pow(-w[19]) * exp(w[17] * (g.value - 3 + w[18]))
        val masked = if (g.value >= Rating.Hard.value) max(sinc, 1.0) else sinc
        return clamp(s * masked, S_MIN, S_MAX)
    }

    /** I(r, S) = (r^(1/DECAY) - 1) / FACTOR · S, at the requested retention. */
    fun intervalModifier(w: DoubleArray = DEFAULT_W, requestRetention: Double = REQUEST_RETENTION): Double =
        (requestRetention.pow(1.0 / decay(w)) - 1.0) / factor(w)

    fun nextInterval(s: Double, w: DoubleArray = DEFAULT_W, requestRetention: Double = REQUEST_RETENTION): Int =
        (s * intervalModifier(w, requestRetention)).roundToInt().coerceIn(MIN_INTERVAL, MAX_INTERVAL)

    /**
     * Compute the next ScheduleState after reviewing with [rating] and [elapsedDays]
     * since the last review. `due` is left at 0 — the caller sets it.
     */
    fun nextState(
        current: ScheduleState?,
        elapsedDays: Int,
        rating: Rating,
        w: DoubleArray = DEFAULT_W,
    ): ScheduleState {
        // New card.
        if (current == null || (current.difficulty == 0.0 && current.stability == 0.0)) {
            val s0 = initStability(rating, w)
            val d0 = initDifficulty(rating, w)
            return ScheduleState(
                state = if (rating == Rating.Again) CardState.Learning else CardState.Review,
                due = 0L,
                lastReview = null,
                stability = s0,
                difficulty = d0,
                reps = 1,
                lapses = if (rating == Rating.Again) 1 else 0,
                scheduledDays = nextInterval(s0, w),
                elapsedDays = elapsedDays,
            )
        }

        val d = current.difficulty
        val s = current.stability
        val r = forgettingCurve(elapsedDays.toDouble(), s, w)

        val newS: Double
        val newState: CardState
        var lapses = current.lapses
        when {
            elapsedDays == 0 -> {
                newS = nextShortTermStability(s, rating, w)
                newState = if (rating == Rating.Again) CardState.Relearning else current.state
            }
            rating == Rating.Again -> {
                val sAfterFail = nextForgetStability(d, s, r, w)
                val sMin = s / exp(w[17] * w[18])
                newS = clamp(sMin, S_MIN, sAfterFail)
                lapses += 1
                newState = CardState.Relearning
            }
            else -> {
                newS = nextRecallStability(d, s, r, rating, w)
                newState = CardState.Review
            }
        }

        return current.copy(
            state = newState,
            stability = newS,
            difficulty = nextDifficulty(d, rating, w),
            reps = current.reps + 1,
            lapses = lapses,
            scheduledDays = nextInterval(newS, w),
            elapsedDays = elapsedDays,
        )
    }

    /** The interval each rating would produce — used to label the rating buttons. */
    fun previewIntervals(
        current: ScheduleState?,
        elapsedDays: Int,
        w: DoubleArray = DEFAULT_W,
    ): Map<Rating, Int> =
        Rating.entries.associateWith { nextState(current, elapsedDays, it, w).scheduledDays }
}
