package com.tiimoo.cmfstereo.nothing

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.tiimoo.cmfstereo.R
import com.tiimoo.cmfstereo.StereoCtl

/**
 * Quick Settings tile: Mono / Stereo in the pull-down panel. Long-press opens
 * the app (its MainActivity answers QS_TILE_PREFERENCES). Every root call runs
 * on a thread of its own; the tile shows "Switching" while the audio HAL
 * restarts.
 */
class StereoTile : TileService() {
    @Volatile private var busy = false

    override fun onStartListening() {
        super.onStartListening()
        if (!busy) refresh()
    }

    override fun onClick() {
        super.onClick()
        if (busy) return
        val tile = qsTile ?: return
        val toStereo = tile.state != Tile.STATE_ACTIVE
        busy = true
        tile.state = Tile.STATE_UNAVAILABLE
        tile.subtitle = getString(R.string.qs_switching)
        tile.updateTile()
        Thread({
            try { StereoCtl.setChannels(toStereo) } catch (_: Throwable) {}
            busy = false
            refresh()
        }, "cmf-tile").start()
    }

    private fun refresh() {
        Thread({
            val s = try { StereoCtl.status() } catch (_: Throwable) { null }
            val tile = qsTile ?: return@Thread
            when {
                s == null || !s.rooted || !s.installed -> {
                    tile.state = Tile.STATE_UNAVAILABLE
                    tile.subtitle = getString(R.string.qs_unavailable)
                }
                else -> {
                    tile.state = if (s.channelsStaged == "stereo") Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    tile.subtitle = getString(if (s.channelsStaged == "stereo") R.string.qs_stereo else R.string.qs_mono)
                }
            }
            tile.updateTile()
        }, "cmf-tile").start()
    }
}
