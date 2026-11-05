package com.tiimoo.cmfstereo.modetest

import android.app.Activity
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.Gravity
import android.view.Surface
import android.widget.TextView
import kotlin.math.PI
import kotlin.math.sin

/**
 * Plays a beep on the left channel only (the right channel is silent) while on
 * screen, and shows the tuning Dirac reports and the screen rotation the audio
 * system is told.
 */
/** How the beep reaches Android: the way each kind of app plays. */
enum class Kind { PCM16, FLOAT, AAUDIO, AAUDIO_SHARED, OFFLOAD }

/** The AAudio player (native: src/main/jni/aaudio_beep.c). */
object AAudioBeep {
    init { System.loadLibrary("cmfaaudio") }
    @JvmStatic external fun start(exclusive: Boolean): String
    @JvmStatic external fun stop()
}

abstract class TestActivity(
    private val mode: String,
    private val withEffect: Boolean = false,
    private val kind: Kind = Kind.PCM16
) : Activity() {
    @Volatile private var playing = false
    /** What the player was given (format, path), and which output Android chose. */
    @Volatile private var how = ""
    @Volatile private var route = "looking..."
    @Volatile private var dirac = "asking for root..."
    @Volatile private var halRot = "?"
    private var watcher: java.lang.Process? = null
    private var player: Thread? = null
    private lateinit var label: TextView
    private val ui = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        label = TextView(this).apply {
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
            textSize = 22f
            gravity = Gravity.CENTER
        }
        setContentView(label)
    }

    override fun onResume() {
        super.onResume()
        playing = true
        player = Thread(::play, "beep").also { it.start() }
        startWatcher()
        ui.post(refresh)
        // FULL CHECK: play for a set time, then go back to the main screen.
        val auto = intent.getLongExtra("autoMs", 0L)
        if (auto > 0) ui.postDelayed({ finish() }, auto)
    }

    override fun onPause() {
        playing = false
        player?.join(1000)
        player = null
        watcher?.destroy()
        watcher = null
        ui.removeCallbacks(refresh)
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Each test has its own process; end it so the next test starts clean.
        Process.killProcess(Process.myPid())
    }

    private val refresh = object : Runnable {
        override fun run() {
            @Suppress("DEPRECATION")
            val rot = when (windowManager.defaultDisplay.rotation) {
                Surface.ROTATION_90 -> "90 (earpiece on your left)"
                Surface.ROTATION_270 -> "270 (earpiece on your right)"
                Surface.ROTATION_180 -> "180"
                else -> "0 (upright)"
            }
            label.text = "$mode TEST\n\nLEFT channel only\n\n" +
                "Dirac reports: $dirac\n" +
                "Screen rotation: $rot\n" +
                "Module reads rotation: $halRot\n" +
                (if (how.isNotEmpty()) "$how\n" else "") +
                "Android plays it through: $route\n\n" +
                "The beep must come from the side on your LEFT.\nBack to stop."
            ui.postDelayed(this, 1000)
        }
    }

    /**
     * Dirac's tuning and the rotation the module stored, once a second. Apps may
     * not read these properties, so one root shell reads them (one grant for the
     * whole app: every test process has the same uid).
     */
    private fun startWatcher() {
        Thread({
            try {
                // During a FULL CHECK each reading is also logged with the time, so
                // the HAL's recordings (named by time) can be matched to the mode.
                val log = if (intent.getLongExtra("autoMs", 0L) > 0)
                    "echo \"$(date +%T) $mode $(getprop persist.vendor.newdirac.cur.config) " +
                        "rot=$(getprop vendor.audiohal.cmf_rotation)\" >> /sdcard/cmf-check.txt; " else ""
                // Which output thread plays this process's track: the thread's
                // name, output flags and the format it hands the HAL.
                val pid = Process.myPid()
                val routeCmd = "dumpsys media.audio_flinger 2>/dev/null | awk -v p=' $pid/' '" +
                    "/^[A-Za-z]+ thread 0x/ { n = \$0; sub(/.*name /, \"\", n); sub(/,.*/, \"\", n); " +
                    "t = \$0; sub(/.*type [0-9]+ /, \"\", t); sub(/:\$/, \"\", t); fl = \"\"; hf = \"\" } " +
                    "/^  AudioStreamOut: / { for (i = 1; i < NF; i++) if (\$i == \"flags\") fl = \$(i + 1) } " +
                    "/^  HAL format: / { hf = \$3 } " +
                    "/ Tracks of which / { tr = 1 } / Effect Chains\$/ { tr = 0 } " +
                    "index(\$0, p) && n != \"\" { w = n \" \" t \", flags \" fl \", HAL format \" hf; " +
                    "if (tr && (\$2 == \"yes\" || \$3 == \"yes\")) { print \"ROUTE \" w; done = 1; exit } " +
                    "if (old == \"\") old = w } " +
                    "END { if (!done && old != \"\") print \"ROUTE \" old \" (from its log)\" }'"
                val p = ProcessBuilder("su", "-c",
                    "i=0; while true; do $log echo \"$(getprop persist.vendor.newdirac.cur.config) " +
                        "$(getprop vendor.audiohal.cmf_rotation)\"; " +
                        "if [ $((i % 5)) -eq 1 ]; then r=$($routeCmd); echo \"\${r:-ROUTE not in the mixer dump}\"; fi; " +
                        "i=$((i + 1)); sleep 1; done")
                    .redirectErrorStream(true).start()
                watcher = p
                p.inputStream.bufferedReader().forEachLine { line ->
                    if (line.startsWith("ROUTE ")) { route = line.removePrefix("ROUTE "); return@forEachLine }
                    val parts = line.trim().split(" ")
                    dirac = parts.getOrNull(0)?.ifEmpty { null } ?: "(empty)"
                    halRot = parts.getOrNull(1) ?: "?"
                }
                if (dirac.startsWith("asking")) dirac = "(no root)"
            } catch (e: Exception) {
                dirac = "(no root)"
            }
        }, "props").start()
    }

    private fun play() {
        try {
            when (kind) {
                Kind.PCM16, Kind.FLOAT -> playTrack()
                Kind.AAUDIO, Kind.AAUDIO_SHARED -> {
                    how = AAudioBeep.start(kind == Kind.AAUDIO)
                    while (playing) Thread.sleep(50)
                    AAudioBeep.stop()
                }
                Kind.OFFLOAD -> playOffload()
            }
        } catch (e: Throwable) {
            how = "Could not play: $e"
        }
    }

    /**
     * Compressed audio handed to the audio DSP as it is (offload), the way
     * music players save battery: an MP3 of the same left-only beep (right
     * channel silent, no joint stereo), written over and over.
     */
    private fun playOffload() {
        if (android.os.Build.VERSION.SDK_INT < 29) { how = "Offload needs Android 10"; return }
        val mp3 = assets.open("left_only.mp3").use { it.readBytes() }
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(48000)
            .setEncoding(AudioFormat.ENCODING_MP3)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        @Suppress("DEPRECATION")
        if (!android.media.AudioManager.isOffloadedPlaybackSupported(format, attrs)) {
            how = "Compressed: this phone refuses MP3 offload"
            return
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(format)
            .setBufferSizeInBytes(64 * 1024)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setOffloadedPlayback(true)
            .build()
        how = "Compressed MP3, offloaded: " + (if (track.isOffloadedPlayback) "YES" else "no")
        try {
            track.play()
            var off = 0
            while (playing) {
                val n = track.write(mp3, off, minOf(8192, mp3.size - off))
                if (n < 0) break
                off += n
                if (off >= mp3.size) off = 0
            }
            track.pause()
            track.flush()
            track.stop()
        } finally {
            track.release()
        }
    }

    /** 0.3 s of 880 Hz on the left, 0.3 s of silence, over and over. */
    private fun playTrack() {
        val rate = 48000
        val frames = rate * 6 / 10
        val buf = ShortArray(frames * 2)
        val on = rate * 3 / 10
        for (i in 0 until on) {
            val fade = minOf(1.0, minOf(i, on - i) / 480.0)
            buf[i * 2] = (sin(2 * PI * 880 * i / rate) * 12000 * fade).toInt().toShort()
            // buf[i * 2 + 1] stays 0: the right channel is silent.
        }
        val float = kind == Kind.FLOAT
        val fbuf = if (float) FloatArray(buf.size) { buf[it] / 32768f } else null
        if (float) how = "Float samples (32-bit floating point)"
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(rate)
                    .setEncoding(if (float) AudioFormat.ENCODING_PCM_FLOAT else AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(buf.size * if (float) 4 else 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        // VLC attaches a DynamicsProcessing effect to its own session (seen in
        // dumpsys media.audio_flinger); this does the same, left neutral.
        val fx = if (withEffect && android.os.Build.VERSION.SDK_INT >= 28) try {
            android.media.audiofx.DynamicsProcessing(0, track.audioSessionId, null).apply { enabled = true }
        } catch (e: Throwable) { null } else null
        try {
            track.play()
            if (fbuf != null) while (playing) track.write(fbuf, 0, fbuf.size, AudioTrack.WRITE_BLOCKING)
            else while (playing) track.write(buf, 0, buf.size)
            track.stop()
        } finally {
            fx?.release()
            track.release()
        }
    }
}

class MusicTest : TestActivity("MUSIC")
class MovieTest : TestActivity("MOVIE")
class MovieEffectTest : TestActivity("MOVIE + EFFECT", withEffect = true)
class GameTest : TestActivity("GAME")
class SpeechTest : TestActivity("SPEECH")
class AAudioTest : TestActivity("AAUDIO (GAME, LOW LATENCY)", kind = Kind.AAUDIO)
class AAudioSharedTest : TestActivity("AAUDIO SHARED", kind = Kind.AAUDIO_SHARED)
class FloatTest : TestActivity("FLOAT", kind = Kind.FLOAT)
class OffloadTest : TestActivity("COMPRESSED (OFFLOAD)", kind = Kind.OFFLOAD)
