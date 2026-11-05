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
        val ctx = context ?: return null
        // The module tells the app of changes made outside it (stereoctl, the
        // Action button, its own switch back to mono): a fresh reading follows.
        if (method == "stateChanged") { StateCache.refreshAsync(); return Bundle() }
        val caller = callingPackage
        if (caller != SETTINGS && caller != ctx.packageName) return null
        return try {
            when (method) {
                "getDynamicSummary" -> Bundle().apply {
                    putString("com.android.settings.summary", summary(ctx, state(ctx)))
                }
                // The Nothing Settings page (SettingsPage): its whole state, and one change.
                "getPageState" -> state(ctx)
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
                    StateCache.refresh().apply { putString("out", out.lineSequence().firstOrNull()?.take(160)) }
                }
                else -> null
            }
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * The stored state at once (no root); read fresh when there is none yet, or
     * in the background when it is older than five minutes (a change nobody
     * announced).
     */
    private fun state(ctx: android.content.Context): Bundle {
        val c = StateCache.cached(ctx) ?: return StateCache.refresh()
        if (StateCache.ageMs(ctx) > 5 * 60_000) StateCache.refreshAsync()
        return c
    }

    private fun summary(ctx: android.content.Context, b: Bundle): String = when {
        !b.getBoolean("rooted") -> ctx.getString(R.string.tile_no_root)
        !b.getBoolean("installed") -> ctx.getString(R.string.tile_no_module)
        !b.getBoolean("armed") -> ctx.getString(R.string.tile_off)
        b.getString("channels") != b.getString("staged") -> ctx.getString(R.string.tile_after_restart, b.getString("staged"))
        b.getString("channels") == "stereo" -> ctx.getString(R.string.tile_stereo)
        else -> ctx.getString(R.string.tile_mono)
    }

    override fun onCreate(): Boolean {
        StereoCtl.appContext = context?.applicationContext
        return true
    }
    override fun query(u: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0

    private companion object {
        const val SETTINGS = "com.android.settings"
    }
}
