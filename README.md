<p align="center"><img src="https://raw.githubusercontent.com/Dr-Essence/CMF-true-stereo-manager/main/docs/banner.png?v=9" alt="CMF Stereo, the app for CMF true stereo" width="100%"></p>

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

**Current release: v1.0.97-beta** (both editions; versions are marked beta from now on), signed with the permanent key (installs over
v1.0.35 and later). For module v1.0.6+ (protection, leveller, instant sync); older modules show
fewer settings. The history of every build is in [CHANGELOG.md](CHANGELOG.md).

**Instruction manual (early beta):** `CMF-Stereo-Manual.pdf`, attached to the latest release,
covers both editions and CMF L/R Test screen by screen (source in [docs/manual](docs/manual)).
It is still being written: some screenshots are still placeholders.

**Verified on the phone** (CMF Phone 1 A015, Nothing OS 4.1 `Tetris-B4.1-260615-1652-EEA`,
Android 16, module v1.0.6)
- Both editions: on/off, Mono/Stereo switching live, earpiece and speaker level; the clean
  edition in NDot from the phone.
- Protection (Speaker boost, Deep bass guard, Earpiece limiter) and the Earpiece leveller, set
  from the apps.
- **Stereo in Nothing Settings** (LSPosed, scope Settings): the "Stereo speakers" row in
  Settings > Sound, above Dirac, without icon or divider, and its page inside Settings:
  Nothing's own rows, the toolbar "STEREO SPEAKERS", levels as dots and choices as pills
  (titles dimmed only when a row is off), a description under every row, the real state on
  open (no reset).
- The Quick Settings tiles appear in the tile editor, one per edition.
- With the apps on internal storage, LSPosed lists them (on adopted storage it cannot; see
  CHANGELOG, v1.0.63).
- CMF L/R Test: MUSIC, MOVIE, GAME, SPEECH, MOVIE + EFFECT, AAudio (both), float and the
  driver recording; compressed audio is refused by the phone (offload not granted).
- Settings search (v1.0.81): searching stereo, earpiece, speaker or bass guard in Nothing's
  Settings lists the "Stereo speakers" rows (v1.0.80 found only the apps; see Fixed in
  v1.0.81), and a tap on a result opens the "Stereo speakers" page inside Settings.

**Not seen on the phone yet**
- **CMF L/R Test: RECORD ONLY, and a 5-second countdown before every test** (Dr.Essence:
  time to make silence and set up). RECORD ONLY runs on any device without root (his S22
  Ultra and Legion Tab) and changes nothing there: it records its own microphone (UNPROCESSED
  where offered, else VOICE_RECOGNITION; two channels if given; 48 kHz float) while the CMF
  plays the RESPONSE STUDY, shows the level live, and saves cmf-record-<device>-<time>.wav
  and .txt to Download. Several devices' microphones colour the sound differently while the
  speakers' sound is the same in each: where they agree is the speakers. (No algorithm can
  separate a speaker from an unknown microphone exactly: a recording is the product of both;
  a calibrated microphone such as a Dayton iMM-6C would remove the last of it.) The countdown
  (`Countdown`) runs before the beep tests, DRIVER STUDY, SYNC TEST, RESPONSE STUDY, RECORD
  ONLY and once at the start of FULL CHECK (not per test there: it runs them on a timer).
  Compiled; not run yet.
- **CMF L/R Test: RESPONSE STUDY** (new, for the EQ; Dr.Essence: "measure every single
  detail and every frequency"; the EQ must stay clean and never change the frequencies in the
  music). Each driver alone, in Stereo, upright, media volume at maximum: an exponential sine
  sweep 20 Hz-20 kHz, 6 s, at -24 and -12 dBFS (under the earpiece limiter's -2.5 and the
  speaker's 6 dB attenuation). Recorded at once: the microphones, the HAL's own recordings
  (what reached it: the sweep after Android's effects, Dirac's speaker tuning among them, which
  the driver study found adds +5 to +6 dB at 200-500 Hz and a second harmonic at 100 Hz), the
  audio DSP's input and output, and the microphones' raw capture inside the HAL; plus plan.txt
  (every sweep's start, channel and level) and notes.txt (the module's settings). Packed into
  /sdcard/cmf-sweep.tgz; the analysis (frequency response and harmonic distortion at every
  frequency, Farina's deconvolution; Dirac's curve; each driver's) is done from the files.
  Compiled; not run on the phone yet. Pending: Dr.Essence's run and its analysis, then the EQ.
- **SYNC TEST, second version, and the first result from the phone: in step.** Dr.Essence's
  run said "the earpiece is 60 ms late"; his recording shows otherwise. By hand: 18 pairs, the
  earpiece-to-speaker gap 249.48 ms every time (spread 0.00 ms) against 250 played, the 0.52 ms
  being the earpiece's extra 15 cm to the bottom microphone (about 0.45 ms): **under 0.1 ms
  apart, steady**. The one stream feeding both drivers keeps them together, as designed. Three
  mistakes of the first version, now fixed, the timing moved to `SyncAnalysis` (plain Kotlin,
  unit tests `./gradlew :modetest:testDebugUnitTest`, also on his recording with
  `-Psync.wav=...`, which gives 18 pairs at -0.44 ms, spread 0.00, one misread pair left out):
  the finger leaving START (the loudest sound in the file, before any click) was taken for the
  first click; the room level was a mean that such a tap pulls up; a sound before the first
  click (likely the route starting, at 1.51 s) became the only anchor. Now clicks are looked
  for only after playback starts, through a 3 kHz band-pass, the room level is a median, every
  early sound is tried as an anchor (left or right) and the best-aligned reading kept, and
  pairs more than 5 ms from the median are left out and counted. His phone gives one
  microphone on both channels (VOICE_RECOGNITION): the result now says so, with the distance
  explanation, instead of claiming to cancel it. The music "tick... tock" (left, then right
  250 ms later) is meant to be heard that way.
- **CMF L/R Test: SYNC TEST** (new): do the earpiece and the speaker play at the same moment?
  Another CMF Phone 1 stereo project (lufrz's LineageOS tree) feeds the earpiece a stream of
  its own and delays it by a measured 72 ms; CMF True Stereo feeds both from one stream, so
  they should be in step by design. The test measures it: phone upright and flat (no swap:
  left is the earpiece), 20 pairs of 2 ms 3 kHz clicks, left then right 250 ms later in the
  same stream, both microphones timing each click (where its envelope first reaches 30% of
  its peak); offset = measured gap - 250 ms, per microphone, and their mean, which cancels
  the microphones' distance to each driver. Checked before the build on synthetic
  recordings with a known answer (0, +3 and -2 ms, room noise, reverb, a quieter earpiece,
  0.45 ms of distance each way): the two microphones' mean gave -0.00, +2.98 and -2.01 ms.
  Results on screen, /sdcard/cmf-sync.txt and the recording /sdcard/cmf-sync.wav.
- **Where CMF L/R Test goes** (Dr.Essence): its APK is built into its own "lr-test"
  artifact on every build, ships with the next tagged release of the apps (attached beside
  the two editions), and no longer goes to the rolling "Latest build" prerelease (the test
  APKs already there stay). Pushes to `claude/*` branches now build too (artifacts only,
  nothing is published: publishing stays on main).
- **v1.0.97-beta, Pro warns before lowering the speaker's margin.** Stereo attenuation under 6 dB
  (the margin CMF Stereo never goes below) now asks first, and a red line under the field
  says what is at stake: with the module's protection running, its limiter still holds the
  speaker's peaks but Awinic's thermal protection is off in stereo; without it (after a live
  switch from a mono boot, until a reboot) only the amp's own cut-outs protect the speaker.
  Not compiled here (the Compose libraries are not cached in this environment); CI builds it.
- The Quick Settings tiles' new names ("Stereo" / "Stereo Pro") and a tap on them.
- The page updating by itself while open after a change made outside it (the app, stereoctl,
  the module switching to mono).
- The MOVE TO INTERNAL STORAGE panel (shown only when an override put the app on adopted
  storage).
- The NOTHING OS UPDATED panel (only after a Nothing OS update, which this phone may not get).

**Pending**
- Nothing: the earpiece EQ was considered and left to the user's own equaliser (the earpiece
  sounds good as it is). DRIVER STUDY's rerun, which was for that, is no longer needed; the
  test stays in CMF L/R Test, its fixed alignment not yet run on the phone.

**Fixed in v1.0.81**
- Settings search found none of the page's rows with v1.0.80 (seen on the phone). Settings
  Intelligence re-reads search providers only when its string of indexed providers and versions
  changes (`IndexDatabaseHelper.buildProviderVersionedNames`), so an indexing done before the
  LSPosed hook was on skipped the rows until the next version; and with both editions installed
  the Pro was skipped whenever CMF Stereo was installed, even an older one without the rows.
  The hook adds a marker to that string, and the Pro gives way only to an edition with the
  search provider. The rows of v1.0.80 (`nothing/SearchProvider.kt`, `SearchRows.kt`,
  `SearchHook.kt`) are otherwise unchanged.

**Fixed in v1.0.79**
- The themed icon (Nothing Launcher's "Nothing" icon style) looked small and grey next to
  Nothing's own glyphs (screenshot): the launcher draws an app's monochrome layer at about
  0.58 of its coloured icon. The monochrome layer is now 1.3x larger with bolder dots (radius
  2.0 instead of 1.37); the coloured icon is unchanged. Not seen on the phone yet.
- CMF L/R Test had Android's default icon. It now has an NDot crash-test target: yellow and
  black quarters on the dot grid (the black ones as unlit grey dots), with a themed layer.
  Not seen on the phone yet.
- Seen on the phone: CMF Stereo Pro still showed v1.0.75's kinked outer wave; that phone's
  Pro app was older than v1.0.76 (both editions are built from the same icon). Updating
  Pro gives it the round wave.

**Fixed in v1.0.76**
- The icon's outer wave had a kink on each side; two of its dots moved one column out, so it
  is a round arc around the speaker like the inner wave (icon and themed icon). Not seen on
  the phone yet. Nothing2Something's (formerly Nothing Launcher+) live icon finds the waves by column (outer: 9-11,
  9 dots), which still holds.

**Fixed in v1.0.75**
- Both apps, the Settings page and the Sound row said "earpiece left, speaker right"; left and
  right follow the screen (module v0.9.5+), and the texts now say so.

**Fixed in v1.0.71** (since v1.0.60; details in CHANGELOG.md)
- The Settings page reset every level and setting when opened.
- The page's toolbar said "MEDIA"; its dropdown menus covered the rows (replaced by dots and
  pills); the dot and pill rows' titles stayed grey while on.
- Settings showed an icon and a divider line on the Sound row; both Quick Settings tiles were
  called "Stereo".
- Apps installed on adopted storage never appeared in LSPosed: both editions are now
  internal-storage only.
- Every look at the state cost a root call (Magisk's toasts hit Android's quota): the state is
  now cached and pushed by the module.

---

## Changelog

The newest release is the stable one; earlier ones are kept as pre-releases for reference.
Each release's own page has its notes, and [CHANGELOG.md](CHANGELOG.md) has every build.

**v1.0.97-beta** (8 Nov 2026): Pro warns before setting the speaker's stereo margin under 6 dB.
**v1.0.95-beta** (8 Nov 2026): versions are marked beta from now on (the build number keeps rising,
so it installs over v1.0.81); new banner.
**v1.0.81** (8 Nov 2026): Settings search finds the stereo settings (v1.0.80 did not).
**v1.0.80** (8 Nov): the Stereo speakers settings are findable in Settings search.
**v1.0.79** (8 Nov): a bolder icon in Nothing's icon style; CMF L/R Test gets an NDot
crash-test icon.
**v1.0.76** (7 Nov): a rounder outer sound wave on the icon.
**v1.0.75** (7 Nov): every text says left and right follow how you hold the phone.
**v1.0.71** (6 Nov): Stereo in Nothing Settings (optional, LSPosed), Quick Settings tile,
changes shown everywhere at once without root, installs on internal storage.
**v1.0.60** (5 Nov): earpiece leveller setting (module v1.0.5).
**v1.0.56** (5 Nov): stereo protection settings: speaker boost, deep bass guard, earpiece
limiter (module v1.0.4).
**v1.0.53** (5 Nov): both speakers in games (module v1.0.3); a notice after a Nothing OS
update.
**v1.0.39** (4 Nov): new dot-matrix icon, animated with Nothing2Something; licence CC BY-NC-SA
4.0.
**v1.0.35** (4 Nov): a permanent signing key, so updates install over the previous version;
instant Mono/Stereo switch; left and right follow the screen.
**v1.0.29** (4 Nov): about 1 MB instead of about 100 MB, and faster.
**v1.0.25** (4 Nov): two editions, CMF Stereo and CMF Stereo Pro; Mono/Stereo switch; the
earpiece at its real maximum; speaker level; Nothing's NDot font.
**v1.0.21** (3 Nov): the first build of the two editions.

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
| Controls | on/off, Mono/Stereo, earpiece and speaker level on a plain 1–10 scale, speaker boost, deep bass guard, earpiece leveller and limiter | the same, in raw values, plus the Pro-only tools below |
| Developer tools | none | mixer controls, diff, probe, sweep, logs, config files |

The clean edition uses Nothing's own NDot font from the phone (it prefers
the JP Extended cut, `Ndot77JPExtended.ttf` on the CMF Phone 1, then NDot 57,
then any NDot), so nothing of Nothing's
ships in the APK. Phones without it fall back to monospace.

Its levels map onto the module like this:

- **Earpiece level** 10 = `Handset Volume` 0 (+8 dB, the receiver's maximum),
  each step 2 dB down to level 1 (−10 dB).
- **Speaker level** (stereo only) 10 = `STEREO_ATTEN_DB` 6 dB, each step 2 dB
  more down to level 1 (24 dB). It never goes below the 6 dB margin that,
  with the module's protection, stands in for Awinic's in stereo.

## What it does

### In both editions

- **Earpiece speaker on/off** (arm / disarm the earpiece output).
- **Channels: Mono / Stereo**: mono is stock audio (both drivers play the
  full mix, with Awinic speaker protection); stereo plays left and right on
  the earpiece and the speaker, following the screen. Switches at once (module v0.9.4+),
  warns before stereo, and offers a restart only when a live switch could not be
  done. In stereo, sets the amp attenuation (`STEREO_ATTEN_DB`). Needs module
  v0.9.0+; on older modules the switch is hidden. How and why: the module's
  README, *Mono and stereo*.
- **Earpiece and speaker levels**: a plain 1–10 scale in CMF Stereo (mapped as
  above), the exact dB in Pro.
- **Protection** (stereo, module v1.0.4+): speaker boost, deep bass guard,
  earpiece limiter, and the earpiece leveller (module v1.0.5+).
- **Quick Settings tile** (Mono / Stereo), and with LSPosed the **Stereo
  speakers page inside Nothing Settings**, found by Settings search (see
  *Nothing Settings and Quick Settings*).

### Only in CMF Stereo Pro

- What is running and what is staged, and whether the module's protection
  library runs.
- Earpiece gain slider in dB, −10 to +8 (`Handset Volume` 18 … 0; on this
  codec 0 is the loudest and 31 is mute; earlier versions had that the
  wrong way round).
- Mode: apply only while audio is playing, or always.
- **Solo**: speaker off, earpiece only, for auditioning.
- **Doctor**: diagnose why the earpiece is silent.
- Developer tools: mixer controls, diff, probe, sweep, log, report and the
  config files, with their output shown in the app.

Everything either edition does is a call to the module's own `stereoctl`, or an edit to
`/data/adb/cmf-stereo/*.conf`. Nothing is duplicated — the module stays the
source of truth, and the terminal keeps working exactly as before.

## Requirements

- The `CMF-true-stereo` module, installed and rebooted ([Releases](https://github.com/Dr-Essence/CMF-true-stereo/releases))
- Root (Magisk / KernelSU / APatch) and superuser permission for this app
- Android 8.0+
- Optional, for Stereo in Nothing Settings: LSPosed, with the app on internal storage

## Nothing Settings and Quick Settings

The apps themselves need no hook: the settings are text files a shell daemon reads, so they
are ordinary root apps. They talk to `su` directly (commands go over its stdin) rather than
through a root library, to keep the dependency list to Compose alone.

Optionally, each edition is also an **LSPosed module** (scope: Settings) that puts Stereo
inside Nothing's Settings: a "Stereo speakers" row in Settings > Sound and its own page inside
Settings, in Nothing's rows. Settings only shows such rows from system apps, so the module's
hooks mark its own row as one and build its page; nothing else in Settings changes, and if a
Settings update no longer matches, nothing is hooked and the row opens the app. Enable one
edition in LSPosed (both would show two rows), then force-stop Settings. The Quick Settings
tile needs no LSPosed.

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


## Credits

- **[Dr.Essence](https://github.com/Dr-Essence)**: author and maintainer. Ideas, design decisions, and every test on a real phone (logs, recordings, measurements) that the work here rests on.
- Code written with [Claude Code](https://claude.com/claude-code).
