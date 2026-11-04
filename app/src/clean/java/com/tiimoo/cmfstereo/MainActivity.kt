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
import kotlin.math.roundToInt

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

/**
 * Plain levels for people, mapped onto what the module actually holds.
 *
 * Earpiece: Handset Volume 0 = +8 dB (loudest) ... 18 = -10 dB, so level
 * 10 = 0 and each level is 2 steps (2 dB) down.
 *
 * Speaker (stereo only): STEREO_ATTEN_DB 6 dB (the module's default margin) at
 * level 10 ... 24 dB at level 1. It never goes below 6 dB here: that margin
 * stands in for the speaker protection stereo turns off.
 */
private object Levels {
    fun earpieceLevel(handsetVolume: Int?): Int? =
        handsetVolume?.takeIf { it in 0..StereoCtl.MAX_GAIN }
            ?.let { (10 - (it / 2.0).roundToInt()).coerceIn(1, 10) }

    fun handsetVolume(level: Int): Int = ((10 - level.coerceIn(1, 10)) * 2)

    fun speakerLevel(attenDb: String): Int =
        (10 - (((attenDb.toIntOrNull() ?: 6) - 6) / 2.0).roundToInt()).coerceIn(1, 10)

    fun attenDb(level: Int): Int = (10 - level.coerceIn(1, 10)) * 2 + 6
}

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
                    "Left from the earpiece, right from the bottom speaker. Hold the phone " +
                        "with the earpiece on your left."
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
    val earLevel = Levels.earpieceLevel(s.gain)
    if (earLevel != null) {
        LevelPanel(
            title = "EARPIECE LEVEL",
            hint = "10 is the loudest the earpiece goes. Turn it down if it ever buzzes.",
            level = earLevel,
            ndot = ndot,
            enabled = !busy
        ) { lv -> run("EARPIECE $lv") { StereoCtl.setGain(Levels.handsetVolume(lv)) } }
    }

    if (s.supportsChannels && s.channels == "stereo") {
        LevelPanel(
            title = "SPEAKER LEVEL",
            hint = "Balances the two sides. Lower it until the earpiece side is as clear " +
                "as the speaker side. Lower is also gentler on the speaker.",
            level = Levels.speakerLevel(s.stereoAttenDb),
            ndot = ndot,
            enabled = !busy
        ) { lv -> run("SPEAKER $lv") { StereoCtl.setStereoAtten(Levels.attenDb(lv)) } }
    }

    if (confirmStereo) {
        NothingDialog(
            ndot = ndot,
            title = "STEREO",
            text = "Real left and right: earpiece left, speaker right.\n\n" +
                "In stereo the speaker's automatic protection is off for music, so " +
                "its level is held lower instead. Use Mono for loud listening.\n\n" +
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
    onSet: (Int) -> Unit
) = Panel {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { Heading(title, ndot) }
        Glow("$level", ndot, 26.sp)
    }
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in 1..10) {
            val lit = i <= level
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
