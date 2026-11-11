package com.tiimoo.cmfstereo.modetest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** The SYNC TEST's timing on recordings with a known answer, and on a real one if given. */
class SyncAnalysisTest {

    private val rate = 48000
    private val pairs = 20
    private val pairFrames = rate * 600 / 1000
    private val gapFrames = rate * 250 / 1000
    private val search = rate * 60 / 1000

    /**
     * A made-up recording like the phone's: a loud tap before playback (as in Dr.Essence's
     * first one), then pairs of clicks, the earpiece's 20 dB quieter than the speaker's, the
     * speaker's [lateMs] late, with room noise and an echo after each click.
     */
    private fun recording(lateMs: Double, playAt: Int): DoubleArray {
        val r = Random(3)
        val x = DoubleArray(playAt + rate / 2 + pairs * pairFrames + rate) { r.nextDouble(-200.0, 200.0) }
        for (i in 0 until 2000) x[(rate * 0.59).toInt() + i] += 20000 * r.nextDouble(-1.0, 1.0) * (1 - i / 2000.0)
        val click = DoubleArray(96) { k -> (0.5 - 0.5 * cos(2 * PI * k / 96)) * sin(2 * PI * 3000 * k / rate) }
        val latency = rate * 85 / 1000
        for (p in 0 until pairs) {
            val left = playAt + latency + rate / 2 + p * pairFrames
            val right = left + gapFrames + (lateMs * rate / 1000).toInt()
            for ((at, amp) in listOf(left to 1500.0, right to 15000.0)) {
                for (k in click.indices) x[at + k] += amp * click[k]
                for (k in 0 until 600) x[at + 96 + k] += amp * 0.1 * r.nextDouble(-1.0, 1.0) * (1 - k / 600.0)
            }
        }
        return x
    }

    private fun measure(x: DoubleArray, playAt: Int): SyncAnalysis.Result =
        SyncAnalysis.offsets(SyncAnalysis.envelope(x, rate), rate, rate * 7 / 10,
            playAt + rate * 3 / 10, pairs, pairFrames, gapFrames, search)

    @Test fun inStep() {
        val playAt = rate * 8 / 10
        val res = measure(recording(0.0, playAt), playAt)
        assertEquals(pairs, res.offsets.size)
        assertEquals(0.0, res.mean, 0.1)
    }

    @Test fun speakerLate() {
        val playAt = rate * 8 / 10
        val res = measure(recording(3.0, playAt), playAt)
        assertEquals(pairs, res.offsets.size)
        assertEquals(3.0, res.mean, 0.1)
    }

    @Test fun earpieceLate() {
        val playAt = rate * 8 / 10
        val res = measure(recording(-2.0, playAt), playAt)
        assertEquals(-2.0, res.mean, 0.1)
    }

    /** Dr.Essence's first recording, if its path is given (-Psync.wav=... to Gradle): measured by hand 249.48 ms. */
    @Test fun realRecording() {
        val path = System.getProperty("sync.wav") ?: return
        val bytes = File(path).readBytes()
        val data = ByteBuffer.wrap(bytes, 44, bytes.size - 44).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val n = data.remaining() / 2
        val a = DoubleArray(n) { data.get(it * 2).toDouble() }
        val b = DoubleArray(n) { data.get(it * 2 + 1).toDouble() }
        val res = measure(a, rate * 8 / 10)
        println("real recording: ${res.offsets.size} pairs, offset %+.2f ms, spread %.2f ms, same microphone: %s"
            .format(res.mean, res.spread, SyncAnalysis.sameMicrophone(a, b)))
        assertTrue(res.offsets.size >= 15)
        assertEquals(-0.5, res.mean, 0.1)       // by hand: -0.52 ms
        assertTrue(res.spread < 0.1)
    }
}
