package com.tiimoo.cmfstereo.nothing

import android.content.Intent
import android.content.pm.ResolveInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * In Settings Intelligence (Nothing's Settings search): the "Stereo speakers" rows are
 * searchable, and a result opens that page inside Settings at its row.
 *
 * Read from Nothing's Settings Intelligence (its classes are not obfuscated):
 * - `search.indexing.PreIndexDataCollector.isWellKnownProvider(ResolveInfo)` takes a search
 *   provider only if it is protected by `READ_SEARCH_INDEXABLES` and its app is a system app.
 *   It is answered true for [SearchProvider], the permission still required. With both
 *   editions installed only CMF Stereo is taken (the Pro's rows would be the same, twice).
 * - `search.indexing.IndexData$Builder.buildIntent()` makes the intent a tap starts. For this
 *   app's rows it becomes Settings' own `SearchResultTrampoline` (exported, takes Settings
 *   Intelligence as its caller) with the page's carrier fragment, title and mark: the
 *   trampoline opens the fragment in Settings' SubSettings, where [SettingsPage] turns it into
 *   the "Stereo speakers" page, as for a tap on the Sound row; the row's key goes along as
 *   `:settings:fragment_args_key`, the key Settings highlights a row by.
 */
internal object SearchHook {

    private const val PERMISSION = "android.permission.READ_SEARCH_INDEXABLES"
    private const val CLEAN = "com.tiimoo.cmfstereo.clean"
    private const val PRO = "com.tiimoo.cmfstereo"
    private const val INDEXING = "com.android.settings.intelligence.search.indexing"

    fun install(cl: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod("$INDEXING.PreIndexDataCollector", cl,
                "isWellKnownProvider", ResolveInfo::class.java, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.result == true) return
                        val info = (param.args[0] as? ResolveInfo)?.providerInfo ?: return
                        val pkg = info.packageName
                        if (pkg != CLEAN && pkg != PRO) return
                        if (info.authority != "$pkg.search" || info.readPermission != PERMISSION
                            || info.writePermission != PERMISSION) return
                        if (pkg == PRO && installed(param.thisObject, CLEAN)) return
                        param.result = true
                    }
                })
            val builder = XposedHelpers.findClass("$INDEXING.IndexData\$Builder", cl)
            XposedHelpers.findAndHookMethod(builder, "buildIntent", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val b = param.thisObject
                    if (XposedHelpers.getObjectField(b, "mIntentAction") != SearchProvider.ACTION) return
                    val key = XposedHelpers.getObjectField(b, "mKey") as? String
                    param.result = Intent("com.android.settings.SEARCH_RESULT_TRAMPOLINE")
                        .setPackage("com.android.settings")
                        .putExtra(":settings:show_fragment", SettingsPage.CARRIER)
                        .putExtra(":settings:show_fragment_title", SettingsPage.TITLE)
                        .putExtra(":settings:fragment_args_key", key)
                        .putExtra(SettingsPage.MARK, true)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            })
            XposedBridge.log("CMF Stereo: settings search ready (2 hooks)")
        } catch (t: Throwable) {
            XposedBridge.log("CMF Stereo: settings search not installed: $t")
        }
    }

    /** Whether [pkg] is installed, seen from Settings Intelligence (it sees every app). */
    private fun installed(collector: Any, pkg: String): Boolean = try {
        val context = XposedHelpers.getObjectField(collector, "mContext") as android.content.Context
        context.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (t: Throwable) {
        false
    }
}
