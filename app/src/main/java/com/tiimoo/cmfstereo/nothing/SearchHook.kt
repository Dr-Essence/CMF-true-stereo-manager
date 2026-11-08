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
 *   editions offering the rows only CMF Stereo is taken (the Pro's would be the same, twice);
 *   an edition without the provider (older than v1.0.80) does not count.
 * - `search.indexing.IndexDatabaseHelper.buildProviderVersionedNames(Context, List)` is the
 *   string compared with the last indexing; only a change reads the providers again. A marker
 *   is added to it, so the first indexing with this hook reads the rows even if Settings
 *   Intelligence indexed this version before the hook was on (v1.0.80 on the phone: no rows).
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
    /** Added to the indexed-versions string; changing it makes Settings Intelligence read again. */
    private const val MARKER = "cmf-search:1"

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
                        if (pkg == PRO && offersRows(param.thisObject, CLEAN)) return
                        param.result = true
                    }
                })
            XposedHelpers.findAndHookMethod("$INDEXING.IndexDatabaseHelper", cl,
                "buildProviderVersionedNames", android.content.Context::class.java,
                List::class.java, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val names = param.result as? String ?: return
                        param.result = "$names,$MARKER"
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
            XposedBridge.log("CMF Stereo: settings search ready (3 hooks)")
        } catch (t: Throwable) {
            XposedBridge.log("CMF Stereo: settings search not installed: $t")
        }
    }

    /** Whether [pkg] is installed with its search rows (v1.0.80+), seen from Settings Intelligence. */
    private fun offersRows(collector: Any, pkg: String): Boolean = try {
        val context = XposedHelpers.getObjectField(collector, "mContext") as android.content.Context
        context.packageManager.resolveContentProvider("$pkg.search", 0) != null
    } catch (t: Throwable) {
        false
    }
}
