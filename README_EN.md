# PaperPhoneLite Android

[简体中文](README.md) · [Changelog](changelog.md)

> Android client for [PaperPhoneLite](https://github.com/619dev/PaperPhoneLite), packaging the upstream React/TypeScript frontend with Capacitor 8 and a bundled Tor client.

[![Upstream](https://img.shields.io/badge/Upstream-619dev%2FPaperPhoneLite-blue?logo=github)](https://github.com/619dev/PaperPhoneLite)
[![Version](https://img.shields.io/badge/Version-3.0.8-orange)](package.json)
[![License](https://img.shields.io/badge/License-AGPL--3.0-blue.svg)](LICENSE)

## Scope

This repository contains only the Android client. The Rust server, MySQL, Redis, onion-service deployment, and self-hosting guides are maintained in the [upstream repository](https://github.com/619dev/PaperPhoneLite).

The current client provides:

- Private and group chats, contacts, groups, and friend requests.
- Text, image, video, document, voice, emoji, and Telegram sticker messages.
- X25519 + ML-KEM-768 hybrid key agreement, XSalsa20-Poly1305 message encryption, group Sender Keys, and safety numbers.
- Message synchronization, local offline caches, a persistent outbox, read receipts, and typing state.
- Message expiry, blocking, friend labels, QR flows, and TOTP two-factor authentication.
- An extra message password, eight text presentations, and foreground/background locking.
- The required Android ntfy registration and subscription flow.

PaperPhoneLite 3.x does **not** provide Moments, a public Timeline, direct or group voice/video calls, LiveKit, Cloudflare R2, or a production Web/PWA distribution.

## Tor and Network Boundary

- The APK bundles Guardian Project `tor-android 0.4.8.17.2` for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`.
- The native Tor service exposes SOCKS5 on `127.0.0.1:9050`; the Android WebView proxy switches to it after a Tor circuit is established.
- If direct Tor cannot establish a circuit within 20 seconds, the client fetches a current WebTunnel bridge from Tor Project's official circumvention-settings endpoint and restarts Tor through bundled IPtProxy/Lyrebird; the last successfully fetched bridge is retained as a temporary fallback.
- Production builds accept only valid v3 `.onion` server addresses. Vite development additionally permits loopback addresses.
- The native proxy bridge refuses requests to clear Tor or replace it with another proxy. Browser development cannot enforce a proxy from JavaScript and is not a supported production distribution.
- Tor can conceal network origin, but cannot guarantee absolute anonymity or prevent correlation through a device, account behavior, notification provider, or voluntary disclosure.

## Notifications and Third Parties

ntfy is the retained and required Android notification path. The app obtains a dedicated topic, registers it with the server, and provides copy/download actions; the user subscribes to that topic in the ntfy app. The client includes no FCM, Firebase, OneSignal, Web Push, or Google Services.

## Local Data and Keys

- The upstream frontend stores identity keys and Sender Keys in IndexedDB, while sessions, settings, message caches, and offline data use localStorage.
- Decrypted message fields are removed before message-cache persistence, but the current 3.0.8 frontend does not encrypt the entire chat cache with Android Keystore.
- Native `SecureStoragePlugin` and `KeepAwakePlugin` compatibility code remains, but the synchronized 3.0 frontend does not call it. It must not be described as active system-backed protection for current keys or caches.
- Clearing app data or uninstalling the app can permanently remove local keys and unrecoverable history.

## Android Identity

| Item | Value |
|---|---|
| App name | `PaperPhoneLite` |
| Application ID | `com.fm619.paperphonelite` |
| Version | `3.0.8` |
| Version code | `30008` |
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

The release APK is written to `android/app/build/outputs/apk/release/app-release.apk`. Official Android APKs are distributed only through this repository's [GitHub Releases](https://github.com/619dev/ppl-android/releases), not Google Play.

## Upstream Synchronization

The upstream frontend lives in `PaperPhoneLite/client`. Preserve the native Android Tor integration, Capacitor configuration, and compatibility plugins when synchronizing, then rebuild the frontend and run `npx cap sync android`. Do not copy upstream server code into this repository.

## License and Issues

This project is released under [AGPL-3.0](LICENSE), matching [PaperPhoneLite upstream](https://github.com/619dev/PaperPhoneLite).

- Android client issues: [619dev/ppl-android/issues](https://github.com/619dev/ppl-android/issues)
- Upstream protocol, frontend, or server issues: [619dev/PaperPhoneLite/issues](https://github.com/619dev/PaperPhoneLite/issues)
