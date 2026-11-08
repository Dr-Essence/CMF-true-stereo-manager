package com.tiimoo.cmfstereo.nothing

/**
 * The rows of the "Stereo speakers" page in Nothing Settings ([SettingsPage]), in one place:
 * the page draws them from here, and Settings search ([SearchProvider]) lists the same titles
 * and descriptions, so the two never differ.
 */
internal object SearchRows {

    /** One row: its preference key, the category it is under, its title and description. */
    data class Row(val key: String, val category: String, val title: String, val summary: String?)

    val armed = Row("cmf_armed", "Speakers", "Earpiece speaker",
        "Play through the earpiece as well as the bottom speaker")
    val stereo = Row("cmf_stereo", "Speakers", "Stereo", null)
    val ear = Row("cmf_ear", "Levels", "Earpiece level",
        "10 is the loudest the earpiece goes. Turn it down if it ever buzzes")
    val spk = Row("cmf_spk", "Levels", "Speaker level",
        "Balances the two sides: lower it until the earpiece is as clear as the speaker")
    val boost = Row("cmf_boost", "Protection", "Speaker boost",
        "Louder speaker without more strain, +1 dB a dot. The first dot is off")
    val bass = Row("cmf_bass", "Protection", "Deep bass guard",
        "Holds back only the deepest bass, and only when it gets loud enough to strain the speaker")
    val leveller = Row("cmf_leveller", "Protection", "Earpiece leveller",
        "Brings up the earpiece's quiet parts so it keeps up with the speaker")
    val limit = Row("cmf_limit", "Protection", "Earpiece limiter",
        "Stops the earpiece crackling at its very loudest")

    val all = listOf(armed, stereo, ear, spk, boost, bass, leveller, limit)
}
