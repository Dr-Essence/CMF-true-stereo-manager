# Security policy

## Supported versions

Only the latest release of **CMF-true-stereo-manager** gets fixes. Please update and check the
problem still happens before reporting.

## Reporting a vulnerability

**Please do not open a public issue for security problems.**

Report privately through GitHub instead: on this repository open the
**Security** tab and choose **Report a vulnerability**. Only the maintainer
sees the report.

Include what you can of:

- the version, your phone model and Android / Nothing OS build;
- what happens, and the steps or input that trigger it;
- logs (`logcat`, the module's own log) or a proof of concept.

## What counts as a security problem here

The app holds root access and passes values to the module's `stereoctl`, so
these are in scope:

- any way to make the app run a root command its user did not intend, for
  example through text fields, config values or file contents it reads;
- the app exposing its root access to other apps (exported components,
  intents, files);
- the app writing values that push the speaker or earpiece past the ranges it
  documents.

Ordinary bugs, crashes that need no special input, and feature requests
belong in regular issues.

## What to expect

You will get an acknowledgement as soon as the report has been read. Confirmed
problems are fixed in the next release, and you will be credited in its notes
unless you would rather not be. Please allow a fix to ship before disclosing
the details publicly.
