package com.tiimoo.cmfstereo

/**
 * Everything the UI needs from the module, expressed as shell calls.
 *
 * Config lives in /data/adb/cmf-stereo and is read by the daemon every cycle,
 * so writes here take effect without restarting anything.
 */
object StereoCtl {

    const val MODULE_DIR = "/data/adb/modules/cmf_stereo"
    const val DATA_DIR = "/data/adb/cmf-stereo"
    private const val CTL = "$MODULE_DIR/scripts/stereoctl"
    private const val ACTIONS = "$DATA_DIR/actions.conf"
    private const val CONF = "$DATA_DIR/stereo.conf"

    /**
     * Handset Volume, read from the MT6369 codec driver: range 0-18, and the
     * register runs opposite to the dB scale ALSA advertises -
     * 0 = +8 dB (loudest), 8 = 0 dB, 18 = -10 dB, 31 = -40 dB (mute).
     *
     * Earlier versions treated 31 as the top, which held the earpiece at the
     * codec's mute value against the HAL's own +8 dB.
     */
    const val MAX_GAIN = 18

    /** dB for a Handset Volume value. */
    fun gainDb(value: Int): Int = 8 - value

    data class Status(
        val installed: Boolean = false,
        val rooted: Boolean = false,
        val armed: Boolean = false,
        val daemonRunning: Boolean = false,
        val playing: Boolean = false,
        val mode: String = "playback",
        /** null means "could not be read" - which is NOT the same as zero. */
        val gain: Int? = null,
        val actionCount: Int = 0,
        val moduleVersion: String = "",
        val supportsDoctor: Boolean = false,
        val supportsReset: Boolean = false,
        val guardBypassed: Boolean = false,
        val interval: String = "2",
        val logLevel: String = "info",
        /** Channels the running system uses: "mono" or "stereo". */
        val channels: String = "mono",
        /** The chosen channels. Differs from [channels] only while a switch waits for a reboot. */
        val channelsStaged: String = "mono",
        /** False on modules older than v0.9.0, which have no channel modes. */
        val supportsChannels: Boolean = false,
        val stereoAttenDb: String = "6",
        /** Module v1.0.4+: the stereo protection (stereoctl protect). */
        val supportsProtect: Boolean = false,
        /** The protection library has marked itself running in the audio HAL. */
        val protRunning: Boolean = false,
        val speakerBoost: Int = 0,
        /** off | light | medium | strong */
        val bassProtect: String = "medium",
        val earpieceLimit: Boolean = true,
        /**
         * Module v0.9.8+: "known" (the analysed Nothing OS build), "updated" (a
         * Nothing OS update changed the audio files: stereo is unavailable, mono
         * runs) or "absent" (not a CMF Phone 1 audio stack). Older modules: "known".
         */
        val build: String = "known",
        val raw: String = ""
    )

    private fun ctl(args: String) = Shell.run("sh $CTL $args")

    /**
     * Everything the screen shows, read in ONE root shell.
     *
     * Each su call is a new root process (Magisk/KernelSU hand it over through
     * their daemon), and this used to make about fourteen of them, plus a full
     * `stereoctl status` that read every configured control back through
     * tinymix. That ran after every tap, which is most of why switching the
     * earpiece on and off felt slow. `status brief` skips the read-back;
     * modules older than v0.9.3 ignore the word and print the full status,
     * which parses the same.
     */
    fun status(): Status {
        val r = Shell.run(
            """
            echo "@uid=${'$'}(id -u)"
            if [ ! -f $CTL ]; then echo '@installed=no'; exit 0; fi
            echo '@installed=yes'
            echo '@status'
            sh $CTL status brief 2>&1
            echo '@help'
            sh $CTL --help 2>&1
            echo '@end'
            echo "@gain=${'$'}(grep '^ctl|Handset Volume|' $ACTIONS 2>/dev/null | head -n 1 | cut -d'|' -f3)"
            echo "@version=${'$'}(grep '^version=' $MODULE_DIR/module.prop 2>/dev/null | cut -d= -f2)"
            [ -f $DATA_DIR/guard_bypass ] && echo '@guard_bypass'
            grep -E '^(WATCH_INTERVAL|LOG_LEVEL|STEREO_ATTEN_DB|SPEAKER_BOOST|BASS_PROTECT|EARPIECE_LIMIT)=' $CONF 2>/dev/null | sed 's/^/@set:/'
            echo "@prot=${'$'}(getprop vendor.audiohal.cmf_prot)"
            """.trimIndent()
        )
        val lines = r.out.lines()
        if (!r.ok && lines.none { it.startsWith("@uid=") }) return Status(rooted = false)
        if (lines.firstOrNull { it.startsWith("@uid=") }?.removePrefix("@uid=")?.trim() != "0") {
            return Status(rooted = false)
        }
        if ("@installed=yes" !in lines) return Status(rooted = true, installed = false)

        fun section(start: String, end: String): String {
            val i = lines.indexOf(start); val j = lines.indexOf(end)
            return if (i >= 0 && j > i) lines.subList(i + 1, j).joinToString("\n") else ""
        }
        val raw = section("@status", "@help")
        val help = section("@help", "@end")
        fun value(tag: String) = lines.firstOrNull { it.startsWith(tag) }?.removePrefix(tag)?.trim().orEmpty()
        // First match wins, quotes stripped: what `grep -m1 | cut | tr -d "'"` gave.
        fun setting(key: String) = lines.firstOrNull { it.startsWith("@set:$key=") }
            ?.substringAfter('=')?.replace("'", "")?.trim().orEmpty()
        fun supports(name: String) = Regex("^  $name", RegexOption.MULTILINE).containsMatchIn(help)

        // "channels:  stereo"  or  "channels:  mono (switches to stereo after reboot)"
        val ch = Regex("""channels:\s+(mono|stereo)(?:\s+\(switches to (mono|stereo))?""").find(raw)
        val running = ch?.groupValues?.get(1) ?: "mono"
        val staged = ch?.groupValues?.get(2)?.ifBlank { null } ?: running
        return Status(
            installed = true,
            rooted = true,
            armed = raw.contains(Regex("""armed:\s+yes""")),
            daemonRunning = raw.contains(Regex("""daemon:\s+running""")),
            playing = raw.contains(Regex("""playback:\s+active""")),
            mode = Regex("""mode:\s+(\w+)""").find(raw)?.groupValues?.get(1) ?: "playback",
            gain = value("@gain=").toIntOrNull(),
            actionCount = Regex("""\((\d+) configured\)""").find(raw)?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            moduleVersion = value("@version="),
            supportsDoctor = supports("doctor"),
            supportsReset = supports("reset"),
            guardBypassed = "@guard_bypass" in lines,
            interval = setting("WATCH_INTERVAL").ifBlank { "2" },
            logLevel = setting("LOG_LEVEL").ifBlank { "info" },
            channels = running,
            channelsStaged = staged,
            supportsChannels = supports("channels"),
            stereoAttenDb = setting("STEREO_ATTEN_DB").ifBlank { "6" },
            supportsProtect = supports("protect"),
            protRunning = value("@prot=") == "1" && running == "stereo",
            speakerBoost = setting("SPEAKER_BOOST").toIntOrNull()?.coerceIn(0, 6) ?: 0,
            bassProtect = setting("BASS_PROTECT").ifBlank { "medium" },
            earpieceLimit = setting("EARPIECE_LIMIT") != "off",
            build = Regex("""build:\s+(\w+)""").find(raw)?.groupValues?.get(1) ?: "known",
            raw = raw
        )
    }

    fun setArmed(on: Boolean) = ctl(if (on) "on" else "off").out

    /**
     * Chooses mono or stereo. Module v0.9.4+ applies it at once by restarting the
     * audio HAL on the wanted files; when that cannot be done safely (or on older
     * modules) it is staged and the status shows it pending until a reboot.
     */
    fun setChannels(stereo: Boolean) = ctl(if (stereo) "channels stereo" else "channels mono").out

    /** Clamped like the module clamps it; the daemon re-reads it every cycle. */
    fun setStereoAtten(db: Int): String {
        val v = db.coerceIn(0, 30)
        setSetting("STEREO_ATTEN_DB", v.toString())
        val after = readSetting("STEREO_ATTEN_DB")
        return if (after == v.toString()) "stereo attenuation -> $v dB (applied on the daemon's next cycle)"
        else "FAILED to write STEREO_ATTEN_DB: stereo.conf reads '${after.ifBlank { "nothing" }}'"
    }

    /** Stereo protection (module v1.0.4+): applied by the daemon within one cycle. */
    fun setSpeakerBoost(db: Int) = ctl("protect boost ${db.coerceIn(0, 6)}").out
    fun setBassProtect(level: String) =
        ctl("protect bass ${if (level in listOf("off", "light", "medium", "strong")) level else "medium"}").out
    fun setEarpieceLimit(on: Boolean) = ctl("protect earpiece ${if (on) "on" else "off"}").out

    fun reboot() = Shell.run("svc power reboot 2>/dev/null || reboot").out

    fun solo(on: Boolean) = ctl(if (on) "solo" else "unsolo").out

    fun doctor() = ctl("doctor").out

    fun log(lines: Int = 60) = ctl("log $lines").out

    fun probe() = ctl("probe").out

    fun report() = ctl("report").out

    /**
     * Returns null when the value cannot be read, so the UI can say so
     * instead of showing 0 - a displayed 0 that then gets written back is how
     * this silently zeroed a working config.
     *
     * No `grep -m1`: Android's grep is toybox and does not take the value
     * jammed onto the flag. `head -n 1` is portable and cannot fail quietly.
     */
    fun readGain(): Int? {
        val r = Shell.run("grep '^ctl|Handset Volume|' $ACTIONS 2>/dev/null | head -n 1 | cut -d'|' -f3")
        if (!r.ok) return null
        return r.out.trim().toIntOrNull()
    }

    /**
     * Rewrite the gain in actions.conf. Clamped to the hardware ceiling: the
     * register field is 5 bits, so 40 wraps to 8 and gets quieter, not louder.
     */
    /**
     * Writes the gain to actions.conf, then asks the daemon to apply it now.
     *
     * Without the apply, the new value only reaches the mixer on the daemon's
     * next cycle - and in MODE=playback that is never, unless audio happens to
     * be running. The slider then looks broken: the file changes, the loudness
     * does not.
     */
    fun setGain(value: Int): String {
        val v = value.coerceIn(0, MAX_GAIN)
        // '#' as the sed delimiter: the pattern itself contains '|'.
        Shell.run("sed -i 's#^ctl|Handset Volume|.*#ctl|Handset Volume|$v#' $ACTIONS")

        // Read it back instead of trusting the exit status. A write that
        // reports success and does nothing is how the slider looked functional
        // while changing nothing at all.
        val after = readGain()
        if (after != v) {
            return "FAILED to write gain: actions.conf still reads ${after ?: "unreadable"}"
        }

        // Push it straight at the mixer too, so the change is audible now
        // rather than at some later route change.
        val live = Shell.run("sh $CTL ctl 'Handset Volume' $v")
        return "gain -> $v (config verified)\n${live.out}"
    }

    fun setMode(mode: String): String {
        val m = if (mode == "always") "always" else "playback"
        return Shell.run("sed -i 's/^MODE=.*/MODE=$m/' $CONF").out
    }

    /**
     * Why MODE=playback may never engage: the daemon looks for a RUNNING
     * substream in procfs, and audio routed through the DSP offload path may
     * never appear there. This dumps what it can actually see.
     */
    fun playbackProbe(): String = Shell.run(
        "echo '--- pcm playback states ---'; " +
        "grep -H state /proc/asound/card*/pcm*p/sub*/status 2>/dev/null; " +
        "echo; echo '--- what the module concludes ---'; " +
        "sh $CTL status | grep -i playback"
    ).out

    /** Live mixer value, as opposed to what actions.conf says it should be. */
    fun liveGain(): String = Shell.run("sh $CTL ctl 'Handset Volume'").out

    fun readConf(): String = Shell.run("cat $CONF 2>/dev/null").out

    // ---- the rest of the stereoctl surface -------------------------------

    fun apply() = ctl("apply").out
    fun revert() = ctl("revert").out
    fun restart() = ctl("restart").out
    fun dump() = ctl("dump").out
    fun scan(pattern: String) =
        if (pattern.isBlank()) ctl("scan").out else ctl("scan '$pattern'").out
    fun diff(seconds: Int) = ctl("diff $seconds").out
    fun guard(on: Boolean) = ctl(if (on) "guard on" else "guard off").out
    fun guardStatus() = ctl("guard status").out
    fun xmlPatch() = ctl("xml-patch").out
    fun xmlRevert() = ctl("xml-revert").out
    fun reset() = ctl("reset").out
    fun help() = ctl("--help").out

    fun ctlGet(name: String) = Shell.run("sh $CTL ctl '$name'").out
    fun ctlSet(name: String, value: String) = Shell.run("sh $CTL ctl '$name' '$value'").out

    /** Escape hatch: run any stereoctl subcommand the UI does not model. */
    fun raw(args: String) = ctl(args).out

    fun setSetting(key: String, value: String): String =
        Shell.run("grep -q '^$key=' $CONF && sed -i 's|^$key=.*|$key=$value|' $CONF || echo '$key=$value' >> $CONF").out

    fun readSetting(key: String): String =
        Shell.run("grep -m1 '^$key=' $CONF 2>/dev/null | cut -d= -f2- | tr -d \"'\"").out.trim()

    fun readActions(): String = Shell.run("cat $ACTIONS 2>/dev/null").out
}
