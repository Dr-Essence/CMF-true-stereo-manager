package com.tiimoo.cmfstereo.modetest

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Process
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * SYNC TEST: do the earpiece (left) and the bottom speaker (right) play at the same moment?
 *
 * Why: another CMF Phone 1 stereo project (lufrz's LineageOS tree, "experimental dual-speaker
 * playback") feeds the earpiece a stream of its own and has to delay it by a measured 72 ms to
 * line it up with the speaker, with a manual trim on top. CMF True Stereo feeds both drivers
 * from one stream, so they should be in step by design; this measures it instead of assuming.
 *
 * How: the screen is locked upright (rotation 0: neither Android nor the module swaps, so left
 * is the earpiece and right the speaker, as in the driver study). One stereo stream plays
 * [PAIRS] pairs of short clicks: a click on the left, then one on the right exactly [GAP_MS]
 * later, in the same buffer, so any extra delay of one driver changes the measured gap. Both
 * microphones record (UNPROCESSED where the phone has it). The timing is [SyncAnalysis]
 * (plain Kotlin, unit-tested on made-up recordings and on Dr.Essence's first one): offset =
 * measured gap - [GAP_MS]; positive means the speaker is late, negative the earpiece. On his
 * phone the input was one microphone on both channels (VOICE_RECOGNITION), so the distance
 * below is not cancelled there; the result says so.
 *
 * The microphones are not halfway between the drivers: the bottom one is nearer the speaker,
 * the top one nearer the earpiece, so each sees a small offset from distance alone (sound
 * travels about 0.03 ms per cm). The two microphones' offsets lean opposite ways, and their
 * mean cancels most of it. The spread across pairs shows whether the timing wanders.
 *
 * Results on screen, in /sdcard/cmf-sync.txt and /sdcard/cmf-sync.wav (the recording).
 */
class SyncActivity : Activity() {

    companion object {
        private const val RATE = 48000
        private const val PAIRS = 20
        /** Time from a left click to its right click, and from one pair to the next. */
        private const val GAP_MS = 250
        private const val PAIR_MS = 600
        /** The click: a 3 kHz burst (both drivers play it well), Hann-shaped, at -9 dBFS. */
        private const val CLICK_HZ = 3000.0
        private const val CLICK_MS = 2
        private const val CLICK_DB = -9.0
        /** Looked for this far round where a click is due, once the first one is found. */
        private const val SEARCH_MS = 60
    }

    private lateinit var label: TextView
    private lateinit var go: Button
    @Volatile private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
            setBackgroundColor(Color.BLACK)
        }
        label = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 16f
            text = "SYNC TEST\n\nDo the earpiece and the speaker play at the same moment?\n\n" +
                "Stereo must be on in CMF Stereo. Put the phone flat on a table, screen up, in a " +
                "quiet room, media volume about half, and do not touch it (about 15 s). It plays " +
                "$PAIRS pairs of short clicks, left then right, and both microphones time them."
        }
        col.addView(label)
        go = Button(this).apply {
            text = "START"
            textSize = 20f
            setOnClickListener { start() }
        }
        col.addView(go, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 48 })
        setContentView(ScrollView(this).apply { addView(col) })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!busy) Process.killProcess(Process.myPid())
    }

    private fun start() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            return
        }
        go.isEnabled = false
        busy = true
        Thread({ run() }, "sync").start()
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, res: IntArray) {
        if (res.firstOrNull() == PackageManager.PERMISSION_GRANTED) start()
        else label.text = "The microphone permission is needed for the sync test."
    }

    private fun say(s: String) = runOnUiThread { label.text = s }

    private fun su(cmd: String): String = try {
        val p = ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start()
        p.inputStream.bufferedReader().readText().trim().also { p.waitFor() }
    } catch (e: Exception) { "no root" }

    private fun run() {
        try {
            val report = measure()
            say(report)
        } catch (t: Throwable) {
            say("The test failed: ${t.javaClass.simpleName}: ${t.message}")
        } finally {
            busy = false
            runOnUiThread { go.isEnabled = true }
        }
    }

    /** Plays the clicks, records both microphones, times every click; returns the report. */
    private fun measure(): String {
        val mode = Regex("running: (stereo|mono)").find(su("stereoctl channels"))?.groupValues?.get(1)
        val pairFrames = RATE * PAIR_MS / 1000
        val gapFrames = RATE * GAP_MS / 1000
        val lead = RATE / 2 // half a second of silence first
        val total = lead + PAIRS * pairFrames + RATE / 2
        val am = getSystemService(AudioManager::class.java)
        val raw = am.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        val src = if (raw) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.VOICE_RECOGNITION
        val minBuf = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        @Suppress("MissingPermission")
        val rec = AudioRecord(src, RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, max(minBuf, RATE))
        val mic = ShortArray((total + RATE * 3) * 2)
        var micN = 0
        var playFrame = RATE * 8 / 10
        val recording = java.util.concurrent.atomic.AtomicBoolean(true)
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
            .setBufferSizeInBytes(RATE * 8 / 5)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        // The whole stream, made first: left clicks, and right clicks exactly gapFrames later.
        val out = FloatArray(total * 2)
        val clickLen = RATE * CLICK_MS / 1000
        val amp = Math.pow(10.0, CLICK_DB / 20)
        for (p in 0 until PAIRS) {
            for ((ch, at) in listOf(0 to lead + p * pairFrames, 1 to lead + p * pairFrames + gapFrames)) {
                for (k in 0 until clickLen) {
                    val w = 0.5 - 0.5 * cos(2 * PI * k / clickLen)
                    out[(at + k) * 2 + ch] = (amp * w * sin(2 * PI * CLICK_HZ * k / RATE)).toFloat()
                }
            }
        }
        try {
            rec.startRecording()
            val reader = Thread {
                val b = ShortArray(4800)
                while (recording.get()) {
                    val n = rec.read(b, 0, b.size)
                    if (n <= 0) continue
                    val k = min(n, mic.size - micN)
                    System.arraycopy(b, 0, mic, micN, k); micN += k
                }
            }.also { it.start() }
            Thread.sleep(800) // room noise, before anything plays
            say("Playing the clicks: do not touch the phone.")
            playFrame = micN / 2 // clicks are looked for only after this (SyncAnalysis)
            track.play()
            var pos = 0
            while (pos < total) {
                val n = min(2400, total - pos)
                track.write(out, pos * 2, n * 2, AudioTrack.WRITE_BLOCKING)
                pos += n
            }
            Thread.sleep(1000)
            track.stop()
            recording.set(false)
            reader.join(2000)
            rec.stop()
        } finally {
            recording.set(false)
            track.release()
            rec.release()
        }
        val frames = micN / 2
        val report = StringBuilder()
        report.append("SYNC TEST RESULT\n\n")
        report.append("Module mode: ${mode ?: "unknown (no answer from stereoctl)"}\n")
        report.append("Microphone source: ${if (raw) "UNPROCESSED" else "VOICE_RECOGNITION"}\n\n")
        if (mode != null && mode != "stereo") {
            report.append("Stereo is not on: both drivers play every click, so this cannot tell " +
                "them apart. Turn Stereo on and run it again.\n\n")
        }
        val channels = Array(2) { ch -> DoubleArray(frames) { mic[it * 2 + ch].toDouble() } }
        val single = SyncAnalysis.sameMicrophone(channels[0], channels[1])
        val results = (if (single) listOf(0) else listOf(0, 1)).map { ch ->
            SyncAnalysis.offsets(SyncAnalysis.envelope(channels[ch], RATE), RATE, quietUntil = playFrame,
                from = playFrame + RATE * 3 / 10, pairs = PAIRS, pairFrames = pairFrames,
                gapFrames = gapFrames, searchFrames = RATE * SEARCH_MS / 1000)
        }
        val means = ArrayList<Double>()
        results.forEachIndexed { k, res ->
            val name = if (single) "Microphone" else "Microphone ${k + 1}"
            if (res.offsets.size < PAIRS / 2) {
                report.append("$name: only ${res.offsets.size} of $PAIRS pairs heard clearly; " +
                    "louder volume or a quieter room.\n")
                return@forEachIndexed
            }
            means += res.mean
            report.append("$name: ${res.offsets.size} pairs, offset ${"%+.2f".format(res.mean)} ms " +
                "(spread ${"%.2f".format(res.spread)} ms, ${"%+.2f".format(res.offsets.min())} to " +
                "${"%+.2f".format(res.offsets.max())})" +
                (if (res.dropped > 0) ", ${res.dropped} misread pair(s) left out" else "") + "\n")
        }
        if (means.size == results.size && means.isNotEmpty()) {
            val both = means.average()
            if (single) {
                report.append("\nThe phone gave one microphone (the same sound on both channels), " +
                    "so its distance to each driver is not cancelled: the earpiece is about 15 cm " +
                    "farther from the bottom microphone, about -0.45 ms of this offset.\n")
            } else {
                report.append("\nBoth microphones (distance mostly cancelled): ${"%+.2f".format(both)} ms\n")
            }
            report.append(when {
                abs(both) < 1.0 -> "In step: under 1 ms apart, below what ears can hear."
                abs(both) < 5.0 -> "Close: ${"%.1f".format(abs(both))} ms apart; hard to hear, worth noting."
                else -> "Out of step: the ${if (both > 0) "speaker" else "earpiece"} is " +
                    "${"%.0f".format(abs(both))} ms late. Send cmf-sync.txt and cmf-sync.wav."
            })
            report.append("\n(Positive: the speaker is later; negative: the earpiece.)\n")
        }
        // Keep the report and the recording.
        val dir = getExternalFilesDir(null) ?: filesDir
        File(dir, "cmf-sync.txt").writeText(report.toString())
        writeWav(File(dir, "cmf-sync.wav"), mic, micN, 2)
        val copied = su("cp '${dir.path}/cmf-sync.txt' '${dir.path}/cmf-sync.wav' /sdcard/ && echo ok")
        report.append(if (copied.contains("ok")) "\nSaved: /sdcard/cmf-sync.txt and cmf-sync.wav"
            else "\nSaved in ${dir.path} (no root to copy to /sdcard)")
        return report.toString()
    }

    private fun writeWav(f: File, d: ShortArray, n: Int, ch: Int) {
        DataOutputStream(FileOutputStream(f).buffered()).use { o ->
            fun i32(v: Int) = o.writeInt(Integer.reverseBytes(v))
            fun i16(v: Int) = o.writeShort(java.lang.Short.reverseBytes(v.toShort()).toInt())
            o.writeBytes("RIFF"); i32(36 + n * 2); o.writeBytes("WAVEfmt ")
            i32(16); i16(1); i16(ch); i32(RATE); i32(RATE * ch * 2); i16(ch * 2); i16(16)
            o.writeBytes("data"); i32(n * 2)
            for (i in 0 until n) i16(d[i].toInt())
        }
    }
}
