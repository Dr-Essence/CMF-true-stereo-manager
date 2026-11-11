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
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * RESPONSE STUDY: every frequency of each driver, for the EQ (Dr.Essence: "measure every
 * single detail and every frequency"). Records only; the analysis is done from the files.
 *
 * What plays: in Stereo, with the screen upright (rotation 0: left is the earpiece, right the
 * speaker, nothing swaps), an exponential sine sweep from [F1] to [F2] Hz, [SWEEP_S] s long, on
 * each driver alone, at each level of [LEVELS] (well under the module's ceilings: the earpiece
 * limiter at -2.5 dBFS, the speaker's 6 dB attenuation), media volume at its maximum so a level
 * means the same every time, as in the driver study.
 *
 * Why this sweep (Farina's method): deconvolving the recording by the sweep gives the driver's
 * impulse response, and each harmonic the driver adds arrives as its own earlier copy, so the
 * frequency response and the distortion at every frequency come from the same recording.
 *
 * What is recorded, at once, to tell the stages apart:
 * - the microphones through Android (UNPROCESSED where the phone has it);
 * - the audio HAL's own recordings (vendor.streamout.pcm.dump): what reached it, that is the
 *   sweep after Android's effects, Dirac's speaker tuning among them (the driver study measured
 *   +5 to +6 dB at 200-500 Hz and a second harmonic at 100 Hz there); and what it sent on;
 * - the audio DSP's input and output (vendor.streamout.dsp.dump);
 * - the microphones' raw capture inside the HAL (vendor.streamin.pcm.dump), before Android's
 *   input processing.
 * So: Dirac = HAL input / sweep; each driver (with the microphone) = microphone / HAL output.
 *
 * Result: /sdcard/cmf-sweep.tgz (all of it, with plan.txt and notes.txt). The mode the phone was
 * in, and the volume, are put back at the end, whatever happens.
 */
class SweepActivity : Activity() {

    companion object {
        private const val RATE = 48000
        private const val F1 = 20.0
        private const val F2 = 20000.0
        private const val SWEEP_S = 6.0
        /** Silence before each sweep (the room's level) and after it (the room's echo). */
        private const val GAP_S = 1.5
        private val LEVELS = doubleArrayOf(-24.0, -12.0)
        private const val FADE_S = 0.05
        private const val DUMPS = "/data/vendor/audiohal/audio_dump"
    }

    /** One sweep: where it starts in the played stream, on which channel, at what level. */
    private class Sweep(val start: Int, val ch: Int, val db: Double)

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
            text = "RESPONSE STUDY\n\nEvery frequency of each driver, 20 Hz to 20 kHz, for the EQ. " +
                "Put the phone flat on a table, screen up, in a quiet room, and do not touch it " +
                "until it says done (about 1 minute).\n\nIt switches to Stereo by itself if needed " +
                "and puts back the mode you are in now. Media volume goes to maximum while it runs " +
                "and back after; the sweeps are quiet (-24 and -12 dBFS), well under the limits."
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
        Thread({ if (Countdown.wait(::say)) run() else busy = false }, "sweep").start()
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

    private fun mode(): String? =
        Regex("running: (stereo|mono)").find(su("stereoctl channels"))?.groupValues?.get(1)

    /** Switches and checks that the mode really runs (not only staged for a reboot). */
    private fun switchTo(want: String, notes: StringBuilder): Boolean {
        if (mode() == want) return true
        say("Switching to ${want.uppercase()}...")
        notes.append("switch to $want: ${su("stereoctl channels $want").lineSequence().firstOrNull()}\n")
        Thread.sleep(3000) // the audio HAL has restarted: let Android reconnect to it
        return mode() == want
    }

    private fun run() {
        val am = getSystemService(AudioManager::class.java)
        val oldVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val work = File(cacheDir, "sweep").apply { deleteRecursively(); mkdirs() }
        val notes = StringBuilder()
        val first = mode()
        if (first == null) {
            say("The CMF stereo module did not answer (stereoctl channels). Nothing was changed.")
            busy = false
            runOnUiThread { go.isEnabled = true }
            return
        }
        notes.append("mode at start: $first\n")
        notes.append(su("echo rot=$(getprop vendor.audiohal.cmf_rotation) swap=$(getprop vendor.audiohal.cmf_swap) " +
            "dirac=$(getprop persist.vendor.newdirac.cur.config) ear_ceil=$(getprop vendor.audiohal.cmf_ear_ceil) " +
            "spk_ceil=$(getprop vendor.audiohal.cmf_spk_ceil) bass_ceil=$(getprop vendor.audiohal.cmf_bass_ceil) " +
            "ear_level=$(getprop vendor.audiohal.cmf_ear_level)") + "\n")
        var saved = ""
        try {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
            if (switchTo("stereo", notes)) record(am, work, notes)
            else notes.append("stereo did not start: nothing recorded\n")
        } catch (e: Throwable) {
            notes.append("stopped: $e\n")
        } finally {
            try { am.setStreamVolume(AudioManager.STREAM_MUSIC, oldVol, 0) } catch (_: Throwable) {}
            dumpsOff(am)
            val back = switchTo(first, notes)
            notes.append("mode at end: ${mode()} (wanted $first${if (back) "" else ", NOT restored"})\n")
            File(work, "notes.txt").writeText(notes.toString())
            saved = su("cd '${work.path}' && tar czf /sdcard/cmf-sweep.tgz . 2>&1; ls -la /sdcard/cmf-sweep.tgz")
            busy = false
            runOnUiThread { go.isEnabled = true }
            say("Done. Mode is ${mode()} again, volume as before.\n\nSend /sdcard/cmf-sweep.tgz " +
                "(tens of MB: Google Drive is fine).\n\n$saved")
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

    /** The plan: each level, each driver (earpiece, then speaker), one sweep with silence round it. */
    private fun plan(): List<Sweep> {
        val gap = (GAP_S * RATE).toInt()
        val len = (SWEEP_S * RATE).toInt()
        val out = ArrayList<Sweep>()
        var at = gap
        for (db in LEVELS) for (ch in 0..1) {
            out += Sweep(at, ch, db)
            at += len + gap
        }
        return out
    }

    /** Sample [k] of the exponential sweep from F1 to F2 over SWEEP_S seconds, faded at both ends. */
    private fun sweep(k: Int, len: Int): Double {
        val t = k.toDouble() / RATE
        val r = ln(F2 / F1)
        val phase = 2 * PI * F1 * SWEEP_S / r * (exp(t * r / SWEEP_S) - 1)
        val fade = (FADE_S * RATE).toInt()
        val w = when {
            k < fade -> 0.5 - 0.5 * cos(PI * k / fade)
            k > len - fade -> 0.5 - 0.5 * cos(PI * (len - k) / fade)
            else -> 1.0
        }
        return w * sin(phase)
    }

    private fun record(am: AudioManager, work: File, notes: StringBuilder) {
        val sweeps = plan()
        val len = (SWEEP_S * RATE).toInt()
        val total = sweeps.last().start + len + (GAP_S * RATE).toInt()
        say("Starting the recordings...")
        su("rm -rf $DUMPS; resetprop vendor.streamout.pcm.dump 1; resetprop vendor.streamin.pcm.dump 1; " +
            "resetprop vendor.streamout.dsp.dump 1")
        am.setParameters("vendor.streamout.pcm.dump=1")
        am.setParameters("vendor.streamin.pcm.dump=1")

        val raw = am.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        val src = if (raw) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.VOICE_RECOGNITION
        notes.append("mic source: ${if (raw) "UNPROCESSED" else "VOICE_RECOGNITION"}\n")
        val minBuf = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        @Suppress("MissingPermission")
        val rec = AudioRecord(src, RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, max(minBuf, RATE))
        val mic = ShortArray((total + RATE * 3) * 2)
        var micN = 0
        var playFrame = 0
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
        val sweepData = FloatArray(len) { sweep(it, len).toFloat() }
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
            Thread.sleep(1000)
            playFrame = micN / 2
            track.play()
            val chunk = FloatArray(2400 * 2)
            var pos = 0
            var si = 0
            while (pos < total) {
                java.util.Arrays.fill(chunk, 0f)
                val n = min(2400, total - pos)
                for (i in 0 until n) {
                    val t = pos + i
                    while (si < sweeps.size && sweeps[si].start + len <= t) si++
                    val s = sweeps.getOrNull(si) ?: continue
                    val k = t - s.start
                    if (k in 0 until len) {
                        chunk[i * 2 + s.ch] = (Math.pow(10.0, s.db / 20) * sweepData[k]).toFloat()
                    }
                }
                track.write(chunk, 0, n * 2, AudioTrack.WRITE_BLOCKING)
                pos += n
                if (pos % (RATE * 2) < 2400) {
                    val s = sweeps.getOrNull(si)
                    say("Playing, do not touch the phone.\n\n" +
                        "${if (s?.ch == 0) "Earpiece" else "Speaker"}, ${s?.db?.toInt()} dBFS\n\n${100 * pos / total}%")
                }
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
            dumpsOff(am)
        }
        su("cp -r $DUMPS/. '${work.path}/' 2>/dev/null; chmod -R a+rX '${work.path}'")
        writeWav(File(work, "mic.wav"), mic, micN, 2)
        // What was played, sample for sample, so the analysis needs nothing else.
        File(work, "plan.txt").writeText(buildString {
            append("rate=$RATE f1=$F1 f2=$F2 sweep_s=$SWEEP_S gap_s=$GAP_S fade_s=$FADE_S\n")
            append("mic_play_frame=$playFrame (the microphone frame at which playback started)\n")
            append("start_frame,channel(0=earpiece 1=speaker),level_dbfs\n")
            for (s in sweeps) append("${s.start},${s.ch},${s.db}\n")
        })
        notes.append("files: ${work.list()?.sorted()?.joinToString(" ")}\n")
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
