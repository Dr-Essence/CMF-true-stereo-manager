package com.tiimoo.cmfstereo.modetest

import android.app.Activity
import android.content.Intent
import android.media.AudioManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

private fun su(cmd: String): String = try {
    val p = ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start()
    p.inputStream.bufferedReader().readText().trim().also { p.waitFor() }
} catch (e: Exception) {
    "no root"
}

/** Four buttons, one per Dirac speaker tuning. */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
            setBackgroundColor(Color.BLACK)
        }
        col.addView(TextView(this).apply {
            text = "CMF L/R TEST\n\nEach button plays a beep on the LEFT channel only, in one of " +
                "Dirac's tunings. Turn the phone sideways both ways: the beep must always come " +
                "from the side that is on your left (earpiece on your left: earpiece; earpiece " +
                "on your right: bottom speaker).\n\nStereo must be on in CMF Stereo."
            setTextColor(Color.WHITE)
            textSize = 16f
        })
        listOf(
            "MUSIC" to MusicTest::class.java,
            "MOVIE" to MovieTest::class.java,
            "GAME" to GameTest::class.java,
            "SPEECH" to SpeechTest::class.java
        ).forEach { (name, cls) ->
            col.addView(Button(this).apply {
                text = name
                textSize = 20f
                gravity = Gravity.CENTER
                setOnClickListener { startActivity(Intent(this@MainActivity, cls)) }
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 32 })
        }
        // The audio HAL records what it receives (vendor.streamout.pcm.dump) once
        // that property is set and the same key reaches it through
        // setParameters (AudioALSAHardware::setParameters -> dynamicSetAudioDump).
        val status = TextView(this).apply { setTextColor(Color.WHITE); textSize = 14f }
        col.addView(Button(this).apply {
            text = "DRIVER RECORDING: OFF"
            textSize = 16f
            setOnClickListener {
                val on = text.endsWith("OFF")
                isEnabled = false
                Thread {
                    val am = getSystemService(AudioManager::class.java)
                    val msg = if (on) {
                        val r = su("rm -rf /data/vendor/audiohal/audio_dump; " +
                            "resetprop vendor.streamout.pcm.dump 1 && resetprop vendor.streamout.dsp.dump 1 && echo ok")
                        am.setParameters("vendor.streamout.pcm.dump=1")
                        if (r.contains("ok")) "Recording. Play a test now, then turn this off." else "Root refused: $r"
                    } else {
                        am.setParameters("vendor.streamout.pcm.dump=0")
                        su("resetprop vendor.streamout.pcm.dump 0; resetprop vendor.streamout.dsp.dump 0; " +
                            "ls -laR /data/vendor/audiohal/audio_dump > /sdcard/cmf-driver-dump.txt 2>&1; " +
                            "tar czf /sdcard/cmf-driver-dump.tgz -C /data/vendor/audiohal audio_dump 2>&1; " +
                            "ls -la /sdcard/cmf-driver-dump.tgz")
                    }
                    runOnUiThread {
                        text = if (on) "DRIVER RECORDING: ON" else "DRIVER RECORDING: OFF"
                        isEnabled = true
                        status.text = if (on) msg else "Saved: $msg\nSend cmf-driver-dump.tgz and cmf-driver-dump.txt."
                    }
                }.start()
            }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 64 })
        col.addView(status)

        // FULL CHECK: record what the HAL receives while each of the four tests
        // plays for 6 s, one after another, then pack the recordings with a log
        // of the mode and rotation each second.
        val tests = listOf(MusicTest::class.java, MovieTest::class.java, GameTest::class.java, SpeechTest::class.java)
        col.addView(Button(this).apply {
            text = "FULL CHECK (about 40 s, hold the phone still)"
            textSize = 16f
            setOnClickListener {
                isEnabled = false
                status.text = "Running: MUSIC, MOVIE, GAME, SPEECH... keep the phone still."
                Thread {
                    val am = getSystemService(AudioManager::class.java)
                    val r = su("rm -rf /data/vendor/audiohal/audio_dump; " +
                        "resetprop vendor.streamout.pcm.dump 1 && resetprop vendor.streamout.dsp.dump 1 && " +
                        "echo \"start $(date +%T) rot=$(getprop vendor.audiohal.cmf_rotation)\" > /sdcard/cmf-check.txt && echo ok")
                    am.setParameters("vendor.streamout.pcm.dump=1")
                    if (r.contains("ok")) {
                        for (cls in tests) {
                            runOnUiThread { startActivity(Intent(this@MainActivity, cls).putExtra("autoMs", 6000L)) }
                            Thread.sleep(8500)
                        }
                    }
                    am.setParameters("vendor.streamout.pcm.dump=0")
                    val out = su("resetprop vendor.streamout.pcm.dump 0; resetprop vendor.streamout.dsp.dump 0; " +
                        "R=\$(getprop vendor.audiohal.cmf_rotation); " +
                        "cp /sdcard/cmf-check.txt /data/vendor/audiohal/audio_dump/ 2>/dev/null; " +
                        "tar czf /sdcard/cmf-full-check-rot\$R.tgz -C /data/vendor/audiohal audio_dump 2>&1; " +
                        "ls /sdcard/cmf-full-check-rot\$R.tgz")
                    runOnUiThread {
                        isEnabled = true
                        status.text = if (r.contains("ok")) "Saved: $out\nTurn the phone over and run it again, then send both files."
                            else "Root refused: $r"
                    }
                }.start()
            }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 32 })
        setContentView(ScrollView(this).apply { addView(col) })
    }
}
