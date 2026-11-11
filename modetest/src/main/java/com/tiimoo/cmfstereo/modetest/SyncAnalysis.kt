package com.tiimoo.cmfstereo.modetest

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The SYNC TEST's timing, apart from Android, so it runs in unit tests on recordings (made-up
 * ones with a known answer, and Dr.Essence's own from the phone).
 *
 * Fixed in the second version, from that first recording from the phone (the test said "the
 * earpiece is 60 ms late", exactly the search window's edge):
 * - the loudest sound in the file came before any click (the finger leaving START, at 0.59 s),
 *   and was taken for the first click, so every window was misplaced. Clicks are now looked for
 *   only after playback started ([from]);
 * - a first-order high-pass let the room through: the earpiece click, about 20 dB quieter than
 *   the speaker's at the bottom microphone, barely rose above it. A 3 kHz band-pass (the
 *   clicks' own frequency, two biquads) separates them: in that recording, noise 20, earpiece
 *   clicks about 1,400, speaker clicks about 15,000;
 * - with the anchor on a speaker click instead of an earpiece one, the pairs were read 250 ms
 *   out: the anchor is checked, and moved to its pair's earpiece click when needed;
 * - the room's level is the median of the quiet part: a tap in it raised the mean so far that
 *   no click passed the threshold (found by the unit test, which has such a tap).
 * Measured by hand on that recording: 18 pairs, the earpiece-to-speaker gap 249.48 ms every
 * time (spread 0.00 ms), against 250 played; this code on the same file: 18 pairs at -0.44 ms
 * each, one dropped (SyncAnalysisTest.realRecording).
 */
object SyncAnalysis {

    /** The pairs' offsets (ms), and how many pairs were dropped as misread ([dropped]). */
    class Result(val offsets: List<Double>, val dropped: Int = 0) {
        val mean get() = offsets.average()
        val spread get() = offsets.average().let { m -> sqrt(offsets.sumOf { (it - m) * (it - m) } / offsets.size) }
    }

    /** The 3 kHz band's size, sample by sample, smoothed over a quarter of a millisecond. */
    fun envelope(x: DoubleArray, rate: Int, hz: Double = 3000.0, q: Double = 2.0): DoubleArray {
        // Two RBJ band-pass biquads (0 dB at the centre), one after the other.
        val w0 = 2 * PI * hz / rate
        val alpha = sin(w0) / (2 * q)
        val a0 = 1 + alpha
        val b0 = alpha / a0; val b2 = -alpha / a0
        val a1 = -2 * cos(w0) / a0; val a2 = (1 - alpha) / a0
        var y = x
        repeat(2) {
            val out = DoubleArray(y.size)
            var x1 = 0.0; var x2 = 0.0; var y1 = 0.0; var y2 = 0.0
            for (i in y.indices) {
                val v = b0 * y[i] + b2 * x2 - a1 * y1 - a2 * y2
                x2 = x1; x1 = y[i]; y2 = y1; y1 = v
                out[i] = v
            }
            y = out
        }
        val w = max(1, rate / 4000)
        val env = DoubleArray(y.size)
        var acc = 0.0
        for (i in y.indices) {
            acc += abs(y[i])
            if (i >= w) acc -= abs(y[i - w])
            env[i] = acc / w
        }
        return env
    }

    /**
     * Each pair's offset in milliseconds: the measured time from its left click to its right
     * click, minus the [gapFrames] they were played apart. [quietUntil] frames of room noise
     * set the threshold; clicks are looked for only from [from] (playback start).
     */
    fun offsets(env: DoubleArray, rate: Int, quietUntil: Int, from: Int, pairs: Int,
            pairFrames: Int, gapFrames: Int, searchFrames: Int): Result {
        val quiet = min(quietUntil, env.size)
        if (quiet <= 0) return Result(emptyList())
        // The room's level: the median of the quiet part, not its mean, which a tap in it
        // (as in the first recording from the phone) would pull far up.
        val sample = DoubleArray((quiet + 15) / 16) { env[it * 16] }.also { it.sort() }
        val noise = sample[sample.size / 2]
        val threshold = max(noise * 8, 1.0)
        // Candidate anchors: every sound rising above the threshold in the first two pairs'
        // time after playback started. Each is tried as a left click and as a right one; the
        // reading that lines up the most pairs wins. (In the first recording from the phone a
        // softer sound came first, at 1.51 s, likely the route starting: as the only anchor
        // it misread every pair.)
        val candidates = ArrayList<Int>()
        var i = max(from, 0)
        val until = min(env.size, max(from, 0) + 2 * pairFrames + gapFrames)
        while (i < until) {
            if (env[i] > threshold) {
                onset(env, i, min(env.size, i + searchFrames / 4), threshold)?.let { candidates += it }
                i += searchFrames / 4
            } else i++
        }
        var best = Result(emptyList())
        for (c in candidates) for (shift in intArrayOf(0, -gapFrames)) {
            val res = read(env, rate, c + shift, pairs, pairFrames, gapFrames, searchFrames, threshold)
            if (res.offsets.size > best.offsets.size) best = res
        }
        return best
    }

    /** The pairs read from [anchor], taken as the first pair's left click. */
    private fun read(env: DoubleArray, rate: Int, anchor: Int, pairs: Int, pairFrames: Int,
            gapFrames: Int, searchFrames: Int, threshold: Double): Result {
        val result = ArrayList<Double>()
        for (p in 0 until pairs) {
            val leftDue = anchor + p * pairFrames
            val l = onset(env, leftDue - searchFrames, leftDue + searchFrames, threshold) ?: continue
            val r = onset(env, l + gapFrames - searchFrames, l + gapFrames + searchFrames, threshold) ?: continue
            result += (r - l - gapFrames) * 1000.0 / rate
        }
        // A pair whose click was missed reads far from the others: in the first recording
        // from the phone, the first pair's earpiece click (likely lost while the earpiece's
        // route switched on) read -45.40 ms while the 18 others read -0.44 ms each. Pairs more
        // than 5 ms from the median are dropped and counted.
        if (result.isEmpty()) return Result(result)
        val median = result.sorted()[result.size / 2]
        val kept = result.filter { kotlin.math.abs(it - median) <= 5.0 }
        return Result(kept, result.size - kept.size)
    }

    /** True if two channels are one microphone twice (the phone's processed voice input). */
    fun sameMicrophone(a: DoubleArray, b: DoubleArray): Boolean {
        val n = min(a.size, b.size)
        if (n == 0) return true
        var ab = 0.0; var aa = 0.0; var bb = 0.0
        for (i in 0 until n) { ab += a[i] * b[i]; aa += a[i] * a[i]; bb += b[i] * b[i] }
        return aa == 0.0 || bb == 0.0 || ab / sqrt(aa * bb) > 0.999
    }

    /** Where a click starts in [from, to): its envelope first reaching 30% of its peak there. */
    private fun onset(x: DoubleArray, from: Int, to: Int, threshold: Double): Int? {
        val a = max(0, from); val b = min(x.size, to)
        if (b <= a) return null
        var top = 0.0
        var at = a
        for (i in a until b) if (x[i] > top) { top = x[i]; at = i }
        if (top < threshold) return null
        for (i in a..at) if (x[i] >= top * 0.3) return i
        return at
    }
}
