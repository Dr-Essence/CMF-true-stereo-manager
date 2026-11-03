# cmf-fake-stereo-manager

A small root app for controlling the
[cmf-fake-stereo](https://github.com/Tiimoo-debug/cmf-fake-stereo) Magisk
module without a terminal.

The module works perfectly well on its own — this is the optional friendly
front end, in the same spirit as
[PerfMTK](https://github.com/JUANIMAN/PerfMTK) and its separate
[PerfMTK-Manager](https://github.com/JUANIMAN/PerfMTK-Manager).

## What it does

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

- The `cmf-fake-stereo` module, installed and rebooted
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

The release build is **debug-signed** so CI can produce an installable APK
without a secret key. Swap in a real signing config before publishing
anything you intend to update in place.
