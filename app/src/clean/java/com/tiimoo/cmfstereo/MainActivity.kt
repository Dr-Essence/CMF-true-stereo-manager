package com.tiimoo.cmfstereo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * CMF Stereo - the everyday edition.
 *
 * Nothing's design language: black, monochrome surfaces, NDot type in glowing
 * green. Only what a listener needs: on/off, mono or stereo, and two levels on
 * a plain 1-10 scale. The developer tools live in CMF Stereo Pro; both drive
 * the same module through [StereoCtl].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CleanApp() }
    }
}

private val Black = Color(0xFF000000)
private val Deep = Color(0xFF0B0B0B)
private val Line = Color(0xFF262626)
private val Green = Color(0xFF4CFF7A)
private val DimGreen = Color(0xFF1C6034)
private val White = Color(0xFFE9E9E9)
private val Grey = Color(0xFF8A8A8A)
private val Red = Color(0xFFD71921)


@Composable
private fun CleanApp() {
    val context = LocalContext.current
    val ndot = remember {
        NdotFont.get(context)?.let { FontFamily(Typeface(it)) } ?: FontFamily.Monospace
    }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<StereoCtl.Status?>(null) }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    fun refresh() = scope.launch {
        busy = true
        status = withContext(Dispatchers.IO) { StereoCtl.status() }
        busy = false
    }

    fun run(done: String, block: () -> String) {
        scope.launch {
            busy = true
            withContext(Dispatchers.IO) { block() }
            status = withContext(Dispatchers.IO) { StereoCtl.status() }
            note = done
            busy = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    MaterialTheme(colorScheme = darkColorScheme(background = Black, surface = Deep, primary = Green)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Black)
                // Android 15 draws apps edge-to-edge: keep clear of the status
                // and navigation bars, or the title sits under the clock.
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Glow("CMF STEREO", ndot, 34.sp)
            val s = status
            Text(
                when {
                    s == null -> "READING..."
                    !s.rooted -> "NEEDS ROOT"
                    !s.installed -> "MODULE NOT INSTALLED"
                    else -> listOf(
                        if (s.armed) "ON" else "OFF",
                        if (s.playing) "PLAYING" else "IDLE",
                        s.channels.uppercase()
                    ).joinToString("  ·  ")
                },
                fontFamily = ndot, color = Grey, fontSize = 13.sp, letterSpacing = 2.sp
            )

            when {
                s == null -> {}
                !s.rooted -> Panel { Body("Grant this app superuser access, then reopen it.") }
                !s.installed -> Panel { Body("Flash the CMF Stereo module in Magisk and reboot.") }
                else -> Controls(s, busy, ndot, ::run)
            }

            if (note.isNotBlank()) {
                Text(note, fontFamily = ndot, color = DimGreen, fontSize = 12.sp, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun Controls(
    s: StereoCtl.Status,
    busy: Boolean,
    ndot: FontFamily,
    run: (String, () -> String) -> Unit
) {
    var confirmStereo by remember { mutableStateOf(false) }
    var confirmReboot by remember { mutableStateOf(false) }
    val pending = s.supportsChannels && s.channels != s.channelsStaged

    // ---- on adopted storage: LSPosed cannot load the Settings tile ------------
    val ctx = androidx.compose.ui.platform.LocalContext.current
    if (StereoCtl.onExternalStorage(ctx)) {
        Panel {
            Heading("MOVE TO INTERNAL STORAGE", ndot)
            Spacer(Modifier.height(8.dp))
            Body(
                "CMF Stereo is on the SD card. Its Settings entry (through LSPosed) only " +
                    "works from internal storage.",
                Red
            )
            Spacer(Modifier.height(8.dp))
            Pill("MOVE", ndot, enabled = !busy) { run("MOVED") { StereoCtl.moveToInternal(ctx) } }
        }
    }

    // ---- after a Nothing OS update ------------------------------------------
    if (s.build == "updated") {
        Panel {
            Heading("NOTHING OS UPDATED", ndot)
            Spacer(Modifier.height(8.dp))
            Body(
                "This Nothing OS version is new to the module, so Stereo is off and both " +
                    "speakers play the stock sound. Stereo comes back with a module update " +
                    "that supports it.",
                Red
            )
        }
    }

    // ---- power -------------------------------------------------------------
    Panel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Heading("EARPIECE SPEAKER", ndot)
                Body(if (s.armed) "Playing through both speakers." else "Bottom speaker only.")
            }
            Switch(
                checked = s.armed,
                enabled = !busy,
                onCheckedChange = { on -> run(if (on) "ON" else "OFF") { StereoCtl.setArmed(on) } },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Black, checkedTrackColor = Green,
                    uncheckedThumbColor = Grey, uncheckedTrackColor = Deep,
                    uncheckedBorderColor = Line
                )
            )
        }
        if (s.armed && !s.daemonRunning) {
            Spacer(Modifier.height(12.dp))
            Body("Not running right now.", Red)
            Spacer(Modifier.height(8.dp))
            Pill("RESTART", ndot, enabled = !busy) { run("RESTARTED") { StereoCtl.restart() } }
        }
    }

    // ---- mono / stereo -----------------------------------------------------
    if (s.supportsChannels) {
        Panel {
            Heading("SOUND", ndot)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Choice("MONO", s.channelsStaged == "mono", ndot, Modifier.weight(1f), !busy) {
                    if (s.channelsStaged != "mono") run("MONO") { StereoCtl.setChannels(false) }
                }
                Choice("STEREO", s.channelsStaged == "stereo", ndot, Modifier.weight(1f), !busy) {
                    if (s.channelsStaged != "stereo") confirmStereo = true
                }
            }
            Spacer(Modifier.height(12.dp))
            Body(
                if (s.channelsStaged == "stereo")
                    "Left and right from the earpiece and the bottom speaker. Turn the phone " +
                        "either way: left and right follow the screen."
                else
                    "Both speakers play the same full sound, with full speaker protection. " +
                        "Best for loud listening."
            )
            if (pending) {
                Spacer(Modifier.height(12.dp))
                Body("Could not switch right away. Restart the phone to switch to ${s.channelsStaged}.", Green)
                Spacer(Modifier.height(8.dp))
                Pill("RESTART PHONE", ndot, enabled = !busy) { confirmReboot = true }
            }
        }
    }

    // ---- levels --------------------------------------------------------------
    val earLevel = StereoLevels.earpieceLevel(s.gain)
    if (earLevel != null) {
        LevelPanel(
            title = "EARPIECE LEVEL",
            hint = "10 is the loudest the earpiece goes. Turn it down if it ever buzzes.",
            level = earLevel,
            ndot = ndot,
            enabled = !busy
        ) { lv -> run("EARPIECE $lv") { StereoCtl.setGain(StereoLevels.handsetVolume(lv)) } }
    }

    if (s.supportsChannels && s.channels == "stereo") {
        LevelPanel(
            title = "SPEAKER LEVEL",
            hint = "Balances the two sides. Lower it until the earpiece side is as clear " +
                "as the speaker side. Lower is also gentler on the speaker.",
            level = StereoLevels.speakerLevel(s.stereoAttenDb),
            ndot = ndot,
            enabled = !busy
        ) { lv -> run("SPEAKER $lv") { StereoCtl.setStereoAtten(StereoLevels.attenDb(lv)) } }
    }

    // ---- protection (module v1.0.4+) -----------------------------------------
    if (s.supportsProtect && s.channels == "stereo") {
        LevelPanel(
            title = "SPEAKER BOOST",
            hint = (if (s.protRunning) "" else "Starts with the next sound on the speaker. ") +
                "Louder speaker without more strain: its loudest moments stay where the " +
                "stock protection keeps them. 0 is the sound you had before.",
            level = s.speakerBoost,
            ndot = ndot,
            enabled = !busy,
            min = 0,
            max = 6
        ) { lv -> run("BOOST $lv") { StereoCtl.setSpeakerBoost(lv) } }
        Panel {
            Heading("DEEP BASS GUARD", ndot)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("off" to "OFF", "light" to "LIGHT", "medium" to "MID", "strong" to "STRONG").forEach { (v, l) ->
                    Choice(l, s.bassProtect == v, ndot, Modifier.weight(1f), !busy) {
                        if (s.bassProtect != v) run("BASS ${l}") { StereoCtl.setBassProtect(v) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Body(
                "Holds back only the deepest bass, and only when it gets loud enough to " +
                    "strain the speaker. Quiet bass is untouched.",
                Grey
            )
        }
        if (s.supportsLeveller) Panel {
            Heading("EARPIECE LEVELLER", ndot)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("off" to "OFF", "light" to "LOW", "medium" to "MID", "strong" to "HIGH").forEach { (v, l) ->
                    Choice(l, s.earpieceLeveller == v, ndot, Modifier.weight(1f), !busy) {
                        if (s.earpieceLeveller != v) run("LEVELLER $l") { StereoCtl.setEarpieceLeveller(v) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Body(
                "Brings up the earpiece's quiet parts so it keeps up with the speaker. Loud " +
                    "parts, bass and every other sound stay as they are.",
                Grey
            )
        }
        Panel {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Heading("EARPIECE LIMITER", ndot)
                    Body("Stops the earpiece crackling at its very loudest.", Grey)
                }
                Switch(
                    checked = s.earpieceLimit,
                    enabled = !busy,
                    onCheckedChange = { on -> run(if (on) "LIMITER ON" else "LIMITER OFF") { StereoCtl.setEarpieceLimit(on) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Black, checkedTrackColor = Green,
                        uncheckedThumbColor = Grey, uncheckedTrackColor = Deep,
                        uncheckedBorderColor = Line
                    )
                )
            }
        }
    }

    if (confirmStereo) {
        NothingDialog(
            ndot = ndot,
            title = "STEREO",
            text = "Real left and right from the earpiece and the speaker, following " +
                "how you hold the phone.\n\n" +
                "In stereo the speaker's automatic protection is off for music, so " +
                "its level is held lower, and its deepest bass is held back when it " +
                "gets loud. Use Mono for loud listening.\n\n" +
                "Switches now: the sound stops for a moment, and music needs play " +
                "again. If it cannot switch safely right away, it asks you to restart.",
            confirm = "SWITCH",
            onConfirm = {
                confirmStereo = false
                run("STEREO") { StereoCtl.setChannels(true) }
            },
            onDismiss = { confirmStereo = false }
        )
    }
    if (confirmReboot) {
        NothingDialog(
            ndot = ndot,
            title = "RESTART",
            text = "The phone restarts now to switch to ${s.channelsStaged}.",
            confirm = "RESTART",
            onConfirm = {
                confirmReboot = false
                run("RESTARTING") { StereoCtl.reboot() }
            },
            onDismiss = { confirmReboot = false }
        )
    }
}

// ---- building blocks -------------------------------------------------------

@Composable
private fun Glow(text: String, ndot: FontFamily, size: TextUnit) = Text(
    text,
    style = TextStyle(
        fontFamily = ndot,
        fontSize = size,
        color = Green,
        letterSpacing = 3.sp,
        shadow = Shadow(color = Green.copy(alpha = 0.75f), offset = Offset.Zero, blurRadius = 22f)
    )
)

@Composable
private fun Heading(text: String, ndot: FontFamily) = Text(
    text,
    style = TextStyle(
        fontFamily = ndot,
        fontSize = 15.sp,
        color = Green,
        letterSpacing = 2.sp,
        shadow = Shadow(color = Green.copy(alpha = 0.6f), offset = Offset.Zero, blurRadius = 14f)
    )
)

@Composable
private fun Body(text: String, color: Color = White) =
    Text(text, color = color, fontSize = 14.sp, lineHeight = 20.sp)

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) = Column(
    Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(Deep)
        .border(BorderStroke(1.dp, Line), RoundedCornerShape(20.dp))
        .padding(18.dp),
    content = content
)

@Composable
private fun Choice(
    text: String,
    selected: Boolean,
    ndot: FontFamily,
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit
) = Box(
    modifier
        .clip(CircleShape)
        .background(if (selected) Green else Black)
        .border(BorderStroke(1.dp, if (selected) Green else Line), CircleShape)
        .clickable(enabled = enabled, onClick = onClick)
        .padding(vertical = 12.dp),
    contentAlignment = Alignment.Center
) {
    Text(text, fontFamily = ndot, color = if (selected) Black else White, letterSpacing = 2.sp)
}

@Composable
private fun Pill(text: String, ndot: FontFamily, enabled: Boolean, onClick: () -> Unit) = Box(
    Modifier
        .clip(CircleShape)
        .border(BorderStroke(1.dp, Green), CircleShape)
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 18.dp, vertical = 10.dp)
) {
    Text(text, fontFamily = ndot, color = Green, letterSpacing = 2.sp, fontSize = 13.sp)
}

/**
 * A 1-10 level as a row of dots, Nothing-style: lit dots up to the level. Tap a
 * dot to set it; the change is written when tapped, not while dragging.
 */
@Composable
private fun LevelPanel(
    title: String,
    hint: String,
    level: Int,
    ndot: FontFamily,
    enabled: Boolean,
    min: Int = 1,
    max: Int = 10,
    onSet: (Int) -> Unit
) = Panel {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { Heading(title, ndot) }
        Glow("$level", ndot, 26.sp)
    }
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in min..max) {
            val lit = i <= level && !(i == 0 && level == 0)
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (lit) Green else Black)
                    .border(BorderStroke(1.dp, if (lit) Green else Line), CircleShape)
                    .clickable(enabled = enabled && i != level) { onSet(i) }
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    Body(hint, Grey)
}

@Composable
private fun NothingDialog(
    ndot: FontFamily,
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) = AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Deep,
    title = { Heading(title, ndot) },
    text = { Text(text, color = White, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Start) },
    confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, fontFamily = ndot, color = Green, letterSpacing = 2.sp) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", fontFamily = ndot, color = Grey, letterSpacing = 2.sp) } }
)
