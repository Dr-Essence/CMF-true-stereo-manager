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

**Current release: v1.0.75** (both editions), signed with the permanent key (installs over
v1.0.35 and later). For module v1.0.6+ (protection, leveller, instant sync); older modules show
fewer settings. The history of every build is in [CHANGELOG.md](CHANGELOG.md).

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

**Not seen on the phone yet**
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
| Controls | on/off, Mono/Stereo, earpiece and speaker level on a plain 1–10 scale, speaker boost, deep bass guard, earpiece leveller and limiter | everything below, in raw values |
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

## What it does (Pro)

- **Channels: Mono / Stereo** — mono is stock audio (both drivers play the
  full mix, with Awinic speaker protection); stereo plays left and right on
  the earpiece and the speaker, following the screen. Switches at once (module v0.9.4+),
  shows what is running and what is staged, warns before stereo, and offers a
  reboot only when a live switch could not be done. In stereo, sets the amp
  attenuation (`STEREO_ATTEN_DB`). Needs module v0.9.0+; on older modules the
  switch is hidden. How and why: the module's README, *Mono and stereo*.
- **Protection** (stereo, module v1.0.4+): speaker boost, deep bass guard,
  earpiece limiter, and the earpiece leveller (module v1.0.5+), with whether the
  module's protection library runs
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
