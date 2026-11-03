package com.tiimoo.cmfstereo

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.os.Build
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

/**
 * Nothing's NDot, taken from where it already is on the phone - it is Nothing's
 * font and does not ship with this app. Looks in the system font folders first,
 * then in Nothing's widgets app. Preference: NDot 57 JP Expanded, then any
 * NDot 57, then any NDot. Null when the phone has none (not a Nothing phone),
 * and the app then falls back to the system monospace.
 *
 * The font is read into a buffer of its own: a font taken straight from another
 * app's resources reads that app's memory, which can be released while it is
 * still being drawn.
 */
object NdotFont {

    private const val WIDGETS_PKG = "com.nothing.hearthstone"
    private val FONT_DIRS = listOf("/system/fonts", "/product/fonts", "/system_ext/fonts")

    @Volatile private var looked = false
    @Volatile private var cached: Typeface? = null

    fun get(context: Context): Typeface? {
        if (!looked) {
            synchronized(this) {
                if (!looked) {
                    cached = fromSystem() ?: fromWidgets(context)
                    looked = true
                }
            }
        }
        return cached
    }

    /** Higher is better; -1 for anything that is not NDot. */
    private fun score(name: String): Int {
        val n = name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
        if (!n.contains("ndot")) return -1
        var s = 0
        if (n.contains("57")) s += 4
        if (n.contains("jp")) s += 2
        if (n.contains("expanded")) s += 1
        return s
    }

    private fun fromSystem(): Typeface? {
        val best = FONT_DIRS.asSequence()
            .flatMap { (File(it).listFiles() ?: emptyArray()).asSequence() }
            .filter { it.name.endsWith(".ttf", true) || it.name.endsWith(".otf", true) }
            .map { it to score(it.name) }
            .filter { it.second >= 0 }
            .maxByOrNull { it.second }
            ?.first ?: return null
        return runCatching { fromBytes(best.readBytes()) }.getOrNull()
    }

    private fun fromWidgets(context: Context): Typeface? = runCatching {
        val res = context.createPackageContext(WIDGETS_PKG, 0).resources
        val names = listOf(
            "ndot57_jp_expanded", "ndot_57_jp_expanded", "ndot57jp_expanded",
            "ndot57", "ndot_57_aligned", "ndot55", "ndot"
        )
        val id = names.asSequence()
            .map { res.getIdentifier(it, "font", WIDGETS_PKG) }
            .firstOrNull { it != 0 } ?: return@runCatching null
        res.openRawResource(id).use { fromBytes(it.readBytes()) }
    }.getOrNull()

    /** A typeface built from [bytes]; null unless they are a TrueType/OpenType font. */
    private fun fromBytes(bytes: ByteArray): Typeface? {
        if (bytes.size < 1024) return null
        val tag = (bytes[0].toInt() and 0xFF shl 24) or (bytes[1].toInt() and 0xFF shl 16) or
            (bytes[2].toInt() and 0xFF shl 8) or (bytes[3].toInt() and 0xFF)
        if (tag != 0x00010000 && tag != 0x4F54544F && tag != 0x74727565) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
            buffer.put(bytes).flip()
            val font = Font.Builder(buffer).build()
            Typeface.CustomFallbackBuilder(FontFamily.Builder(font).build()).build()
        } else {
            // No in-memory font API before Android 10. Every phone with NDot runs
            // Nothing OS on Android 12 or newer, so older phones get monospace.
            null
        }
    }
}
