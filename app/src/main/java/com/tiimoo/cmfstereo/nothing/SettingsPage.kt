package com.tiimoo.cmfstereo.nothing

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Rect
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.PopupWindow
import android.widget.Spinner
import android.widget.TextView
import com.tiimoo.cmfstereo.BuildConfig
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * The "Stereo speakers" page, inside Nothing Settings (runs in the Settings
 * process, installed by SettingsHook).
 *
 * Settings only opens its own pages, so a tap on the tile is sent to Settings'
 * own SubSettings with a small, allowed Sound page (MediaControlsSettings) as
 * the carrier; once that page has built itself, its rows are replaced with
 * this page's, made from Nothing's own preference widgets
 * (NtCustSwitchPreferenceCompat, NtCustDropDownPreference, PreferenceCategory).
 * Settings skips the carrier's own controllers once their rows are gone
 * (DashboardFragment: "Cannot find preference with key").
 *
 * Settings' androidx.preference is obfuscated: the method names below are the
 * ones in Nothing Settings 16.0.0 (read from its code), and every one is
 * looked up and checked against its signature before anything is hooked. If
 * one is missing (a Settings update), nothing is hooked and the tile opens the
 * app as before.
 *
 * All state and every change go through the app's SettingsTileProvider (the
 * app has root, Settings does not), on a thread of its own; Settings' UI is
 * only touched on its main thread.
 */
internal class SettingsPage private constructor(private val cl: ClassLoader) {
    private val pref = XposedHelpers.findClass("androidx.preference.Preference", cl)
    private val group = XposedHelpers.findClass("androidx.preference.PreferenceGroup", cl)
    private val twoState = XposedHelpers.findClass("androidx.preference.TwoStatePreference", cl)
    private val list = XposedHelpers.findClass("androidx.preference.ListPreference", cl)
    private val category = XposedHelpers.findClass("androidx.preference.PreferenceCategory", cl)
    private val switchCls = XposedHelpers.findClass("com.android.settingslib.ntuisupport.preference.NtCustSwitchPreferenceCompat", cl)
    private val dropCls = XposedHelpers.findClass("com.nothing.ui.support.NtCustDropDownPreference", cl)
    private val dashboard = XposedHelpers.findClass("com.android.settings.dashboard.DashboardFragment", cl)

    private val setKey = m(pref, "V0", String::class.java)
    private val setTitle = m(pref, "l1", CharSequence::class.java)
    private val setSummary = m(pref, "i1", CharSequence::class.java)
    private val setEnabled = m(pref, "O0", Boolean::class.javaPrimitiveType!!)
    private val setPersistent = m(pref, "b1", Boolean::class.javaPrimitiveType!!)
    private val setOrder = m(pref, "a1", Int::class.javaPrimitiveType!!)
    private val getContext = pref.getMethod("B").also { check(it.returnType == Context::class.java) }
    // setSummaryProvider: a theme may give dropdowns an automatic summary, and
    // setSummary throws while one is set ("already has a SummaryProvider").
    private val setSummaryProvider = pref.methods.single { it.name == "j1" && it.parameterTypes.size == 1 }
    private val setOnChange = listenerSetter("Y0", "onPreferenceChange")
    private val setOnClick = listenerSetter("Z0", "onPreferenceClick")
    private val removeAll = m(group, "E1")
    private val add = m(group, "u1", pref)
    private val setChecked = m(twoState, "v1", Boolean::class.javaPrimitiveType!!)
    private val setEntries = m(list, "Q1", Array<CharSequence>::class.java)
    private val setValues = m(list, "R1", Array<CharSequence>::class.java)
    private val setValue = m(list, "S1", String::class.java)
    private val getValue = m(list, "N1").also { check(it.returnType == String::class.java) }
    private val newSwitch = switchCls.getConstructor(Context::class.java)
    private val newDrop = dropCls.getConstructor(Context::class.java)
    private val newCategory = category.getConstructor(Context::class.java)

    private fun m(c: Class<*>, name: String, vararg p: Class<*>): Method = c.getMethod(name, *p)

    /** A listener setter: one parameter, an interface with exactly the named method. */
    private fun listenerSetter(name: String, callback: String): Method {
        val s = pref.methods.single { it.name == name && it.parameterTypes.size == 1 && it.parameterTypes[0].isInterface }
        check(s.parameterTypes[0].methods.any { it.name == callback })
        return s
    }

    private fun listener(iface: Class<*>, body: (Array<Any?>) -> Boolean): Any =
        Proxy.newProxyInstance(cl, arrayOf(iface)) { proxy, method, args ->
            when (method.name) {
                "onPreferenceChange", "onPreferenceClick" -> try { body(args ?: emptyArray()) } catch (t: Throwable) { log(t); false }
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.get(0)
                "toString" -> "CmfStereoListener"
                else -> null
            }
        }

    private fun install() {
        // 1. A tap on the tile opens this page in Settings instead of the app.
        val redirect = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val i = param.args.firstOrNull() as? Intent ?: return
                if (i.component?.packageName != BuildConfig.APPLICATION_ID || i.component?.className != ENTRY) return
                i.component = ComponentName(SETTINGS, "$SETTINGS.SubSettings")
                i.putExtra(":settings:show_fragment", CARRIER)
                i.putExtra(":settings:show_fragment_title", TITLE)
                i.putExtra(MARK, true)
            }
        }
        XposedHelpers.findAndHookMethod(Activity::class.java, "startActivityForResult",
            Intent::class.java, Int::class.javaPrimitiveType, Bundle::class.java, redirect)
        XposedHelpers.findAndHookMethod(Activity::class.java, "startActivityAsUser",
            Intent::class.java, Bundle::class.java, android.os.UserHandle::class.java, redirect)
        // 2. The carrier page, once built, becomes this page.
        XposedHelpers.findAndHookMethod(dashboard, "onCreate", Bundle::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val f = param.thisObject
                if (f.javaClass.name != CARRIER) return
                val act = XposedHelpers.callMethod(f, "getActivity") as? Activity ?: return
                if (!act.intent.getBooleanExtra(MARK, false)) return
                try { build(f, act) } catch (t: Throwable) { log(t) }
            }
        })
        // 3. This page's dropdowns open as a card in line with their row.
        XposedHelpers.findAndHookMethod(Spinner::class.java, "performClick", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                try { placePopup(param.thisObject as Spinner) } catch (t: Throwable) { log(t) }
            }
        })
    }

    // ---- the dropdown card -----------------------------------------------------

    /**
     * Where this page's dropdown menus open. The menu itself stays Nothing's
     * (NtCustDropDownPreference's popup: its rounded `nt_cust_drop_down_spinner`
     * and text style); only its place and width change, on this page's rows.
     *
     * A DropDownPreference opens its menu from an invisible Spinner at the start
     * of its row, so the menu lands over the row's own title and summary (seen
     * on the phone, in Nothing's own Dark theme > Schedule as well) and is only
     * as wide as its longest entry. Android places the popup from the Spinner's
     * position on screen (PopupWindow.findDropDownPosition, transforms included)
     * and takes the width set before it opens (Spinner.DropdownPopup
     * .computeContentWidth), so a translation of that invisible Spinner and a
     * width put the menu as a card lined up with the row's text: just below the
     * row, or just above it when there is no room below; nothing Nothing lays
     * out is moved. If it fits neither way, Nothing's own placement stays.
     */
    private fun placePopup(sp: Spinner) {
        if (sp.javaClass != Spinner::class.java || sp.layoutDirection == View.LAYOUT_DIRECTION_RTL) return
        if (sp.context.activity()?.intent?.getBooleanExtra(MARK, false) != true) return
        val row = sp.parent as? View ?: return
        val title = row.findViewById<TextView>(android.R.id.title) ?: return
        val adapter = sp.adapter ?: return
        val pad = Rect().also { sp.popupBackground?.getPadding(it) }
        sp.translationX = 0f; sp.translationY = 0f; sp.dropDownWidth = -2
        val a = IntArray(2).also { sp.getLocationOnScreen(it) }
        val r = IntArray(2).also { row.getLocationOnScreen(it) }
        val t = IntArray(2).also { title.getLocationOnScreen(it) }
        val margin = t[0] - r[0]
        val boxWidth = row.width - 2 * margin
        if (margin <= 0 || boxWidth <= pad.left + pad.right) return
        val content = boxWidth - pad.left - pad.right
        val spec = View.MeasureSpec.makeMeasureSpec(content, View.MeasureSpec.EXACTLY)
        val free = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val height = pad.top + pad.bottom + (0 until adapter.count).sumOf { i ->
            adapter.getDropDownView(i, null, sp).let { it.measure(spec, free); it.measuredHeight }
        }
        val frame = Rect().also { sp.rootView.getWindowVisibleDisplayFrame(it) }
        val gap = (4 * sp.resources.displayMetrics.density).toInt()
        val top = when {
            r[1] + row.height + gap + height <= frame.bottom -> r[1] + row.height + gap
            r[1] - gap - height >= frame.top -> r[1] - gap - height
            else -> return
        }
        // The popup's top: the Spinner's top, plus its height unless it overlaps it.
        val popupTop = a[1] + if (overlaps(sp)) 0 else sp.height
        // The popup's left: the Spinner's left, less its background's padding,
        // plus the Spinner's own (computeContentWidth).
        val popupLeft = a[0] - pad.left + sp.paddingLeft
        sp.dropDownWidth = content
        sp.translationX = (t[0] - popupLeft).toFloat()
        sp.translationY = (top - popupTop).toFloat()
    }

    /** Whether the Spinner's popup overlaps it (its style's overlapAnchor; not set on the phone). */
    private fun overlaps(sp: Spinner): Boolean = try {
        val popup = XposedHelpers.getObjectField(sp, "mPopup")
        (XposedHelpers.getObjectField(popup, "mPopup") as PopupWindow).overlapAnchor
    } catch (t: Throwable) { false }

    private fun Context.activity(): Activity? {
        var c: Context? = this
        while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
        return null
    }

    // ---- the page ------------------------------------------------------------

    private class Rows(val screen: Any) {
        /**
         * False until the real state is shown. Seen on the phone: a dropdown
         * reports its first entry as a change when its spinner is first bound
         * while it has no value yet (DropDownPreference), so every open reset
         * the levels and protection until this guard.
         */
        @Volatile var ready = false
        var armed: Any? = null; var stereo: Any? = null
        var ear: Any? = null; var spk: Any? = null
        var boost: Any? = null; var bass: Any? = null; var leveller: Any? = null; var limit: Any? = null
        var protCat: Any? = null; var spkCat: Any? = null
    }

    private fun build(fragment: Any, act: Activity) {
        val screen = XposedHelpers.callMethod(fragment, "getPreferenceScreen") ?: return
        removeAll.invoke(screen)
        // The toolbar shows the screen's own title: Settings' PreferenceFragment
        // copies it to the activity in onResume (the carrier's is "Media").
        setTitle.invoke(screen, TITLE)
        val ctx = getContext.invoke(screen) as Context
        val rows = Rows(screen)
        var order = 0
        fun <T> place(p: T, parent: Any): T { setOrder.invoke(p, order++); setPersistent.invoke(p, false); add.invoke(parent, p); return p }
        fun cat(title: String, parent: Any = screen): Any = place(newCategory.newInstance(ctx), parent).also { setTitle.invoke(it, title) }
        fun sw(key: String, title: String, summary: String?, parent: Any, onChange: (Boolean) -> Unit): Any =
            place(newSwitch.newInstance(ctx), parent).also { p ->
                setKey.invoke(p, key); setTitle.invoke(p, title); summary?.let { setSummary.invoke(p, it) }
                setOnChange.invoke(p, listener(setOnChange.parameterTypes[0]) { a ->
                    if (!rows.ready) return@listener false
                    onChange(a[1] as Boolean); true
                })
            }
        fun drop(key: String, title: String, entries: List<String>, values: List<String>, parent: Any, onChange: (String) -> Unit): Any =
            place(newDrop.newInstance(ctx), parent).also { p ->
                setSummaryProvider.invoke(p, null)
                setKey.invoke(p, key); setTitle.invoke(p, title)
                setEntries.invoke(p, entries.toTypedArray<CharSequence>()); setValues.invoke(p, values.toTypedArray<CharSequence>())
                setOnChange.invoke(p, listener(setOnChange.parameterTypes[0]) { a ->
                    val v = a[1] as String
                    // Only a real choice: not before the state is shown, and not
                    // the value it already holds (a rebind re-selecting it).
                    if (!rows.ready || v == getValue.invoke(a[0])) return@listener false
                    setSummary.invoke(a[0], entries.getOrElse(values.indexOf(v)) { v })
                    onChange(v); true
                })
            }

        val levels = (1..10).map { "$it" }
        val sound = cat("Speakers")
        rows.armed = sw("cmf_armed", "Earpiece speaker", "Play through the earpiece as well as the bottom speaker", sound) { set(rows, act, "armed", if (it) "on" else "off") }
        rows.stereo = sw("cmf_stereo", "Stereo", null, sound) { set(rows, act, "channels", if (it) "stereo" else "mono") }
        val lv = cat("Levels")
        rows.ear = drop("cmf_ear", "Earpiece level", levels, levels, lv) { set(rows, act, "earLevel", it) }
        rows.spk = drop("cmf_spk", "Speaker level", levels, levels, lv) { set(rows, act, "spkLevel", it) }
        rows.spkCat = lv
        val prot = cat("Protection")
        rows.protCat = prot
        rows.boost = drop("cmf_boost", "Speaker boost", (0..6).map { if (it == 0) "Off" else "+$it dB" }, (0..6).map { "$it" }, prot) { set(rows, act, "boost", it) }
        rows.bass = drop("cmf_bass", "Deep bass guard", listOf("Off", "Light", "Medium", "Strong"), listOf("off", "light", "medium", "strong"), prot) { set(rows, act, "bass", it) }
        rows.leveller = drop("cmf_leveller", "Earpiece leveller", listOf("Off", "Low", "Mid", "High"), listOf("off", "light", "medium", "strong"), prot) { set(rows, act, "leveller", it) }
        rows.limit = sw("cmf_limit", "Earpiece limiter", "Stops the earpiece crackling at its very loudest", prot) { set(rows, act, "limit", if (it) "on" else "off") }
        val more = cat("More")
        place(pref.getConstructor(Context::class.java).newInstance(ctx), more).also { p ->
            setKey.invoke(p, "cmf_open"); setTitle.invoke(p, "CMF Stereo"); setSummary.invoke(p, "Open the app")
            setOnClick.invoke(p, listener(setOnClick.parameterTypes[0]) {
                act.startActivity(Intent().setClassName(BuildConfig.APPLICATION_ID, "com.tiimoo.cmfstereo.MainActivity")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
            })
        }
        setAll(rows, false)
        load(rows, act)
        // Kept in step with every change, wherever it is made (the apps, the
        // Quick Settings tile, stereoctl, the module itself): the app notifies
        // this uri, as Settings' own pages are notified.
        val observer = object : ContentObserver(main) {
            override fun onChange(selfChange: Boolean) {
                if (act.isDestroyed) act.contentResolver.unregisterContentObserver(this) else load(rows, act)
            }
        }
        act.contentResolver.registerContentObserver(Uri.parse("content://${BuildConfig.APPLICATION_ID}.settingstile/state"), false, observer)
    }

    private val main = Handler(Looper.getMainLooper())

    private fun load(rows: Rows, act: Activity) = Thread({
        val b = call(act, "getPageState", null)
        main.post { if (b != null) show(rows, b) }
    }, "cmf-settings").start()

    private fun set(rows: Rows, act: Activity, what: String, value: String) {
        // The tapped row already shows its new value; only Mono / Stereo, which
        // restarts the audio for a moment, holds the two switches until it is done.
        if (what == "channels" || what == "armed") {
            listOf(rows.armed, rows.stereo).forEach { it?.let { p -> setEnabled.invoke(p, false) } }
            rows.stereo?.let { setSummary.invoke(it, "Switching…") }
        }
        Thread({
            val b = call(act, "setPage", Bundle().apply { putString("what", what); putString("value", value) })
            main.post { if (b != null) show(rows, b) else setAll(rows, true) }
        }, "cmf-settings").start()
    }

    private fun show(rows: Rows, b: Bundle) {
        val ok = b.getBoolean("rooted") && b.getBoolean("installed") && b.getString("build") != "updated"
        val stereo = b.getString("staged") == "stereo"
        val pending = b.getString("channels") != b.getString("staged")
        rows.armed?.let { setChecked.invoke(it, b.getBoolean("armed")) }
        rows.stereo?.let {
            setChecked.invoke(it, stereo)
            setSummary.invoke(it, when {
                !b.getBoolean("rooted") -> "CMF Stereo needs root access"
                !b.getBoolean("installed") -> "The CMF true stereo module is not installed"
                b.getString("build") == "updated" -> "Not available on this Nothing OS version yet"
                pending -> "Switches after a restart"
                stereo -> "Earpiece left, speaker right"
                else -> "Both speakers play everything"
            })
        }
        val ear = b.getInt("earLevel", -1)
        rows.ear?.let { if (ear > 0) { setValue.invoke(it, "$ear"); setSummary.invoke(it, "$ear") } }
        rows.spk?.let { val v = b.getInt("spkLevel", 10); setValue.invoke(it, "$v"); setSummary.invoke(it, "$v") }
        rows.boost?.let { val v = b.getInt("boost"); setValue.invoke(it, "$v"); setSummary.invoke(it, if (v == 0) "Off" else "+$v dB") }
        fun named(v: String?, names: List<String>) = when (v) { "light" -> names[1]; "medium" -> names[2]; "strong" -> names[3]; else -> names[0] }
        rows.bass?.let { val v = b.getString("bass") ?: "medium"; setValue.invoke(it, v); setSummary.invoke(it, named(v, listOf("Off", "Light", "Medium", "Strong"))) }
        rows.leveller?.let { val v = b.getString("leveller") ?: "off"; setValue.invoke(it, v); setSummary.invoke(it, named(v, listOf("Off", "Low", "Mid", "High"))) }
        rows.limit?.let { setChecked.invoke(it, b.getBoolean("limit", true)) }
        rows.ready = true
        setAll(rows, ok)
        // Speaker level and protection exist in stereo only.
        val inStereo = ok && b.getString("channels") == "stereo"
        rows.spk?.let { setEnabled.invoke(it, inStereo) }
        val prot = inStereo && b.getBoolean("supportsProtect")
        listOf(rows.boost, rows.bass, rows.limit).forEach { it?.let { p -> setEnabled.invoke(p, prot) } }
        rows.leveller?.let { setEnabled.invoke(it, prot && b.getBoolean("supportsLeveller")) }
    }

    private fun setAll(rows: Rows, on: Boolean) =
        listOf(rows.armed, rows.stereo, rows.ear, rows.spk, rows.boost, rows.bass, rows.leveller, rows.limit)
            .forEach { it?.let { p -> setEnabled.invoke(p, on) } }

    private fun call(ctx: Context, method: String, extras: Bundle?): Bundle? = try {
        ctx.contentResolver.call(Uri.parse("content://${BuildConfig.APPLICATION_ID}.settingstile"), method, null, extras)
    } catch (t: Throwable) { log(t); null }

    companion object {
        private const val SETTINGS = "com.android.settings"
        private const val CARRIER = "com.android.settings.sound.MediaControlsSettings"
        private const val ENTRY = "com.tiimoo.cmfstereo.nothing.SoundSettingsEntry"
        private const val MARK = "cmf_stereo_page"
        private const val TITLE = "Stereo speakers"

        private fun log(t: Throwable) = XposedBridge.log("CMF Stereo: settings page: $t")

        /** Installs the page, or nothing (the tile then opens the app) if Settings does not match. */
        fun install(cl: ClassLoader) {
            try {
                SettingsPage(cl).install()
                XposedBridge.log("CMF Stereo: settings page ready")
            } catch (t: Throwable) {
                XposedBridge.log("CMF Stereo: settings page not installed (Settings differs): $t")
            }
        }
    }
}
