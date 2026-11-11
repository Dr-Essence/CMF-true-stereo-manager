package com.tiimoo.cmfstereo.modetest

/**
 * The pause before every test (Dr.Essence: time to make silence and set things up): [SECONDS]
 * seconds, shown counting down. Call it on the test's own thread, never the main one.
 * Returns false if [alive] turns false meanwhile (the test was left): nothing should play then.
 */
object Countdown {
    const val SECONDS = 5

    fun wait(show: (String) -> Unit, alive: () -> Boolean = { true }): Boolean {
        for (s in SECONDS downTo 1) {
            if (!alive()) return false
            show("Starting in $s s: quiet, hands off.")
            Thread.sleep(1000)
        }
        return alive()
    }
}
