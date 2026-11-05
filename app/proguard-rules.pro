# LSPosed loads the hook by the name in assets/xposed_init.
-keep class com.tiimoo.cmfstereo.nothing.SettingsHook { *; }
# The Xposed API is provided at runtime, not packaged.
-dontwarn de.robv.android.xposed.**
