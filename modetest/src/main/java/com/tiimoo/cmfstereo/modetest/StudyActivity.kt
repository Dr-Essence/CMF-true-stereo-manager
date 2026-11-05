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
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * DRIVER STUDY: measures how the earpiece and the speaker behave, to design
 * protection for both from data. Plays tones on the earpiece (left channel)
 * and then on the speaker (right channel), from 100 Hz to 8 kHz at seven levels
 * up to -3 dBFS, while recording
 *  - both microphones raw (AudioSource.UNPROCESSED, 48 kHz, two channels),
 *  - the audio HAL's own copy of what it received (vendor.streamout.pcm.dump:
 *    the level that really reached the drivers, after Dirac),
 *  - the HAL's own microphone capture (vendor.streamin.pcm.dump).
 * Then it measures, for every tone, the level that reached the HAL and the
 * fundamental and harmonics each microphone heard, into /sdcard/cmf-study.csv.
 * The screen is locked upright: at rotation 0 neither Android nor the module
 * swaps, so left is the earpiece and right the speaker throughout.
 */
class StudyActivity : Activity() {
    private data class Seg(val start: Int, val len: Int, val ch: Int, val f: Double, val db: Double, val name: String)

    private val rate = 48000
    private lateinit var label: TextView
    private lateinit var go: Button
    @Volatile private var running = false

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
            text = "DRIVER STUDY\n\nStereo must be on in CMF Stereo.\n\n" +
                "Put the phone flat on a table, screen up, in a quiet room, and do not touch it " +
                "until it says done (about 2 minutes).\n\nIt sets media volume to maximum while it runs " +
                "(and puts it back after): tones on the earpiece, then on the speaker, never louder " +
                "than -3 dB below full scale."
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
        if (!running) Process.killProcess(Process.myPid())
    }

    private fun start() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            return
        }
        go.isEnabled = false
        running = true
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

    /** The tone plan: a 1 kHz marker on the earpiece to line recordings up, then both drivers. */
    private fun plan(): List<Seg> {
        val freqs = doubleArrayOf(100.0, 150.0, 200.0, 300.0, 500.0, 800.0, 1200.0, 2000.0, 3000.0, 5000.0, 8000.0)
        val levels = doubleArrayOf(-30.0, -24.0, -18.0, -12.0, -9.0, -6.0, -3.0)
        val on = rate * 3 / 10
        val gap = rate * 3 / 10
        val out = ArrayList<Seg>()
        var t = rate / 2
        out.add(Seg(t, rate / 2, 0, 1000.0, -12.0, "marker")); t += rate / 2 + rate / 2
        for ((ch, name) in listOf(0 to "earpiece", 1 to "speaker")) {
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
        val segs = plan()
        val total = segs.last().let { it.start + it.len } + rate
        val dir = File(cacheDir, "study").apply { deleteRecursively(); mkdirs() }
        var rec: AudioRecord? = null
        val notes = StringBuilder()
        try {
            say("Starting the driver's own recordings...")
            notes.append("root: " + su("rm -rf /data/vendor/audiohal/audio_dump; " +
                "resetprop vendor.streamout.pcm.dump 1; resetprop vendor.streamin.pcm.dump 1; " +
                "echo rot=$(getprop vendor.audiohal.cmf_rotation) swap=$(getprop vendor.audiohal.cmf_swap)") + "\n")
            am.setParameters("vendor.streamout.pcm.dump=1")
            am.setParameters("vendor.streamin.pcm.dump=1")
            am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)

            // Microphones: raw if the phone offers it, else the least processed source.
            val raw = am.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
            val src = if (raw) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.VOICE_RECOGNITION
            notes.append("mic source: " + (if (raw) "UNPROCESSED" else "VOICE_RECOGNITION") + "\n")
            val minBuf = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)
            @Suppress("MissingPermission")
            val r = AudioRecord(src, rate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, max(minBuf, rate))
            rec = r
            val mic = ShortArray((total + rate * 2) * 2)
            var micN = 0
            r.startRecording()
            val reader = Thread {
                val b = ShortArray(4800)
                while (running) {
                    val n = r.read(b, 0, b.size)
                    if (n <= 0) continue
                    val k = min(n, mic.size - micN)
                    System.arraycopy(b, 0, mic, micN, k); micN += k
                }
            }.also { it.start() }
            Thread.sleep(1000)

            // Playback: generated on the fly, float, left = earpiece, right = speaker.
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(rate)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                .setBufferSizeInBytes(rate * 8 / 5)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
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
                            chunk[i * 2 + s.ch] = (a * w * sin(2 * PI * s.f * k / rate)).toFloat()
                        }
                    }
                }
                track.write(chunk, 0, n * 2, AudioTrack.WRITE_BLOCKING)
                pos += n
                if (pos % (rate * 2) < 2400) {
                    val s = segs.getOrNull(si)
                    say("Playing, do not touch the phone.\n\n${s?.name ?: ""} ${s?.f?.toInt() ?: ""} Hz, " +
                        "${s?.db?.toInt() ?: ""} dB\n\n${100 * pos / total}%")
                }
            }
            Thread.sleep(800)
            track.stop(); track.release()
            running = false
            reader.join(2000)
            r.stop()

            am.setParameters("vendor.streamout.pcm.dump=0")
            am.setParameters("vendor.streamin.pcm.dump=0")
            su("resetprop vendor.streamout.pcm.dump 0; resetprop vendor.streamin.pcm.dump 0")
            am.setStreamVolume(AudioManager.STREAM_MUSIC, oldVol, 0)

            say("Measuring...")
            writeWav(File(dir, "mic.wav"), mic, micN, 2)
            // The HAL's copy of the playback: the largest hal_streamout file of this run.
            val halSrc = su("ls -S /data/vendor/audiohal/audio_dump/hal_streamout_* 2>/dev/null | head -n 1")
            val hal = File(dir, "hal.wav")
            if (halSrc.startsWith("/")) su("cp '$halSrc' '${hal.path}' && chmod 644 '${hal.path}'")
            notes.append("hal file: $halSrc\n")
            val halCh = if (hal.length() > 44) readWav32(hal) else null
            notes.append("hal frames: ${halCh?.get(0)?.size ?: 0}, mic frames: ${micN / 2}\n")
            val csv = analyse(segs, mic, micN / 2, halCh, notes)
            File(dir, "study.csv").writeText(csv)
            File(dir, "notes.txt").writeText(notes.toString())
            File(dir, "plan.txt").writeText(segs.joinToString("\n") { "${it.start},${it.len},${it.name},${it.f},${it.db}" })

            val out = su("cp '${dir.path}/study.csv' /sdcard/cmf-study.csv; cp '${dir.path}/notes.txt' /sdcard/cmf-study-notes.txt; " +
                "cp '${dir.path}'/mic.wav '${dir.path}'/plan.txt /data/vendor/audiohal/audio_dump/ 2>/dev/null; " +
                "tar czf /sdcard/cmf-study-raw.tgz -C /data/vendor/audiohal audio_dump 2>&1; " +
                "ls -la /sdcard/cmf-study.csv /sdcard/cmf-study-raw.tgz")
            say("Done.\n\nSend /sdcard/cmf-study.csv and /sdcard/cmf-study-notes.txt " +
                "(small). Keep /sdcard/cmf-study-raw.tgz in case it is needed.\n\n$out")
        } catch (e: Throwable) {
            say("Stopped: $e")
        } finally {
            running = false
            try { rec?.release() } catch (_: Throwable) {}
            try {
                am.setParameters("vendor.streamout.pcm.dump=0")
                am.setParameters("vendor.streamin.pcm.dump=0")
                am.setStreamVolume(AudioManager.STREAM_MUSIC, oldVol, 0)
            } catch (_: Throwable) {}
            su("resetprop vendor.streamout.pcm.dump 0; resetprop vendor.streamin.pcm.dump 0")
        }
    }

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

    /** First block where 1 kHz stands 20 dB above the level before it: the marker's start. */
    private fun onset(x: (Int) -> Double, frames: Int): Int {
        val blk = rate / 100
        var floor = 1e-9
        for (b in 0 until min(30, frames / blk)) floor = max(floor, tone(x, b * blk, blk, 1000.0))
        for (b in 0 until frames / blk - 1) if (tone(x, b * blk, blk, 1000.0) > floor * 10) return b * blk
        return -1
    }

    private fun analyse(segs: List<Seg>, mic: ShortArray, micFrames: Int, hal: Array<FloatArray>?, notes: StringBuilder): String {
        val m0: (Int) -> Double = { i -> if (i in 0 until micFrames) mic[i * 2] / 32768.0 else 0.0 }
        val m1: (Int) -> Double = { i -> if (i in 0 until micFrames) mic[i * 2 + 1] / 32768.0 else 0.0 }
        val marker = segs[0].start
        // Line the microphones up on the marker (whichever mic hears it better).
        val o0 = onset(m0, micFrames); val o1 = onset(m1, micFrames)
        val micOff = listOf(o0, o1).filter { it >= 0 }.minOrNull()?.minus(marker)
        notes.append("mic marker onsets: $o0 $o1 -> offset ${micOff ?: "not found"}\n")
        var halOff: Int? = null
        var h0: (Int) -> Double = { 0.0 }; var h1: (Int) -> Double = { 0.0 }
        if (hal != null) {
            val hl = hal[0]; val hr = hal[1]
            h0 = { i -> if (i in hl.indices) hl[i].toDouble() else 0.0 }
            h1 = { i -> if (i in hr.indices) hr[i].toDouble() else 0.0 }
            val o = onset(h0, hl.size)
            halOff = if (o >= 0) o - marker else null
            notes.append("hal marker onset: $o -> offset ${halOff ?: "not found"}\n")
        }
        val sb = StringBuilder("driver,freq,level_dbfs,hal_l_db,hal_r_db,mic,fund_db,h2_db,h3_db,h4_db,h5_db,thd_pct,peak_db\n")
        val win = rate * 15 / 100       // 150 ms from 100 ms into each 300 ms tone
        for (s in segs.drop(1)) {
            var halL = Double.NaN; var halR = Double.NaN
            if (halOff != null) {
                val a = s.start + halOff + rate / 10
                halL = db(tone(h0, a, win, s.f)); halR = db(tone(h1, a, win, s.f))
            }
            for ((mi, mx) in listOf(0 to m0, 1 to m1)) {
                if (micOff == null) { sb.append("${s.name},${s.f.toInt()},${s.db},$halL,$halR,$mi,,,,,,,\n"); continue }
                val a = s.start + micOff + rate / 10
                val fund = tone(mx, a, win, s.f)
                val hs = (2..5).map { k -> if (s.f * k < rate / 2 - 1000) tone(mx, a, win, s.f * k) else 0.0 }
                val thd = 100 * sqrt(hs.sumOf { it * it }) / max(fund, 1e-12)
                var pk = 0.0
                for (i in a until a + win) pk = max(pk, kotlin.math.abs(mx(i)))
                sb.append("${s.name},${s.f.toInt()},${s.db},${"%.1f".format(halL)},${"%.1f".format(halR)},$mi," +
                    "${"%.1f".format(db(fund))},${hs.joinToString(",") { "%.1f".format(db(it)) }}," +
                    "${"%.2f".format(thd)},${"%.1f".format(db(pk))}\n")
            }
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

    /** The HAL's recording: 2-channel 32- or 16-bit integer WAV (as the HAL writes it), read in chunks. */
    private fun readWav32(f: File): Array<FloatArray>? {
        RandomAccessFile(f, "r").use { raf ->
            val head = ByteArray(min(4096L, raf.length()).toInt()); raf.readFully(head)
            val bb = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN)
            var p = 12; var ch = 2; var bits = 32; var dataAt = -1; var dataLen = 0L
            while (p + 8 <= head.size) {
                val id = String(head, p, 4); val sz = bb.getInt(p + 4).toLong() and 0xffffffffL
                if (id == "fmt ") { ch = bb.getShort(p + 10).toInt(); bits = bb.getShort(p + 22).toInt() }
                if (id == "data") { dataAt = p + 8; dataLen = sz; break }
                p += 8 + sz.toInt()
            }
            if (dataAt < 0 || ch < 2 || (bits != 32 && bits != 16)) return null
            val bps = bits / 8
            val fsz = bps * ch
            // The HAL may not fill in the size of a file it is still writing: use what is there.
            val len = if (dataLen in 1..(raf.length() - dataAt)) dataLen else raf.length() - dataAt
            val frames = (len / fsz).toInt()
            val l = FloatArray(frames); val r = FloatArray(frames)
            val buf = ByteArray(fsz * 8192)
            raf.seek(dataAt.toLong())
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
