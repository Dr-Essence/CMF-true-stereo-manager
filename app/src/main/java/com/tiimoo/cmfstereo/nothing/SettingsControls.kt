package com.tiimoo.cmfstereo.nothing

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

/**
 * The controls of the Stereo speakers page inside Nothing Settings, the same
 * two the app has: a level as a row of dots, lit up to the level, and a choice
 * as a row of pills. They sit under a Nothing row's summary and take its font
 * and Settings' own colours (passed in by SettingsPage, read from the row);
 * they are made in code, as Settings has no resources of this app.
 */
internal object SettingsControls {
    /** Colours and type of the row the control sits in. */
    class Look(val lit: Int, val line: Int, val text: Int, val font: Typeface?, val textPx: Float)

    /** Levels [min]..[max] as dots; dots up to [value] are lit (none at 0). A tap on a dot picks it. */
    @SuppressLint("ViewConstructor")
    class Dots(
        ctx: Context, private val min: Int, private val max: Int, private val value: Int?,
        private val look: Look, private val on: Boolean, private val pick: (Int) -> Unit
    ) : View(ctx) {
        private val d = resources.displayMetrics.density
        private val size = 24 * d
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = look.lit }
        private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = d; color = look.line }
        private val count = max - min + 1

        init {
            alpha = if (on) 1f else 0.38f
            isClickable = on
            contentDescription = value?.toString()
        }

        override fun onMeasure(w: Int, h: Int) =
            setMeasuredDimension(MeasureSpec.getSize(w), (size + 2 * d).roundToInt())

        private fun step() = if (count > 1) (width - size) / (count - 1) else 0f

        override fun onDraw(c: Canvas) {
            val r = size / 2
            for (k in 0 until count) {
                val i = min + k
                val lit = value != null && i <= value && !(i == 0 && value == 0)
                val cx = r + k * step()
                if (lit) c.drawCircle(cx, height / 2f, r, fill) else c.drawCircle(cx, height / 2f, r - d / 2, ring)
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(e: MotionEvent): Boolean {
            if (!on || value == null) return false
            if (e.action == MotionEvent.ACTION_UP) {
                val k = if (count > 1) ((e.x - size / 2) / step()).roundToInt().coerceIn(0, count - 1) else 0
                if (min + k != value) pick(min + k)
                performClick()
            }
            return true
        }
    }

    /** A choice of [labels] as pills; the one at [selected] is filled. A tap on another picks it. */
    fun pills(
        ctx: Context, labels: List<String>, selected: Int?, look: Look, on: Boolean, pick: (Int) -> Unit
    ): View = LinearLayout(ctx).apply {
        val d = resources.displayMetrics.density
        orientation = LinearLayout.HORIZONTAL
        alpha = if (on) 1f else 0.38f
        labels.forEachIndexed { k, label ->
            val sel = k == selected
            addView(TextView(ctx).apply {
                text = label
                gravity = Gravity.CENTER
                maxLines = 1
                look.font?.let { typeface = it }
                setTextSize(TypedValue.COMPLEX_UNIT_PX, look.textPx)
                setTextColor(if (sel) Color.BLACK else look.text)
                setPadding(0, (10 * d).roundToInt(), 0, (10 * d).roundToInt())
                background = GradientDrawable().apply {
                    cornerRadius = 100 * d
                    if (sel) setColor(look.lit) else setStroke(d.roundToInt().coerceAtLeast(1), look.line)
                }
                isEnabled = on
                if (on && selected != null && !sel) setOnClickListener { pick(k) }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (k > 0) marginStart = (6 * d).roundToInt()
            })
        }
    }
}
