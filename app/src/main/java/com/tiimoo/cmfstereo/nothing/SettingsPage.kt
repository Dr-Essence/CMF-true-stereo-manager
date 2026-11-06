package com.tiimoo.cmfstereo.nothing

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import com.tiimoo.cmfstereo.BuildConfig
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.WeakHashMap

/**
 * The "Stereo speakers" page, inside Nothing Settings (runs in the Settings
 * process, installed by SettingsHook).
 *
 * Settings only opens its own pages, so a tap on the tile is sent to Settings'
 * own SubSettings with a small, allowed Sound page (MediaControlsSettings) as
 * the carrier; once that page has built itself, its rows are replaced with
 * this page's, made from Nothing's own preference widgets
 * (NtCustSwitchPreferenceCompat, Preference, PreferenceCategory); levels and
 * choices are the app's dots and pills (SettingsControls) under their row's
 * summary.
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
    private val category = XposedHelpers.findClass("androidx.preference.PreferenceCategory", cl)
    private val switchCls = XposedHelpers.findClass("com.android.settingslib.ntuisupport.preference.NtCustSwitchPreferenceCompat", cl)
    private val dashboard = XposedHelpers.findClass("com.android.settings.dashboard.DashboardFragment", cl)

    private val setKey = m(pref, "V0", String::class.java)
    private val setTitle = m(pref, "l1", CharSequence::class.java)
    private val setSummary = m(pref, "i1", CharSequence::class.java)
    private val setEnabled = m(pref, "O0", Boolean::class.javaPrimitiveType!!)
    private val setPersistent = m(pref, "b1", Boolean::class.javaPrimitiveType!!)
    private val setOrder = m(pref, "a1", Int::class.javaPrimitiveType!!)
    private val getContext = pref.getMethod("B").also { check(it.returnType == Context::class.java) }
    private val isEnabled = pref.getMethod("c0").also { check(it.returnType == Boolean::class.javaPrimitiveType) }
    // onBindViewHolder(PreferenceViewHolder), and the holder's itemView (RecyclerView.ViewHolder).
    private val holder = XposedHelpers.findClass("androidx.preference.g0", cl)
    private val bind = m(pref, "o0", holder)
    private val itemView = XposedHelpers.findClass("androidx.recyclerview.widget.m1", cl).getField("c")
        .also { check(it.type == View::class.java && it.declaringClass.isAssignableFrom(holder)) }
    private val setOnChange = listenerSetter("Y0", "onPreferenceChange")
    private val setOnClick = listenerSetter("Z0", "onPreferenceClick")
    private val removeAll = m(group, "E1")
    private val add = m(group, "u1", pref)
    private val setChecked = m(twoState, "v1", Boolean::class.javaPrimitiveType!!)
    private val newSwitch = switchCls.getConstructor(Context::class.java)
    private val newPref = pref.getConstructor(Context::class.java)
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
        // 3. This page's levels and choices: dots and pills under their row's summary.
        XposedBridge.hookMethod(bind, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try { bindControl(param.thisObject, itemView.get(param.args[0]) as View) } catch (t: Throwable) { log(t) }
            }
        })
    }

    // ---- the dots and pills ----------------------------------------------------

    /**
     * A level (dots [min]..[max]) or a choice (pills with [labels] for [values])
     * shown under a row's summary; [value] is what the row holds, null until the
     * state is shown (nothing is lit and taps do nothing until then).
     */
    private class Control(
        val min: Int = 0, val max: Int = 0,
        val labels: List<String> = emptyList(), val values: List<String> = emptyList(),
        val summary: (String) -> String = { it }, val onPick: (String) -> Unit
    ) { var value: String? = null }

    /** This page's rows with a control; weak, so a closed page leaves nothing behind. */
    private val controls = WeakHashMap<Any, Control>()

    /**
     * After Settings binds a row: a control of ours is put under the summary,
     * in the row's text column; one left there from a recycled row is removed
     * first. The row itself stays as Settings laid it out.
     */
    private fun bindControl(p: Any, row: View) {
        val title = row.findViewById<TextView>(android.R.id.title) ?: return
        val box = title.parent as? RelativeLayout ?: return
        box.findViewWithTag<View>(CONTROL)?.let { box.removeView(it) }
        val c = controls[p] ?: return
        val summary = row.findViewById<TextView>(android.R.id.summary)
        val look = SettingsControls.Look(
            lit = categoryColor(title.context),
            line = ((summary?.currentTextColor ?: title.currentTextColor) and 0x00FFFFFF) or 0x66000000,
            text = title.currentTextColor,
            font = title.typeface,
            textPx = summary?.textSize ?: title.textSize
        )
        val on = isEnabled.invoke(p) as Boolean
        val view = if (c.labels.isEmpty()) {
            SettingsControls.Dots(box.context, c.min, c.max, c.value?.toIntOrNull(), look, on) { pick(p, c, "$it") }
        } else {
            SettingsControls.pills(box.context, c.labels, c.values.indexOf(c.value).takeIf { it >= 0 }, look, on) { pick(p, c, c.values[it]) }
        }
        view.tag = CONTROL
        val d = box.resources.displayMetrics.density
        box.addView(view, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.BELOW, if (summary != null) android.R.id.summary else android.R.id.title)
            topMargin = (12 * d).toInt()
        })
    }

    /** A tap on a dot or pill: the row shows it at once, then it is set. */
    private fun pick(p: Any, c: Control, v: String) {
        c.value = v
        setSummary.invoke(p, summaryOf(c, v))
        c.onPick(v)
    }

    private fun summaryOf(c: Control, v: String): String =
        if (c.labels.isEmpty()) c.summary(v) else c.labels.getOrElse(c.values.indexOf(v)) { v }

    private fun boostName(v: String) = if (v == "0") "Off" else "+$v dB"

    /** The colour of Settings' own category titles and switches (its preferenceCategoryTitleTextColor). */
    private fun categoryColor(ctx: Context): Int {
        val tv = TypedValue()
        val attr = ctx.resources.getIdentifier("preferenceCategoryTitleTextColor", "attr", SETTINGS)
        if (attr != 0 && ctx.theme.resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) return ctx.getColorStateList(tv.resourceId).defaultColor
            if (tv.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT) return tv.data
        }
        return if (ctx.theme.resolveAttribute(android.R.attr.colorAccent, tv, true)) tv.data else 0xFF79C5DA.toInt()
    }

    // ---- the page ------------------------------------------------------------

    private class Rows(val screen: Any) {
        /**
         * False until the real state is shown: no change is taken before then.
         * Seen on the phone with the dropdowns this page had before: a
         * DropDownPreference reported its first entry as a change when first
         * bound without a value, so every open reset the levels and
         * protection. The dots and pills take no tap until they hold a value.
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
        /** A row with dots (a level) or pills (a choice) under its summary. */
        fun ctl(key: String, title: String, parent: Any, c: Control): Any =
            place(newPref.newInstance(ctx), parent).also { p ->
                // Left selectable: Preference.o0 paints an unselectable row's title
                // in its summary's colour of the moment, still the dimmed one when
                // the row has just been turned on (seen on the phone).
                setKey.invoke(p, key); setTitle.invoke(p, title)
                controls[p] = c
            }
        fun dots(key: String, title: String, min: Int, max: Int, parent: Any, summary: (String) -> String = { it }, onPick: (String) -> Unit) =
            ctl(key, title, parent, Control(min = min, max = max, summary = summary, onPick = onPick))
        fun pills(key: String, title: String, labels: List<String>, parent: Any, onPick: (String) -> Unit) =
            ctl(key, title, parent, Control(labels = labels, values = CHOICES, onPick = onPick))

        val sound = cat("Speakers")
        rows.armed = sw("cmf_armed", "Earpiece speaker", "Play through the earpiece as well as the bottom speaker", sound) { set(rows, act, "armed", if (it) "on" else "off") }
        rows.stereo = sw("cmf_stereo", "Stereo", null, sound) { set(rows, act, "channels", if (it) "stereo" else "mono") }
        val lv = cat("Levels")
        rows.ear = dots("cmf_ear", "Earpiece level", 1, 10, lv) { set(rows, act, "earLevel", it) }
        rows.spk = dots("cmf_spk", "Speaker level", 1, 10, lv) { set(rows, act, "spkLevel", it) }
        rows.spkCat = lv
        val prot = cat("Protection")
        rows.protCat = prot
        rows.boost = dots("cmf_boost", "Speaker boost", 0, 6, prot, ::boostName) { set(rows, act, "boost", it) }
        rows.bass = pills("cmf_bass", "Deep bass guard", BASS, prot) { set(rows, act, "bass", it) }
        rows.leveller = pills("cmf_leveller", "Earpiece leveller", LEVELLER, prot) { set(rows, act, "leveller", it) }
        rows.limit = sw("cmf_limit", "Earpiece limiter", "Stops the earpiece crackling at its very loudest", prot) { set(rows, act, "limit", if (it) "on" else "off") }
        val more = cat("More")
        place(newPref.newInstance(ctx), more).also { p ->
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
        // A new value redraws its row's dots or pills: the summary changes with
        // it, and Settings rebinds a row whose summary changes.
        fun value(p: Any?, v: String) { val c = p?.let { controls[it] } ?: return; c.value = v; setSummary.invoke(p, summaryOf(c, v)) }
        b.getInt("earLevel", -1).let { if (it > 0) value(rows.ear, "$it") }
        value(rows.spk, "${b.getInt("spkLevel", 10)}")
        value(rows.boost, "${b.getInt("boost")}")
        value(rows.bass, (b.getString("bass") ?: "medium").takeIf { it in CHOICES } ?: "medium")
        value(rows.leveller, (b.getString("leveller") ?: "off").takeIf { it in CHOICES } ?: "off")
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
        private const val CONTROL = "cmf_stereo_control"
        private val CHOICES = listOf("off", "light", "medium", "strong")
        private val BASS = listOf("Off", "Light", "Medium", "Strong")
        private val LEVELLER = listOf("Off", "Low", "Mid", "High")

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
