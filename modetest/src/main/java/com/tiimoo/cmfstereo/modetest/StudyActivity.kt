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
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * DRIVER STUDY: measures how the earpiece and the speaker behave, to design
 * protection for both from data. It switches the module's mode itself, after
 * reading which one is running, and always puts that one back:
 *
 *  1. Stereo: tones on the earpiece (left) and then the speaker (right), 100 Hz
 *     to 8 kHz at seven levels up to -3 dBFS, recorded by both microphones raw
 *     (UNPROCESSED), and by the HAL itself (what reached it, after Dirac).
 *  2. Mono (stock files, Awinic's protection on): the same kind of tones on both
 *     channels (the stock HAL folds left and right together before Awinic), with
 *     the audio DSP's recordings of Awinic's input, output and the amp's current
 *     and voltage sense: what Awinic does to the speaker at each note and level.
 *
 * Results: /sdcard/cmf-study.csv (stereo), /sdcard/cmf-study-mono.csv,
 * /sdcard/cmf-study-notes.txt; raw recordings in /sdcard/cmf-study-raw.tgz.
 * The screen is locked upright: at rotation 0 neither Android nor the module
 * swaps, so left is the earpiece and right the speaker throughout.
 */
class StudyActivity : Activity() {
    /** One tone. ch: 0 left (earpiece), 1 right (speaker), -1 both. */
    private data class Seg(val start: Int, val len: Int, val ch: Int, val f: Double, val db: Double, val name: String)

    /** What one session recorded: the microphones, and the files the HAL and DSP wrote. */
    private class Take(val mic: ShortArray, val micFrames: Int, val dumps: File)

    private val rate = 48000
    private val dumpDir = "/data/vendor/audiohal/audio_dump"
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
            text = "DRIVER STUDY\n\nPut the phone flat on a table, screen up, in a quiet room, and do not " +
                "touch it until it says done (about 3 minutes).\n\nIt switches Stereo and Mono by itself " +
                "(the sound stops for a moment at each switch) and puts back the mode you are in now. " +
                "Media volume goes to maximum while it runs and back after: tones never louder than " +
                "-3 dB below full scale."
        }
        go = Button(this).apply {
            text = "START"
            textSize = 20f
            gravity = Gravity.CENTER
            setOnClickListener { start() }
        }
        col.addView(label)
        col.addView(go, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 48 })
        setContentView(col)
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
        Thread({ run() }, "study").start()
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, res: IntArray) {
        if (res.firstOrNull() == PackageManager.PERMISSION_GRANTED) start()
        else label.text = "The microphone permission is needed for the study."
    }

    private fun say(s: String) = runOnUiThread { label.text = s }

    private fun su(cmd: String): String = try {
        val p = ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start()
        p.inputStream.bufferedReader().readText().trim().also { p.waitFor() }
    } catch (e: Exception) { "no root" }

    /** The mode the module is running now ("stereo" / "mono"), or null if it does not answer. */
    private fun mode(): String? =
        Regex("running: (stereo|mono)").find(su("stereoctl channels"))?.groupValues?.get(1)

    /** Switches and checks that the mode really runs (not only staged for a reboot). */
    private fun switchTo(want: String, notes: StringBuilder): Boolean {
        if (mode() == want) return true
        say("Switching to ${want.uppercase()}...")
        val out = su("stereoctl channels $want")
        notes.append("switch to $want: ${out.lineSequence().firstOrNull()}\n")
        Thread.sleep(3000)   // the audio HAL has restarted: let Android reconnect to it
        return mode() == want
    }

    /** Stereo: a 1 kHz marker on the left to line recordings up, then each driver. */
    private fun stereoPlan(): List<Seg> {
        val freqs = doubleArrayOf(100.0, 150.0, 200.0, 300.0, 500.0, 800.0, 1200.0, 2000.0, 3000.0, 5000.0, 8000.0)
        return tones(listOf(0 to "earpiece", 1 to "speaker"), freqs)
    }

    /** Mono: the same on both channels (the stock HAL folds them together anyway). */
    private fun monoPlan(): List<Seg> =
        tones(listOf(-1 to "mono"), doubleArrayOf(100.0, 150.0, 200.0, 300.0, 500.0, 1000.0, 3000.0))

    private fun tones(drivers: List<Pair<Int, String>>, freqs: DoubleArray): List<Seg> {
        val levels = doubleArrayOf(-30.0, -24.0, -18.0, -12.0, -9.0, -6.0, -3.0)
        val on = rate * 3 / 10
        val gap = rate * 3 / 10
        val out = ArrayList<Seg>()
        var t = rate / 2
        out.add(Seg(t, rate / 2, drivers[0].first, 1000.0, -12.0, "marker")); t += rate
        for ((ch, name) in drivers) {
            for (f in freqs) for (db in levels) {
                out.add(Seg(t, on, ch, f, db, name)); t += on + gap
            }
            t += rate
        }
        return out
    }

    private fun run() {
        val am = getSystemService(AudioManager::class.java)
        val oldVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val work = File(cacheDir, "study").apply { deleteRecursively(); mkdirs() }
        val notes = StringBuilder()
        val first = mode()
        if (first == null) {
            say("The CMF stereo module did not answer (stereoctl channels). Nothing was changed.")
            busy = false
            return
        }
        notes.append("mode at start: $first\n")
        notes.append(su("echo rot=$(getprop vendor.audiohal.cmf_rotation) swap=$(getprop vendor.audiohal.cmf_swap) " +
            "dirac=$(getprop persist.vendor.newdirac.cur.config)") + "\n")
        val done = StringBuilder()
        try {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)

            if (switchTo("stereo", notes)) {
                val segs = stereoPlan()
                val take = session("stereo", segs, false, am, work, notes)
                say("Measuring the stereo part...")
                File(work, "study.csv").writeText(analyseStereo(segs, take, notes))
                done.append("/sdcard/cmf-study.csv\n")
            } else notes.append("stereo did not start: stereo part skipped\n")

            if (switchTo("mono", notes)) {
                val segs = monoPlan()
                val take = session("mono", segs, true, am, work, notes)
                say("Measuring the mono part...")
                File(work, "study-mono.csv").writeText(analyseMono(segs, take, notes))
                done.append("/sdcard/cmf-study-mono.csv\n")
            } else notes.append("mono did not start: mono part skipped\n")
        } catch (e: Throwable) {
            notes.append("stopped: $e\n")
        } finally {
            try { am.setStreamVolume(AudioManager.STREAM_MUSIC, oldVol, 0) } catch (_: Throwable) {}
            dumpsOff(am)
            // Whatever happened, back to the mode the phone was in.
            val back = switchTo(first, notes)
            notes.append("mode at end: ${mode()} (wanted $first${if (back) "" else ", NOT restored"})\n")
            File(work, "notes.txt").writeText(notes.toString())
            val out = su("cd '${work.path}' && { [ -f study.csv ] && cp study.csv /sdcard/cmf-study.csv; " +
                "[ -f study-mono.csv ] && cp study-mono.csv /sdcard/cmf-study-mono.csv; " +
                "cp notes.txt /sdcard/cmf-study-notes.txt; " +
                "tar czf /sdcard/cmf-study-raw.tgz . 2>&1; ls -la /sdcard/cmf-study*; }")
            busy = false
            say("Done. Mode is ${mode()} again.\n\nSend these (small):\n$done/sdcard/cmf-study-notes.txt\n\n" +
                "Keep /sdcard/cmf-study-raw.tgz in case it is needed.\n\n$out")
        }
    }

    private fun dumpsOff(am: AudioManager) {
        try {
            am.setParameters("vendor.streamout.pcm.dump=0")
            am.setParameters("vendor.streamin.pcm.dump=0")
        } catch (_: Throwable) {}
        su("resetprop vendor.streamout.pcm.dump 0; resetprop vendor.streamin.pcm.dump 0; " +
            "resetprop vendor.streamout.dsp.dump 0")
    }

    /** Plays one plan while the microphones and the HAL (and the DSP, if asked) record. */
    private fun session(tag: String, segs: List<Seg>, dsp: Boolean, am: AudioManager, work: File, notes: StringBuilder): Take {
        val total = segs.last().let { it.start + it.len } + rate
        say("Starting the recordings ($tag)...")
        su("rm -rf $dumpDir; resetprop vendor.streamout.pcm.dump 1; resetprop vendor.streamin.pcm.dump 1; " +
            "resetprop vendor.streamout.dsp.dump ${if (dsp) 1 else 0}")
        am.setParameters("vendor.streamout.pcm.dump=1")
        am.setParameters("vendor.streamin.pcm.dump=1")

        val raw = am.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        val src = if (raw) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.VOICE_RECOGNITION
        notes.append("$tag mic source: ${if (raw) "UNPROCESSED" else "VOICE_RECOGNITION"}\n")
        val minBuf = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        @Suppress("MissingPermission")
        val r = AudioRecord(src, rate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, max(minBuf, rate))
        val mic = ShortArray((total + rate * 2) * 2)
        var micN = 0
        val recording = java.util.concurrent.atomic.AtomicBoolean(true)
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate)
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
            .setBufferSizeInBytes(rate * 8 / 5)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        try {
            r.startRecording()
            val reader = Thread {
                val b = ShortArray(4800)
                while (recording.get()) {
                    val n = r.read(b, 0, b.size)
                    if (n <= 0) continue
                    val k = min(n, mic.size - micN)
                    System.arraycopy(b, 0, mic, micN, k); micN += k
                }
            }.also { it.start() }
            Thread.sleep(1000)
            track.play()
            val chunk = FloatArray(2400 * 2)
            var pos = 0
            var si = 0
            val fade = rate / 100
            while (pos < total) {
                java.util.Arrays.fill(chunk, 0f)
                val n = min(2400, total - pos)
                for (i in 0 until n) {
                    val t = pos + i
                    while (si < segs.size && segs[si].start + segs[si].len <= t) si++
                    if (si < segs.size) {
                        val s = segs[si]
                        val k = t - s.start
                        if (k in 0 until s.len) {
                            val a = Math.pow(10.0, s.db / 20)
                            val w = if (k < fade) 0.5 - 0.5 * cos(PI * k / fade)
                                else if (k > s.len - fade) 0.5 - 0.5 * cos(PI * (s.len - k) / fade) else 1.0
                            val v = (a * w * sin(2 * PI * s.f * k / rate)).toFloat()
                            if (s.ch != 1) chunk[i * 2] = v
                            if (s.ch != 0) chunk[i * 2 + 1] = v
                        }
                    }
                }
                track.write(chunk, 0, n * 2, AudioTrack.WRITE_BLOCKING)
                pos += n
                if (pos % (rate * 2) < 2400) {
                    val s = segs.getOrNull(si)
                    say("${tag.uppercase()}: playing, do not touch the phone.\n\n${s?.name ?: ""} " +
                        "${s?.f?.toInt() ?: ""} Hz, ${s?.db?.toInt() ?: ""} dB\n\n${100 * pos / total}%")
                }
            }
            Thread.sleep(800)
            track.stop()
            recording.set(false)
            reader.join(2000)
            r.stop()
        } finally {
            recording.set(false)
            track.release()
            r.release()
            dumpsOff(am)
        }
        // Keep this session's HAL and DSP files with the microphone recording.
        val dir = File(work, tag).apply { mkdirs() }
        su("cp -r $dumpDir/. '${dir.path}/' 2>/dev/null; chmod -R a+rX '${dir.path}'")
        writeWav(File(dir, "mic.wav"), mic, micN, 2)
        File(dir, "plan.txt").writeText(segs.joinToString("\n") { "${it.start},${it.len},${it.name},${it.f},${it.db}" })
        notes.append("$tag files: ${dir.list()?.sorted()?.joinToString(" ")}\n")
        return Take(mic, micN / 2, dir)
    }

    /** The largest file of this session whose name starts with [prefix] and ends with [suffix]. */
    private fun largest(dir: File, prefix: String, suffix: String): File? =
        dir.listFiles()?.filter { it.name.startsWith(prefix) && it.name.endsWith(suffix) }?.maxByOrNull { it.length() }

    /** Amplitude (relative to full scale) of frequency f in x[from, from+n), Hann window. */
    private fun tone(x: (Int) -> Double, from: Int, n: Int, f: Double): Double {
        var re = 0.0; var im = 0.0; var ws = 0.0
        val w0 = 2 * PI * f / rate
        for (i in 0 until n) {
            val w = 0.5 - 0.5 * cos(2 * PI * i / (n - 1))
            val v = x(from + i) * w
            re += v * cos(w0 * i); im -= v * sin(w0 * i); ws += w
        }
        return 2 * sqrt(re * re + im * im) / ws
    }

    private fun db(a: Double) = if (a <= 1e-12) -240.0 else 20 * log10(a)
    private fun f1(v: Double) = "%.1f".format(v)

    /** First block where 1 kHz stands 20 dB above the level before it: the marker's start. */
    private fun onset(x: (Int) -> Double, frames: Int): Int {
        val blk = rate / 100
        var floor = 1e-9
        for (b in 0 until min(30, frames / blk)) floor = max(floor, tone(x, b * blk, blk, 1000.0))
        for (b in 0 until frames / blk - 1) if (tone(x, b * blk, blk, 1000.0) > floor * 10) return b * blk
        return -1
    }

    private fun channel(a: FloatArray?, scale: Double = 1.0): (Int) -> Double =
        { i -> if (a != null && i in a.indices) a[i] * scale else 0.0 }

    /** Where the plan's time 0 sits in a recording, found on its marker. */
    private fun offset(x: (Int) -> Double, frames: Int, marker: Int, what: String, notes: StringBuilder): Int? {
        val o = onset(x, frames)
        notes.append("$what marker onset $o\n")
        return if (o >= 0) o - marker else null
    }

    private val win get() = rate * 15 / 100   // 150 ms, from 100 ms into each 300 ms tone

    /** Fundamental, harmonics 2-5, distortion (%) and peak of one microphone over one tone. */
    private fun micCols(x: (Int) -> Double, at: Int, f: Double): String {
        val fund = tone(x, at, win, f)
        val hs = (2..5).map { k -> if (f * k < rate / 2 - 1000) tone(x, at, win, f * k) else 0.0 }
        val thd = 100 * sqrt(hs.sumOf { it * it }) / max(fund, 1e-12)
        var pk = 0.0
        for (i in at until at + win) pk = max(pk, kotlin.math.abs(x(i)))
        return "${f1(db(fund))},${hs.joinToString(",") { f1(db(it)) }},${"%.2f".format(thd)},${f1(db(pk))}"
    }

    /** The HAL's own microphone capture (8.24 fixed point in 32 bits), if it wrote one. */
    private fun rawMics(t: Take): Array<FloatArray>? = t.dumps.listFiles()
        ?.filter { it.name.startsWith("hal_capturedataprovider_") && it.name.endsWith(".pcm") && it.name.contains("2ch") }
        ?.maxByOrNull { it.length() }?.let { readPcm(it, false) }

    private fun micChannels(t: Take): List<(Int) -> Double> = listOf(
        { i: Int -> if (i in 0 until t.micFrames) t.mic[i * 2] / 32768.0 else 0.0 },
        { i: Int -> if (i in 0 until t.micFrames) t.mic[i * 2 + 1] / 32768.0 else 0.0 })

    /**
     * Where a microphone recording sits against the HAL's copy of the playback:
     * the lag (in 10 ms blocks, up to 10 s) at which their loudness envelopes
     * match best. Measured on the phone: a click when recording starts fooled a
     * marker search, the envelope match lined them up (correlation 0.79).
     */
    private fun lag(ref: (Int) -> Double, refN: Int, x: (Int) -> Double, xN: Int, what: String, notes: StringBuilder): Int? {
        val blk = rate / 100
        fun env(f: (Int) -> Double, n: Int) = DoubleArray(n / blk) { b ->
            var e = 0.0
            for (i in b * blk until (b + 1) * blk) { val v = f(i); e += v * v }
            ln(sqrt(e / blk) + 1e-9)
        }
        val a = env(ref, refN); val b = env(x, xN)
        val am = a.average()
        var best = -2.0; var bestL = -1
        for (l in 0 until min(1000, b.size)) {
            val n = min(a.size, b.size - l)
            if (n < 500) break
            var sa = 0.0; var sb = 0.0; var sab = 0.0; var bm = 0.0
            for (i in 0 until n) bm += b[i + l]
            bm /= n
            for (i in 0 until n) {
                val da = a[i] - am; val db = b[i + l] - bm
                sab += da * db; sa += da * da; sb += db * db
            }
            val c = sab / sqrt(sa * sb + 1e-30)
            if (c > best) { best = c; bestL = l }
        }
        notes.append("$what lag ${bestL * blk} frames, correlation ${"%.2f".format(best)}\n")
        return if (best > 0.5) bestL * blk else null
    }

    private fun analyseStereo(segs: List<Seg>, t: Take, notes: StringBuilder): String {
        val marker = segs[0].start
        val hal = largest(t.dumps, "hal_streamout", ".wav")?.let { readPcm(it, true) }
        notes.append("stereo hal file: ${largest(t.dumps, "hal_streamout", ".wav")?.name}\n")
        val hl = channel(hal?.get(0)); val hr = channel(hal?.get(1))
        val hn = hal?.get(0)?.size ?: 0
        val ho = if (hal != null) offset(hl, hn, marker, "stereo hal", notes) else null
        val both: (Int) -> Double = { i -> hl(i) + hr(i) }
        // Microphones 0, 1: what Android gave the app; 2, 3: the driver's own raw capture.
        val app = micChannels(t)
        val raw = rawMics(t)
        val mics = ArrayList<Pair<(Int) -> Double, Int?>>()
        if (ho != null) {
            val la = lag(both, hn, { i -> app[0](i) + app[1](i) }, t.micFrames, "app mics", notes)
            app.forEach { mics.add(it to la?.let { l -> ho + l }) }
            if (raw != null) {
                val r0 = channel(raw[0], 256.0); val r1 = channel(raw[1], 256.0)
                val lr = lag(both, hn, { i -> r0(i) + r1(i) }, raw[0].size, "raw mics", notes)
                listOf(r0, r1).forEach { mics.add(it to lr?.let { l -> ho + l }) }
            }
        }
        val sb = StringBuilder("driver,freq,level_dbfs,hal_l_db,hal_r_db,mic,fund_db,h2_db,h3_db,h4_db,h5_db,thd_pct,peak_db\n")
        for (s in segs.drop(1)) {
            val ha = if (ho != null) s.start + ho + rate / 10 else -1
            val hcols = if (ho != null) "${f1(db(tone(hl, ha, win, s.f)))},${f1(db(tone(hr, ha, win, s.f)))}" else ","
            mics.forEachIndexed { mi, (x, off) ->
                val mcols = if (off != null) micCols(x, s.start + off + rate / 10, s.f) else ",,,,,,"
                sb.append("${s.name},${s.f.toInt()},${s.db},$hcols,$mi,$mcols\n")
            }
        }
        return sb.toString()
    }

    /**
     * Mono: Awinic's input and output on the audio DSP (8.24 fixed point in a
     * 32-bit word, hence x256) and the amp's current/voltage sense, per tone.
     */
    private fun analyseMono(segs: List<Seg>, t: Take, notes: StringBuilder): String {
        val marker = segs[0].start
        val hal = largest(t.dumps, "hal_streamout", ".wav")?.let { readPcm(it, true) }
        val inF = t.dumps.listFiles()
            ?.filter { it.name.startsWith("adsp_Playback_") && it.name.contains("_in_") && it.name.endsWith(".pcm") }
            ?.maxByOrNull { it.length() }
        val outF = inF?.let { File(it.parentFile, it.name.replace("_in_", "_out_")) }?.takeIf { it.exists() }
        val ivF = inF?.let { File(it.parentFile, it.name.replace("_in_", "_ivdump_")) }?.takeIf { it.exists() }
        notes.append("mono files: hal ${largest(t.dumps, "hal_streamout", ".wav")?.name}, dsp in ${inF?.name}, " +
            "out ${outF?.name}, iv ${ivF?.name}\n")
        val dIn = inF?.let { readPcm(it, false) }
        val dOut = outF?.let { readPcm(it, false) }
        val dIv = ivF?.let { readPcm(it, false) }
        val hl = channel(hal?.get(0)); val hr = channel(hal?.get(1))
        val il = channel(dIn?.get(0), 256.0); val ir = channel(dIn?.get(1), 256.0)
        val ol = channel(dOut?.get(0), 256.0); val or = channel(dOut?.get(1), 256.0)
        val v0 = channel(dIv?.get(0)); val v1 = channel(dIv?.get(1))
        val ho = if (hal != null) offset(hl, hal[0].size, marker, "mono hal", notes) else null
        val raw = rawMics(t)
        val m = raw?.let { listOf(channel(it[0], 256.0), channel(it[1], 256.0)) } ?: micChannels(t)
        val mN = raw?.get(0)?.size ?: t.micFrames
        notes.append("mono mics: ${if (raw != null) "driver raw capture" else "app recording"}\n")
        val mo = if (ho != null && hal != null) lag(hl, hal[0].size, { i -> m[0](i) + m[1](i) }, mN, "mono mics", notes)?.let { ho + it } else null
        val io = if (dIn != null) offset(il, dIn[0].size, marker, "dsp in", notes) else null
        val oo = if (dOut != null) offset(ol, dOut[0].size, marker, "dsp out", notes) ?: io else io
        val sb = StringBuilder("driver,freq,level_dbfs,hal_l_db,hal_r_db,dsp_in_l_db,dsp_in_r_db,dsp_out_l_db,dsp_out_r_db," +
            "iv0_db,iv1_db,mic0_fund_db,mic0_thd_pct,mic1_fund_db,mic1_thd_pct\n")
        fun lv(x: (Int) -> Double, off: Int?, s: Seg) = if (off == null) "" else f1(db(tone(x, s.start + off + rate / 10, win, s.f)))
        for (s in segs.drop(1)) {
            val mc = (0..1).joinToString(",") { mi ->
                if (mo == null) "," else {
                    val c = micCols(m[mi], s.start + mo + rate / 10, s.f).split(",")
                    "${c[0]},${c[5]}"
                }
            }
            sb.append("${s.name},${s.f.toInt()},${s.db},${lv(hl, ho, s)},${lv(hr, ho, s)},${lv(il, io, s)},${lv(ir, io, s)}," +
                "${lv(ol, oo, s)},${lv(or, oo, s)},${lv(v0, oo, s)},${lv(v1, oo, s)},$mc\n")
        }
        return sb.toString()
    }

    private fun writeWav(f: File, d: ShortArray, n: Int, ch: Int) {
        DataOutputStream(FileOutputStream(f).buffered()).use { o ->
            val bytes = n * 2
            fun i32(v: Int) = o.writeInt(Integer.reverseBytes(v))
            fun i16(v: Int) = o.writeShort(java.lang.Short.reverseBytes(v.toShort()).toInt())
            o.writeBytes("RIFF"); i32(36 + bytes); o.writeBytes("WAVEfmt ")
            i32(16); i16(1); i16(ch); i32(rate); i32(rate * ch * 2); i16(ch * 2); i16(16)
            o.writeBytes("data"); i32(bytes)
            for (i in 0 until n) i16(d[i].toInt())
        }
    }

    /**
     * Two channels of a HAL recording (WAV, 32- or 16-bit integer) or of a DSP
     * recording (raw 32-bit, no header), read in chunks.
     */
    private fun readPcm(f: File, wav: Boolean): Array<FloatArray>? {
        RandomAccessFile(f, "r").use { raf ->
            var ch = 2; var bits = 32; var dataAt = 0L; var dataLen = raf.length()
            if (wav) {
                val head = ByteArray(min(4096L, raf.length()).toInt()); raf.readFully(head)
                val bb = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN)
                var p = 12; var found = false
                while (p + 8 <= head.size) {
                    val id = String(head, p, 4); val sz = bb.getInt(p + 4).toLong() and 0xffffffffL
                    if (id == "fmt ") { ch = bb.getShort(p + 10).toInt(); bits = bb.getShort(p + 22).toInt() }
                    if (id == "data") { dataAt = (p + 8).toLong(); dataLen = sz; found = true; break }
                    p += 8 + sz.toInt()
                }
                if (!found) return null
            }
            if (ch < 2 || (bits != 32 && bits != 16)) return null
            val bps = bits / 8
            val fsz = bps * ch
            // The HAL may not fill in the size of a file it is still writing: use what is there.
            val len = if (dataLen in 1..(raf.length() - dataAt)) dataLen else raf.length() - dataAt
            val frames = (len / fsz).toInt()
            val l = FloatArray(frames); val r = FloatArray(frames)
            val buf = ByteArray(fsz * 8192)
            raf.seek(dataAt)
            var i = 0
            while (i < frames) {
                val n = min(8192, frames - i)
                raf.readFully(buf, 0, n * fsz)
                val d = ByteBuffer.wrap(buf, 0, n * fsz).order(ByteOrder.LITTLE_ENDIAN)
                for (k in 0 until n) {
                    val at = k * fsz
                    if (bits == 32) { l[i + k] = d.getInt(at) / 2147483648f; r[i + k] = d.getInt(at + 4) / 2147483648f }
                    else { l[i + k] = d.getShort(at) / 32768f; r[i + k] = d.getShort(at + 2) / 32768f }
                }
                i += n
            }
            return arrayOf(l, r)
        }
    }
}
