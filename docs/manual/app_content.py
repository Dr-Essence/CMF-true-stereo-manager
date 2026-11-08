# Content of the CMF Stereo user manual (both editions), v1.0.97-beta.
# Every fact here comes from the app's code and docs (repo cmf-fake-stereo-manager)
# or, where the module is explained, from the CMF True Stereo module's README.

TITLE = "CMF Stereo"
SUBTITLE = "User manual for CMF Stereo and CMF Stereo Pro, the apps for the CMF True Stereo module on the CMF Phone 1"
VERSION = "v1.0.97-beta"

CHAPTERS = [

# =====================================================================
# 1  WELCOME
# =====================================================================
{
"title": "Welcome",
"intro": "<p>Thank you for using CMF Stereo. This chapter tells you what the app is, how it relates to the module it controls, which of the two editions suits you, what you need before you start, and the few safety rules that matter most.</p>",
"sections": [

{"title": "About this manual",
"html": """<p>This manual covers version <strong>v1.0.97-beta</strong> of both editions of the app: <strong>CMF Stereo</strong> (the everyday edition) and <strong>CMF Stereo Pro</strong> (the edition for tinkerers). It also covers the small companion app <strong>CMF L/R Test</strong>, which ships with every release.</p>
<p>Each topic is first explained in plain words. Where precise detail helps, it follows in a marked box:</p>
<div class="note"><p><strong>Note</strong> boxes give tips and useful background.</p></div>
<div class="warn"><p><strong>Warning</strong> boxes are about the safety of your speaker, your earpiece or your phone. Please read every one.</p></div>
<div class="tech"><p><strong>Technical</strong> boxes give exact values, file names and commands, for readers who want to know what happens underneath.</p></div>
<p>On-screen labels are written exactly as the app shows them. CMF Stereo writes its labels in capitals (for example EARPIECE SPEAKER); CMF Stereo Pro uses normal sentence case (for example Earpiece output).</p>"""},

{"title": "What CMF Stereo is",
"html": """<p>The CMF Phone 1 has two sound outlets on its front and bottom: the <strong>earpiece</strong> at the top (the one you hold to your ear in a call) and the <strong>bottom speaker</strong>. Out of the box, media plays from the bottom speaker only.</p>
<p>The <strong>CMF True Stereo</strong> module makes the earpiece play media too. In <strong>Mono</strong> both play the same full sound; in <strong>Stereo</strong> they become a real left and right pair, and left and right follow the screen however you turn the phone.</p>
<p><strong>CMF Stereo</strong> is the app for that module. It lets you control the module with taps instead of terminal commands: switch the earpiece on and off, choose Mono or Stereo, set the earpiece and speaker levels, and set the module's stereo protection.</p>
<div class="note"><p>The module works perfectly well on its own. The app is the optional, friendly front end: everything it does is a call to the module's own command, <code>stereoctl</code>, or an edit to the module's settings files. The module stays the source of truth, and the terminal keeps working exactly as before.</p></div>"""},

{"title": "The module and the app",
"html": """<p>Two separate pieces work together:</p>
<table>
<tr><th></th><th>The module: CMF True Stereo</th><th>The app: CMF Stereo / CMF Stereo Pro</th></tr>
<tr><td>What it is</td><td>A Magisk / KernelSU / APatch module (a zip file)</td><td>An ordinary Android app (an APK file)</td></tr>
<tr><td>How you install it</td><td>Flash it in your root manager, then reboot</td><td>Install it like any app</td></tr>
<tr><td>What it does</td><td>All the real work: routing the sound to the earpiece, Mono and Stereo, protection</td><td>Shows the module's state and changes its settings</td></tr>
<tr><td>Needed?</td><td>Yes</td><td>Optional</td></tr>
</table>
<p>Because the app only talks to the module, it can do nothing without it. Install the module first.</p>
<div class="tech"><p>The app reads and writes through a root shell: the module's command line at <code>/data/adb/modules/cmf_stereo/scripts/stereoctl</code>, and its settings in <code>/data/adb/cmf-stereo/stereo.conf</code> and <code>/data/adb/cmf-stereo/actions.conf</code>. The module's daemon reads its configuration every cycle, so changes take effect without restarting anything.</p></div>"""},

{"title": "Two editions",
"html": """<p>Both editions are built from the same code, drive the same module and can be installed side by side.</p>
<table>
<tr><th></th><th>CMF Stereo</th><th>CMF Stereo Pro</th></tr>
<tr><td>Who it is for</td><td>Everyone, for everyday listening</td><td>Tinkerers, for tuning and diagnosing</td></tr>
<tr><td>Look</td><td>Nothing's design language: black, NDot type in glowing green</td><td>Material dark</td></tr>
<tr><td>Controls</td><td>On/off, Mono/Stereo, earpiece and speaker level on a plain 1 to 10 scale, speaker boost, deep bass guard, earpiece leveller and limiter</td><td>The same, in raw values (dB), plus status details and Pro-only tools</td></tr>
<tr><td>Developer tools</td><td>None</td><td>Mixer controls, diff, probe, sweep, logs, config files, and a built-in guide</td></tr>
<tr><td>Quick Settings tile</td><td>Stereo</td><td>Stereo Pro</td></tr>
<tr><td>Release file</td><td><code>CMF-true-stereo-clean-….apk</code></td><td><code>CMF-true-stereo-pro-….apk</code></td></tr>
</table>
<p>Only CMF Stereo Pro has: the detailed status card, the Mode setting (While playing / Always on), Solo and Un-solo, Doctor, the earpiece gain slider in dB, the stereo attenuation field in dB, the daemon settings, the Danger zone, the developer tools and the Guide.</p>"""},

{"title": "Which edition is for you",
"html": """<ul>
<li><strong>Choose CMF Stereo</strong> if you want to switch stereo on, set two levels and listen. Every control is a simple tap, and the speaker can never be set below its safety margin.</li>
<li><strong>Choose CMF Stereo Pro</strong> if you want to see exactly what the module is doing, set values in decibels, or diagnose a problem.</li>
<li><strong>Install both</strong> if you like: use CMF Stereo day to day and keep Pro for the occasional look underneath. They do not get in each other's way, because both read the same state from the module.</li>
</ul>
<div class="note"><p>If you also want the Stereo speakers page inside Nothing's Settings (chapter 6), enable only <em>one</em> edition in LSPosed.</p></div>"""},

{"title": "What you need",
"html": """<ul>
<li>A <strong>CMF Phone 1 (A015)</strong>. On any other phone the module installs and does nothing.</li>
<li><strong>Root</strong> through Magisk, KernelSU or APatch, and superuser permission granted to the app.</li>
<li>The <strong>CMF True Stereo module</strong>, flashed and the phone rebooted. Mono/Stereo needs module v0.9.0 or newer; the protection settings need v1.0.4 or newer; the earpiece leveller needs v1.0.5 or newer; instant updates of the Nothing Settings page for changes made outside the app need v1.0.6. This release of the app is made for module v1.0.6 or newer; older modules show fewer settings.</li>
<li><strong>Android 8.0</strong> or newer.</li>
<li>The app installed on <strong>internal storage</strong> (it refuses to install anywhere else).</li>
<li>Optional, for Stereo inside Nothing Settings and Settings search: <strong>LSPosed</strong>.</li>
</ul>
<div class="tech"><p>Tested phone: CMF Phone 1 (A015), Nothing OS 4.1 <code>Tetris-B4.1-260615-1652-EEA</code>, Android 16, with module v1.0.6. Other regions (Global, India) are untested: the module checks the audio files by hash and only patches the exact ones it was built for, so on a build whose files differ it refuses Stereo and stays in Mono.</p></div>"""},

{"title": "Safety first",
"html": """<p>Please read this before you use Stereo.</p>
<div class="warn"><p><strong>The speaker in Stereo.</strong> In Mono, Awinic's speaker protection (the phone's own) keeps the speaker within its limits from a live model of the speaker. Stereo has to switch that processing off for media. In its place, the module holds the speaker 6 dB below the amp's full output at its loudest, holds back its deepest bass when it gets loud, and relies on the amp's own hardware cut-outs. These are fixed limits: there is <strong>no temperature model</strong>. For long, loud listening, use <strong>Mono</strong>. In Stereo, lower the speaker level and back off at the first distortion.</p></div>
<div class="warn"><p><strong>The earpiece.</strong> The earpiece is a receiver, built for calls. It has no protection circuit, no excursion limit and no thermal cut-off. Distortion, buzz or rattle means turn it down at once: that damage is permanent, and it also costs you the earpiece for phone calls. If you play it loud for hours, check whether the top of the phone gets warm.</p></div>
<ul>
<li>Calls always keep Awinic's protection, in Mono and in Stereo.</li>
<li>CMF Stereo never sets the speaker below its 6 dB margin. CMF Stereo Pro can, but only after a clear warning.</li>
<li>If the phone fails to boot twice in a row with Stereo on, the module removes Stereo by itself and falls back to Mono.</li>
</ul>"""},

{"title": "Mono and Stereo in one page",
"html": """<table>
<tr><th></th><th>Mono</th><th>Stereo</th></tr>
<tr><td>What you hear</td><td>Both speakers play the same full sound</td><td>The earpiece and the bottom speaker play left and right</td></tr>
<tr><td>Speaker protection</td><td>Awinic's full protection, as stock</td><td>The module's own protection and a fixed attenuation</td></tr>
<tr><td>Best for</td><td>Loud listening</td><td>Games, films and music with real sides</td></tr>
<tr><td>Default</td><td>Yes</td><td>No</td></tr>
</table>
<p><strong>Left and right follow the screen.</strong> Hold the phone sideways with the earpiece on your left and the earpiece plays the left channel; turn it the other way and it plays the right. Stereo is never reversed (module v0.9.5 or newer).</p>
<p><strong>Switching is instant.</strong> With module v0.9.4 or newer, the module restarts the phone's audio service: the sound stops for about a second, and you press play again. Only when that cannot be done safely does it ask you to restart the phone.</p>"""},

{"title": "Words used in this manual",
"html": """<table>
<tr><th>Word</th><th>Meaning</th></tr>
<tr><td>Earpiece</td><td>The small speaker at the top of the phone, used for calls. Also called the receiver.</td></tr>
<tr><td>Speaker</td><td>The bottom speaker, driven by an Awinic smart amplifier.</td></tr>
<tr><td>Armed / disarmed</td><td>Whether the module plays media through the earpiece (EARPIECE SPEAKER on or off).</td></tr>
<tr><td>Daemon</td><td>The module's small background program that keeps the earpiece routing applied.</td></tr>
<tr><td>Audio HAL</td><td>The phone's audio service (MediaTek's). The module restarts it to switch Mono and Stereo.</td></tr>
<tr><td>Awinic protection</td><td>The speaker protection processing that runs on the phone's audio DSP in Mono.</td></tr>
<tr><td>Protection library</td><td>The module's own protection (bass guard, limiters, boost, leveller), which runs inside the audio HAL in Stereo.</td></tr>
<tr><td>dB, dBFS</td><td>Decibels. dBFS is relative to digital full scale (0 dBFS is the loudest a signal can be).</td></tr>
<tr><td>Staged / pending</td><td>A Mono/Stereo choice that is saved but waits for a restart before it runs.</td></tr>
<tr><td>LSPosed</td><td>A framework that lets an app change other apps (here: Nothing's Settings). Optional.</td></tr>
</table>"""},
]},

# =====================================================================
# 2  INSTALLING, UPDATING AND REMOVING
# =====================================================================
{
"title": "Installing, updating and removing",
"intro": "<p>Installing takes two steps: the module first, then the app. This chapter also explains where to get the files, how updates work, and how to remove the app or the module again.</p>",
"sections": [

{"title": "Before you start",
"html": """<ul>
<li>Make sure your phone is rooted with Magisk, KernelSU or APatch and that the root manager opens normally.</li>
<li>Download the module zip from the CMF True Stereo releases page (repository <code>Dr-Essence/CMF-true-stereo</code>).</li>
<li>Download the app from the app's releases page (repository <code>Dr-Essence/CMF-true-stereo-manager</code>), or use Komi Store (see below).</li>
</ul>
<div class="note"><p>Every module release lists the SHA-256 checksum of its files, in the release description and in <code>SHA256SUMS.txt</code>, so you can check your download.</p></div>"""},

{"title": "Step 1: flash the module",
"html": """<ol>
<li>Open Magisk, KernelSU or APatch and go to its Modules page.</li>
<li>Install the module zip from storage.</li>
<li>Reboot the phone.</li>
</ol>
<p>After the reboot the module runs in Mono, its default. You can already switch to Stereo without the app: tap <strong>Action</strong> on the module's card in your root manager, or run <code>su -c stereoctl channels stereo</code> in a terminal.</p>
<div class="note"><p>The module is a Magisk zip, not an app, so it is not offered in app stores such as Komi Store. Your root manager offers new module releases as updates.</p></div>"""},

{"title": "Step 2: install the app",
"html": """<ol>
<li>Open the APK of the edition you want: <code>CMF-true-stereo-clean-….apk</code> for CMF Stereo, <code>CMF-true-stereo-pro-….apk</code> for CMF Stereo Pro.</li>
<li>Allow installing from that source if Android asks.</li>
<li>Open the app. Your root manager asks whether to grant it superuser access: allow it.</li>
</ol>
<p>The app then reads the module's state and shows its controls.</p>
<figure class="shot" data-need="Magisk superuser request dialog asking to grant root to CMF Stereo, on first launch"></figure>
<div class="tech"><p>Package names: CMF Stereo is <code>com.tiimoo.cmfstereo.clean</code>, CMF Stereo Pro is <code>com.tiimoo.cmfstereo</code>. Each release APK is about 1 MB.</p></div>"""},

{"title": "Granting root",
"html": """<p>The app needs root because the module's files and command live in the protected <code>/data/adb</code> folder. Without root it can only show a message (NEEDS ROOT in CMF Stereo, No root access in Pro).</p>
<p>If you refused the request by mistake, open your root manager's Superuser list, allow CMF Stereo (or CMF Stereo Pro), and reopen the app.</p>
<div class="note"><p>Your root manager may show a short message (a toast) each time the app uses root. The app keeps these to a minimum: it reads everything it needs in a single root call, and the Nothing Settings page and Sound row read the last known state without root at all.</p></div>"""},

{"title": "Both editions side by side",
"html": """<p>You can install CMF Stereo and CMF Stereo Pro at the same time: they have different package names. Both show the same state, because they both read it from the module. A change made in one appears in the other the next time it reads the state (in Pro, tap Refresh).</p>
<p>Each edition has its own Quick Settings tile (Stereo and Stereo Pro), so two installed tiles are easy to tell apart.</p>
<div class="warn"><p>For the Nothing Settings page, enable only one edition in LSPosed. With both enabled, Settings shows two Stereo speakers rows.</p></div>
<p>If both are installed, update both when a new release comes out.</p>"""},

{"title": "Internal storage only",
"html": """<p>Both editions install on internal storage only. This matters for the Nothing Settings page: LSPosed starts before adopted storage (an SD card formatted as internal storage) is mounted, so it can never load an app that sits there.</p>
<p>If an override still put the app on the SD card, the app shows a panel that moves it back: <strong>MOVE TO INTERNAL STORAGE</strong> in CMF Stereo, <strong>On adopted storage</strong> in Pro. See chapters 3 and 4.</p>
<div class="tech"><p>The manifest sets <code>android:installLocation="internalOnly"</code>. The developer option <em>Force allow apps on external</em> overrides it. The app detects adopted storage by its install path starting with <code>/mnt/expand/</code>; LSPosed then logs "Failed to find package info" at boot. The move runs <code>pm move-package &lt;package&gt; internal</code> as root, and the app restarts.</p></div>"""},

{"title": "Staying updated with Komi Store",
"html": """<p><strong>Komi Store</strong> is a free, open-source app store for apps released on GitHub. Search for <em>CMF-true-stereo-manager</em> there, install the edition you want, and Komi Store keeps it updated from the project's releases.</p>
<p>This works because every release is signed with the same key, so each update installs straight over the app you have.</p>"""},

{"title": "Updating the app",
"html": """<p>Install the new APK over the old one, or let Komi Store do it. You do not need to uninstall first, and your settings stay as they were (they are stored by the module, not by the app).</p>
<ul>
<li>Releases from v1.0.35 on are signed with the project's permanent key and install over each other.</li>
<li>From v1.0.95-beta on, versions are marked <strong>beta</strong>. The build number keeps rising, so v1.0.95-beta installed straight over v1.0.81, and app stores see each one as an update.</li>
<li>If both editions are installed, update both.</li>
</ul>
<div class="warn"><p>Builds older than v1.0.35 were signed with temporary keys. If you still have one, uninstall it once before installing a current release. A build you compile yourself, or one from a fork, is debug-signed and cannot update an installed release either.</p></div>"""},

{"title": "Updating the module",
"html": """<p>Your root manager (Magisk, KernelSU or APatch) offers new module releases as updates; install the update and reboot. The module's settings in <code>/data/adb/cmf-stereo/</code> survive module updates.</p>
<p>Some app features depend on the module version. When an app release needs a newer module, its release notes say so (for example, v1.0.71 asked for module v1.0.6).</p>"""},

{"title": "Which files are in a release",
"html": """<table>
<tr><th>File</th><th>What it is</th></tr>
<tr><td><code>CMF-true-stereo-clean-v&lt;version&gt;-&lt;commit&gt;.apk</code></td><td>CMF Stereo, the everyday edition</td></tr>
<tr><td><code>CMF-true-stereo-pro-v&lt;version&gt;-&lt;commit&gt;.apk</code></td><td>CMF Stereo Pro</td></tr>
<tr><td><code>CMF-LR-test-&lt;commit&gt;.apk</code></td><td>CMF L/R Test, the left/right test app (chapter 7)</td></tr>
</table>
<p>The newest tagged release is the stable one; earlier ones are kept as pre-releases for reference. There is also a rolling pre-release called <em>Latest build</em>, which moves with every change to the code. For normal use, take the tagged release.</p>"""},

{"title": "Uninstalling the app",
"html": """<p>Uninstall CMF Stereo or CMF Stereo Pro like any other app. The module keeps working with the settings you last chose, because those settings belong to the module, not to the app. Its Quick Settings tile goes with it, and so does its Stereo speakers row in Nothing Settings if you used LSPosed.</p>
<div class="note"><p>If you used the LSPosed part, you can also untick the app in LSPosed before uninstalling.</p></div>"""},

{"title": "Uninstalling the module",
"html": """<p>Remove the module in your root manager and reboot. Its uninstaller stops the daemon and restores every audio control it touched.</p>
<p>The folder <code>/data/adb/cmf-stereo/</code> (the module's settings and routing) is left in place, so a reinstall does not lose your settings. Delete it by hand for a clean slate.</p>
<p>Without the module, the app shows MODULE NOT INSTALLED (CMF Stereo) or Module not installed (Pro).</p>"""},

{"title": "If the phone does not boot",
"html": """<p>The module protects the boot by itself: if two boots in a row fail while the Stereo overlay is active, it removes Stereo and the phone boots in Mono. A bad boot costs one extra reboot instead of a trip to recovery.</p>
<p>If you ever need to switch the module off by hand:</p>
<pre>adb wait-for-device shell
su
touch /data/adb/modules/cmf_stereo/disable
reboot</pre>
<p>You can also hold volume down for safe mode. Both Magisk and KernelSU honour the <code>disable</code> file.</p>"""},
]},

# =====================================================================
# 3  CMF STEREO
# =====================================================================
{
"title": "CMF Stereo, the everyday app",
"intro": "<p>CMF Stereo is one scrolling screen of panels in Nothing's style: black, with NDot type in glowing green. This chapter goes through it from top to bottom, including every message it can show.</p>",
"sections": [

{"title": "The screen at a glance",
"html": """<p>From top to bottom, CMF Stereo shows:</p>
<ol>
<li>The title, <strong>CMF STEREO</strong>, and a status line under it.</li>
<li>Any notice that needs your attention: MOVE TO INTERNAL STORAGE, NOTHING OS UPDATED.</li>
<li><strong>EARPIECE SPEAKER</strong>: on or off.</li>
<li><strong>SOUND</strong>: MONO or STEREO.</li>
<li><strong>EARPIECE LEVEL</strong>: 1 to 10.</li>
<li>In Stereo only: <strong>SPEAKER LEVEL</strong>, <strong>SPEAKER BOOST</strong>, <strong>DEEP BASS GUARD</strong>, <strong>EARPIECE LEVELLER</strong> and <strong>EARPIECE LIMITER</strong>.</li>
<li>A short confirmation of your last change, in dim green, at the bottom.</li>
</ol>
<p>Scroll down to see all of it. Panels that do not apply right now are not shown at all.</p>
<figure class="shot" data-need="CMF Stereo main screen in Mono, earpiece on: title CMF STEREO, status line ON · IDLE · MONO, EARPIECE SPEAKER, SOUND with MONO selected, EARPIECE LEVEL"></figure>
<figure class="shot" data-need="CMF Stereo main screen in Stereo, scrolled to show SPEAKER LEVEL, SPEAKER BOOST, DEEP BASS GUARD, EARPIECE LEVELLER and EARPIECE LIMITER panels"></figure>"""},

{"title": "The title and the status line",
"html": """<p>Under the glowing <strong>CMF STEREO</strong> title, a grey line in capitals tells you the state at a glance. While the app reads the module it says <strong>READING...</strong>. Then it shows three words separated by dots, for example <strong>ON · PLAYING · STEREO</strong>:</p>
<table>
<tr><th>Part</th><th>Values</th><th>Meaning</th></tr>
<tr><td>1</td><td>ON / OFF</td><td>Whether the earpiece is switched on (EARPIECE SPEAKER)</td></tr>
<tr><td>2</td><td>PLAYING / IDLE</td><td>Whether the module sees audio playing right now</td></tr>
<tr><td>3</td><td>MONO / STEREO</td><td>The channels the phone is running now</td></tr>
</table>
<p>Instead of those three words, the line can say <strong>NEEDS ROOT</strong> or <strong>MODULE NOT INSTALLED</strong> (next section).</p>
<div class="note"><p>CMF Stereo reads the state when it opens and after every change you make. To see a change made somewhere else (Pro, the Quick Settings tile, a terminal), close and reopen the app.</p></div>"""},

{"title": "NEEDS ROOT and MODULE NOT INSTALLED",
"html": """<p>If the app cannot use root, or cannot find the module, it shows a single panel instead of the controls:</p>
<table>
<tr><th>Status line</th><th>Panel text</th><th>What to do</th></tr>
<tr><td>NEEDS ROOT</td><td>Grant this app superuser access, then reopen it.</td><td>Allow CMF Stereo in your root manager's Superuser list, then close and reopen the app.</td></tr>
<tr><td>MODULE NOT INSTALLED</td><td>Flash the CMF Stereo module in Magisk and reboot.</td><td>Flash the CMF True Stereo module zip in Magisk, KernelSU or APatch, and reboot.</td></tr>
</table>
<figure class="shot" data-need="CMF Stereo showing NEEDS ROOT status line and the panel Grant this app superuser access, then reopen it."></figure>
<figure class="shot" data-need="CMF Stereo showing MODULE NOT INSTALLED and the panel Flash the CMF Stereo module in Magisk and reboot."></figure>
<div class="tech"><p>NEEDS ROOT appears when the root shell does not start or does not run as user 0. MODULE NOT INSTALLED appears when <code>/data/adb/modules/cmf_stereo/scripts/stereoctl</code> does not exist.</p></div>"""},

{"title": "MOVE TO INTERNAL STORAGE",
"html": """<p>This panel appears only if CMF Stereo has ended up on adopted storage (an SD card formatted as internal storage). It says, in red: <em>CMF Stereo is on the SD card. Its Settings entry (through LSPosed) only works from internal storage.</em></p>
<p>Tap <strong>MOVE</strong>. The app moves itself to internal storage and restarts; the confirmation line says <strong>MOVED</strong>. If you use the Nothing Settings page, reboot afterwards so LSPosed can load it.</p>
<figure class="shot" data-need="CMF Stereo with the MOVE TO INTERNAL STORAGE panel and its MOVE pill (only shown when the app is on adopted storage)"></figure>
<div class="note"><p>Everything else in the app works from the SD card too; only the LSPosed part needs internal storage. This panel has not yet been seen on a phone, because the app normally refuses to install on the SD card.</p></div>"""},

{"title": "NOTHING OS UPDATED",
"html": """<p>After a Nothing OS update, the phone's audio files may change. The module only patches files it knows, so it then keeps Stereo off. CMF Stereo tells you with a panel: <em>This Nothing OS version is new to the module, so Stereo is off and both speakers play the stock sound. Stereo comes back with a module update that supports it.</em></p>
<p>There is nothing to set. Keep using Mono, and update the module when a release supports your new Nothing OS build.</p>
<figure class="shot" data-need="CMF Stereo with the NOTHING OS UPDATED panel in red text (only after a Nothing OS update the module does not know)"></figure>
<div class="tech"><p>Shown when <code>stereoctl status</code> reports <code>build: updated</code> (module v0.9.8 or newer). Before anything is mounted at boot the module checks the vendor audio files against the analysed build; if they differ, the Stereo overlay is removed and the phone boots on stock Mono. This panel has not yet been seen on a phone.</p></div>"""},

{"title": "EARPIECE SPEAKER",
"html": """<p>The main switch. On, media plays through the earpiece as well as the bottom speaker; the panel says <em>Playing through both speakers.</em> Off, the module leaves the earpiece alone; the panel says <em>Bottom speaker only.</em></p>
<p>Tap the switch to change it. The confirmation line says <strong>ON</strong> or <strong>OFF</strong>. The choice survives a reboot: a switched-off module stays off until you switch it on again.</p>
<div class="warn"><p>In Stereo, switching the earpiece off stops the module's daemon, and the speaker amp goes back to its stock left slot on the next stream, so the speaker would play the left channel alone. If you want the earpiece off for a while, choose <strong>MONO</strong> first.</p></div>
<div class="tech"><p>On runs <code>stereoctl on</code> (arm), off runs <code>stereoctl off</code> (disarm), which stops the daemon and restores every control it touched.</p></div>"""},

{"title": "Not running right now",
"html": """<p>If the earpiece is switched on but the module's background program (the daemon) is not running, the EARPIECE SPEAKER panel adds a red line, <em>Not running right now.</em>, and a <strong>RESTART</strong> pill.</p>
<p>Tap <strong>RESTART</strong> to start the daemon again; the confirmation line says <strong>RESTARTED</strong>. If the line comes back, see chapter 8.</p>
<figure class="shot" data-need="CMF Stereo EARPIECE SPEAKER panel with the switch on, the red line Not running right now. and the RESTART pill"></figure>
<div class="tech"><p>RESTART runs <code>stereoctl restart</code>, which restarts the watcher daemon.</p></div>"""},

{"title": "SOUND: MONO or STEREO",
"html": """<p>The SOUND panel has two buttons, <strong>MONO</strong> and <strong>STEREO</strong>. The filled one is your choice. Under them, a line explains it:</p>
<ul>
<li>MONO: <em>Both speakers play the same full sound, with full speaker protection. Best for loud listening.</em></li>
<li>STEREO: <em>Left and right from the earpiece and the bottom speaker. Turn the phone either way: left and right follow the screen.</em></li>
</ul>
<p>Tap <strong>MONO</strong> to switch to Mono at once. Tap <strong>STEREO</strong> and the app first shows what Stereo means (next section).</p>
<p>When you switch, the sound stops for a moment and music players pause: press play again.</p>
<div class="note"><p>The SOUND panel only appears with module v0.9.0 or newer. On older modules there is no Mono/Stereo switch.</p></div>
<div class="tech"><p>The buttons run <code>stereoctl channels mono</code> or <code>stereoctl channels stereo</code>. With module v0.9.4 or newer this applies at once by restarting the audio HAL on the wanted files. The selected button shows the chosen (staged) mode, which differs from the running one only while a switch waits for a restart.</p></div>"""},

{"title": "The STEREO dialog",
"html": """<p>Before switching to Stereo, CMF Stereo shows a dialog titled <strong>STEREO</strong>:</p>
<p><em>Real left and right from the earpiece and the speaker, following how you hold the phone.</em></p>
<p><em>In stereo the speaker's automatic protection is off for music, so its level is held lower, and its deepest bass is held back when it gets loud. Use Mono for loud listening.</em></p>
<p><em>Switches now: the sound stops for a moment, and music needs play again. If it cannot switch safely right away, it asks you to restart.</em></p>
<p>Tap <strong>SWITCH</strong> to go to Stereo (the confirmation line says <strong>STEREO</strong>), or <strong>CANCEL</strong> to stay in Mono.</p>
<figure class="shot" data-need="CMF Stereo STEREO confirmation dialog with SWITCH and CANCEL buttons"></figure>"""},

{"title": "Could not switch right away: RESTART PHONE",
"html": """<p>Sometimes the module cannot switch safely while the phone runs. It then saves your choice for the next start, and the SOUND panel says, in green: <em>Could not switch right away. Restart the phone to switch to stereo.</em> (or mono), with a <strong>RESTART PHONE</strong> pill.</p>
<p>Tap <strong>RESTART PHONE</strong>. A dialog titled <strong>RESTART</strong> says <em>The phone restarts now to switch to stereo.</em> (or mono). Tap <strong>RESTART</strong> to reboot now (the confirmation line says <strong>RESTARTING</strong>), or <strong>CANCEL</strong> to restart later yourself.</p>
<figure class="shot" data-need="CMF Stereo SOUND panel with the green line Could not switch right away. Restart the phone to switch to stereo. and the RESTART PHONE pill"></figure>
<figure class="shot" data-need="CMF Stereo RESTART dialog: The phone restarts now to switch to stereo., with RESTART and CANCEL"></figure>
<div class="tech"><p>Reasons the module refuses a live switch: no verified files, a call in progress, or the audio HAL does not come back. The reboot runs <code>svc power reboot</code> (or <code>reboot</code>).</p></div>"""},

{"title": "EARPIECE LEVEL",
"html": """<p>How loud the earpiece plays, from 1 to 10. The current level is shown as a large glowing number, and as a row of ten dots lit up to that level.</p>
<p>Tap a dot to set that level; the confirmation line says, for example, <strong>EARPIECE 8</strong>. The hint reads: <em>10 is the loudest the earpiece goes. Turn it down if it ever buzzes.</em></p>
<p>Level 10 is the earpiece amplifier's real maximum, the same value the phone itself uses in calls. Each level down is 2 dB quieter. The exact numbers are in chapter 5.</p>
<div class="warn"><p>If the earpiece ever buzzes, rattles or distorts, lower this level straight away.</p></div>
<div class="note"><p>This panel only appears when the app can read the earpiece gain from the module's settings. If it is missing, see chapter 8.</p></div>
<figure class="shot" data-need="CMF Stereo EARPIECE LEVEL panel at level 10, all ten dots lit, large glowing 10"></figure>"""},

{"title": "SPEAKER LEVEL (Stereo only)",
"html": """<p>How loud the bottom speaker plays in Stereo, from 1 to 10. It appears only while Stereo is running.</p>
<p>The earpiece is smaller than the speaker and will not match it. Use this level to balance the two: lower it until the earpiece side sounds as clear as the speaker side. The hint reads: <em>Balances the two sides. Lower it until the earpiece side is as clear as the speaker side. Lower is also gentler on the speaker.</em></p>
<p>Tap a dot to set it; the confirmation line says, for example, <strong>SPEAKER 7</strong>.</p>
<div class="note"><p>Level 10 is the safety margin itself: the speaker held 6 dB below the amp's full output. CMF Stereo can never go louder than that. Each level down is 2 dB more, down to 24 dB at level 1. In Mono this setting does not apply.</p></div>"""},

{"title": "SPEAKER BOOST (Stereo only)",
"html": """<p>A louder speaker without more strain, from 0 to 6. The hint reads: <em>Louder speaker without more strain: its loudest moments stay where the stock protection keeps them. 0 is the sound you had before.</em></p>
<p>The row has seven dots, for 0 to 6. Tap one to set it; the confirmation line says, for example, <strong>BOOST 3</strong>.</p>
<p>If the module's protection is not running yet, the hint starts with <em>Starts with the next sound on the speaker.</em> The boost only takes effect while the protection runs.</p>
<div class="note"><p>Shown with module v1.0.4 or newer, while Stereo is running. How it works: chapter 5.</p></div>"""},

{"title": "DEEP BASS GUARD (Stereo only)",
"html": """<p>Four pills: <strong>OFF</strong>, <strong>LIGHT</strong>, <strong>MID</strong> and <strong>STRONG</strong>. The filled one is active. The text reads: <em>Holds back only the deepest bass, and only when it gets loud enough to strain the speaker. Quiet bass is untouched.</em></p>
<p>Tap a pill to choose it; the confirmation line says, for example, <strong>BASS MID</strong>. The default is MID.</p>
<div class="warn"><p>The bass guard is part of what keeps the speaker safe in Stereo. Leave it at MID or STRONG unless you have a reason; OFF lets the deepest bass through at any level.</p></div>
<div class="tech"><p>MID is the module's <code>medium</code> setting; the app runs <code>stereoctl protect bass off|light|medium|strong</code>.</p></div>"""},

{"title": "EARPIECE LEVELLER (Stereo only)",
"html": """<p>Four pills: <strong>OFF</strong>, <strong>LOW</strong>, <strong>MID</strong> and <strong>HIGH</strong>. The text reads: <em>Brings up the earpiece's quiet parts so it keeps up with the speaker. Loud parts, bass and every other sound stay as they are.</em></p>
<p>Tap a pill to choose it; the confirmation line says, for example, <strong>LEVELLER LOW</strong>. The default is OFF.</p>
<div class="tech"><p>Shown with module v1.0.5 or newer. LOW, MID and HIGH are the module's <code>light</code>, <code>medium</code> and <code>strong</code>; the app runs <code>stereoctl protect leveller …</code>.</p></div>"""},

{"title": "EARPIECE LIMITER (Stereo only)",
"html": """<p>A switch, on by default. The text reads: <em>Stops the earpiece crackling at its very loudest.</em></p>
<p>Tap it to change; the confirmation line says <strong>LIMITER ON</strong> or <strong>LIMITER OFF</strong>.</p>
<div class="warn"><p>Keep the limiter on. Measurements showed the earpiece clips in the top 1.5 dB of its range; the limiter holds it below that.</p></div>
<figure class="shot" data-need="CMF Stereo DEEP BASS GUARD panel with MID selected, EARPIECE LEVELLER panel with OFF selected, and EARPIECE LIMITER switch on"></figure>"""},

{"title": "The confirmation line",
"html": """<p>After each change, a short word or two appears in dim green at the bottom of the screen: ON, OFF, RESTARTED, MONO, STEREO, RESTARTING, MOVED, EARPIECE 1 to 10, SPEAKER 1 to 10, BOOST 0 to 6, BASS OFF/LIGHT/MID/STRONG, LEVELLER OFF/LOW/MID/HIGH, LIMITER ON/OFF.</p>
<p>It tells you which change was sent. The panels above it then show the state the app read back from the module afterwards, which is the one that counts.</p>
<div class="note"><p>While a change is being made, every control is briefly disabled, so you cannot send two at once.</p></div>"""},

{"title": "How the dots and pills work",
"html": """<ul>
<li><strong>Dots</strong> (levels): lit dots show the level. Tap any unlit dot, or a lower lit one, to set that level. Tapping the dot you are already on does nothing. The value is written when you tap, not while you slide a finger.</li>
<li><strong>Pills</strong> (choices): the filled pill is the active choice. Tap another to choose it.</li>
<li><strong>Switches</strong>: green means on.</li>
</ul>
<p>On SPEAKER BOOST, the first dot stands for 0 (off). At 0 no dot is lit; at 1 or more, the first dot lights along with the others.</p>"""},

{"title": "When each panel appears",
"html": """<table>
<tr><th>Panel</th><th>Shown when</th></tr>
<tr><td>MOVE TO INTERNAL STORAGE</td><td>The app is on adopted storage</td></tr>
<tr><td>NOTHING OS UPDATED</td><td>The module reports an unknown Nothing OS build (module v0.9.8+)</td></tr>
<tr><td>EARPIECE SPEAKER</td><td>Always (with root and the module)</td></tr>
<tr><td>SOUND</td><td>Module v0.9.0+</td></tr>
<tr><td>EARPIECE LEVEL</td><td>The earpiece gain can be read and is in range</td></tr>
<tr><td>SPEAKER LEVEL</td><td>Stereo is running (module v0.9.0+)</td></tr>
<tr><td>SPEAKER BOOST, DEEP BASS GUARD, EARPIECE LIMITER</td><td>Stereo is running and the module has protection (v1.0.4+)</td></tr>
<tr><td>EARPIECE LEVELLER</td><td>As above, and the module has the leveller (v1.0.5+)</td></tr>
</table>
<div class="note"><p>"Running" matters: while a switch to Stereo waits for a restart, the Stereo-only panels stay hidden until the phone really runs in Stereo.</p></div>"""},

{"title": "The look: NDot and the icon",
"html": """<p>CMF Stereo uses Nothing's own <strong>NDot</strong> dot-matrix font, taken from your phone at runtime: no Nothing font ships inside the app. It prefers the JP Extended cut (<code>Ndot77JPExtended.ttf</code> on the CMF Phone 1), then NDot 57, then any NDot it finds. On a phone without NDot it falls back to a monospace font.</p>
<p>The app icon is a speaker with two sound waves drawn as a dot matrix, with Nothing's red dot, on black. It has a themed (one-colour) layer for Nothing Launcher's Nothing icon style and Android 13+ themed icons. With Nothing2Something (formerly Nothing Launcher+) active on Nothing Launcher, the icon's sound waves pulse out from the speaker while it is on screen.</p>
<div class="tech"><p>The font is looked for in <code>/system/fonts</code>, <code>/product/fonts</code> and <code>/system_ext/fonts</code>, then in Nothing's widgets app (<code>com.nothing.hearthstone</code>). It is copied into the app's own memory before use.</p></div>"""},
]},

# =====================================================================
# 4  CMF STEREO PRO
# =====================================================================
{
"title": "CMF Stereo Pro",
"intro": "<p>CMF Stereo Pro shows everything the module does, in raw values, and adds the tools for tuning and diagnosing. It has four tabs: Speaker, Settings, Developer and Guide. This chapter goes through them card by card.</p>",
"sections": [

{"title": "The screen at a glance",
"html": """<ul>
<li>A top bar with the title <strong>CMF Stereo</strong> and a <strong>Refresh</strong> button.</li>
<li>A progress bar while the app works, with a line saying what is running.</li>
<li>Four tabs: <strong>Speaker</strong> (everyday controls and status), <strong>Settings</strong> (the module's daemon), <strong>Developer</strong> (tools) and <strong>Guide</strong> (a searchable reference).</li>
<li>An <strong>Output</strong> card at the bottom of every tab, showing what the last command printed.</li>
</ul>
<figure class="shot" data-need="CMF Stereo Pro Speaker tab, top: top bar with Refresh, tab row Speaker / Settings / Developer / Guide, and the Status card"></figure>"""},

{"title": "Top bar, Refresh and the progress line",
"html": """<p>Tap <strong>Refresh</strong> to read the module's state again, for example after a change made elsewhere. It is disabled while the app is busy.</p>
<p>While a command runs, a progress bar appears and every control is greyed out. For slow commands the app says what it is doing:</p>
<ul>
<li><em>Running: probe - dumping every mixer control, /sys, dmesg and the param XMLs. This takes a minute or two.</em></li>
<li><em>Running: report - folding the newest probe into one file.</em></li>
<li><em>Running: diff - snapshotting, waiting, then comparing. Start or stop playback now.</em></li>
</ul>
<p>Other commands show <em>Running:</em> followed by their name, for example <em>Running: channels</em>.</p>"""},

{"title": "No root access and Module not installed",
"html": """<p>Without root or without the module, Pro shows a red card instead of the tabs:</p>
<table>
<tr><th>Card</th><th>Text</th></tr>
<tr><td>No root access</td><td>Grant superuser permission, then Refresh.</td></tr>
<tr><td>Module not installed</td><td>Flash CMF-true-stereo and reboot.</td></tr>
</table>
<p>Fix the cause, then tap <strong>Refresh</strong>.</p>
<figure class="shot" data-need="CMF Stereo Pro red card No root access: Grant superuser permission, then Refresh."></figure>"""},

{"title": "The Output card",
"html": """<p>After every action, the <strong>Output</strong> card shows what the module printed, in a small monospace font. If a command printed nothing, it shows the action's name followed by <em>: done</em>. Tap <strong>Clear</strong> to hide it.</p>
<p>This is the place to look when a change seems not to work: the module's own message is there.</p>
<div class="note"><p>Values that the app writes itself are read back and checked. If a write did not stick, the Output card says so, for example <em>FAILED to write gain: actions.conf still reads …</em> or <em>FAILED to write STEREO_ATTEN_DB: stereo.conf reads …</em>.</p></div>"""},

{"title": "Speaker tab: On adopted storage",
"html": """<p>If Pro has ended up on adopted storage, a red card titled <strong>On adopted storage</strong> appears at the top: <em>Installed under /mnt/expand: LSPosed starts before that storage is mounted and never loads the Settings tile's hook ("Failed to find package info"). Move it to internal storage, then reboot.</em></p>
<p>Tap <strong>Move to internal storage</strong>, then reboot.</p>"""},

{"title": "Speaker tab: the Status card",
"html": """<p>The <strong>Status</strong> card lists what the module reports. Values in the accent colour are fine; values in red need a look.</p>
<table>
<tr><th>Row</th><th>Values</th><th>Meaning</th></tr>
<tr><td>Armed</td><td>yes / no</td><td>Whether the earpiece is switched on</td></tr>
<tr><td>Daemon</td><td>running / stopped</td><td>Whether the module's background program runs</td></tr>
<tr><td>Playback</td><td>active / idle</td><td>Whether the module sees audio playing</td></tr>
<tr><td>Mode</td><td>playback / always</td><td>When the routing is applied (see Mode below)</td></tr>
<tr><td>Channels</td><td>mono, stereo, or e.g. <em>mono -&gt; stereo after reboot</em></td><td>The running channels, and a pending switch (module v0.9.0+)</td></tr>
<tr><td>Nothing OS</td><td><em>new build: stereo unavailable</em> or <em>not a CMF Phone 1</em></td><td>Shown only when the Nothing OS build is not the known one</td></tr>
<tr><td>Routing</td><td>e.g. <em>3 actions</em></td><td>How many routing actions are configured; red at 0</td></tr>
<tr><td>Earpiece gain</td><td>e.g. <em>8 dB</em>, <em>unreadable</em>, or <em>31 = mute (update the module)</em></td><td>The configured earpiece gain</td></tr>
<tr><td>Guard</td><td>active / bypassed</td><td>Whether the earpiece follows the speaker amp</td></tr>
<tr><td>Module</td><td>e.g. <em>v1.0.9-beta</em></td><td>The installed module version</td></tr>
<tr><td>App</td><td>e.g. <em>1.0.97-beta</em></td><td>This app's version</td></tr>
</table>
<div class="tech"><p>The card is filled from one root shell: <code>stereoctl status brief</code> (modules before v0.9.3 ignore <em>brief</em> and print the full status), <code>stereoctl --help</code> (to see which commands the module supports), the <code>Handset Volume</code> line of <code>actions.conf</code>, <code>module.prop</code>, the <code>guard_bypass</code> file, <code>stereo.conf</code> and the property <code>vendor.audiohal.cmf_prot</code>.</p></div>"""},

{"title": "Speaker tab: Channels",
"html": """<p>The <strong>Channels</strong> card has a two-part button, <strong>Mono</strong> and <strong>Stereo</strong>, with the chosen one selected. The text under it explains the choice:</p>
<ul>
<li>Mono: <em>Mono: stock audio. Both drivers play the full mix, with Awinic's speaker protection.</em></li>
<li>Stereo: <em>Stereo: the earpiece and the speaker play left and right, following how the phone is held (module v0.9.5+). Awinic's speaker protection is off for media; the amp is held 6 dB down instead, and the module's own protection (module v1.0.4+) holds the deep bass and the earpiece. Calls keep Awinic's.</em> (The number is your current stereo attenuation.)</li>
</ul>
<p>Tapping <strong>Mono</strong> switches at once. Tapping <strong>Stereo</strong> first shows the <em>Switch to stereo?</em> dialog.</p>
<p>The card only appears with module v0.9.0 or newer.</p>
<figure class="shot" data-need="CMF Stereo Pro Channels card in Stereo: Mono/Stereo segmented button, explanation text, Stereo attenuation (dB) field with Set, and the Protection block"></figure>"""},

{"title": "The Switch to stereo? dialog",
"html": """<p>Pro explains in detail what Stereo changes before it switches:</p>
<p><em>Stereo switches off two mono folds: one in MediaTek's audio HAL, and Awinic's speaker-protection processing on the audio DSP. The phone's own vendor files are patched in the module overlay after hash checks; anything unrecognised is refused.</em></p>
<p><em>Without Awinic's processing, media on the speaker relies on the amp's hardware cut-outs, a fixed attenuation and the module's own protection (deep bass guard, earpiece limiter). Use Mono for loud listening and back off at the first distortion.</em></p>
<p><em>Applied at once by restarting the audio HAL (a moment of silence; players pause). If that cannot be done safely - no verified files, a call in progress, or the HAL does not come back - it is undone and waits for a reboot. Two failed boots put it back to mono automatically.</em></p>
<p>Tap <strong>Switch to stereo</strong> or <strong>Cancel</strong>.</p>
<figure class="shot" data-need="CMF Stereo Pro dialog Switch to stereo? with its three paragraphs and the buttons Switch to stereo and Cancel"></figure>"""},

{"title": "Running mono until you reboot: Reboot now",
"html": """<p>If a switch could not be applied live, the Channels card says in red, for example, <em>Running mono until you reboot.</em>, with a <strong>Reboot now</strong> button. The Status card's Channels row shows the same, e.g. <em>mono -&gt; stereo after reboot</em>.</p>
<p>Tap <strong>Reboot now</strong>. The dialog <strong>Reboot now?</strong> says <em>The phone restarts to switch to stereo.</em> Tap <strong>Reboot</strong> or <strong>Cancel</strong>.</p>
<figure class="shot" data-need="CMF Stereo Pro Channels card with the red line Running mono until you reboot. and the Reboot now button"></figure>"""},

{"title": "Stereo attenuation (dB)",
"html": """<p>When Stereo is chosen, the Channels card has a field <strong>Stereo attenuation (dB)</strong> and a <strong>Set</strong> button. This is how far the speaker amp is held down in Stereo: the bigger the number, the quieter and gentler on the speaker.</p>
<p>Type a number from 0 to 30 (digits only, at most two) and tap <strong>Set</strong>. The default is 6. The text under the field reads: <em>How far the speaker amp is held down in stereo (0-30, default 6). This margin stands in for Awinic's protection. Raise it if you play loud; use Mono for loud listening.</em></p>
<p>This is the same setting as CMF Stereo's SPEAKER LEVEL: 6 dB is level 10, 8 dB is level 9, and so on to 24 dB at level 1 (chapter 5). Pro lets you use any whole number in between, and above 24.</p>
<div class="tech"><p>Writes <code>STEREO_ATTEN_DB</code> in <code>stereo.conf</code> (clamped to 0-30), reads it back, and reports <em>stereo attenuation -&gt; N dB (applied on the daemon's next cycle)</em>. The daemon applies it through the amp's own volume control, <code>aw_dev_0_volume</code>, in 1/8 dB steps. An empty field disables Set; a value that is not a number is taken as 6.</p></div>"""},

{"title": "The warning under 6 dB (new in v1.0.97-beta)",
"html": """<p>6 dB is the margin that stands in for Awinic's protection in Stereo. CMF Stereo never goes below it. Pro can, but now warns you first, in two ways.</p>
<p><strong>A red line under the field.</strong> As soon as you type a value under 6, a red line appears:</p>
<ul>
<li>With the module's protection running: <em>Under 6 dB the speaker is held only by the module's protection (its peak limiter). Risky for long loud listening.</em></li>
<li>Without it: <em>DANGER: the module's protection is not running, so under 6 dB nothing but the amp's own cut-outs protects the speaker.</em></li>
</ul>
<p><strong>A dialog before it is set.</strong> Tapping Set with a value under 6 opens a dialog instead of setting it. With the protection running it is titled <strong>Lower the speaker's margin?</strong>; without it, <strong>Speaker unprotected</strong>. Both begin: <em>In stereo, Awinic's speaker protection is off for media. The 6 dB attenuation is the margin that stands in for it; CMF Stereo never goes below it.</em> Then:</p>
<ul>
<li>Protection running: <em>The module's protection is running: its limiter still holds the speaker's peaks where Awinic would in mono. But Awinic's thermal protection is off in stereo, so long loud listening at N dB can overheat and damage the speaker.</em></li>
<li>Not running: <em>The module's protection is NOT running (after a live switch from a mono boot it only starts after a reboot). At N dB only the amp's hardware cut-outs protect the speaker: loud audio can damage it. Reboot in stereo first, or keep 6 dB or more.</em></li>
</ul>
<p>Both end: <em>Back off at the first distortion; use Mono for loud listening.</em> The buttons are <strong>Set N dB anyway</strong> (in red) and <strong>Cancel</strong>.</p>
<div class="warn"><p>Values under 6 dB are for short tests only. Never use them without the protection running, and never for long, loud listening.</p></div>
<figure class="shot" data-need="CMF Stereo Pro Stereo attenuation field with 3 typed and the red warning line under it (protection running)"></figure>
<figure class="shot" data-need="CMF Stereo Pro dialog Lower the speaker's margin? with Set 3 dB anyway in red and Cancel"></figure>
<figure class="shot" data-need="CMF Stereo Pro dialog Speaker unprotected (protection not running, after a live switch from a mono boot) with Set N dB anyway and Cancel"></figure>
<div class="note"><p>This warning is new in v1.0.97-beta and has not been seen on a phone yet.</p></div>"""},

{"title": "Protection block",
"html": """<p>When Stereo is chosen and the module has protection (v1.0.4+), the Channels card continues with <strong>Protection</strong>:</p>
<ul>
<li><strong>Library</strong>: <em>running</em>, or <em>not running yet</em> in red. This is whether the module's protection library is active in the audio service.</li>
<li><strong>Speaker boost: N dB</strong>, with seven buttons 0 to 6.</li>
<li><strong>Deep bass guard</strong>: Off, Light, Medium, Strong.</li>
<li><strong>Earpiece leveller</strong> (module v1.0.5+): Off, Light, Medium, Strong, with the text <em>Raises the earpiece's quiet and medium passages towards -18 dBFS of loudness by at most 4 / 8 / 12 dB (over 400 ms; down within 10 ms), leaves passages below -60 dBFS alone, changes no frequency, and keeps peaks at the earpiece's ceiling (-0.1 dBFS with the limiter off).</em></li>
<li><strong>Earpiece limiter (-2.5 dBFS)</strong>: a switch.</li>
</ul>
<p>At the bottom, the measured basis: <em>Measured with DRIVER STUDY: Awinic in mono holds the speaker's peaks about 5 dB below the amp's full output and cuts 100 Hz by 16 dB at any level. The boost lowers the fixed attenuation by up to 6 dB, only while the library runs, with a limiter that keeps the peaks at Awinic's level. The bass guard holds the band below 100 Hz at -8 / -14 / -20 dB below full output, only when it is louder. The earpiece clips in its top 1.5 dB; its limiter holds it at -2.5 dBFS.</em></p>
<p>These are the same settings as in CMF Stereo; chapter 5 explains them.</p>"""},

{"title": "Speaker tab: Earpiece output",
"html": """<p>The next card starts with the <strong>Earpiece output</strong> switch, with <em>Armed</em> or <em>Disabled</em> under it. It is the same as CMF Stereo's EARPIECE SPEAKER: on plays media through the earpiece too; off stops the module's daemon and restores the controls it touched.</p>
<figure class="shot" data-need="CMF Stereo Pro Earpiece output card: switch, Mode (While playing / Always on), Everyday buttons Solo / Un-solo / Doctor, Earpiece gain slider"></figure>"""},

{"title": "Speaker tab: Mode",
"html": """<p><strong>Mode</strong> decides when the earpiece routing is applied:</p>
<ul>
<li><strong>While playing</strong> (the default): only while audio plays. Kinder to the earpiece, which is not held energised all day.</li>
<li><strong>Always on</strong>: permanently. Harder on the earpiece, and it costs idle power.</li>
</ul>
<p>If the mode is While playing and nothing is playing, the card adds: <em>Nothing is applied in this mode until audio is playing. If the earpiece never engages while music plays, the module cannot see your playback - use Always on, and send me the output of Tools &gt; Playback probe.</em> (Playback probe is on the Developer tab.)</p>
<div class="tech"><p>Sets <code>MODE=playback</code> or <code>MODE=always</code> in <code>stereo.conf</code>. CMF Stereo has no Mode setting; it uses whatever the module is set to.</p></div>"""},

{"title": "Speaker tab: Everyday (Solo, Un-solo, Doctor)",
"html": """<ul>
<li><strong>Solo</strong>: silences the speaker so you hear the earpiece alone. Useful to check that the earpiece really plays, or to judge it.</li>
<li><strong>Un-solo</strong>: brings the speaker back.</li>
<li><strong>Doctor</strong>: explains why the earpiece is silent, if it ever is. Disabled on modules that do not have it (it needs module v0.7.1+).</li>
</ul>
<p>The card says: <em>Solo silences the speaker so you can hear the earpiece alone. Doctor explains why it is silent, if it ever is.</em></p>
<div class="tech"><p>Solo runs <code>stereoctl solo</code>, which bypasses the guard first (otherwise turning the amp off would read as "headphones took over" and silence the earpiece too). Doctor runs <code>stereoctl doctor</code>: it walks the six things that can cause silence (kill switch, dead daemon, unusable tinymix, idle playback, a blocking guard, a control that will not take its value) and names the one responsible. It deliberately does nothing when playback is idle, so play something first.</p></div>"""},

{"title": "Speaker tab: Earpiece gain",
"html": """<p>The earpiece's gain in decibels, on a slider from <strong>-10</strong> (left, quietest) to <strong>+8 dB</strong> (right, loudest) in 1 dB steps. The value is shown above the slider. Let go of the slider to set it.</p>
<p>The card says: <em>The receiver amp's gain, -10 to +8 dB. +8 dB is its maximum and the value the HAL itself uses. The earpiece will not match the speaker; in Stereo, balance them by turning the speaker down (Stereo attenuation), not by pushing the earpiece. It has no protection circuit: back off at the first buzz.</em></p>
<p>If the gain cannot be read, the slider is disabled and shows <em>unavailable</em>, with the red note: <em>Could not read the gain from actions.conf, so the slider is disabled rather than risk writing a wrong value over it.</em></p>
<div class="tech"><p>Writes <code>Handset Volume</code> in <code>actions.conf</code> (register value = 8 - dB, clamped to 0-18), reads it back, then applies it to the mixer at once with <code>stereoctl ctl 'Handset Volume' N</code> so you hear it immediately. The Output card shows <em>gain -&gt; N (config verified)</em>. CMF Stereo's EARPIECE LEVEL sets the same value in 2 dB steps.</p></div>"""},

{"title": "Settings tab: Daemon",
"html": """<p>The <strong>Daemon</strong> card controls the module's background program:</p>
<ul>
<li><strong>Apply</strong>: applies the configured routing once.</li>
<li><strong>Revert</strong>: restores the values saved before the module touched them.</li>
<li><strong>Restart</strong>: restarts the daemon, for example after you edited the config by hand.</li>
</ul>
<figure class="shot" data-need="CMF Stereo Pro Settings tab: Daemon card (Apply, Revert, Restart, Poll interval, Guard, Log level) and the Danger zone card"></figure>"""},

{"title": "Settings tab: Poll interval",
"html": """<p><strong>Poll interval (seconds)</strong>: how often the routing is re-asserted. Type a whole number and tap <strong>Set</strong>. The module's default is 2.</p>
<p>The card explains: <em>How often the routing is re-asserted. The HAL resets mixer controls on every route change, so this cannot be very large.</em></p>
<div class="tech"><p>Writes <code>WATCH_INTERVAL</code> in <code>stereo.conf</code>.</p></div>"""},

{"title": "Settings tab: Guard",
"html": """<p>The <strong>Guard</strong> switch shows <em>Active</em> or <em>Bypassed</em>. When active, <em>the earpiece follows the speaker amp and stays quiet under headphones or Bluetooth.</em> Bypass it only if you want the routing applied regardless.</p>
<div class="tech"><p>Runs <code>stereoctl guard on</code> or <code>guard off</code>. On the CMF Phone 1 the guard is <code>aw_dev_0_switch = Enable</code>: the audio HAL disables the speaker amp when headphones or Bluetooth take over, so the earpiece stops with it.</p></div>"""},

{"title": "Settings tab: Log level",
"html": """<p>Three buttons, <strong>debug</strong>, <strong>info</strong> and <strong>warn</strong>, set how much the daemon writes to its log. Use debug while chasing a problem, then go back to info.</p>
<div class="tech"><p>Writes <code>LOG_LEVEL</code> in <code>stereo.conf</code>. Read the log on the Developer tab, Config files, Log.</p></div>"""},

{"title": "Settings tab: Danger zone",
"html": """<p>A red card with tools you should not normally need. It explains: <em>The audio policy overlay is for other hardware. On the CMF Phone 1 it is a no-op: the speaker port is already stereo, and true stereo comes from Channels on the Speaker tab instead. Needs a reboot either way.</em></p>
<ul>
<li><strong>XML patch</strong>: installs the audio policy overlay that widens the speaker port to stereo.</li>
<li><strong>XML revert</strong>: removes it.</li>
<li><strong>Reset state</strong> (modules with <code>reset</code>, v0.7.1+): clears the saved original values, the guard bypass, the solo state and the kill switch, then restarts. The saved originals survive reinstalls, so a bad one would make every revert restore a wrong value; this is the way out.</li>
</ul>"""},

{"title": "Developer tab: overview",
"html": """<p>The Developer tab opens with: <em>Developer tools. Nothing here is needed for normal use - the Speaker tab covers that. These exist for mapping unknown hardware and for diagnosing the module itself.</em></p>
<p>Its cards: Gain sweep, Mixer control, Hardware dump, Run any stereoctl command, and Config files. All output appears in the Output card.</p>
<figure class="shot" data-need="CMF Stereo Pro Developer tab: Gain sweep card with From / To / Sec fields and Start sweep, and the Mixer control card"></figure>"""},

{"title": "Gain sweep",
"html": """<p>Steps the earpiece gain across a range, holding each value so you can judge it by ear. Play music and use <strong>Solo</strong> first.</p>
<ol>
<li>Fill in <strong>From</strong> and <strong>To</strong> (register values, default 0 and 18) and <strong>Sec</strong> (seconds per step, default 4).</li>
<li>Tap <strong>Start sweep</strong>. The card shows <em>Now playing gain: N</em> in large type.</li>
<li>Tap <strong>Stop</strong> to end early.</li>
</ol>
<p>At the end, your starting value is restored, and the Output card lists every step with <em>Sweep done. Set whichever value sounded best on the Speaker tab.</em></p>
<div class="note"><p>The sweep counts in register values, where 0 is the loudest: 0 = +8 dB, 8 = 0 dB, 18 = -10 dB. To set the value you liked on the Speaker tab's slider, take dB = 8 minus the value.</p></div>
<div class="tech"><p>Each step writes the config as well as the mixer; otherwise the daemon re-asserts the configured gain within a couple of seconds and every step snaps back. <strong>To</strong> is capped at 18.</p></div>"""},

{"title": "Mixer control, Dump all and Live gain",
"html": """<p>Read or write one of the phone's audio mixer controls.</p>
<ul>
<li><strong>Control name</strong> (default <code>Handset Volume</code>) and <strong>Value (blank to read)</strong>.</li>
<li>The button says <strong>Read</strong> when Value is empty and <strong>Write</strong> when it is filled.</li>
<li><strong>Dump all</strong>: every mixer control and its current value (about 1400 lines on this phone).</li>
<li><strong>Live gain</strong>: the earpiece gain the mixer holds right now, as opposed to what <code>actions.conf</code> says it should be.</li>
</ul>
<div class="warn"><p>Writing mixer controls by hand can silence the audio, crash the audio service or drive the earpiece past what it is built for. Write only what you understand.</p></div>
<div class="tech"><p>These run <code>stereoctl ctl NAME [VALUE]</code> and <code>stereoctl dump</code>.</p></div>"""},

{"title": "Scan and Diff",
"html": """<ul>
<li><strong>Scan pattern</strong> (default <code>rcv|handset|spk</code>) and <strong>Scan</strong>: searches the mixer for controls matching the pattern.</li>
<li><strong>Diff s</strong> (default 20) and <strong>Diff</strong>: <em>Diff snapshots the mixer, waits, then shows what changed - start or stop playback during the wait to find which controls carry media.</em> This is how the media path on this phone was found in the first place.</li>
</ul>
<div class="tech"><p><code>stereoctl scan [pattern]</code> and <code>stereoctl diff [seconds]</code>.</p></div>"""},

{"title": "Hardware dump",
"html": """<p><em>Only needed on hardware this module has not been mapped on. The CMF Phone 1 routing is already known and shipped.</em></p>
<ul>
<li><strong>Probe (slow)</strong>: a full hardware dump (audio topology, all mixer controls, amplifier drivers, audio service configs, MediaTek's parameter tree). Takes a minute or two.</li>
<li><strong>Report</strong>: folds the newest probe into one shareable text file, <code>/sdcard/cmf-stereo-report.txt</code>.</li>
<li><strong>Playback probe</strong>: shows what the module can see of playback (the state of every playback stream, and what the module concludes). Use it when the earpiece never engages in While playing mode.</li>
</ul>"""},

{"title": "Run any stereoctl command",
"html": """<p>Type any <code>stereoctl</code> arguments in <strong>arguments</strong> and tap <strong>Run</strong>, for example <code>protect</code> or <code>channels</code>. <strong>Help</strong> shows the module's full command list.</p>
<div class="warn"><p>The command runs as root. Type only commands you understand.</p></div>"""},

{"title": "Config files",
"html": """<p>Three buttons show the module's files in the Output card:</p>
<ul>
<li><strong>stereo.conf</strong>: the module's behaviour settings (mode, channels, attenuation, protection, poll interval, log level).</li>
<li><strong>actions.conf</strong>: the routing itself, one action per line.</li>
<li><strong>Log</strong>: the last 60 lines of the daemon's log.</li>
</ul>
<figure class="shot" data-need="CMF Stereo Pro Developer tab, lower cards: Hardware dump, Run any stereoctl command, Config files and Output"></figure>"""},

{"title": "Guide tab",
"html": """<p>A built-in reference, written from what was measured on the CMF Phone 1. Type in <strong>Search the guide</strong> to filter it; while you search, the matching entries open by themselves. Otherwise tap <strong>Show</strong> on an entry to open it and <strong>Hide</strong> to close it. If nothing matches, it says <em>Nothing matches "…".</em></p>
<table>
<tr><th>Section</th><th>Entries</th></tr>
<tr><td>Start here</td><td>What this module does; Mono or stereo; Everyday use; If the earpiece goes silent</td></tr>
<tr><td>Commands</td><td>status; doctor; on / off; solo / unsolo; guard on / off; apply / revert; restart; log [n]; reset; ctl NAME [VALUE]; scan [pattern]; dump; diff [seconds]; probe [dir]; report; channels [mono|stereo]; xml-patch / xml-revert</td></tr>
<tr><td>Settings (stereo.conf)</td><td>MODE; PLAYBACK_CTL / PLAYBACK_VALUE; GUARD_CTL / GUARD_VALUE; WATCH_INTERVAL; CHANNELS; STEREO_ATTEN_DB; LOG_LEVEL; actions.conf syntax</td></tr>
<tr><td>This device</td><td>How the earpiece is driven; Where the mono fold is; Earpiece gain (Handset Volume); Safety; Recovery</td></tr>
</table>
<figure class="shot" data-need="CMF Stereo Pro Guide tab with a search for doctor and the matching entries opened"></figure>"""},
]},

# =====================================================================
# 5  LEVELS AND PROTECTION EXPLAINED
# =====================================================================
{
"title": "Levels and protection explained",
"intro": "<p>This chapter explains what the levels and the protection settings do underneath, with the exact numbers, and how to use them well and safely.</p>",
"sections": [

{"title": "Earpiece level and Earpiece gain",
"html": """<p>CMF Stereo's EARPIECE LEVEL (1 to 10), Pro's Earpiece gain slider (-10 to +8 dB) and the Nothing Settings page's Earpiece level all set the same thing: the earpiece amplifier's gain, called <code>Handset Volume</code>.</p>
<table>
<tr><th>Level</th><th>Gain</th><th>Handset Volume</th></tr>
<tr><td>10</td><td>+8 dB (maximum)</td><td>0</td></tr>
<tr><td>9</td><td>+6 dB</td><td>2</td></tr>
<tr><td>8</td><td>+4 dB</td><td>4</td></tr>
<tr><td>7</td><td>+2 dB</td><td>6</td></tr>
<tr><td>6</td><td>0 dB</td><td>8</td></tr>
<tr><td>5</td><td>-2 dB</td><td>10</td></tr>
<tr><td>4</td><td>-4 dB</td><td>12</td></tr>
<tr><td>3</td><td>-6 dB</td><td>14</td></tr>
<tr><td>2</td><td>-8 dB</td><td>16</td></tr>
<tr><td>1</td><td>-10 dB</td><td>18</td></tr>
</table>
<p>Pro also reaches the odd steps in between (+7, +5 dB and so on). CMF Stereo then shows the nearest level, rounding a half step to the quieter level.</p>
<div class="tech"><p>Handset Volume is the receiver amp's gain on the phone's MT6369 codec. Its range is 0-18, and the register runs opposite to the dB scale: 0 = +8 dB (loudest), 8 = 0 dB, 18 = -10 dB, and 31 = -40 dB (the codec's mute value). The audio HAL itself sets 0 whenever it drives the receiver. Module versions before v0.9.2 shipped 31, believing it was the top; the module's installer now corrects any value above 18 to 0. Pro's Status card shows <em>31 = mute (update the module)</em> if it still finds such a value. The digital gain before the DAC (<code>ADDA_DL_GAIN</code>) is left to the HAL, because it is shared with the headphone path.</p></div>"""},

{"title": "Speaker level and Stereo attenuation",
"html": """<p>CMF Stereo's SPEAKER LEVEL (1 to 10), Pro's Stereo attenuation (dB) and the Nothing Settings page's Speaker level all set how far the speaker amp is held down in Stereo, called <code>STEREO_ATTEN_DB</code>.</p>
<table>
<tr><th>Level</th><th>Attenuation</th></tr>
<tr><td>10</td><td>6 dB (the safety margin)</td></tr>
<tr><td>9</td><td>8 dB</td></tr>
<tr><td>8</td><td>10 dB</td></tr>
<tr><td>7</td><td>12 dB</td></tr>
<tr><td>6</td><td>14 dB</td></tr>
<tr><td>5</td><td>16 dB</td></tr>
<tr><td>4</td><td>18 dB</td></tr>
<tr><td>3</td><td>20 dB</td></tr>
<tr><td>2</td><td>22 dB</td></tr>
<tr><td>1</td><td>24 dB</td></tr>
</table>
<p>Pro accepts any whole number from 0 to 30. Values under 6 show as level 10 in CMF Stereo, values over 24 as level 1; odd values show as the nearest level.</p>
<div class="note"><p>If Pro set a value under 6, CMF Stereo shows level 10 already lit, so tapping the tenth dot does nothing. To bring it back to the 6 dB margin from CMF Stereo, tap dot 9 and then dot 10, or set 6 in Pro.</p></div>
<div class="tech"><p>The daemon holds the amp at least this many dB down through the amp's own volume control, <code>aw_dev_0_volume</code> (1/8 dB steps), less the speaker boost while the protection runs. It re-reads the value every cycle; no restart is needed. In Mono the amp gets its previous value back.</p></div>"""},

{"title": "Balancing the two sides",
"html": """<p>The earpiece is much smaller than the bottom speaker and will not match it. To get an even stereo picture:</p>
<ol>
<li>Leave the earpiece at level 10 (+8 dB), unless it buzzes.</li>
<li>Switch on the EARPIECE LIMITER (it is on by default).</li>
<li>Play something with clear left and right, and lower the SPEAKER LEVEL until the earpiece side sounds as clear as the speaker side.</li>
<li>If the earpiece's quiet passages get lost, try the EARPIECE LEVELLER at LOW or MID.</li>
</ol>
<div class="warn"><p>Balance by turning the <strong>speaker down</strong>, never by pushing the earpiece. Lower is also gentler on the speaker.</p></div>
<div class="note"><p>The CMF L/R Test app (chapter 7) and the module's test files <code>LEFT_ONLY_3min.wav</code> and <code>LEFT10s_RIGHT10s.wav</code>, which come with every module release, help you hear which side is which.</p></div>"""},

{"title": "Why 6 dB is the floor in CMF Stereo",
"html": """<p>In Mono, Awinic's protection holds the speaker's peaks about 5 dB below the amp's full output. Stereo switches that processing off for media, so the module holds the speaker 6 dB down instead. That 6 dB margin is what stands in for Awinic's protection, which is why CMF Stereo's loudest speaker level is exactly 6 dB and never less.</p>
<p>CMF Stereo Pro can go lower for testing, after the warning described in chapter 4.</p>"""},

{"title": "The module's protection: overview",
"html": """<p>In Stereo, the module runs its own protection inside the phone's audio service, built from measurements of what Awinic does in Mono. It has four parts:</p>
<table>
<tr><th>Setting</th><th>Protects</th><th>Default</th></tr>
<tr><td>Speaker boost</td><td>(makes the speaker louder, with a peak limiter)</td><td>0 (off)</td></tr>
<tr><td>Deep bass guard</td><td>The speaker, from loud deep bass</td><td>Medium (MID)</td></tr>
<tr><td>Earpiece limiter</td><td>The earpiece, from clipping</td><td>On</td></tr>
<tr><td>Earpiece leveller</td><td>(brings up the earpiece's quiet parts)</td><td>Off</td></tr>
</table>
<p>The protection applies in Stereo only. In Mono, Awinic's own protection is on, and the earpiece plays the stock way, as it always did.</p>
<div class="tech"><p>Needs module v1.0.4 (the leveller v1.0.5). Set with <code>stereoctl protect boost 0-6</code>, <code>protect bass off|light|medium|strong</code>, <code>protect earpiece on|off</code> and <code>protect leveller off|light|medium|strong</code>; stored in <code>stereo.conf</code> as <code>SPEAKER_BOOST</code>, <code>BASS_PROTECT</code>, <code>EARPIECE_LIMIT</code> and <code>EARPIECE_LEVELLER</code>. The daemon applies a change within one cycle.</p></div>"""},

{"title": "When the protection runs",
"html": """<p>The protection library starts with the first sound on the speaker after the phone has booted in Stereo. CMF Stereo's SPEAKER BOOST hint says <em>Starts with the next sound on the speaker.</em> until it runs; Pro's Library row says <em>not running yet</em>.</p>
<p><strong>After a live switch from a Mono boot</strong>, the protection does not start until the next reboot: the library is not in place until then. Stereo still plays, held at the fixed attenuation, but the boost is ignored. Reboot once in Stereo to get the full protection.</p>
<div class="tech"><p>The app treats the protection as running when the property <code>vendor.audiohal.cmf_prot</code> is 1 and the running channels are stereo. The module's README lists the protection after a live switch from a Mono boot as not yet tested on a phone.</p></div>"""},

{"title": "Speaker boost",
"html": """<p>Makes the speaker louder by lowering the fixed attenuation by up to 6 dB, one step per dB. A limiter then keeps the speaker's loudest moments where Awinic keeps them in Mono, about 5 dB below the amp's full output. So the speaker gets louder on average, but its peaks do not.</p>
<ul>
<li>0 is the sound you had before.</li>
<li>It works only while the protection library runs.</li>
</ul>
<div class="note"><p>Boost 6 has been tried only briefly; a longer listen is still to be tested.</p></div>"""},

{"title": "Deep bass guard",
"html": """<p>Holds back only the deepest bass (below 100 Hz), and only when it gets loud enough to strain the speaker. Quiet bass is untouched.</p>
<table>
<tr><th>CMF Stereo</th><th>Pro / Settings page</th><th>Deep bass held at</th></tr>
<tr><td>OFF</td><td>Off</td><td>not held</td></tr>
<tr><td>LIGHT</td><td>Light</td><td>-8 dB below full output</td></tr>
<tr><td>MID</td><td>Medium</td><td>-14 dB below full output</td></tr>
<tr><td>STRONG</td><td>Strong</td><td>-20 dB below full output</td></tr>
</table>
<div class="tech"><p>Measured: Awinic in Mono cuts 100 Hz by 16 dB at any level. Before the guard (module v1.0.4), Stereo let the speaker get up to about 10 dB more deep bass than Awinic does.</p></div>"""},

{"title": "Earpiece limiter",
"html": """<p>Keeps the earpiece below the point where it crackles. Measurements showed the earpiece clips in its top 1.5 dB of full scale; the limiter holds it at -2.5 dBFS. It is on by default.</p>
<div class="warn"><p>Turning the limiter off removes the earpiece's only protection in Stereo.</p></div>"""},

{"title": "Earpiece leveller",
"html": """<p>Brings up the earpiece's quiet parts so it keeps up with the speaker. Loud parts, bass and every frequency stay as they are, and the earpiece's peaks are never raised.</p>
<table>
<tr><th>CMF Stereo / Settings page</th><th>Pro</th><th>Quiet passages raised by at most</th></tr>
<tr><td>OFF</td><td>Off</td><td>0</td></tr>
<tr><td>LOW</td><td>Light</td><td>4 dB</td></tr>
<tr><td>MID</td><td>Medium</td><td>8 dB</td></tr>
<tr><td>HIGH</td><td>Strong</td><td>12 dB</td></tr>
</table>
<div class="tech"><p>It raises the earpiece's quiet and medium passages towards -18 dBFS of loudness, rising over 400 ms and coming down within 10 ms, leaves passages below -60 dBFS alone, changes no frequency, and keeps peaks at the earpiece's ceiling (the limiter's -2.5 dBFS, or -0.1 dBFS with the limiter off). On the test phone it made a clear difference on the earpiece side, with no distortion heard.</p></div>"""},

{"title": "Calls, headphones and Bluetooth",
"html": """<ul>
<li><strong>Calls</strong> always keep Awinic's speaker protection, in Mono and Stereo, and are not affected by a Mono/Stereo switch.</li>
<li><strong>Headphones and Bluetooth</strong>: with the guard active (the default), the earpiece follows the speaker amp, so it stays quiet when headphones or Bluetooth take over.</li>
</ul>"""},

{"title": "The module's safety nets",
"html": """<p>Besides the protection, the module has safety nets that act on their own:</p>
<ul>
<li><strong>Boot watchdog</strong>: two failed boots with the Stereo overlay in place remove it and set Mono.</li>
<li><strong>Crash-loop guard</strong>: if the audio service restarts 3 times within a minute in Stereo, the module puts the stock files back (Mono) at once and tells the apps.</li>
<li><strong>Unknown Nothing OS build</strong>: Stereo stays off (NOTHING OS UPDATED).</li>
<li><strong>Hash checks</strong>: the module patches only audio files whose checksums match the analysed build; anything else is refused and the phone stays in Mono.</li>
</ul>
<div class="note"><p>These are covered by the module's tests on fake files; none has been needed on the phone yet.</p></div>"""},
]},

# =====================================================================
# 6  QUICK SETTINGS AND NOTHING SETTINGS
# =====================================================================
{
"title": "Quick Settings and Nothing Settings",
"intro": "<p>You do not have to open the app to switch Stereo. Each edition adds a Quick Settings tile, and, with LSPosed, a Stereo speakers page inside Nothing's own Settings that you can also find with Settings search.</p>",
"sections": [

{"title": "The Quick Settings tile",
"html": """<p>Each edition has a Quick Settings tile: <strong>Stereo</strong> for CMF Stereo, <strong>Stereo Pro</strong> for CMF Stereo Pro. It needs no LSPosed.</p>
<p>To add it: pull down the Quick Settings panel, open the tile editor, find Stereo or Stereo Pro, and drag it into your tiles.</p>
<figure class="shot" data-need="Quick Settings tile editor showing the Stereo (CMF Stereo) and Stereo Pro tiles with the dot-matrix speaker icon"></figure>"""},

{"title": "Using the tile",
"html": """<ul>
<li><strong>Tap</strong>: switches between Mono and Stereo. While the audio service restarts, the tile is greyed and says <em>Switching…</em>.</li>
<li><strong>Long press</strong>: opens the app.</li>
</ul>
<table>
<tr><th>Tile</th><th>Subtitle</th><th>Meaning</th></tr>
<tr><td>On (lit)</td><td>Earpiece + speaker</td><td>Stereo is chosen</td></tr>
<tr><td>Off</td><td>Mono</td><td>Mono is chosen</td></tr>
<tr><td>Greyed</td><td>Switching…</td><td>A switch is in progress</td></tr>
<tr><td>Greyed</td><td>Unavailable</td><td>No root for the app, or the module is not installed</td></tr>
</table>
<div class="warn"><p>Unlike the apps, the tile switches to Stereo straight away, without the warning dialog. Read the safety notes (chapter 1) before you rely on it.</p></div>
<div class="note"><p>The tile shows the chosen mode. If a switch could not be applied live and waits for a restart, the tile already shows the new mode; open the app to see that a restart is needed. The tiles' names and a tap on them have not yet been checked on a phone.</p></div>
<figure class="shot" data-need="Quick Settings panel with the Stereo tile lit, subtitle Earpiece + speaker"></figure>"""},

{"title": "Stereo inside Nothing Settings: what it is",
"html": """<p>With LSPosed, each edition can also put Stereo inside Nothing's Settings:</p>
<ul>
<li>a <strong>Stereo speakers</strong> row in <strong>Settings &gt; Sound</strong>, right above Dirac, drawn like Nothing's own rows;</li>
<li>a <strong>Stereo speakers</strong> page inside Settings, built from Nothing's own rows, with the levels as dots and the choices as pills;</li>
<li>the page's settings findable with <strong>Settings search</strong>.</li>
</ul>
<p>This part is optional. The apps and the tile work without it.</p>
<div class="tech"><p>Settings already supports rows from other apps (its <code>com.android.settings.action.EXTRA_SETTINGS</code> tile injection), but only takes them from system apps. The app's LSPosed hook marks only its own entry as a system one, then builds the page. Nothing else in Settings changes, and any failure leaves Settings as it was.</p></div>"""},

{"title": "Setting up LSPosed",
"html": """<ol>
<li>Install LSPosed and make sure the app is on internal storage.</li>
<li>Open LSPosed, go to Modules and enable <strong>one</strong> edition (CMF Stereo or CMF Stereo Pro). Its description reads <em>Adds Stereo speakers to Nothing Settings &gt; Sound. Scope: Settings.</em></li>
<li>In its scope, tick both <strong>Settings</strong> (<code>com.android.settings</code>) and <strong>Settings Intelligence</strong> (<code>com.android.settings.intelligence</code>, needed for Settings search).</li>
<li>Force-stop Settings, and force-stop Settings Intelligence once (or simply reboot).</li>
<li>Open Settings &gt; Sound.</li>
</ol>
<div class="warn"><p>Enable only one edition. With both enabled, Settings shows two Stereo speakers rows.</p></div>
<figure class="shot" data-need="LSPosed module scope screen for CMF Stereo with Settings and Settings Intelligence ticked"></figure>"""},

{"title": "The Stereo speakers row in Sound",
"html": """<p>In Settings &gt; Sound, the <strong>Stereo speakers</strong> row sits right above Dirac, as a plain row like Nothing's own (no icon, no divider). Under its title, a live summary shows the state:</p>
<table>
<tr><th>Summary</th><th>Meaning</th></tr>
<tr><td>Stereo · left and right follow the screen</td><td>Stereo is running</td></tr>
<tr><td>Mono · both speakers play everything</td><td>Mono is running</td></tr>
<tr><td>Earpiece speaker off</td><td>The earpiece is switched off</td></tr>
<tr><td>Switches to stereo after a restart</td><td>A switch waits for a restart (or mono)</td></tr>
<tr><td>CMF true stereo module not installed</td><td>The module is missing</td></tr>
<tr><td>Needs root access for CMF Stereo</td><td>The app has no root</td></tr>
</table>
<p>Tap the row to open the Stereo speakers page.</p>
<figure class="shot" data-need="Nothing Settings &gt; Sound with the Stereo speakers row above Dirac and its summary Stereo · left and right follow the screen"></figure>"""},

{"title": "The Stereo speakers page",
"html": """<p>The page opens inside Settings, with the toolbar title <strong>STEREO SPEAKERS</strong>, and every row has a description under its title. It has four groups:</p>
<table>
<tr><th>Group</th><th>Row</th><th>Control</th><th>Description</th></tr>
<tr><td>Speakers</td><td>Earpiece speaker</td><td>Switch</td><td>Play through the earpiece as well as the bottom speaker</td></tr>
<tr><td>Speakers</td><td>Stereo</td><td>Switch</td><td>Changes with the state (next section)</td></tr>
<tr><td>Levels</td><td>Earpiece level</td><td>10 dots</td><td>10 is the loudest the earpiece goes. Turn it down if it ever buzzes</td></tr>
<tr><td>Levels</td><td>Speaker level</td><td>10 dots</td><td>Balances the two sides: lower it until the earpiece is as clear as the speaker</td></tr>
<tr><td>Protection</td><td>Speaker boost</td><td>7 dots</td><td>Louder speaker without more strain, +1 dB a dot. The first dot is off</td></tr>
<tr><td>Protection</td><td>Deep bass guard</td><td>Pills: Off, Light, Medium, Strong</td><td>Holds back only the deepest bass, and only when it gets loud enough to strain the speaker</td></tr>
<tr><td>Protection</td><td>Earpiece leveller</td><td>Pills: Off, Low, Mid, High</td><td>Brings up the earpiece's quiet parts so it keeps up with the speaker</td></tr>
<tr><td>Protection</td><td>Earpiece limiter</td><td>Switch</td><td>Stops the earpiece crackling at its very loudest</td></tr>
<tr><td>More</td><td>CMF Stereo</td><td>Tap</td><td>Open the app</td></tr>
</table>
<p>One tap on a dot or pill sets it. The values and their meaning are the same as in the app (chapter 5).</p>
<figure class="shot" data-need="Nothing Settings Stereo speakers page, top: toolbar STEREO SPEAKERS, Speakers group with Earpiece speaker and Stereo switches, Levels group with dots"></figure>
<figure class="shot" data-need="Nothing Settings Stereo speakers page, scrolled: Protection group with Speaker boost dots, Deep bass guard and Earpiece leveller pills, Earpiece limiter, and More &gt; CMF Stereo Open the app"></figure>"""},

{"title": "The Stereo row's summary and greyed rows",
"html": """<p>The Stereo switch's description tells you the state:</p>
<ul>
<li><em>Left and right follow the screen</em>: Stereo is chosen.</li>
<li><em>Both speakers play everything</em>: Mono is chosen.</li>
<li><em>Switches after a restart</em>: a switch waits for a restart.</li>
<li><em>Switching…</em>: a switch (or an earpiece on/off) is in progress; both switches are held until it is done.</li>
<li><em>CMF Stereo needs root access</em>, <em>The CMF true stereo module is not installed</em>, or <em>Not available on this Nothing OS version yet</em>: the page cannot be used, and every row is greyed.</li>
</ul>
<p>Rows are also greyed when they do not apply: Speaker level and the protection rows only work while Stereo is running, and Earpiece leveller only with a module that has it. A greyed row's title is dimmed too.</p>
<div class="note"><p>When the page opens, its rows stay greyed for a moment until the real state has loaded; taps are ignored until then. That is deliberate: it is what stops the page from changing anything just by being opened.</p></div>
<div class="warn"><p>Like the tile, the page's Stereo switch has no warning dialog.</p></div>"""},

{"title": "Always in step",
"html": """<p>The row, the page, the tile and both apps all show the same state. A change made in any of them, or outside them (the module's Action button, a <code>stereoctl</code> command in a terminal, or the module switching itself back to Mono) appears on the page while it is open and in the Sound row's summary, without a root call each time.</p>
<div class="tech"><p>The app keeps the last state it read (StateCache). Each reading is published to Settings through the app's provider; when something changed, it notifies <code>content://&lt;app&gt;.settingstile/state</code> (the page listens to it) and the Sound row's summary address. Settings itself has no root: it reads the stored state instantly, and the app reads afresh in the background when the stored state is older than five minutes. Changes made outside the apps reach it because the module (v1.0.6+) calls the provider's <code>stateChanged</code> method after each change. The page updating by itself after an outside change has not yet been seen on a phone.</p></div>"""},

{"title": "Settings search",
"html": """<p>With Settings Intelligence in the LSPosed scope, type a setting's name in Nothing's Settings search, for example <em>stereo</em>, <em>earpiece</em>, <em>speaker</em>, <em>earpiece level</em> or <em>bass guard</em>. The Stereo speakers rows are listed, with <em>Stereo speakers</em> under each result. Tap one to open the Stereo speakers page inside Settings, at that row.</p>
<p>With both editions installed, the rows are listed once (CMF Stereo's).</p>
<figure class="shot" data-need="Nothing Settings search for bass guard, listing Deep bass guard with Stereo speakers under it"></figure>
<div class="tech"><p>The rows come from the app's search provider (authority <code>&lt;package&gt;.search</code>, protected by <code>READ_SEARCH_INDEXABLES</code>, which only Settings Intelligence holds), with the keywords <em>stereo, speakers, earpiece, CMF</em> and the row's group. The hook in Settings Intelligence accepts this provider, adds a marker to its list of indexed providers so it reads them again, and turns a tap on a result into Settings' own search trampoline with the page and the row's key. Settings Intelligence re-reads providers only when that list changes, which is why v1.0.80 showed nothing; fixed in v1.0.81.</p></div>"""},

{"title": "If Nothing Settings changes",
"html": """<p>The page is built from parts of Nothing's Settings app whose internal names are checked every time Settings starts. If a Settings update no longer matches, nothing is hooked: the Stereo speakers row still appears and opens the app instead of the page. Nothing in Settings breaks.</p>
<div class="tech"><p>The page was built against Nothing Settings 16.0.0. In the LSPosed log, look for <em>CMF Stereo: settings page ready</em> and <em>CMF Stereo: settings search ready (3 hooks)</em>, or for <em>settings page not installed (Settings differs)</em>, <em>settings hook not installed</em> or <em>settings search not installed</em> with the reason.</p></div>"""},

{"title": "Without LSPosed",
"html": """<p>Without LSPosed (or with the app not enabled in it), Settings does not show the Stereo speakers row at all, because it only takes such rows from system apps. Settings search does not list the rows either. The apps and the Quick Settings tile work exactly the same.</p>"""},
]},

# =====================================================================
# 7  CMF L/R TEST
# =====================================================================
{
"title": "CMF L/R Test",
"intro": "<p>CMF L/R Test is a small companion app that checks left and right. It plays a beep on the left channel only, in each of the ways apps hand sound to the phone, so you can hear that the beep always comes from the side on your left. It also has tools that record what the audio driver receives, used to develop the module.</p>",
"sections": [

{"title": "What it is for",
"html": """<p>On the CMF Phone 1, Nothing's sound tuning (Dirac) has different tunings for music, films, speech and games, and picks one from the app that plays. Apps also hand their sound to Android in different ways (plain, low-latency, floating point, compressed). CMF L/R Test plays a left-only beep through each, so you can check that Stereo keeps left and right correct in every case, in both sideways positions.</p>
<p>It is a test tool, not something you need every day.</p>"""},

{"title": "Installing it",
"html": """<p>Each release includes <code>CMF-LR-test-&lt;commit&gt;.apk</code>. Install it like any app; it appears as <strong>CMF L/R Test</strong>, with a crash-test-target icon (yellow and black quarters on a dot grid).</p>
<ul>
<li>Stereo must be on (in CMF Stereo, Pro, the tile or the module) for the left/right tests to mean anything.</li>
<li>Root is used to read Dirac's tuning, the rotation the module stored, and which output Android plays through, and for the recording tools. Without root the test screens say <em>(no root)</em> and the recordings say <em>Root refused</em>.</li>
<li>DRIVER STUDY asks for the microphone permission.</li>
</ul>
<div class="tech"><p>Package <code>com.tiimoo.cmfstereo.modetest</code>. Permissions: <code>MODIFY_AUDIO_SETTINGS</code> (to turn on the audio HAL's own recording) and <code>RECORD_AUDIO</code> (DRIVER STUDY).</p></div>"""},

{"title": "The main screen",
"html": """<p>The main screen says: <em>CMF L/R TEST. Each button plays a beep on the LEFT channel only, in one of Dirac's tunings. Turn the phone sideways both ways: the beep must always come from the side that is on your left (earpiece on your left: earpiece; earpiece on your right: bottom speaker). Stereo must be on in CMF Stereo.</em></p>
<p>Below it are the test buttons, then three tools: DRIVER STUDY, DRIVER RECORDING and FULL CHECK.</p>
<figure class="shot" data-need="CMF L/R Test main screen: intro text and the buttons MUSIC, MOVIE, MOVIE + EFFECT, GAME, SPEECH, AAUDIO (GAME, LOW LATENCY), AAUDIO SHARED, FLOAT, COMPRESSED (OFFLOAD)"></figure>"""},

{"title": "The tuning tests",
"html": """<table>
<tr><th>Button</th><th>What it tests</th></tr>
<tr><td>MUSIC</td><td>Dirac's music tuning (Nothing's default)</td></tr>
<tr><td>MOVIE</td><td>Dirac's film tuning</td></tr>
<tr><td>MOVIE + EFFECT</td><td>The film tuning, with an audio effect attached to the app's own sound, as VLC does</td></tr>
<tr><td>GAME</td><td>Dirac's game tuning</td></tr>
<tr><td>SPEECH</td><td>Dirac's speech tuning (navigation)</td></tr>
</table>
<div class="tech"><p>Nothing's audio service picks the tuning from the name of the process that plays, matched against built-in lists (music, video, navigation, game, in that order). Each test runs in its own process, named to land in one tuning: <code>:music</code>, <code>org.videolan.vlc.cmftest</code>, <code>org.videolan.vlc.cmftest2</code> (with a neutral DynamicsProcessing effect on its own session), <code>com.google.android.apps.maps.cmftest</code>, <code>com.ludo.king.cmftest</code>. These are only process names inside this app.</p></div>"""},

{"title": "The playback-path tests",
"html": """<table>
<tr><th>Button</th><th>What it tests</th></tr>
<tr><td>AAUDIO (GAME, LOW LATENCY)</td><td>Native AAudio, low latency and exclusive, as a game asks</td></tr>
<tr><td>AAUDIO SHARED</td><td>Native AAudio in shared mode</td></tr>
<tr><td>FLOAT</td><td>32-bit floating-point samples</td></tr>
<tr><td>COMPRESSED (OFFLOAD)</td><td>An MP3 of the same beep, decoded by the audio DSP, as music players do to save battery</td></tr>
</table>
<p>Each screen shows what Android granted, for example whether the sound was offloaded.</p>
<div class="note"><p>On the CMF Phone 1, COMPRESSED (OFFLOAD) shows <em>Compressed: this phone refuses MP3 offload</em> and plays nothing: Android's offload check says no on this phone, so apps never use that path here. That is expected.</p></div>"""},

{"title": "Reading a test screen",
"html": """<p>A test screen plays a short beep (880 Hz, 0.3 s on, 0.3 s off) on the left channel while it is open, and shows once a second:</p>
<ul>
<li><em>&lt;NAME&gt; TEST</em> and <em>LEFT channel only</em>;</li>
<li><em>Dirac reports:</em> the tuning Dirac is using;</li>
<li><em>Screen rotation:</em> <em>90 (earpiece on your left)</em>, <em>270 (earpiece on your right)</em>, <em>180</em> or <em>0 (upright)</em>;</li>
<li><em>Module reads rotation:</em> the rotation the module stored;</li>
<li>for some tests, how the beep was played;</li>
<li><em>Android plays it through:</em> the output Android chose;</li>
<li><em>The beep must come from the side on your LEFT. Back to stop.</em></li>
</ul>
<p>The test screens turn with the phone between the two landscape positions.</p>
<figure class="shot" data-need="CMF L/R Test MUSIC TEST screen in landscape, earpiece on the left: Dirac reports, Screen rotation 90 (earpiece on your left), Module reads rotation, Android plays it through"></figure>"""},

{"title": "Running a left/right check",
"html": """<ol>
<li>Switch to Stereo.</li>
<li>Open CMF L/R Test and tap a test, for example MUSIC.</li>
<li>Hold the phone sideways with the earpiece on your left: the beep must come from the earpiece.</li>
<li>Turn it the other way, earpiece on your right: the beep must now come from the bottom speaker.</li>
<li>Press Back to stop, and repeat with the other buttons.</li>
</ol>
<p>On the test phone, MUSIC, MOVIE, MOVIE + EFFECT, GAME, SPEECH, AAUDIO, AAUDIO SHARED and FLOAT all came out on the correct side.</p>
<div class="note"><p>In Mono the beep comes from both speakers: that is correct for Mono.</p></div>"""},

{"title": "DRIVER RECORDING",
"html": """<p>A switch button that turns on the audio driver's own recording of what it receives, to see in the data which channel a left-only sound is on.</p>
<ol>
<li>Tap <strong>DRIVER RECORDING: OFF</strong>. It becomes <strong>DRIVER RECORDING: ON</strong> and says <em>Recording. Play a test now, then turn this off.</em></li>
<li>Play a test.</li>
<li>Tap it again to stop. It says <em>Saved: …</em> and <em>Send cmf-driver-dump.tgz and cmf-driver-dump.txt.</em></li>
</ol>
<p>The files are saved as <code>/sdcard/cmf-driver-dump.tgz</code> and <code>/sdcard/cmf-driver-dump.txt</code>.</p>
<div class="tech"><p>It sets <code>vendor.streamout.pcm.dump</code> and <code>vendor.streamout.dsp.dump</code> with <code>resetprop</code> and sends the same key through <code>AudioManager.setParameters</code>, which is what makes the HAL start recording. Recordings are collected from <code>/data/vendor/audiohal/audio_dump</code>.</p></div>"""},

{"title": "FULL CHECK",
"html": """<p><strong>FULL CHECK (about 40 s, hold the phone still)</strong> runs MUSIC, MOVIE, GAME and SPEECH one after another for 6 seconds each, with the driver recording on, and logs Dirac's tuning and the rotation each second.</p>
<ol>
<li>Hold the phone sideways and tap FULL CHECK. It says <em>Running: MUSIC, MOVIE, GAME, SPEECH... keep the phone still.</em></li>
<li>When it is done it says <em>Saved: …</em> and <em>Turn the phone over and run it again, then send both files.</em></li>
</ol>
<p>The file is named after the rotation: <code>/sdcard/cmf-full-check-rot&lt;rotation&gt;.tgz</code>.</p>"""},

{"title": "DRIVER STUDY",
"html": """<p>A measurement used to design the module's protection. It plays tones on each driver, records them with the phone's microphones and the driver's own recordings, and writes the results to files.</p>
<p>Tap <strong>DRIVER STUDY (about 2 min, phone flat on a table)</strong>. The study screen says: <em>Put the phone flat on a table, screen up, in a quiet room, and do not touch it until it says done (about 3 minutes). It switches Stereo and Mono by itself (the sound stops for a moment at each switch) and puts back the mode you are in now. Media volume goes to maximum while it runs and back after: tones never louder than -3 dB below full scale.</em> Tap <strong>START</strong>; allow the microphone when asked.</p>
<p>While it runs it shows the part, the tone and the progress, for example <em>STEREO: playing, do not touch the phone.</em> At the end: <em>Done. Mode is … again.</em> and the files to send.</p>
<ul>
<li>If the module does not answer, it stops: <em>The CMF stereo module did not answer (stereoctl channels). Nothing was changed.</em></li>
<li>Without the microphone permission: <em>The microphone permission is needed for the study.</em></li>
</ul>
<div class="tech"><p>The screen is locked upright (no left/right swap). Stereo part: 300 ms tones on the earpiece, then the speaker, 11 frequencies from 100 Hz to 8 kHz at -30 to -3 dBFS. Mono part (stock files, Awinic on): tones on both channels, 7 frequencies, with the DSP's recordings of Awinic's input, output and current/voltage sense. Results: <code>/sdcard/cmf-study.csv</code>, <code>/sdcard/cmf-study-mono.csv</code>, <code>/sdcard/cmf-study-notes.txt</code>; raw recordings in <code>/sdcard/cmf-study-raw.tgz</code>. It checks after each switch that the mode really runs, and always switches back to the mode it found. Its rerun is no longer needed; the test stays in the app, its fixed alignment not yet run on a phone.</p></div>
<figure class="shot" data-need="CMF L/R Test DRIVER STUDY screen with its instructions and the START button"></figure>"""},
]},

# =====================================================================
# 8  TROUBLESHOOTING
# =====================================================================
{
"title": "Troubleshooting",
"intro": "<p>Find your problem by topic. Each entry says what you see, why, and what to do. If the answer is not here, see Reporting a problem at the end of this chapter.</p>",
"sections": [

{"title": "The app shows a message instead of its controls",
"html": """<p><strong>NEEDS ROOT / No root access.</strong> The app could not get root. Allow it in your root manager's Superuser list, then reopen CMF Stereo, or tap Refresh in Pro.</p>
<p><strong>MODULE NOT INSTALLED / Module not installed.</strong> The module is missing or not active. Flash it, reboot, and check in your root manager that it is enabled.</p>
<p><strong>READING... stays on screen.</strong> The root request may be waiting for an answer: look for your root manager's dialog.</p>"""},

{"title": "The earpiece is silent",
"html": """<ol>
<li>Check that EARPIECE SPEAKER (Earpiece output in Pro) is on.</li>
<li>If CMF Stereo says <em>Not running right now.</em>, tap RESTART.</li>
<li>Check that no headphones or Bluetooth device are connected: with the guard active, the earpiece stays quiet then.</li>
<li>In Pro, play some music and tap <strong>Doctor</strong> on the Speaker tab. It names the cause.</li>
<li>If the earpiece never engages while music plays in <em>While playing</em> mode, set Mode to <strong>Always on</strong> and run <strong>Playback probe</strong> on the Developer tab; send its output to the developer.</li>
<li>Use <strong>Solo</strong> to hear the earpiece alone and be sure.</li>
</ol>"""},

{"title": "Switching Mono and Stereo",
"html": """<p><strong>It asks me to restart the phone.</strong> The module could not switch safely while the phone runs: no verified files, a call in progress, or the audio service did not come back. Your choice is saved; restart to apply it.</p>
<p><strong>The music stopped when I switched.</strong> That is normal: the audio service restarts for about a second and players pause. Press play again.</p>
<p><strong>There is no SOUND panel / Channels card.</strong> Your module is older than v0.9.0. Update it.</p>
<p><strong>Stereo switched itself back to Mono.</strong> A safety net acted: two failed boots, the audio service restarting 3 times within a minute, or a Nothing OS update the module does not know. See NOTHING OS UPDATED below.</p>
<p><strong>NOTHING OS UPDATED / Nothing OS: new build: stereo unavailable.</strong> The module does not know your new Nothing OS build yet and keeps Stereo off. Use Mono and update the module when a release supports the build.</p>
<p><strong>Pro shows Nothing OS: not a CMF Phone 1.</strong> The module does not find a CMF Phone 1 audio stack; it does nothing on other phones.</p>"""},

{"title": "Panels or settings are missing",
"html": """<table>
<tr><th>Missing</th><th>Why</th><th>What to do</th></tr>
<tr><td>SPEAKER LEVEL and the protection panels (CMF Stereo)</td><td>Stereo is not running (Mono, or a switch waiting for a restart)</td><td>Switch to Stereo; restart if asked</td></tr>
<tr><td>Protection panels only</td><td>Module older than v1.0.4</td><td>Update the module</td></tr>
<tr><td>EARPIECE LEVELLER</td><td>Module older than v1.0.5</td><td>Update the module</td></tr>
<tr><td>EARPIECE LEVEL</td><td>The earpiece gain could not be read from the module's settings, or is out of range</td><td>Open Pro: its Status card shows <em>unreadable</em> or <em>31 = mute (update the module)</em>. Update the module, whose installer corrects an out-of-range value</td></tr>
<tr><td>Pro's Earpiece gain slider is disabled (<em>unavailable</em>)</td><td>Same cause; the app will not write a value it could not read</td><td>As above</td></tr>
<tr><td>Pro's Doctor is greyed</td><td>Module older than v0.7.1</td><td>Update the module</td></tr>
</table>"""},

{"title": "Protection not running",
"html": """<p><strong>CMF Stereo's SPEAKER BOOST hint starts with <em>Starts with the next sound on the speaker.</em>, or Pro's Library row says <em>not running yet</em>.</strong></p>
<ul>
<li>If the phone booted in Stereo: play something on the speaker; the protection starts with the next sound.</li>
<li>If you switched to Stereo live after a Mono boot: the protection only starts after a reboot. Reboot once in Stereo. Until then, the boost does nothing and the speaker relies on the fixed attenuation and the amp's cut-outs.</li>
</ul>
<div class="warn"><p>Without the protection running, never set Pro's stereo attenuation under 6 dB.</p></div>"""},

{"title": "Sound quality and loudness",
"html": """<p><strong>The earpiece buzzes, rattles or crackles.</strong> Lower EARPIECE LEVEL at once, and make sure EARPIECE LIMITER is on.</p>
<p><strong>The speaker distorts in Stereo.</strong> Lower SPEAKER LEVEL, check that DEEP BASS GUARD is not OFF, reduce SPEAKER BOOST, or use Mono for loud listening.</p>
<p><strong>The earpiece side is too quiet.</strong> Lower SPEAKER LEVEL to balance; try EARPIECE LEVELLER. Do not push the earpiece past level 10 by other means.</p>
<p><strong>Left and right are reversed.</strong> Left and right follow the screen: with the earpiece on your left, the earpiece plays left. You need module v0.9.5 or newer. One known limit: an app with its own audio effect (VLC) and an ordinary app playing at the same time, earpiece on your right, can leave one of them reversed. Check with CMF L/R Test.</p>
<p><strong>The first second of a stream sounds different.</strong> In Stereo, the amp's driver resets its slot on every stream start, so the first second or two plays as stock until the module's next cycle.</p>"""},

{"title": "Full volume broken by another module",
"html": """<p>Other modules that change the phone's audio or volume can interfere with CMF True Stereo. A real case from the test phone: NothingTweaks' <strong>Customise Volume Steps</strong>, set to 24 steps, made the highest volume step silent, and the middle of the bar was the loudest, even with CMF True Stereo off. With the custom steps turned off, full volume was correct again.</p>
<p>If the sound behaves strangely and you run other audio or volume tweaks:</p>
<ol>
<li>Turn off the other module's audio or volume feature, or disable that module, and reboot.</li>
<li>Check again with only CMF True Stereo active.</li>
<li>Switch other modules back on one at a time to find the one that clashes.</li>
</ol>
<div class="warn"><p>Do not run the Hi-Res Audio module at the same time as CMF True Stereo: they touch the same files.</p></div>"""},

{"title": "The Stereo speakers row is missing from Settings",
"html": """<ol>
<li>Check that LSPosed is installed and working, and that the edition is enabled in it.</li>
<li>Check the scope: <strong>Settings</strong> (<code>com.android.settings</code>) must be ticked.</li>
<li>Check that the app is on internal storage. On adopted storage LSPosed cannot load it (its log says <em>Failed to find package info</em>); use MOVE TO INTERNAL STORAGE and reboot.</li>
<li>Force-stop Settings (or reboot) after any change in LSPosed.</li>
<li>Look in the LSPosed log for lines starting with <em>CMF Stereo:</em>.</li>
</ol>
<p><strong>Two Stereo speakers rows.</strong> Both editions are enabled in LSPosed. Disable one, then force-stop Settings.</p>
<p><strong>The row opens the app instead of the page.</strong> Your Settings app no longer matches what the page was built for (a Settings update); the LSPosed log says <em>settings page not installed (Settings differs)</em>. Everything else still works.</p>
<p><strong>The page's rows stay greyed.</strong> The page could not get the state: the app has no root, the module is missing, or Nothing OS is a build the module does not know. The Stereo row's description says which.</p>"""},

{"title": "Settings search shows nothing",
"html": """<ol>
<li>In LSPosed, make sure <strong>Settings Intelligence</strong> (<code>com.android.settings.intelligence</code>) is ticked in the app's scope, as well as Settings.</li>
<li>Force-stop <strong>Settings Intelligence</strong> once, or reboot.</li>
<li>Search for <em>bass guard</em>.</li>
<li>If both editions are installed, update both to v1.0.81 or newer. Before v1.0.81, an older edition could hide the newer one's rows, and rows indexed before the hook was on were skipped until the next version.</li>
</ol>"""},

{"title": "The Quick Settings tile",
"html": """<p><strong>The tile says Unavailable.</strong> The app has no root, or the module is not installed. Open the app to see which.</p>
<p><strong>The tile stays on Switching….</strong> The audio service is restarting; wait a moment. If it stays, open the app to see the state.</p>
<p><strong>Two tiles look alike.</strong> CMF Stereo's tile is called Stereo, Pro's Stereo Pro. If both say Stereo, update Pro (older versions used the same name).</p>"""},

{"title": "Installing and updating problems",
"html": """<p><strong>"App not installed" when updating.</strong> The installed app was signed with a different key: a build older than v1.0.35, or a debug build (your own or a fork's). Uninstall it once, then install the release.</p>
<p><strong>The app will not install on the SD card.</strong> That is deliberate: both editions are internal-storage only.</p>
<p><strong>The text is not in NDot.</strong> CMF Stereo found no NDot font on the phone and uses monospace. On a Nothing phone this should not happen.</p>
<p><strong>The icon still shows a kinked outer wave.</strong> That edition is older than v1.0.76; update it (both editions get the same icon).</p>
<p><strong>Many root toasts.</strong> Each root call can make your root manager show a toast. The app makes one root call per reading, and Settings uses the stored state without root.</p>"""},

{"title": "The phone does not boot",
"html": """<p>Wait: after two failed boots with Stereo on, the module removes Stereo and the phone boots in Mono. If it still does not boot, disable the module by hand (chapter 2, <em>If the phone does not boot</em>), or boot into safe mode by holding volume down.</p>"""},

{"title": "Reporting a problem",
"html": """<p>Report bugs as issues on the app's GitHub repository. Include, where you can:</p>
<ul>
<li>the app and module versions (Pro's Status card shows both), your phone model and Nothing OS build;</li>
<li>what happens and the steps that cause it;</li>
<li>Pro's Output card after Doctor, Playback probe or Log, or the module's log;</li>
<li>for Settings problems, the LSPosed log lines starting with <em>CMF Stereo:</em>.</li>
</ul>
<div class="note"><p>Security problems are reported privately, not as public issues: on the repository open the Security tab and choose Report a vulnerability. Only the latest release gets fixes, so update first and check that the problem still happens.</p></div>"""},
]},

# =====================================================================
# 9  FAQ
# =====================================================================
{
"title": "Frequently asked questions",
"intro": "<p>Short answers to the questions people ask most.</p>",
"sections": [

{"title": "General",
"html": """<p><strong>Do I need the app?</strong> No. The module works on its own; the app is a friendly way to control it. You can also switch Mono and Stereo with the Action button on the module's card in your root manager, or with <code>stereoctl</code>.</p>
<p><strong>Which edition should I install?</strong> CMF Stereo for everyday use, Pro for tuning and diagnosing. You can install both.</p>
<p><strong>Does the app run in the background?</strong> No. The module's daemon does the work; the app only reads and changes its settings when you use it (or when Settings, the tile or the module asks it to).</p>
<p><strong>Does it work on other phones?</strong> The module is for the CMF Phone 1 (A015). On other phones it installs and does nothing.</p>
<p><strong>Why is it called beta?</strong> CMF Stereo is still being tested; from v1.0.95-beta on, the versions say so.</p>
<p><strong>Will I lose my settings when I update or uninstall the app?</strong> No. The settings belong to the module and live in <code>/data/adb/cmf-stereo/</code>.</p>"""},

{"title": "Sound",
"html": """<p><strong>Mono or Stereo: which is louder?</strong> Mono, with Awinic's full protection. Use Mono for loud listening.</p>
<p><strong>Why does the speaker sound quieter in Stereo?</strong> It is held at least 6 dB down to keep it safe without Awinic's protection. SPEAKER BOOST makes it louder while keeping its peaks where Awinic keeps them.</p>
<p><strong>Why doesn't the earpiece sound as full as the speaker?</strong> It is a small receiver built for calls. Balance the two by lowering the speaker.</p>
<p><strong>Can I make the earpiece louder than level 10?</strong> No. Level 10 (+8 dB) is the receiver amp's maximum, the value the phone itself uses.</p>
<p><strong>Do I have to rotate the stereo myself?</strong> No. Left and right follow the screen automatically.</p>
<p><strong>Does Stereo affect calls?</strong> No. Calls keep Awinic's protection, and a Mono/Stereo switch is refused during a call.</p>
<p><strong>What about headphones and Bluetooth?</strong> With the guard active (the default), the earpiece stays quiet when they take over.</p>"""},

{"title": "Safety",
"html": """<p><strong>Can Stereo damage my speaker?</strong> In Stereo the speaker has fixed limits but no temperature model. Use Mono for long, loud listening, lower the speaker level in Stereo, and back off at the first distortion.</p>
<p><strong>Can it damage my earpiece?</strong> The earpiece has no protection circuit of its own. Keep the limiter on, and turn it down at the first buzz. Damage is permanent and would also affect calls.</p>
<p><strong>Why can Pro go below 6 dB when CMF Stereo cannot?</strong> For tests. Pro warns you first and explains the risk.</p>
<p><strong>What if Stereo goes wrong at boot?</strong> After two failed boots the module goes back to Mono by itself.</p>"""},

{"title": "Nothing Settings and LSPosed",
"html": """<p><strong>Do I need LSPosed?</strong> Only for the Stereo speakers row and page in Nothing Settings and for Settings search. The apps and the tile work without it.</p>
<p><strong>Does the LSPosed part change anything else in Settings?</strong> No. It marks only its own row as a system one and builds its own page. If Settings changes, it hooks nothing.</p>
<p><strong>Why does the page need Settings Intelligence in the scope?</strong> Settings Intelligence is the app behind Settings search. Without it the page still works, but search does not list its rows.</p>
<p><strong>Can I enable both editions in LSPosed?</strong> You should not: Settings would show two rows.</p>"""},
]},

# =====================================================================
# 10  TECHNICAL REFERENCE
# =====================================================================
{
"title": "Technical reference",
"intro": "<p>Exact names, paths, commands and requirements, for readers who want to see what happens underneath or check something in a terminal.</p>",
"sections": [

{"title": "Packages and components",
"html": """<table>
<tr><th>App</th><th>Package</th><th>Label</th><th>Tile label</th></tr>
<tr><td>CMF Stereo</td><td><code>com.tiimoo.cmfstereo.clean</code></td><td>CMF Stereo</td><td>Stereo</td></tr>
<tr><td>CMF Stereo Pro</td><td><code>com.tiimoo.cmfstereo</code></td><td>CMF Stereo Pro</td><td>Stereo Pro</td></tr>
<tr><td>CMF L/R Test</td><td><code>com.tiimoo.cmfstereo.modetest</code></td><td>CMF L/R Test</td><td>(none)</td></tr>
</table>
<p>Each edition contains:</p>
<ul>
<li>the app screen (also opened by a long press on the tile);</li>
<li>a Settings entry, <em>Stereo speakers</em>, in category <code>com.android.settings.category.ia.sound</code>, order -61, key hint <code>cmf_stereo</code>;</li>
<li>a provider <code>&lt;package&gt;.settingstile</code> (the row's summary, the page's state and changes);</li>
<li>a search provider <code>&lt;package&gt;.search</code>, protected by <code>android.permission.READ_SEARCH_INDEXABLES</code>;</li>
<li>the Quick Settings tile service;</li>
<li>LSPosed module metadata: minimum Xposed API 93, scope <code>com.android.settings</code> and <code>com.android.settings.intelligence</code>.</li>
</ul>
<p>Minimum Android version 8.0 (API 26); target API 35. Version names are <code>1.0.&lt;build&gt;-beta</code>, the build number being the commit count.</p>"""},

{"title": "Module files and paths",
"html": """<table>
<tr><th>Path</th><th>What</th></tr>
<tr><td><code>/data/adb/modules/cmf_stereo</code></td><td>The installed module</td></tr>
<tr><td><code>/data/adb/modules/cmf_stereo/scripts/stereoctl</code></td><td>The command line the apps call</td></tr>
<tr><td><code>/data/adb/modules/cmf_stereo/module.prop</code></td><td>The module version</td></tr>
<tr><td><code>/data/adb/cmf-stereo/stereo.conf</code></td><td>Behaviour: mode, channels, attenuation, protection, poll interval, log level</td></tr>
<tr><td><code>/data/adb/cmf-stereo/actions.conf</code></td><td>The routing, one action per line (including <code>ctl|Handset Volume|N</code>)</td></tr>
<tr><td><code>/data/adb/cmf-stereo/stereo.log</code></td><td>What the daemon did</td></tr>
<tr><td><code>/data/adb/cmf-stereo/state/</code></td><td>Saved original values, used to revert</td></tr>
<tr><td><code>/data/adb/cmf-stereo/guard_bypass</code></td><td>Present while the guard is bypassed</td></tr>
<tr><td><code>/sdcard/cmf-stereo-report.txt</code></td><td>Written by Report</td></tr>
</table>
<p>The configuration lives outside the module folder and survives module updates and reinstalls.</p>"""},

{"title": "What each control runs",
"html": """<table>
<tr><th>Control</th><th>Command or file change</th></tr>
<tr><td>EARPIECE SPEAKER / Earpiece output / Earpiece speaker</td><td><code>stereoctl on</code> / <code>off</code></td></tr>
<tr><td>MONO / STEREO, tile, Stereo switch</td><td><code>stereoctl channels mono</code> / <code>stereo</code></td></tr>
<tr><td>EARPIECE LEVEL, Earpiece gain</td><td><code>Handset Volume</code> in <code>actions.conf</code>, read back, then <code>stereoctl ctl 'Handset Volume' N</code></td></tr>
<tr><td>SPEAKER LEVEL, Stereo attenuation</td><td><code>STEREO_ATTEN_DB</code> in <code>stereo.conf</code> (0-30), read back</td></tr>
<tr><td>SPEAKER BOOST</td><td><code>stereoctl protect boost 0-6</code></td></tr>
<tr><td>DEEP BASS GUARD</td><td><code>stereoctl protect bass off|light|medium|strong</code></td></tr>
<tr><td>EARPIECE LEVELLER</td><td><code>stereoctl protect leveller off|light|medium|strong</code></td></tr>
<tr><td>EARPIECE LIMITER</td><td><code>stereoctl protect earpiece on|off</code></td></tr>
<tr><td>RESTART (daemon), Restart</td><td><code>stereoctl restart</code></td></tr>
<tr><td>RESTART PHONE, Reboot now</td><td><code>svc power reboot</code>, or <code>reboot</code></td></tr>
<tr><td>MOVE, Move to internal storage</td><td><code>pm move-package &lt;package&gt; internal</code></td></tr>
<tr><td>Mode</td><td><code>MODE=playback|always</code> in <code>stereo.conf</code></td></tr>
<tr><td>Solo / Un-solo</td><td><code>stereoctl solo</code> / <code>unsolo</code></td></tr>
<tr><td>Doctor</td><td><code>stereoctl doctor</code></td></tr>
<tr><td>Apply / Revert</td><td><code>stereoctl apply</code> / <code>revert</code></td></tr>
<tr><td>Poll interval</td><td><code>WATCH_INTERVAL</code> in <code>stereo.conf</code></td></tr>
<tr><td>Guard</td><td><code>stereoctl guard on|off</code></td></tr>
<tr><td>Log level</td><td><code>LOG_LEVEL</code> in <code>stereo.conf</code></td></tr>
<tr><td>XML patch / XML revert / Reset state</td><td><code>stereoctl xml-patch</code> / <code>xml-revert</code> / <code>reset</code></td></tr>
<tr><td>Read / Write, Dump all, Live gain</td><td><code>stereoctl ctl NAME [VALUE]</code>, <code>stereoctl dump</code>, <code>stereoctl ctl 'Handset Volume'</code></td></tr>
<tr><td>Scan, Diff</td><td><code>stereoctl scan [pattern]</code>, <code>stereoctl diff [seconds]</code></td></tr>
<tr><td>Probe, Report</td><td><code>stereoctl probe</code>, <code>stereoctl report</code></td></tr>
<tr><td>Playback probe</td><td>playback states from <code>/proc/asound/card*/pcm*p/sub*/status</code>, and <code>stereoctl status</code>'s playback line</td></tr>
<tr><td>Run, Help</td><td><code>stereoctl &lt;arguments&gt;</code>, <code>stereoctl --help</code></td></tr>
<tr><td>stereo.conf, actions.conf, Log</td><td>shows the files; <code>stereoctl log 60</code></td></tr>
</table>
<div class="tech"><p>Commands are written to the root shell's standard input rather than passed with <code>su -c</code>, so the root shell parses exactly the text the app wrote (quoting survives). Each root call times out after 180 seconds.</p></div>"""},

{"title": "stereo.conf settings",
"html": """<table>
<tr><th>Key</th><th>Values</th><th>Default</th><th>Set from</th></tr>
<tr><td><code>MODE</code></td><td>playback, always</td><td>playback</td><td>Pro: Mode</td></tr>
<tr><td><code>CHANNELS</code></td><td>mono, stereo</td><td>mono</td><td>Mono/Stereo (set it through the switch, not by hand)</td></tr>
<tr><td><code>STEREO_ATTEN_DB</code></td><td>0-30</td><td>6</td><td>SPEAKER LEVEL, Stereo attenuation</td></tr>
<tr><td><code>SPEAKER_BOOST</code></td><td>0-6</td><td>0</td><td>SPEAKER BOOST</td></tr>
<tr><td><code>BASS_PROTECT</code></td><td>off, light, medium, strong</td><td>medium</td><td>DEEP BASS GUARD</td></tr>
<tr><td><code>EARPIECE_LIMIT</code></td><td>on, off</td><td>on</td><td>EARPIECE LIMITER</td></tr>
<tr><td><code>EARPIECE_LEVELLER</code></td><td>off, light, medium, strong</td><td>off</td><td>EARPIECE LEVELLER</td></tr>
<tr><td><code>WATCH_INTERVAL</code></td><td>seconds</td><td>2</td><td>Pro: Poll interval</td></tr>
<tr><td><code>LOG_LEVEL</code></td><td>debug, info, warn</td><td>info</td><td>Pro: Log level</td></tr>
<tr><td><code>PLAYBACK_CTL</code> / <code>PLAYBACK_VALUE</code></td><td>mixer controls separated by |</td><td>the DSP playback task switches</td><td>by hand</td></tr>
<tr><td><code>GUARD_CTL</code> / <code>GUARD_VALUE</code></td><td>a mixer control and value</td><td><code>aw_dev_0_switch</code> = Enable</td><td>by hand</td></tr>
</table>
<p><code>actions.conf</code> syntax, one action per line, applied in order: <code>ctl|&lt;mixer control&gt;|&lt;value&gt;</code>, <code>sysfs|&lt;path&gt;|&lt;value&gt;</code>, <code>prop|&lt;property&gt;|&lt;value&gt;</code>. The original value of everything touched is saved and restored on stop.</p>"""},

{"title": "How the app reads the state",
"html": """<p>Everything the screen shows is read in one root shell (it used to take about fourteen). From <code>stereoctl status brief</code> the app takes:</p>
<ul>
<li><code>armed: yes</code>, <code>daemon: running</code>, <code>playback: active</code>, <code>mode: …</code>, <code>(N configured)</code>;</li>
<li><code>channels: stereo</code>, or <code>channels: mono (switches to stereo after reboot)</code> for a pending switch;</li>
<li><code>build: known|updated|absent</code>.</li>
</ul>
<p>From <code>stereoctl --help</code> it learns which features the module has: <code>doctor</code>, <code>reset</code>, <code>channels</code>, <code>protect</code> and <code>protect leveller</code>. Panels for features the module lacks are hidden.</p>
<p>A value that cannot be read is shown as unreadable, never as 0: a displayed 0 written back is how an early version once zeroed a working configuration.</p>"""},

{"title": "Module version requirements",
"html": """<table>
<tr><th>Feature</th><th>Module version</th></tr>
<tr><td>Doctor, Reset state</td><td>v0.7.1</td></tr>
<tr><td>Mono/Stereo (SOUND, Channels)</td><td>v0.9.0</td></tr>
<tr><td>Correct earpiece maximum (Handset Volume 0)</td><td>v0.9.2</td></tr>
<tr><td>Fast status reading (<code>status brief</code>), near-instant on/off</td><td>v0.9.3</td></tr>
<tr><td>Instant Mono/Stereo switch</td><td>v0.9.4</td></tr>
<tr><td>Left and right follow the screen</td><td>v0.9.5</td></tr>
<tr><td>NOTHING OS UPDATED notice; Action button</td><td>v0.9.8</td></tr>
<tr><td>Both speakers in games with While playing</td><td>v1.0.3</td></tr>
<tr><td>Protection: boost, deep bass guard, earpiece limiter</td><td>v1.0.4</td></tr>
<tr><td>Earpiece leveller</td><td>v1.0.5</td></tr>
<tr><td>Nothing Settings and tile follow outside changes</td><td>v1.0.6</td></tr>
</table>
<p>This app release is made for module v1.0.6 or newer.</p>"""},

{"title": "Nothing Settings internals",
"html": """<ul>
<li><strong>Row</strong>: Settings' own tile injection. Settings takes such rows only from system apps (it checks <code>resolveInfo.system</code>); the hook on <code>queryIntentActivitiesAsUser</code> marks only this app's entry as system.</li>
<li><strong>Page</strong>: a tap on the row is sent to Settings' own <code>SubSettings</code> with a small Sound page (<code>MediaControlsSettings</code>) as carrier, titled Stereo speakers; once that page has built itself its rows are replaced with Nothing's own widgets (<code>NtCustSwitchPreferenceCompat</code>, <code>Preference</code>, <code>PreferenceCategory</code>), and the dots and pills are added under each row's summary in Settings' own colours and font.</li>
<li><strong>State</strong>: through the provider methods <code>getDynamicSummary</code>, <code>getPageState</code> and <code>setPage</code>, which answer only Settings and the app itself, on a thread of their own. <code>stateChanged</code> (called by the module) only triggers a fresh reading.</li>
<li><strong>Search</strong>: three hooks in Settings Intelligence: <code>PreIndexDataCollector.isWellKnownProvider</code>, <code>IndexDatabaseHelper.buildProviderVersionedNames</code> (marker <code>cmf-search:1</code>) and <code>IndexData$Builder.buildIntent</code> (results open Settings' <code>SEARCH_RESULT_TRAMPOLINE</code> with the row's key as <code>:settings:fragment_args_key</code>).</li>
</ul>
<p>The obfuscated names in Settings were read from Nothing Settings 16.0.0 and are checked against their signatures before anything is hooked.</p>"""},

{"title": "LSPosed log messages",
"html": """<table>
<tr><th>Message</th><th>Meaning</th></tr>
<tr><td>CMF Stereo: settings page ready</td><td>The page is installed</td></tr>
<tr><td>CMF Stereo: settings page not installed (Settings differs): …</td><td>Settings does not match; the row opens the app</td></tr>
<tr><td>CMF Stereo: settings hook not installed: …</td><td>The row could not be let in</td></tr>
<tr><td>CMF Stereo: settings tile: …</td><td>An error while marking the row</td></tr>
<tr><td>CMF Stereo: settings page: …</td><td>An error inside the page (it carries on)</td></tr>
<tr><td>CMF Stereo: settings search ready (3 hooks)</td><td>Settings search is installed</td></tr>
<tr><td>CMF Stereo: settings search not installed: …</td><td>Settings Intelligence does not match</td></tr>
</table>"""},

{"title": "Privacy and permissions",
"html": """<ul>
<li>The apps ask for root through your root manager, and use it only to run the module's command and read or write its files.</li>
<li>The apps' own manifest requests no Android permissions; there is no internet permission, so nothing is sent anywhere.</li>
<li>The state is kept on the phone, in the app's private storage, so Settings can show it without root.</li>
<li>The Settings provider answers only Nothing's Settings and the app itself; the search provider can only be read by Settings Intelligence.</li>
<li>CMF L/R Test asks for <code>MODIFY_AUDIO_SETTINGS</code> and, for DRIVER STUDY, the microphone (<code>RECORD_AUDIO</code>). Its recordings are saved only to your phone's storage, for you to send if you choose.</li>
</ul>
<div class="note"><p>Because the app holds root, the project treats as security problems: any way to make it run a root command you did not intend, exposing its root access to other apps, or writing values that push the speaker or earpiece past the documented ranges.</p></div>"""},

{"title": "Signing and builds",
"html": """<p>Releases are signed with the project's permanent key (from v1.0.35). A local build without the key, or a build from a fork, is debug-signed and cannot update an installed release. Release builds are shrunk, which is why each APK is about 1 MB (an early version was about 100 MB).</p>
<p>To build it yourself: <code>./gradlew assembleRelease</code>.</p>"""},

{"title": "Version history",
"html": """<table>
<tr><th>Version</th><th>Date</th><th>Change</th></tr>
<tr><td>v1.0.97-beta</td><td>8 Nov 2026</td><td>Pro warns before setting the speaker's stereo margin under 6 dB</td></tr>
<tr><td>v1.0.95-beta</td><td>8 Nov 2026</td><td>Versions marked beta; new banner</td></tr>
<tr><td>v1.0.81</td><td>8 Nov 2026</td><td>Settings search finds the stereo settings</td></tr>
<tr><td>v1.0.80</td><td>8 Nov</td><td>Stereo speakers settings in Settings search</td></tr>
<tr><td>v1.0.79</td><td>8 Nov</td><td>Bolder themed icon; CMF L/R Test gets its own icon</td></tr>
<tr><td>v1.0.76</td><td>7 Nov</td><td>Rounder outer sound wave on the icon</td></tr>
<tr><td>v1.0.75</td><td>7 Nov</td><td>Every text says left and right follow how you hold the phone</td></tr>
<tr><td>v1.0.71</td><td>6 Nov</td><td>Stereo in Nothing Settings (LSPosed), Quick Settings tile, instant sync without root, internal storage only</td></tr>
<tr><td>v1.0.60</td><td>5 Nov</td><td>Earpiece leveller (module v1.0.5)</td></tr>
<tr><td>v1.0.56</td><td>5 Nov</td><td>Speaker boost, deep bass guard, earpiece limiter (module v1.0.4)</td></tr>
<tr><td>v1.0.53</td><td>5 Nov</td><td>Both speakers in games (module v1.0.3); Nothing OS update notice</td></tr>
<tr><td>v1.0.39</td><td>4 Nov</td><td>Dot-matrix icon, live with Nothing2Something; licence CC BY-NC-SA 4.0</td></tr>
<tr><td>v1.0.35</td><td>4 Nov</td><td>Permanent signing key; instant Mono/Stereo; left and right follow the screen</td></tr>
<tr><td>v1.0.29</td><td>4 Nov</td><td>About 1 MB instead of about 100 MB; faster</td></tr>
<tr><td>v1.0.25</td><td>4 Nov</td><td>Two editions; Mono/Stereo; earpiece at its real maximum; speaker level; NDot font</td></tr>
<tr><td>v1.0.21</td><td>3 Nov</td><td>The first build of the two editions</td></tr>
</table>"""},

{"title": "Not yet seen on a phone",
"html": """<p>As of v1.0.97-beta, these features are built and checked in code but have not yet been seen on a real phone:</p>
<ul>
<li>Pro's warning before stereo attenuation under 6 dB (v1.0.97-beta);</li>
<li>the Quick Settings tiles' names (Stereo / Stereo Pro) and a tap on them;</li>
<li>the Stereo speakers page updating by itself while open after a change made outside it;</li>
<li>the MOVE TO INTERNAL STORAGE panel;</li>
<li>the NOTHING OS UPDATED panel.</li>
</ul>
<p>Verified on the phone: both editions' on/off, live Mono/Stereo switching and levels; the protection and leveller settings; the Stereo speakers row and page in Nothing Settings; the tiles in the tile editor; LSPosed listing the apps on internal storage; CMF L/R Test; Settings search.</p>"""},
]},

# =====================================================================
# 11  CREDITS AND LICENCE
# =====================================================================
{
"title": "Credits and licence",
"intro": "<p>Who made CMF Stereo, and the terms you may use it under.</p>",
"sections": [

{"title": "Credits",
"html": """<ul>
<li><strong>Dr.Essence</strong>: author and maintainer. Ideas, design decisions, and every test on a real phone (logs, recordings, measurements) that the work rests on.</li>
<li>Code written with Claude Code.</li>
<li>Shout-out to <strong>Komi Store</strong>, the free, open-source app store for GitHub releases that keeps the app updated.</li>
<li>The app is the front end for the <strong>CMF True Stereo</strong> module, in the same spirit as PerfMTK and its separate PerfMTK-Manager.</li>
</ul>
<p>The module thanks <strong>Adinata (@Adivenxnataly)</strong>, whose Hi-Res Audio module pointed the project at the right part of MediaTek's audio stack, and the people who worked out dual-speaker mods on other phones and wrote them up.</p>"""},

{"title": "Licence",
"html": """<p>© 2026 Dr.Essence, under the <strong>Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International</strong> licence (CC BY-NC-SA 4.0). You may use, copy, change and share it, as long as you:</p>
<ul>
<li>credit Dr.Essence;</li>
<li>do not use it commercially, including selling it or anything built from it;</li>
<li>share changed versions under the same licence.</li>
</ul>
<p>Releases up to v1.0.35 were published under 0BSD; copies obtained under it keep those terms.</p>
<p>CMF Stereo draws Nothing's NDot font from the phone at runtime; no Nothing font ships in the app. The module bundles a <code>tinymix</code> binary built from tinyalsa (BSD-3-Clause).</p>"""},

]},
]
