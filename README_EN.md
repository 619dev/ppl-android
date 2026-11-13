# PaperPhoneLite Android

[简体中文](README.md) · [Changelog](changelog.md)

> Android client for [PaperPhoneLite](https://github.com/619dev/PaperPhoneLite), packaging the upstream React/TypeScript frontend with Capacitor 8 and a bundled Tor client.

[![Upstream](https://img.shields.io/badge/Upstream-619dev%2FPaperPhoneLite-blue?logo=github)](https://github.com/619dev/PaperPhoneLite)
[![Version](https://img.shields.io/badge/Version-3.1.5-orange)](package.json)
[![License](https://img.shields.io/badge/License-AGPL--3.0-blue.svg)](LICENSE)

## Scope

This repository contains only the Android client. The Rust server, MySQL, Redis, onion-service deployment, and self-hosting guides are maintained in the [upstream repository](https://github.com/619dev/PaperPhoneLite).

The current client provides:

- Private and group chats, contacts, groups, and friend requests.
- Text, image, video, document, voice, emoji, and Telegram sticker messages.
- X25519 key agreement, XSalsa20-Poly1305 message encryption, Ed25519 message and group-key signatures, safety numbers, and identity badges.
- Message synchronization, local offline caches, a persistent outbox, read receipts, and typing state.
- Message expiry, blocking, friend labels, QR flows, and TOTP two-factor authentication.
- An extra message password, eight text presentations, foreground/background locking, and a startup unlock prompt.
- The optional Android ntfy registration and subscription flow.

PaperPhoneLite 3.x does **not** provide Moments, a public Timeline, direct or group voice/video calls, LiveKit, Cloudflare R2, or a production Web/PWA distribution.

## Tor and Network Boundary

- Release APKs bundle Guardian Project `tor-android 0.4.8.17.2` for 64-bit ARM devices only (`arm64-v8a`). Release builds enable R8 code minification, with keep rules for reflective and JNI entry points. Resource shrinking stays disabled. Debug builds remain available for local development testing.
- The native Tor service exposes SOCKS5 on `127.0.0.1:9050`; the Android WebView proxy switches to it only after Tor reaches 100% bootstrap, preventing login requests while the circuit is not yet usable.
- If direct Tor cannot establish a circuit within 20 seconds, the client fetches a current WebTunnel bridge from Tor Project's official circumvention-settings endpoint and restarts Tor through bundled IPtProxy/Lyrebird. Bridges are normalized with `utls=none`, and the last successfully fetched bridge is retained as a temporary fallback.
- Before a WebTunnel restart, the client removes only rebuildable Tor directory-consensus caches so stale consensus data cannot leave bootstrap stalled; account, message, and key data are not cleared.
- Production builds accept only valid v3 `.onion` server addresses. Vite development additionally permits loopback addresses.
- The native proxy bridge refuses requests to clear Tor or replace it with another proxy. Browser development cannot enforce a proxy from JavaScript and is not a supported production distribution.
- Tor can conceal network origin, but cannot guarantee absolute anonymity or prevent correlation through a device, account behavior, notification provider, or voluntary disclosure.

## Notifications and Third Parties

ntfy is the only supported, optional Android background-notification path. When enabled, the app obtains a dedicated topic, registers it with the server, and provides copy/download actions; the user subscribes to that topic in the ntfy app. The client includes no FCM, Firebase, OneSignal, Web Push, or Google Services.

## Local Data and Keys

- Identity keys, Sender Keys and identity trust records use account-scoped browser secret storage, encrypted in IndexedDB with a non-exportable WebCrypto AES-GCM key. This is not Android Keystore. Sessions, settings, message caches and offline data use localStorage.
- Decrypted message fields are removed before message-cache persistence, but the current 3.1.5 frontend does not encrypt the entire chat cache with Android Keystore.
- Android system backup is disabled, and app data (including WebView identity keys and message caches) is excluded from cloud backup and device transfer.
- When text-appearance encryption is enabled, startup asks for the extra password; cancelling or entering an incorrect password leaves only the styled ciphertext visible. This does not replace a device lock or Android Keystore.
- Native `SecureStoragePlugin` and `KeepAwakePlugin` compatibility code remains, but the synchronized 3.1.5 frontend does not call it. It must not be described as active system-backed protection for current keys or caches.
- Clearing app data or uninstalling the app can permanently remove local keys and unrecoverable history.

## Android Identity

| Item | Value |
|---|---|
| App name | `PaperPhoneLite` |
| Application ID | `com.fm619.paperphonelite` |
| Version | `3.1.5` |
| Version code | `30105` |
| Minimum Android API | 24 |

## Build

Node.js, npm, JDK 21, and the Android SDK are required.

```bash
npm ci
npm run build
npx cap sync android --deployment
cd android
./gradlew assembleDebug
```

The debug APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`.

## Release Build and Verification

For F-Droid signature copying, build an unsigned APK first, then sign it with the alignment-preserving script. Remove signing variables from the build environment:

```bash
npm ci
npm run test:security
npm run test:i18n
npm run build
npx cap sync android --deployment
cd android
env -u KEYSTORE_PASSWORD -u KEY_PASSWORD ./gradlew clean assembleRelease lintRelease
cd ..
```

The unsigned output is `android/app/build/outputs/apk/release/app-release-unsigned.apk`. Set `ANDROID_SDK_ROOT`, `KEYSTORE_PASSWORD`, and `KEY_PASSWORD` in a private local environment, then run:

```bash
scripts/sign-release.sh android/app/build/outputs/apk/release/app-release-unsigned.apk release/3.1.5/app-release.apk
```

The default keystore is `paperphone-release.keystore`; `PPL_RELEASE_KEYSTORE` can point to an external private file. Never commit keystores or passwords. The script uses Build Tools 35.0.0, preserves ZIP alignment and verifies v2/v3 signatures. Release builds support `arm64-v8a` only and enable R8 with resource shrinking disabled.

## Current Release and F-Droid

- [Release 3.1.5](https://github.com/619dev/ppl-android/releases/tag/v3.1.5) includes the signed APK and `SHA256SUMS`.
- The immutable source tag is `v3.1.5`; the upstream client baseline is `2823996`.
- Initial inclusion [MR !46295](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/46295) was merged. [Version-update MR !57182](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/57182) adds 3.1.5 (30105) and preserves existing build entries.
- Consult the version-update MR for independent F-Droid build and signature reproduction results.
- F-Droid metadata enables `Binaries` and the allowed signing certificate for reproducibility verification. In-place upgrades across channels depend on matching APK certificates; verify the final signatures first. This app is not published on Google Play.

See [`fdroid/README.md`](fdroid/README.md) for publishing and verification, and [`fdroid/com.fm619.paperphonelite.yml`](fdroid/com.fm619.paperphonelite.yml) for build metadata.

## 3.1.5 Upgrade and Security Boundaries

Upgrade both clients and the server together. The updated server repairs historical one-sided friendships at startup, and Android refreshes contacts after relationship changes. Narrow, short and landscape layouts respect safe areas, and the login page can scroll. Identity verification uses a text-appearance password exchanged offline. A green identity badge requires a valid peer proof and a receipt for the local challenge; a signature alone does not establish ownership of an unknown key. Identity changes revoke trust and require verification again.

New group messages use Sender Key encryption and Ed25519 signatures with member public keys published by the server. They do not require pairwise private verification. Text appearance passwords apply to new private messages; older group messages still support decryption with the original password. Membership changes advance the key epoch; offline clients recover distributions for historical epochs. Rotation cannot revoke old keys or plaintext already held by a departed member, or recover keys previously lost.

Legacy unsigned messages are labelled explicitly. Once a contact identity is pinned, unsigned messages, including old history, are rejected. The registration field `kem_pub` currently holds an Ed25519 signing public key; the signed-message path does not use ML-KEM and does not implement working post-quantum key registration. Message signing does not end-to-end encrypt uploaded attachment bytes. Local caches retain at most 2,000 messages per conversation; server auto-delete and attachment retention still apply.

See [`tests/README.md`](tests/README.md) for regression coverage and manual acceptance steps.

## Upstream Synchronization

The upstream frontend lives in `PaperPhoneLite/client`. Preserve the native Android Tor integration, Capacitor configuration, and compatibility plugins when synchronizing, then rebuild the frontend and run `npx cap sync android`. Do not copy upstream server code into this repository.

## License and Issues

This project is released under [AGPL-3.0](LICENSE), matching [PaperPhoneLite upstream](https://github.com/619dev/PaperPhoneLite).

- Android client issues: [619dev/ppl-android/issues](https://github.com/619dev/ppl-android/issues)
- Upstream protocol, frontend, or server issues: [619dev/PaperPhoneLite/issues](https://github.com/619dev/PaperPhoneLite/issues)
