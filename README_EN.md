# PaperPhoneLite Android

[简体中文](README.md) · [Changelog](changelog.md)

> Android client for [PaperPhoneLite](https://github.com/619dev/PaperPhoneLite), packaging the upstream React/TypeScript frontend with Capacitor 8 and a bundled Tor client.

[![Upstream](https://img.shields.io/badge/Upstream-619dev%2FPaperPhoneLite-blue?logo=github)](https://github.com/619dev/PaperPhoneLite)
[![Version](https://img.shields.io/badge/Version-3.0.20-orange)](package.json)
[![License](https://img.shields.io/badge/License-AGPL--3.0-blue.svg)](LICENSE)

## Scope

This repository contains only the Android client. The Rust server, MySQL, Redis, onion-service deployment, and self-hosting guides are maintained in the [upstream repository](https://github.com/619dev/PaperPhoneLite).

The current client provides:

- Private and group chats, contacts, groups, and friend requests.
- Text, image, video, document, voice, emoji, and Telegram sticker messages.
- X25519 + ML-KEM-768 hybrid key agreement, XSalsa20-Poly1305 message encryption, group Sender Keys, and safety numbers.
- Message synchronization, local offline caches, a persistent outbox, read receipts, and typing state.
- Message expiry, blocking, friend labels, QR flows, and TOTP two-factor authentication.
- An extra message password, eight text presentations, foreground/background locking, and a startup unlock prompt.
- The optional Android ntfy registration and subscription flow.

PaperPhoneLite 3.x does **not** provide Moments, a public Timeline, direct or group voice/video calls, LiveKit, Cloudflare R2, or a production Web/PWA distribution.

## Tor and Network Boundary

- Release APKs bundle Guardian Project `tor-android 0.4.8.17.2` for 64-bit ARM devices only (`arm64-v8a`). Version 3.0.20 disables R8 code minification and Android resource shrinking to prevent runtime crashes in reflection/JNI dependencies. Debug builds remain available for local development testing.
- The native Tor service exposes SOCKS5 on `127.0.0.1:9050`; the Android WebView proxy switches to it only after Tor reaches 100% bootstrap, preventing login requests while the circuit is not yet usable.
- If direct Tor cannot establish a circuit within 20 seconds, the client fetches a current WebTunnel bridge from Tor Project's official circumvention-settings endpoint and restarts Tor through bundled IPtProxy/Lyrebird. Bridges are normalized with `utls=none`, and the last successfully fetched bridge is retained as a temporary fallback.
- Before a WebTunnel restart, the client removes only rebuildable Tor directory-consensus caches so stale consensus data cannot leave bootstrap stalled; account, message, and key data are not cleared.
- Production builds accept only valid v3 `.onion` server addresses. Vite development additionally permits loopback addresses.
- The native proxy bridge refuses requests to clear Tor or replace it with another proxy. Browser development cannot enforce a proxy from JavaScript and is not a supported production distribution.
- Tor can conceal network origin, but cannot guarantee absolute anonymity or prevent correlation through a device, account behavior, notification provider, or voluntary disclosure.

## Notifications and Third Parties

ntfy is the only supported, optional Android background-notification path. When enabled, the app obtains a dedicated topic, registers it with the server, and provides copy/download actions; the user subscribes to that topic in the ntfy app. The client includes no FCM, Firebase, OneSignal, Web Push, or Google Services.

## Local Data and Keys

- The upstream frontend stores identity keys and Sender Keys in IndexedDB, while sessions, settings, message caches, and offline data use localStorage.
- Decrypted message fields are removed before message-cache persistence, but the current 3.0.20 frontend does not encrypt the entire chat cache with Android Keystore.
- When text-appearance encryption is enabled, startup asks for the extra password; cancelling or entering an incorrect password leaves only the styled ciphertext visible. This does not replace a device lock or Android Keystore.
- Native `SecureStoragePlugin` and `KeepAwakePlugin` compatibility code remains, but the synchronized 3.0 frontend does not call it. It must not be described as active system-backed protection for current keys or caches.
- Clearing app data or uninstalling the app can permanently remove local keys and unrecoverable history.

## Android Identity

| Item | Value |
|---|---|
| App name | `PaperPhoneLite` |
| Application ID | `com.fm619.paperphonelite` |
| Version | `3.0.20` |
| Version code | `30020` |
| Minimum Android API | 24 |

## Build

Node.js, npm, JDK 21, and the Android SDK are required.

```bash
npm install
npm run build
npx cap sync android
cd android
./gradlew assembleDebug
```

The debug APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`.

A release build requires a private, untracked `paperphone-release.keystore`. Supply signing passwords through environment variables:

```bash
export KEYSTORE_PASSWORD='...'
export KEY_PASSWORD='...'
cd android
./gradlew clean assembleRelease
```

With signing environment variables, the release APK is written to `android/app/build/outputs/apk/release/app-release.apk`; without signing credentials, Gradle produces `app-release-unsigned.apk`. Releases support `arm64-v8a` only; version 3.0.20 disables R8 and resource shrinking to fix the startup crash, and the signed build has passed installation and runtime testing. Official Android APKs are distributed through this repository's [GitHub Releases](https://github.com/619dev/ppl-android/releases). F-Droid inclusion is in progress through [RFP #4291](https://gitlab.com/fdroid/rfp/-/work_items/4291) and [fdroiddata MR !46295](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/46295); the app is not published on Google Play. F-Droid signs its build with a separate key, so F-Droid and GitHub Release APKs cannot upgrade each other in place.

See [`fdroid/README.md`](fdroid/README.md) for the submission and verification procedure and [`fdroid/com.fm619.paperphonelite.yml`](fdroid/com.fm619.paperphonelite.yml) for the candidate build metadata.

## Upstream Synchronization

The upstream frontend lives in `PaperPhoneLite/client`. Preserve the native Android Tor integration, Capacitor configuration, and compatibility plugins when synchronizing, then rebuild the frontend and run `npx cap sync android`. Do not copy upstream server code into this repository.

## License and Issues

This project is released under [AGPL-3.0](LICENSE), matching [PaperPhoneLite upstream](https://github.com/619dev/PaperPhoneLite).

- Android client issues: [619dev/ppl-android/issues](https://github.com/619dev/ppl-android/issues)
- Upstream protocol, frontend, or server issues: [619dev/PaperPhoneLite/issues](https://github.com/619dev/PaperPhoneLite/issues)
