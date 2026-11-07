package com.tiimoo.cmfstereo

/**
 * In-app reference. Everything here is what was actually measured on the
 * CMF Phone 1 (A015 / Tetris, MT6878) rather than general Android lore -
 * where a claim is device-specific it says so.
 */
data class HelpEntry(val title: String, val body: String, val tag: String)

object Help {

    val sections: List<Pair<String, List<HelpEntry>>> = listOf(

        "Start here" to listOf(
            HelpEntry(
                "What this module does",
                "It plays media through the earpiece as well as the bottom speaker, " +
                    "so both drivers are producing sound at once. In Mono (the default) " +
                    "both play the full mix: louder, fuller and taller than the single " +
                    "bottom-firing speaker. In Stereo the earpiece and the speaker play " +
                    "left and right, following how you hold the phone.",
                "overview intro what"
            ),
            HelpEntry(
                "Mono or stereo",
                "Mono is stock audio with Awinic's speaker protection. Stereo gives real " +
                    "left/right (earpiece and speaker, following the screen) by switching off the two " +
                    "places the phone folds the speaker to mono, and replaces Awinic's " +
                    "protection on media with a fixed amp attenuation. Use Mono for loud " +
                    "listening. Switching restarts the audio HAL (a moment of silence) and falls back " +
                    "to a reboot only when that cannot be done safely. See 'Where the mono fold is'.",
                "stereo mono channels left right"
            ),
            HelpEntry(
                "Everyday use",
                "There isn't any. Flash the module, reboot, play something. The " +
                    "routing applies while audio plays and releases when it stops. " +
                    "The app is optional - the module never needs it.",
                "usage daily normal"
            ),
            HelpEntry(
                "If the earpiece goes silent",
                "Speaker tab > Doctor. It walks the six things that can cause " +
                    "silence - kill switch, dead daemon, unusable tinymix, idle " +
                    "playback, a blocking guard, a control that will not take its " +
                    "value - and names the one responsible.",
                "silent broken troubleshoot doctor"
            )
        ),

        "Commands" to listOf(
            HelpEntry("status", "Module, daemon and live control state. 'want=' is what the config asks for, 'now=' is what the mixer actually holds; a mismatch is the fault.", "status"),
            HelpEntry("doctor", "Diagnoses silence in six ordered steps and names the culprit. Needs module v0.7.1+. It deliberately does nothing when playback is idle - a diagnostic that changes what it measures is worse than none.", "doctor diagnose"),
            HelpEntry("on / off", "Arm or disarm. 'off' stops the daemon and restores every control it touched. It survives a reboot, so a disarmed module stays disarmed until you arm it again.", "on off arm disarm enable"),
            HelpEntry("solo / unsolo", "Silences the speaker amp so you can hear the earpiece alone. It bypasses the guard first, because disabling the amp is otherwise read as 'headphones took over' and would silence the earpiece too.", "solo audition listen"),
            HelpEntry("guard on / off", "The guard applies the routing only while the speaker amp is enabled, so the earpiece follows the speaker and stays quiet under headphones or Bluetooth. Bypass it when you want the routing regardless.", "guard headphones bluetooth"),
            HelpEntry("apply / revert", "One-shot apply of the configured actions, or restore of the values saved before the module touched them.", "apply revert restore"),
            HelpEntry("restart", "Restarts the watcher daemon. Needed if you edited config by hand and want it picked up immediately.", "restart daemon"),
            HelpEntry("log [n]", "Tail the daemon log. Set LOG_LEVEL=debug in stereo.conf for more.", "log logs debug"),
            HelpEntry("reset", "Clears saved originals, guard bypass, solo state and the kill switch, then restarts. The saved originals survive reinstalls, so a bad one makes every revert restore a wrong value - this is the way out. Needs v0.7.1+.", "reset clear stuck"),
            HelpEntry("ctl NAME [VALUE]", "Read or write one mixer control. With no value it reads. This is the lowest-level tool and the one most worth understanding.", "ctl mixer control"),
            HelpEntry("scan [pattern]", "Search the mixer for controls matching a pattern. Use it to find candidates on unfamiliar hardware.", "scan search find"),
            HelpEntry("dump", "Every mixer control and its current value. On this device that is about 1400 lines.", "dump all controls"),
            HelpEntry("diff [seconds]", "Snapshots the mixer, waits, then shows what changed. Start or stop playback during the wait: this is how the media path on this device was found in the first place.", "diff snapshot compare"),
            HelpEntry("probe [dir]", "Full hardware dump - ALSA topology, all mixer controls, amplifier drivers, HAL configs, MediaTek's AudioParam tree. Only needed on hardware the module has not been mapped on. It is slow (a minute or two).", "probe dump hardware"),
            HelpEntry("report", "Folds the newest probe into one shareable text file at /sdcard/cmf-stereo-report.txt.", "report share"),
            HelpEntry("channels [mono|stereo]", "Shows the running and staged channel mode, or stages one. Stereo patches this phone's own audio HAL and DSP config in the module overlay after SHA-256 checks against the analysed build, and refuses anything else. Applied at once by restarting the audio HAL (module v0.9.4+; refused during a call, undone if the HAL does not come back), otherwise after a reboot; two failed boots revert to mono. Needs module v0.9.0+.", "channels stereo mono"),
            HelpEntry("xml-patch / xml-revert", "Overlays an audio policy that widens the speaker port to stereo. On the CMF Phone 1 it is a no-op: the speaker port is already stereo. It exists for other hardware.", "xml policy")
        ),

        "Settings (stereo.conf)" to listOf(
            HelpEntry("MODE", "'playback' applies the routing only while audio plays - the default, and kinder to the earpiece. 'always' keeps it applied permanently.", "mode playback always"),
            HelpEntry("PLAYBACK_CTL / PLAYBACK_VALUE", "How 'audio is playing' is detected: one or more mixer controls separated by '|', any of them on means playing. Empty means scan /proc/asound for a RUNNING substream, which misses DSP playback entirely. On this phone every kind of output has its own DSP task switch (games: dsp_fast, music players: dsp_music and dsp_primary), so module v1.0.3+ watches all of them.", "playback detect offload"),
            HelpEntry("GUARD_CTL / GUARD_VALUE", "Apply the routing only while this control holds this value. Set to aw_dev_0_switch = Enable here, so the earpiece follows the speaker amp.", "guard"),
            HelpEntry("WATCH_INTERVAL", "How often the routing is re-asserted, in seconds. The HAL resets mixer controls on every route change, so this cannot be large. 2 is the default.", "interval poll"),
            HelpEntry("CHANNELS", "mono or stereo. Set it with the Channels switch or 'stereoctl channels' rather than by hand: stereo needs files staged, and the switch restarts the audio HAL (or waits for a reboot).", "channels stereo mono"),
            HelpEntry("STEREO_ATTEN_DB", "In stereo, the speaker amp is held at least this many dB down (0-30, default 6) through its own volume control, aw_dev_0_volume, which counts in 1/8 dB steps. With the module's protection (v1.0.4+: deep bass guard, earpiece limiter, speaker boost), it is the margin that stands in for Awinic's protection; the speaker boost lowers it while the protection runs. Read every cycle, no restart needed.", "attenuation stereo volume"),
            HelpEntry("LOG_LEVEL", "debug, info or warn.", "log level"),
            HelpEntry("actions.conf syntax", "One action per line, applied in order:\n\n  ctl|<mixer control>|<value>\n  sysfs|<path>|<value>\n  prop|<property>|<value>\n\nThe original value of everything touched is saved and restored on stop.", "actions config syntax")
        ),

        "This device" to listOf(
            HelpEntry(
                "How the earpiece is driven",
                "Media never touches the internal codec on this phone. It runs:\n\n" +
                    "  DL_24CH_CH1/CH2 -> I2SOUT4 -> Awinic aw_dev_0 -> speaker\n\n" +
                    "The earpiece hangs off the internal codec's DAC (ADDA_DL), whose " +
                    "input switches are all off - which is why pointing RCV Mux at the " +
                    "receiver alone produces silence. The switch that matters is " +
                    "ADDA_DL_CH1 DL_24CH_CH1: it feeds that DAC from the same stream " +
                    "already going to the speaker amp. Then RCV Mux = Voice Playback " +
                    "routes it to the earpiece.",
                "routing how works adda rcv"
            ),
            HelpEntry(
                "Where the mono fold is",
                "Measured with a left-only test file and the amp on its right slot " +
                    "(silence there = channels really separate):\n\n" +
                    "- Android output: stereo. The speaker port is already stereo.\n" +
                    "- Dirac: not the fold; removing it alone changed nothing.\n" +
                    "- MediaTek HAL: folds. doStereoToMonoConversionIfNeed writes " +
                    "(L+R)/2 when the phone is set up for one speaker.\n" +
                    "- Awinic speaker protection on the audio DSP (smartpa_awinic): " +
                    "folds. One speaker model, 0.5/0.5 weights in every preset.\n" +
                    "- Routing and amp: stereo. Register 0x06 bits 11:10 pick the amp's " +
                    "slot (left / right / mono).\n\n" +
                    "With both folds off the right slot went silent: real stereo. The " +
                    "earpiece always takes the left channel - its DAC input has no " +
                    "right-channel source. A kernel was never the limit.",
                "stereo why channels split fold dsp hal"
            ),
            HelpEntry(
                "Earpiece gain (Handset Volume)",
                "The receiver amp's gain on the MT6369 codec. Read from its driver: " +
                    "range 0-18, and the register runs opposite to the dB scale ALSA " +
                    "advertises:\n\n" +
                    "  0 = +8 dB (loudest)\n  8 = 0 dB\n  18 = -10 dB\n" +
                    "  31 = -40 dB (the codec's mute value)\n\n" +
                    "The HAL itself sets 0 whenever it drives the receiver. Module " +
                    "versions before v0.9.2 shipped 31, believing it was the top, so " +
                    "the daemon kept pulling the earpiece down to mute while the HAL " +
                    "put it back - the repeating 'Handset Volume: 0 -> 31' log lines. " +
                    "The installer now corrects it to 0.\n\n" +
                    "ADDA_DL_GAIN, the digital gain before the DAC, stays with the HAL " +
                    "(63311 of 65535, -0.3 dB); it is shared with the headphone path.",
                "handset volume gain slider loud louder 31 18 adda"
            ),
            HelpEntry(
                "Safety",
                "In Stereo, Awinic's speaker protection is off for media: the speaker " +
                    "relies on the amp's hardware cut-outs, the stereo attenuation and the module's " +
                    "protection (v1.0.4+: deep bass guard, earpiece limiter). " +
                    "Use Mono for loud listening.\n\n" +
                    "The earpiece is a receiver: no protection circuit, no excursion limit, " +
                    "no thermal cutoff. Distortion, buzz or rattle means back off " +
                    "immediately - that damage is permanent, and it also costs you the " +
                    "earpiece for phone calls. If you run it loud for hours, check " +
                    "whether the top of the phone gets warm.",
                "safety damage warning heat"
            ),
            HelpEntry(
                "Recovery",
                "If a boot ever fails, the module disarms itself after two failed " +
                    "boots. Otherwise: adb shell, then\n\n" +
                    "  touch /data/adb/modules/cmf_stereo/disable\n\n" +
                    "and reboot. Magisk and KernelSU both honour that file.",
                "recovery bootloop disable"
            )
        )
    )
}
