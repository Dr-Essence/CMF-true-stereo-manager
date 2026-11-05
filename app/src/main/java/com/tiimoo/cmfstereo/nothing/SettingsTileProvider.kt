package com.tiimoo.cmfstereo.nothing

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.tiimoo.cmfstereo.R
import com.tiimoo.cmfstereo.StereoCtl

/**
 * Answers Nothing Settings for the "Stereo speakers" tile in Sound (see
 * SettingsHook): the live summary, the switch's state, and switching. Settings
 * calls these on a background thread (com.android.settings.dashboard, read
 * from Settings 16.0.0), so a switch that restarts the audio HAL for a few
 * seconds never holds up its screen. Only Settings and this app may call it.
 *
 * The protocol, as Settings uses it: call(method, uri, extras) with method
 * getDynamicSummary (answer: "com.android.settings.summary"), isChecked
 * ("checked_state") or onCheckedChanged (extras "checked_state"; answer
 * "set_checked_error" and "set_checked_error_message" when it fails, and
 * Settings turns the switch back).
 */
class SettingsTileProvider : ContentProvider() {
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val caller = callingPackage
        if (caller != SETTINGS && caller != context?.packageName) return null
        val ctx = context ?: return null
        return try {
            when (method) {
                "getDynamicSummary" -> Bundle().apply {
                    putString("com.android.settings.summary", summary(ctx, StereoCtl.status()))
                }
                "isChecked" -> Bundle().apply {
                    putBoolean("checked_state", StereoCtl.status().channelsStaged == "stereo")
                }
                "onCheckedChanged" -> {
                    val want = extras?.getBoolean("checked_state") ?: return null
                    val out = StereoCtl.setChannels(want)
                    val s = StereoCtl.status()
                    ctx.contentResolver.notifyChange(Uri.parse("content://$authority/getDynamicSummary/$KEY"), null)
                    Bundle().apply {
                        if ((s.channelsStaged == "stereo") != want) {
                            putBoolean("set_checked_error", true)
                            putString("set_checked_error_message", out.lineSequence().firstOrNull()?.take(120))
                        }
                    }
                }
                else -> null
            }
        } catch (t: Throwable) {
            null
        }
    }

    private val authority get() = "${context?.packageName}.settingstile"

    private fun summary(ctx: android.content.Context, s: StereoCtl.Status): String = when {
        !s.rooted -> ctx.getString(R.string.tile_no_root)
        !s.installed -> ctx.getString(R.string.tile_no_module)
        !s.armed -> ctx.getString(R.string.tile_off)
        s.channels != s.channelsStaged -> ctx.getString(R.string.tile_after_restart, s.channelsStaged)
        s.channels == "stereo" -> ctx.getString(R.string.tile_stereo)
        else -> ctx.getString(R.string.tile_mono)
    }

    override fun onCreate() = true
    override fun query(u: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0

    private companion object {
        const val SETTINGS = "com.android.settings"
        const val KEY = "cmf_stereo"
    }
}
