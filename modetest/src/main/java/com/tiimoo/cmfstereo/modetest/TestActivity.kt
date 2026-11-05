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
abstract class TestActivity(private val mode: String) : Activity() {
    @Volatile private var playing = false
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
        ui.post(refresh)
    }

    override fun onPause() {
        playing = false
        player?.join(1000)
        player = null
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
                "Dirac reports: ${prop("persist.vendor.newdirac.cur.config")}\n" +
                "Screen rotation: $rot\n\n" +
                "The beep must come from the side on your LEFT.\nBack to stop."
            ui.postDelayed(this, 1000)
        }
    }

    private fun prop(key: String): String = try {
        val p = ProcessBuilder("getprop", key).redirectErrorStream(true).start()
        p.inputStream.bufferedReader().readText().trim().ifEmpty { "(not readable)" }
    } catch (e: Exception) {
        "(not readable)"
    }

    /** 0.3 s of 880 Hz on the left, 0.3 s of silence, over and over. */
    private fun play() {
        val rate = 48000
        val frames = rate * 6 / 10
        val buf = ShortArray(frames * 2)
        val on = rate * 3 / 10
        for (i in 0 until on) {
            val fade = minOf(1.0, minOf(i, on - i) / 480.0)
            buf[i * 2] = (sin(2 * PI * 880 * i / rate) * 12000 * fade).toInt().toShort()
            // buf[i * 2 + 1] stays 0: the right channel is silent.
        }
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
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(buf.size * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        try {
            track.play()
            while (playing) track.write(buf, 0, buf.size)
            track.stop()
        } finally {
            track.release()
        }
    }
}

class MusicTest : TestActivity("MUSIC")
class MovieTest : TestActivity("MOVIE")
class GameTest : TestActivity("GAME")
class SpeechTest : TestActivity("SPEECH")
