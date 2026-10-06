# Privacy Policy — AsyncAPI Companion

**Effective date:** 2026-10-06

AsyncAPI Companion is a Gap Hunter Labs plugin for IntelliJ Platform IDEs.
This policy is short because the plugin's design makes it short: there
is nothing to disclose beyond what's below.

## What this plugin collects

**Nothing.** AsyncAPI Companion does not collect, transmit, or sell
any data — no source code, no file contents, no usage analytics, no
telemetry, no crash reports, no personally identifiable information.

## What it keeps on your machine

To decide when to show its one-time rating prompt, the plugin keeps two values
in the IDE's own settings on your computer: whether you have answered the
prompt, and a list of up to 500 findings it has already counted. Until the
next release, each entry in that list is the file path and line of a finding,
sometimes with its message. From the next release on, each entry is a one-way
fingerprint that cannot be turned back into a path, and the old list is
deleted. None of this is ever sent anywhere.

## Network access

**None.** AsyncAPI Companion makes zero network calls, ever. Every `$ref`
resolution it performs runs entirely in-process, inside your IDE,
against local files already on disk. An `http(s)://` `$ref` is shown as
unresolved rather than fetched over the network. Free, no license
check, no telemetry of any kind.

## Third parties

None. AsyncAPI Companion has no third-party SDKs, no analytics
libraries, no ad networks, no external dependencies that phone home.

## Changes to this policy

If this ever changes, this file will be updated and the change will be
noted in the plugin's `CHANGELOG.md`.

## Contact

Questions about this policy: **gaphunterlabs@gmail.com**
