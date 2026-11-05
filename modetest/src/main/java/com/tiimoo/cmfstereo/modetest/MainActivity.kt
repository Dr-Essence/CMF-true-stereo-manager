package com.tiimoo.cmfstereo.modetest

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

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
        setContentView(ScrollView(this).apply { addView(col) })
    }
}
