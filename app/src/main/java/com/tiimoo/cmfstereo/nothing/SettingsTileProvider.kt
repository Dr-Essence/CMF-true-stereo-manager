package com.tiimoo.cmfstereo.nothing

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.tiimoo.cmfstereo.R
import com.tiimoo.cmfstereo.StereoCtl
import com.tiimoo.cmfstereo.StereoLevels

/**
 * Answers Nothing Settings for "Stereo speakers": the tile's live summary in
 * Sound (Settings' protocol: call("getDynamicSummary"), answered in
 * "com.android.settings.summary", on a background thread, read from Settings
 * 16.0.0), and the state and changes of the Stereo page inside Settings
 * (SettingsPage: getPageState, setPage), which runs from Settings' process and
 * has no root of its own. Only Settings and this app may call it.
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
                // The Nothing Settings page (SettingsPage): its whole state, and one change.
                "getPageState" -> pageState(StereoCtl.status())
                "setPage" -> {
                    val what = extras?.getString("what") ?: return null
                    val value = extras.getString("value") ?: return null
                    val out = when (what) {
                        "armed" -> StereoCtl.setArmed(value == "on")
                        "channels" -> StereoCtl.setChannels(value == "stereo")
                        "earLevel" -> StereoCtl.setGain(StereoLevels.handsetVolume(value.toIntOrNull() ?: 10))
                        "spkLevel" -> StereoCtl.setStereoAtten(StereoLevels.attenDb(value.toIntOrNull() ?: 10))
                        "boost" -> StereoCtl.setSpeakerBoost(value.toIntOrNull() ?: 0)
                        "bass" -> StereoCtl.setBassProtect(value)
                        "leveller" -> StereoCtl.setEarpieceLeveller(value)
                        "limit" -> StereoCtl.setEarpieceLimit(value == "on")
                        else -> return null
                    }
                    ctx.contentResolver.notifyChange(Uri.parse("content://$authority/getDynamicSummary/$KEY"), null)
                    pageState(StereoCtl.status()).apply { putString("out", out.lineSequence().firstOrNull()?.take(160)) }
                }
                else -> null
            }
        } catch (t: Throwable) {
            null
        }
    }

    private fun pageState(s: StereoCtl.Status) = Bundle().apply {
        putBoolean("rooted", s.rooted)
        putBoolean("installed", s.installed)
        putBoolean("armed", s.armed)
        putString("channels", s.channels)
        putString("staged", s.channelsStaged)
        putString("build", s.build)
        putInt("earLevel", StereoLevels.earpieceLevel(s.gain) ?: -1)
        putInt("spkLevel", StereoLevels.speakerLevel(s.stereoAttenDb))
        putBoolean("supportsProtect", s.supportsProtect)
        putBoolean("protRunning", s.protRunning)
        putInt("boost", s.speakerBoost)
        putString("bass", s.bassProtect)
        putBoolean("supportsLeveller", s.supportsLeveller)
        putString("leveller", s.earpieceLeveller)
        putBoolean("limit", s.earpieceLimit)
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
