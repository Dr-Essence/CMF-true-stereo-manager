package com.tiimoo.cmfstereo.modetest

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max

/**
 * RECORD ONLY: this device's microphone, recorded as it is, for listening to the CMF Phone 1's
 * RESPONSE STUDY from a second device (Dr.Essence's S22 Ultra and Legion Tab). Each device's
 * microphone colours the sound its own way while the speakers' sound is the same in every
 * recording, so where the recordings agree is the speakers and where they differ is a
 * microphone. Needs no root and changes nothing on the device.
 *
 * Recorded with the least processing the device offers: UNPROCESSED (no noise suppression, no
 * automatic gain) where the device reports it, else VOICE_RECOGNITION; two channels if it gives
 * them; 48 kHz, 32-bit float. Saved to Download as cmf-record-<device>-<time>.wav, with a
 * matching .txt saying how it was recorded. The analysis finds the sweeps in it by their shape,
 * so it does not need to start at the same moment as the CMF.
 */
class RecordOnlyActivity : Activity() {

    companion object {
        private const val RATE = 48000
        /** Stops by itself after this, so a forgotten recording does not fill the storage. */
        private const val MAX_S = 300
    }

    private lateinit var label: TextView
    private lateinit var go: Button
    @Volatile private var recording = false
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
            text = "RECORD ONLY\n\nThis device listens; the CMF Phone 1 plays.\n\n" +
                "1. Put this device about 10 cm in front of the CMF's screen, microphone towards it, " +
                "centred between the earpiece and the bottom speaker.\n" +
                "2. Press START here (5 s to step back).\n" +
                "3. Start RESPONSE STUDY on the CMF.\n" +
                "4. When the CMF says done, press STOP here.\n\n" +
                "Saved in Download as cmf-record-${model()}-<time>.wav."
        }
        col.addView(label)
        go = Button(this).apply {
            text = "START"
            textSize = 20f
            setOnClickListener { if (recording) recording = false else start() }
        }
        col.addView(go, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 48 })
        setContentView(ScrollView(this).apply { addView(col) })
    }

    override fun onDestroy() {
        recording = false
        super.onDestroy()
    }

    private fun model() = "${Build.MANUFACTURER}-${Build.MODEL}".replace(Regex("[^A-Za-z0-9-]"), "")

    private fun say(s: String) = runOnUiThread { label.text = s }

    private fun start() {
        if (busy) return
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            return
        }
        busy = true
        go.isEnabled = false
        Thread({
            if (Countdown.wait(::say)) record()
            busy = false
            runOnUiThread { go.isEnabled = true; go.text = "START" }
        }, "record").start()
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, res: IntArray) {
        if (res.firstOrNull() == PackageManager.PERMISSION_GRANTED) start()
        else label.text = "The microphone permission is needed to record."
    }

    private fun record() {
        val am = getSystemService(AudioManager::class.java)
        val raw = am.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        val src = if (raw) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.VOICE_RECOGNITION
        // Two channels if the device gives them, else one.
        var channels = 2
        var rec = open(src, AudioFormat.CHANNEL_IN_STEREO)
        if (rec == null) { channels = 1; rec = open(src, AudioFormat.CHANNEL_IN_MONO) }
        if (rec == null) { say("This device refused to record (no microphone could be opened)."); return }
        val pcm = ByteArrayOutputStream()
        val buf = FloatArray(4800 * channels)
        val bytes = ByteBuffer.allocate(buf.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        var frames = 0L
        recording = true
        runOnUiThread { go.isEnabled = true; go.text = "STOP" }
        try {
            rec.startRecording()
            var peak = 0f
            var shown = 0L
            while (recording && frames < MAX_S.toLong() * RATE) {
                val n = rec.read(buf, 0, buf.size, AudioRecord.READ_BLOCKING)
                if (n <= 0) continue
                bytes.clear()
                for (i in 0 until n) { bytes.putFloat(buf[i]); peak = max(peak, abs(buf[i])) }
                pcm.write(bytes.array(), 0, n * 4)
                frames += n / channels
                if (frames - shown >= RATE / 2) {
                    shown = frames
                    val db = if (peak > 0) 20 * log10(peak) else -120f
                    say("RECORDING (${if (raw) "UNPROCESSED" else "VOICE_RECOGNITION"}, $channels ch)\n\n" +
                        "${frames / RATE} s   level ${"%.0f".format(db)} dBFS\n\n" +
                        "Start RESPONSE STUDY on the CMF now. Press STOP when it says done." +
                        if (db > -1) "\n\nToo loud: move this device a little further away." else "")
                    peak = 0f
                }
            }
            rec.stop()
        } finally {
            rec.release()
            recording = false
        }
        say("Saving...")
        val name = "cmf-record-${model()}-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}"
        val notes = "device=${Build.MANUFACTURER} ${Build.MODEL} android=${Build.VERSION.RELEASE}\n" +
            "source=${if (raw) "UNPROCESSED" else "VOICE_RECOGNITION"} channels=$channels rate=$RATE " +
            "format=float32 seconds=${"%.1f".format(frames.toDouble() / RATE)}\n"
        val where = try {
            save("$name.wav", "audio/wav") { wav(it, pcm.toByteArray(), channels) } +
                "\n" + save("$name.txt", "text/plain") { it.write(notes.toByteArray()) }
        } catch (t: Throwable) {
            "Could not save: $t"
        }
        say("Done: ${"%.0f".format(frames.toDouble() / RATE)} s recorded.\n\n$where\n\n" +
            "Send the .wav (and the .txt) with the CMF's cmf-sweep.tgz.")
    }

    private fun open(src: Int, mask: Int): AudioRecord? = try {
        val min = AudioRecord.getMinBufferSize(RATE, mask, AudioFormat.ENCODING_PCM_FLOAT)
        @Suppress("MissingPermission")
        val r = AudioRecord(src, RATE, mask, AudioFormat.ENCODING_PCM_FLOAT, max(min, RATE * 4))
        if (r.state == AudioRecord.STATE_INITIALIZED) r else { r.release(); null }
    } catch (t: Throwable) {
        null
    }

    /** Saves to Download (MediaStore from Android 10, needing no permission; else the app's folder). */
    private fun save(name: String, mime: String, write: (OutputStream) -> Unit): String {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw java.io.IOException("Download refused the file")
            contentResolver.openOutputStream(uri)!!.use(write)
            return "Download/$name"
        }
        val file = java.io.File(getExternalFilesDir(null), name)
        file.outputStream().use(write)
        return file.path
    }

    /** A WAV of 32-bit float samples (format 3, IEEE float). */
    private fun wav(o: OutputStream, data: ByteArray, ch: Int) {
        val h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        h.put("RIFF".toByteArray()).putInt(36 + data.size).put("WAVEfmt ".toByteArray())
        h.putInt(16).putShort(3).putShort(ch.toShort()).putInt(RATE).putInt(RATE * ch * 4)
            .putShort((ch * 4).toShort()).putShort(32)
        h.put("data".toByteArray()).putInt(data.size)
        o.write(h.array())
        o.write(data)
    }
}
