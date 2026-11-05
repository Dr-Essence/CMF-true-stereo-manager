<p align="center"><img src="https://raw.githubusercontent.com/Dr-Essence/CMF-true-stereo-manager/main/docs/banner.png" alt="CMF Stereo manager" width="100%"></p>

# CMF-true-stereo-manager

**The app for [CMF-true-stereo](https://github.com/Dr-Essence/CMF-true-stereo)**
— real left/right stereo on the CMF Phone 1, controlled without a terminal.

<p align="center">
  <a href="https://github.com/Dr-Essence/CMF-true-stereo-manager/releases/latest"><b>Download the app</b></a> ·
  <a href="https://github.com/Dr-Essence/CMF-true-stereo/releases/latest"><b>Get the module</b></a>
</p>

| | **CMF Stereo** | **CMF Stereo Pro** |
|---|---|---|
| Who it is for | everyone | tinkerers |
| What you get | on/off, Mono/Stereo, earpiece and speaker level 1–10 | everything, in raw values, plus developer tools |
| Look | Nothing style: black, NDot type in glowing green | Material dark |
| File | `CMF-true-stereo-clean-….apk` | `CMF-true-stereo-pro-….apk` |

Install either or both — they sit side by side. You need root and the
[module](https://github.com/Dr-Essence/CMF-true-stereo/releases/latest)
flashed first.

### Stay updated with Komi Store

Shout-out to **[Komi Store](https://github.com/komi-store/komi-store)**, the free, open-source
app store for apps released on GitHub. Search for *CMF-true-stereo-manager* there, install
the edition you want, and Komi Store keeps it updated from this repo's releases. Every release
is signed with the same key, so updates install straight over the app you have.

The module itself is a Magisk zip, not an app, so it is not offered there: flash it from
[its releases](https://github.com/Dr-Essence/CMF-true-stereo/releases/latest) in
Magisk / KernelSU / APatch.

## Status

**Current release: v1.0.56** (both editions, about 1 MB each), signed with the permanent key
(installs over v1.0.35). Works best with module v0.9.5+ (live switching, orientation swap).

**Verified on the phone**
- CMF Stereo (clean): NDot 77 JP Extended loads from the phone, the glow and dot levels
  render, earpiece and speaker levels read correctly, and the title sits below the status bar.

**Not tested on a phone yet**
- v1.0.56: stereo protection settings (module v1.0.4+, `stereoctl protect`). Clean: a
  SPEAKER BOOST row (0-6), DEEP BASS GUARD (off / light / mid / strong) and an EARPIECE
  LIMITER switch, shown in stereo. Pro: the same in the Channels card, with whether the
  library runs. Compiled by CI; not yet seen on the phone.
- Pro help: PLAYBACK_CTL now explains the list of DSP task switches (module v1.0.3: games
  play through `dsp_fast`, so watching `dsp_music` alone left them on the speaker only in
  "While playing", the clean edition's only mode). Text only.
- New: **CMF L/R Test** (`modetest/`, `CMF-LR-test-*.apk` on the rolling build), a small app
  that plays a left-only beep in each of Dirac's speaker tunings: MUSIC, MOVIE, GAME, SPEECH.
  Nothing's audio service picks the tuning from the name of the process that plays
  (`String.contains` against its built-in lists, music then video, navigation, game), so each
  test runs in its own process, named to land in one tuning (`:music`, `org.videolan.vlc.cmftest`,
  `com.google.android.apps.maps.cmftest`, `com.ludo.king.cmftest`). It shows the tuning Dirac
  reports and the screen rotation. First run on the phone: only MUSIC came out right; Dirac's
  tuning showed "not readable" (an app may not read `persist.vendor.newdirac.*`), so the
  test now reads it, and the rotation the module stored, through one root shell. It also has a
  DRIVER RECORDING switch: the audio HAL's own PCM recording (`vendor.streamout.pcm.dump`, set as
  a property and sent through `AudioManager.setParameters`, which is what makes the HAL start
  it), saved to `/sdcard/cmf-driver-dump.tgz`, to see in the data which channel a left-only
  sound is on when it reaches the HAL. FULL CHECK runs the four tests one after another with
  the recording on and logs Dirac's tuning and the rotation each second
  (`/sdcard/cmf-full-check-rot<rotation>.tgz`). First recording (MUSIC, rotation 270, module
  v1.0.0): the beep reaches the HAL on the RIGHT channel (`hal_streamout`), the module's swap
  puts it back on the left (`hal_playbackhandler`), and the DSP passes it on unchanged: the
  other swap happens before the HAL, with AudioFlinger's own swap already switched off.
  MOVIE + EFFECT: the MOVIE beep with a DynamicsProcessing effect on its own session, as VLC
  has, to check whether Android's rotation swap skips such streams (VLC is the one app found
  that it does not swap). Verified on the phone with module v1.0.2: MOVIE + EFFECT and VLC
  follow the rotation.
  New buttons, for the ways apps hand sound to Android other than a plain 16-bit AudioTrack:
  AAUDIO (GAME, LOW LATENCY) (native AAudio, low latency and exclusive, as a game asks; the
  phone's direct MMAP output, `mmap_no_irq_out`, lists only the earpiece and wired headsets
  in the audio policy, so on the speaker Android is expected to use its fast mixer instead),
  AAUDIO SHARED, FLOAT (32-bit float samples) and COMPRESSED (OFFLOAD) (an MP3 of the same
  beep, right channel silent, decoded by the audio DSP; the policy lists MP3 and AAC offload
  on the speaker). Each screen shows what Android granted (MMAP used or not, offloaded or
  not) and the output thread that plays it, read from `dumpsys media.audio_flinger` as
  root. The native player was compiled here with NDK r27c (`-Wall -Werror`); the Kotlin side
  is compiled by CI only (Maven was rate-limited here). Run on the phone (module v1.0.2,
  rotation 270): AAUDIO, AAUDIO SHARED and FLOAT come out on the correct side. COMPRESSED
  shows "this phone refuses MP3 offload" and plays nothing (Android's offload check said no,
  so the test stops there).
  New: DRIVER STUDY, to design protection for the earpiece and the speaker from measurements.
  It reads the running mode first (`stereoctl channels`; it stops without changing anything if
  the module does not answer), switches modes itself, checks after each switch that the mode
  really runs (not only staged for a reboot), and always switches back to the mode it found.
  Upright (rotation 0: no swap), media volume at maximum for the run and restored after:
  1. Stereo: 300 ms tones on the earpiece, then the speaker (11 frequencies, 100 Hz-8 kHz,
     -30 to -3 dBFS, never above -3), with both microphones raw (`UNPROCESSED`, 48 kHz,
     2 channels) and the HAL's own playback and microphone recordings. Per tone: the level
     that reached the HAL on each channel (after Dirac), and each microphone's fundamental,
     harmonics 2-5 and distortion: `/sdcard/cmf-study.csv`.
  2. Mono (stock files, Awinic on): tones on both channels (7 frequencies), with the audio
     DSP's recordings of Awinic's input, output and the amp's current/voltage sense, per tone:
     `/sdcard/cmf-study-mono.csv`. (The stock HAL folds left and right before Awinic, so this
     shows what Awinic does to the speaker, not whether it keeps the channels apart.)
  Notes in `/sdcard/cmf-study-notes.txt`, raw files in `/sdcard/cmf-study-raw.tgz`. The
  measurement is checked with `modetest/study_sim.py` (the same steps on a simulated driver:
  levels read exactly, simulated clipping found, recordings aligned within 10 ms); the Kotlin
  compiles against android-35 here and in CI. First run on the phone (the stereo-only build):
  the phone offers no `UNPROCESSED` source (the app fell back to `VOICE_RECOGNITION`), the
  HAL also wrote its own raw 24-bit two-microphone capture (`hal_capturedataprovider_DspRaw`),
  and a click when recording started fooled the microphones' marker search, so that CSV was
  misaligned. Now the microphones are lined up on the HAL's copy of the playback by their
  loudness envelopes (on that run: 0.76 app, 0.79 raw correlation, the same lag as found by
  hand with `modetest/study_analyse.py`), and the HAL's raw capture is analysed as well (mics
  2 and 3). Next run not yet done.
- Next build: after a Nothing OS update (module v0.9.8+ reports `build: updated`), the clean
  edition shows a NOTHING OS UPDATED panel (stereo off, stock sound until a module update)
  and Pro a "Nothing OS" status row. Compiled by CI; seen only after the next Nothing OS
  update.
- v1.0.39: the NDot-style icon (a speaker and two waves as a dot matrix, with Nothing's red
  dot, on black) and its themed-icon layer, and its live animation with Nothing Launcher+.
  Rendered and checked here; not yet seen on the phone.
- v1.0.35: the Stereo text (left and right follow the screen), and the Mono/Stereo buttons
  switching live (module v0.9.4+).
- Next build: Mono/Stereo applies at once with module v0.9.4 (the module restarts the audio HAL;
  the app only shows "restart the phone" when that could not be done). Wording updated in both
  editions and the Pro guide; compiled, not yet run on the phone.
- v1.0.29: R8-shrunk release (clean 0.86 MB, pro 1.1 MB, built and checked locally),
  and the status read in a single root shell (its script checked against the module).
- CMF Stereo Pro's new dB earpiece slider and its Channels card.
- The Restart phone button in both editions.
- Either edition on a phone without NDot (should fall back to monospace).

**Pending**
- **Permanent signing key:** in place from v1.0.35 (certificate SHA-256
  `35:65:18:89:…:27:EA:8B:FC`). v1.0.35 needs the old app uninstalled one last time; every
  release after it updates in place, also through Komi Store.

**Fixed recently**
- v1.0.29: the APK was 96.7 MB. Release builds were not shrunk, and they carried the whole
  Material extended icon set, which the app never uses. Both are fixed.
- v1.0.29: every refresh after a tap started about fourteen root shells, one per value,
  plus a full status that read every control back. Now it is one shell with `status brief`.
- v1.0.25: clean edition drew its title under the status bar (edge-to-edge on Android 15+).
- v1.0.21: earpiece slider treated 31 as the top; it is the codec's mute value.
- v1.0.21: two editions, Mono/Stereo channels, new names and links.

---

## About

A small root app for controlling the
[CMF-true-stereo](https://github.com/Dr-Essence/CMF-true-stereo) Magisk
module without a terminal.

The module works perfectly well on its own — this is the optional friendly
front end, in the same spirit as
[PerfMTK](https://github.com/JUANIMAN/PerfMTK) and its separate
[PerfMTK-Manager](https://github.com/JUANIMAN/PerfMTK-Manager).

## Two editions

Both are built from this repo and install side by side:

| | **CMF Stereo** (`clean`) | **CMF Stereo Pro** (`pro`) |
|---|---|---|
| For | everyday listening | tuning and diagnosing |
| Look | Nothing's design language: black, NDot type in glowing green | Material dark |
| Controls | on/off, Mono/Stereo, earpiece level and speaker level on a plain 1–10 scale | everything below, in raw values |
| Developer tools | none | mixer controls, diff, probe, sweep, logs, config files |

The clean edition uses Nothing's own NDot font from the phone (it prefers
the JP Extended cut, `Ndot77JPExtended.ttf` on the CMF Phone 1, then NDot 57,
then any NDot), so nothing of Nothing's
ships in the APK. Phones without it fall back to monospace.

Its levels map onto the module like this:

- **Earpiece level** 10 = `Handset Volume` 0 (+8 dB, the receiver's maximum),
  each step 2 dB down to level 1 (−10 dB).
- **Speaker level** (stereo only) 10 = `STEREO_ATTEN_DB` 6 dB, each step 2 dB
  more down to level 1 (24 dB). It never goes below the 6 dB margin that
  stands in for the speaker protection stereo turns off.

## What it does (Pro)

- **Channels: Mono / Stereo** — mono is stock audio (both drivers play the
  full mix, with Awinic speaker protection); stereo puts the left channel on
  the earpiece and the right on the speaker. Shows what is running and what
  is staged, warns before staging stereo, and offers a reboot when the two
  differ. In stereo, sets the amp attenuation that stands in for Awinic's
  protection (`STEREO_ATTEN_DB`). Needs module v0.9.0+; on older modules the
  switch is hidden. How and why: the module's README, *Mono and stereo*.
- Arm / disarm the earpiece output
- Earpiece gain slider in dB, −10 to +8 (`Handset Volume` 18 … 0; on this
  codec 0 is the loudest and 31 is mute — earlier versions had that the
  wrong way round)
- Mode: apply only while audio is playing, or always
- **Solo** — speaker off, earpiece only, for auditioning
- **Doctor** — diagnose why the earpiece is silent
- Log, probe and report, with output shown in the app

Everything it does is a call to the module's own `stereoctl`, or an edit to
`/data/adb/cmf-stereo/*.conf`. Nothing is duplicated — the module stays the
source of truth, and the terminal keeps working exactly as before.

## Requirements

- The `CMF-true-stereo` module, installed and rebooted ([Releases](https://github.com/Dr-Essence/CMF-true-stereo/releases))
- Root (Magisk / KernelSU / APatch) and superuser permission for this app
- Android 8.0+

## Not an Xposed module

There is nothing to hook. The settings are text files a shell daemon reads,
so this is an ordinary root app. It talks to `su` directly (commands go
over its stdin) rather than through a root library, to keep the dependency
list to Compose alone.

## Build

CI builds it on every push (`.github/workflows/build.yml`) and uploads the APK
as an artifact. Locally:

```sh
./gradlew assembleRelease
```

Releases are signed with the project's permanent key, which CI reads from the
repository secrets `SIGNING_KEYSTORE_B64` (the keystore, base64), `SIGNING_STORE_PASSWORD`,
`SIGNING_KEY_ALIAS` and `SIGNING_KEY_PASSWORD`. A local build without them is debug-signed,
and so is a CI build of a fork; neither can update an installed release.

## Licence

© 2026 Dr.Essence, under [CC BY-NC-SA 4.0](LICENSE): use, copy, change and
share it, as long as you credit Dr.Essence, do not use it commercially
(including selling it or anything built from it), and share changed versions
under the same licence. Releases up to v1.0.35 were published under 0BSD;
copies obtained under it keep those terms. The clean edition draws Nothing's
NDot font from the phone at runtime; no Nothing font ships in this app.
