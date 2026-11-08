package com.tiimoo.cmfstereo.nothing

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.tiimoo.cmfstereo.BuildConfig

/**
 * The "Stereo speakers" page's rows for Nothing's Settings search.
 *
 * Settings Intelligence (the app behind Settings search) reads every provider answering
 * `android.content.action.SEARCH_INDEXABLES_PROVIDER` that is protected by
 * `READ_SEARCH_INDEXABLES` (read from Nothing's Settings Intelligence,
 * `search/indexing/PreIndexDataCollector`): `settings/indexables_raw` rows by column position
 * (title 1, summary 2-3, keywords 5, screen title 6, class 7, the intent's action, package and
 * class 9-11, key 12). It only takes system apps; [SearchHook] lets it take this one, and turns
 * a tap on a result into the page inside Settings at that row.
 */
class SearchProvider : ContentProvider() {

    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
            selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val columns = projection ?: arrayOf()
        val cursor = MatrixCursor(columns)
        if (uri.lastPathSegment == "indexables_raw") {
            for (row in SearchRows.all) cursor.addRow(rawRow(columns.size, row))
        }
        return cursor // xml resources, non-indexable keys, site map: none
    }

    private fun rawRow(width: Int, r: SearchRows.Row): Array<Any?> {
        val row = arrayOfNulls<Any?>(maxOf(width, 14))
        row[0] = 0                                          // rank
        row[1] = r.title
        row[2] = r.summary                                  // summary on
        row[3] = r.summary                                  // summary off
        row[5] = "stereo, speakers, earpiece, CMF, ${r.category}" // keywords
        row[6] = SettingsPage.TITLE                         // screen title, shown under the result
        row[7] = ENTRY                                      // class name
        row[8] = 0                                          // icon: the app's own
        row[9] = ACTION                                     // SearchHook swaps this intent
        row[10] = BuildConfig.APPLICATION_ID
        row[11] = ENTRY
        row[12] = r.key
        row[13] = 0                                         // user id
        return if (width >= row.size) row else row.copyOf(width)
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?,
            selectionArgs: Array<out String>?) = 0

    companion object {
        /** Marks this app's results for [SearchHook]. */
        const val ACTION = "com.tiimoo.cmfstereo.SEARCH_RESULT"
        /** The Settings tile's entry, which also opens the app without LSPosed. */
        const val ENTRY = "com.tiimoo.cmfstereo.nothing.SoundSettingsEntry"
    }
}
