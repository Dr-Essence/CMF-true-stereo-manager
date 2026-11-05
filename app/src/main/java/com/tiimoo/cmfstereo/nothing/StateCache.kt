package com.tiimoo.cmfstereo.nothing

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.tiimoo.cmfstereo.StereoCtl
import com.tiimoo.cmfstereo.StereoLevels

/**
 * The last state the app read from the module, kept so Nothing Settings (its
 * Sound row and the Stereo speakers page) can show it at once, without a root
 * call per look (each one is a Magisk "granted" toast and a short wait).
 *
 * Every StereoCtl.status() publishes here; when the state differs from the
 * stored one, content://<app>.settingstile/state (the page) and the Sound row's
 * summary uri are notified, the way Settings' own pages learn of changes.
 * Changes made outside the app (stereoctl, Magisk's Action button, the
 * module's own switch back to mono) reach it through the provider's
 * stateChanged call, which the module makes after each change.
 */
object StateCache {
    private const val PREFS = "settings_state"

    fun stateUri(ctx: Context): Uri = Uri.parse("content://${ctx.packageName}.settingstile/state")
    private fun summaryUri(ctx: Context): Uri =
        Uri.parse("content://${ctx.packageName}.settingstile/getDynamicSummary/cmf_stereo")

    fun toBundle(s: StereoCtl.Status) = Bundle().apply {
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

    /** Stores a fresh reading; notifies Settings when it changed anything. */
    fun publish(ctx: Context, s: StereoCtl.Status) {
        val b = toBundle(s)
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = p.getString("state", null)
        val now = encode(b)
        p.edit().putString("state", now).putLong("at", System.currentTimeMillis()).apply()
        if (old != now) {
            ctx.contentResolver.notifyChange(stateUri(ctx), null)
            ctx.contentResolver.notifyChange(summaryUri(ctx), null)
        }
    }

    /** The stored state, or null before the first reading. */
    fun cached(ctx: Context): Bundle? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("state", null)?.let(::decode)

    fun ageMs(ctx: Context): Long =
        System.currentTimeMillis() - ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("at", 0)

    /** Reads the module (root) now; publish() runs inside status(). */
    fun refresh(): Bundle = toBundle(StereoCtl.status())

    fun refreshAsync() = Thread({ try { StereoCtl.status() } catch (_: Throwable) {} }, "cmf-state").start()

    // One line per value: "k\tt\tv" (t: b boolean, i int, s string).
    private fun encode(b: Bundle) = b.keySet().sorted().joinToString("\n") { k ->
        when (val v = b.get(k)) {
            is Boolean -> "$k\tb\t$v"
            is Int -> "$k\ti\t$v"
            else -> "$k\ts\t${v ?: ""}"
        }
    }

    private fun decode(t: String) = Bundle().apply {
        t.lineSequence().forEach { line ->
            val f = line.split('\t', limit = 3)
            if (f.size == 3) when (f[1]) {
                "b" -> putBoolean(f[0], f[2] == "true")
                "i" -> putInt(f[0], f[2].toIntOrNull() ?: 0)
                else -> putString(f[0], f[2])
            }
        }
    }
}
