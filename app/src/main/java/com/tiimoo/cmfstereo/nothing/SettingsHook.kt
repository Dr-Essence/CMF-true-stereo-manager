package com.tiimoo.cmfstereo.nothing

import android.content.Intent
import android.content.pm.ResolveInfo
import com.tiimoo.cmfstereo.BuildConfig
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * LSPosed hook (scope: com.android.settings) that lets this app's own Settings
 * tile into Nothing's Sound screen.
 *
 * Settings already supports tiles from other apps - an activity with the
 * action com.android.settings.action.EXTRA_SETTINGS and a category - and draws
 * them itself, in its own style (com.android.settingslib.drawer, read from
 * Nothing's Settings 16.0.0): title, icon, a live summary and a switch, the last
 * two through this app's SettingsTileProvider, called on a background thread.
 * It only takes them from system apps (TileUtils keeps a ResolveInfo only when
 * resolveInfo.system is true). The one change: for that query, this app's own
 * entry is marked system. Nothing else in Settings is touched, and any failure
 * leaves Settings exactly as it was.
 */
class SettingsHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != SETTINGS) return
        try {
            val pm = XposedHelpers.findClass("android.app.ApplicationPackageManager", lpparam.classLoader)
            XposedBridge.hookAllMethods(pm, "queryIntentActivitiesAsUser", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val intent = param.args.firstOrNull() as? Intent ?: return
                        if (intent.action != EXTRA_SETTINGS) return
                        val list = param.result as? List<*> ?: return
                        for (ri in list) {
                            if (ri is ResolveInfo && ri.activityInfo?.packageName == BuildConfig.APPLICATION_ID) {
                                XposedHelpers.setBooleanField(ri, "system", true)
                            }
                        }
                    } catch (t: Throwable) {
                        XposedBridge.log("CMF Stereo: settings tile: $t")
                    }
                }
            })
        } catch (t: Throwable) {
            XposedBridge.log("CMF Stereo: settings hook not installed: $t")
        }
    }

    private companion object {
        const val SETTINGS = "com.android.settings"
        const val EXTRA_SETTINGS = "com.android.settings.action.EXTRA_SETTINGS"
    }
}
