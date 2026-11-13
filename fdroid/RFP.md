# Original F-Droid packaging request

Historical reference for [RFP #4291](https://gitlab.com/fdroid/rfp/-/work_items/4291).
Initial inclusion [MR !46295](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/46295)
was merged. Version updates use a separate MR; do not resubmit the original
new-app checklist.

#### APPLICATION ID: com.fm619.paperphonelite

```yaml
Categories:
  - Messaging

License: AGPL-3.0-only

AuthorName: 619dev
AuthorEmail:
AuthorWebSite: https://github.com/619dev

WebSite: https://paperphone.app
SourceCode: https://github.com/619dev/ppl-android
IssueTracker: https://github.com/619dev/ppl-android/issues

Donate:
Bitcoin:
LiberaPay:

AutoName: PaperPhoneLite

RepoType: git
Repo: https://github.com/619dev/ppl-android.git
```

Current release: `3.1.5` (`versionCode 30105`)

Source ref: `v3.1.5` (the immutable release tag must resolve to the reviewed commit)

Release: https://github.com/619dev/ppl-android/releases/tag/v3.1.5

Version-update MR: [!57182](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/57182).

Source commit: resolve the immutable `v3.1.5` tag. The recipe adds 3.1.5 (30105) and preserves existing build entries.

Independent F-Droid reproduction status is recorded in the version-update MR CI.

Why do you want this app added to F-Droid:

> PaperPhoneLite provides a privacy-focused client for self-hosted messaging.
> Distribution through F-Droid gives users a transparent, independently built
> installation and update channel without requiring Google Play services.

Summary:

> Private messaging client for a self-hosted onion service

Description:

> PaperPhoneLite is a client for private messaging through a self-hosted
> PaperPhoneLite onion service. It bundles Tor and supports private and group
> chats, attachments, local offline data, disappearing messages, safety
> numbers, signed messages and group keys, offline-password identity verification,
> optional text-appearance encryption, and optional ntfy notifications.
>
> If Tor has no circuit after 20 seconds, the app connects directly outside Tor
> to bridges.torproject.org to request WebTunnel bridges with country code cn.
> It includes a recovery WebTunnel bridge at ame.neverfeltsogood.top for a known stale cached bridge.
> Android system backup and app-data device transfer are disabled.
>
> A compatible self-hosted server is required. The app does not include Google
> Play services, Firebase, advertising, or analytics.

Version 3.1.5 requires compatible clients and server, with signed Sender Key group messaging using server-published member keys.
Private password verification remains independent of group sending. The matching
server repairs historical one-sided friendships at startup. See [release guidance](README.md) and the
[client security boundaries](../README_EN.md#315-upgrade-and-security-boundaries).
