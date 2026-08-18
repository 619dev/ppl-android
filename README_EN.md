# PaperPhonePlus — Android Client

🌐 **其他语言 / Other Languages:** [中文](README.md)

> This repository is the **Android native client** (Capacitor-wrapped) of [PaperPhonePlus](https://github.com/619dev/Paperphone-plus), built from the upstream frontend codebase for Google Play and sideload distribution.

[![Upstream](https://img.shields.io/badge/Upstream-619dev%2FPaperphone--plus-blue?logo=github)](https://github.com/619dev/Paperphone-plus)
[![React](https://img.shields.io/badge/React-19-blue)](#)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.7-blue)](#)
[![Capacitor](https://img.shields.io/badge/Capacitor-8-green)](#)
[![Version](https://img.shields.io/badge/Version-3.0.1-orange)](package.json)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](LICENSE)

[![Google Play](https://img.shields.io/badge/Google%20Play-Download-green?logo=google-play)](https://play.google.com/store/apps/details?id=com.fm619.paperphoneplus)

---

## 📖 Overview

PaperPhonePlus is a WeChat-style end-to-end encrypted instant messaging application. This repository contains the Android native client, which uses [Capacitor](https://capacitorjs.com/) to package the React + TypeScript frontend into an Android APK/AAB.

### Key Features

| Feature | Description |
|---------|-------------|
| 🔐 End-to-End Encryption | Stateless ECDH + XSalsa20-Poly1305, per-message ephemeral keys, forward secrecy |
| 🗝️ Secure Local Keys | Identity and group Sender Keys are protected by Android Keystore; chat caches use a device-bound AES-256-GCM key |
| 📹 Video/Voice Calls | LiveKit SFU for both direct and group calls (up to 100 participants), with discussion and lecture modes |
| 🎙️ Real-time Voice Changer | Voice messages & calls support 3 modes (0.8x / 1.0x / 1.2x) |
| 📱 Session Persistence | Keeps users signed in through network loss, ordinary authorization failures, and server URL changes; signs out only on an explicit server revocation |
| 📴 Offline Access | Account-isolated caching for contacts, groups, chats, Moments, Timeline, and media keeps previously loaded content available offline |
| 👥 Group Chat | Up to 2,000 members, encrypted & unencrypted modes |
| 💬 Rich Messaging | Text, images, video, files, voice messages, message replies, emoji panel, Telegram sticker packs, read receipts |
| 🌐 Moments | Post updates (text + images/video), likes, comments, tag-based visibility |
| 📰 Timeline | Xiaohongshu-style public feed with waterfall layout, anonymous posting |
| 🔔 Notifications | Android uses ntfy; FCM, Firebase, and OneSignal are not included |
| 🌐 Multi-language | Chinese, English, Japanese, Korean, French, German, Russian, Spanish |
| 🔑 Two-Factor Auth | Google Authenticator-compatible TOTP with 8 recovery codes |
| 📷 QR Code Scanning | Scan to add friends or join groups |

### Changelog

The complete release history has moved to [changelog.md](changelog.md).

### What's New in v3.0.1

- Synchronized the frontend with the PaperPhoneLite 3.0 series and aligned the app, Android platform, and Profile-page version labels.
- Removed OneSignal, FCM, Firebase, Web Push, and Google Services; Android notifications now use ntfy only.
- Bundled the Guardian Project Tor client, routing WebView traffic through its local SOCKS5 endpoint after bootstrap with no clearnet fallback.
- Production builds accept only v3 `.onion` server addresses.

---

## 🔐 Extra-encrypted text appearance: design and security boundary

This feature is **extra insurance on top of the existing end-to-end encryption (E2EE)**. It does not replace E2EE with a visual encoding, and it never bypasses or weakens the original encryption. Private chats remain protected by the existing X25519 / ML-KEM-768 key agreement and message-encryption path; group chats continue to use the Sender Key protocol. Identity private keys and group Sender Keys remain protected by Android Keystore.

When enabled, every message is processed in this order:

1. The sender first protects the message body with the extra password shared by both participants or by all group members. PBKDF2-SHA-256 (210,000 iterations and a random salt) derives an AES-256-GCM key; every message has an independent random IV and an authentication tag for integrity.
2. The complete extra-encryption frame (version, salt, IV, and ciphertext) is then encoded with one of eight selectable text appearances. This is not merely decorative character substitution: the visible characters carry the extra encrypted ciphertext.
3. That appearance ciphertext then enters the project's original encryption path: private-chat E2EE or group Sender Key encryption. The server still receives the original E2EE/Sender-Key ciphertext plus metadata required for delivery.
4. The recipient reverses the order: first decrypt the original E2EE/Sender-Key layer, then decode the text-appearance frame and decrypt the body with the extra password.

The extra password is never uploaded, synchronized automatically, or distributed by the server. Both people in a private chat must set the same password; every group member who needs to read the plaintext must also set that same password. Text appearances do not need to match: every message carries its own appearance identifier, so the recipient automatically detects and decodes the sender's choice. For example, one person may send Buddhist text while another sends Hangul; if the extra password matches, both decrypt normally. A user's appearance setting controls only the ciphertext appearance of messages they send. If the password is missing, locked, or different, messages are still sent and received normally and the original E2EE layer still decrypts successfully, but the app can display only the appearance ciphertext—not the original text.

The app does not persist the extra password. While unlocked it exists only in the current process memory; locally, the app stores only a random salt and AES-GCM verification data used to check whether an entered password is correct. Users can lock immediately or automatically 5, 15, 30, or 60 minutes after the app leaves the foreground. This layer adds an independent shared secret beyond E2EE; it does not replace a strong password, device lock, or system secure storage, and it cannot provide absolute protection on a fully compromised device while the password remains in memory.

## 🏗️ Architecture

```
ppp-android/
├── android/                  # Capacitor Android native project
├── src/                      # React + TypeScript frontend source
│   ├── App.tsx               # Router + auth guard
│   ├── index.css             # Design system (dark/light, glassmorphism)
│   ├── main.tsx              # React entry point
│   ├── api/                  # HTTP client + WebSocket client
│   ├── components/           # UI components (TabBar, call overlay, QR code, etc.)
│   ├── contexts/             # React contexts
│   ├── crypto/               # Encryption modules
│   │   ├── ratchet.ts        # ECDH + XSalsa20-Poly1305 encryption
│   │   ├── keystore.ts       # Android Keystore-backed private key persistence
│   │   └── groupCrypto.ts    # Group encryption (Sender Key protocol)
│   ├── hooks/                # Custom hooks
│   │   └── useGroupCall.ts   # LiveKit group meetings and host controls
│   ├── i18n/                 # Internationalization
│   ├── pages/                # Page components
│   ├── store/                # Zustand state management
│   └── utils/                # Utility functions
│       ├── messagePayload.ts # Compatible message body and reply metadata encoding
│       ├── offlineCache.ts   # Account-isolated offline data and media cache
│       ├── stickerCache.ts   # Persistent Telegram sticker cache
│       └── session.ts        # Session termination detection and safe sign-out
├── capacitor.config.ts       # Capacitor configuration
├── vite.config.ts            # Vite build configuration
├── package.json              # Dependency management
└── android/                  # Capacitor Android and embedded Tor client
```

### Tech Stack

- **Frontend Framework**: React 19 + TypeScript 5.7
- **Build Tool**: Vite 6
- **State Management**: Zustand 5
- **Native Bridge**: Capacitor 8 (Android)
- **Crypto Library**: libsodium-wrappers-sumo (WebAssembly, Curve25519 / XSalsa20-Poly1305)
- **Post-Quantum Crypto**: crystals-kyber-js (CRYSTALS-Kyber key encapsulation)
- **Video Calls**: LiveKit SFU (direct and group calls)
- **Network privacy**: bundled Guardian Project Tor; onion services only
- **Android notifications**: ntfy (no Google push framework)
- **UI Icons**: Lucide React
- **Animations**: Lottie Web
- **QR Code**: qrcode + jsqr

---

## 🚀 Getting Started

### Prerequisites

- [Node.js](https://nodejs.org/) ≥ 18
- [Android Studio](https://developer.android.com/studio) + Android SDK
- JDK 17+

### Installation & Build

```bash
# 1. Clone the repository
git clone <repo-url> && cd ppp-android

# 2. Install dependencies
npm install

# 3. Build the frontend
npm run build

# 4. Sync to Android project
npx cap sync android

# 5. Open in Android Studio
npx cap open android
```

### Development Mode

```bash
# Start the frontend dev server
npm run dev

# In another terminal, run Android with live reload
npx cap run android --livereload --external
```

> **Note**: In development mode, you need to configure the backend server address. You can manually enter the backend URL on the login page, e.g., `https://your-server.com`.

---

## 🔧 Configuration

### Capacitor Configuration

Core settings are in [capacitor.config.ts](capacitor.config.ts):

| Setting | Value | Description |
|---------|-------|-------------|
| `appId` | `com.fm619.paperphoneplus` | Android application package name |
| `appName` | `PaperPhonePlus` | App display name |
| `webDir` | `dist` | Frontend build output directory |
| `androidScheme` | `https` | Required for WebRTC and crypto APIs |

### Android Network and Notifications

The Android APK bundles Tor and routes WebView traffic through its local SOCKS5 port after bootstrap. Notifications use ntfy; Firebase and `google-services.json` are not required.

---

## 📦 Building for Release

### Build Release APK/AAB

```bash
# Build the frontend
npm run build

# Sync to Android project
npx cap sync android

# In Android Studio, build a signed bundle
# Build → Generate Signed Bundle / APK
```

### Signing Configuration

The project includes a release signing keystore `paperphone-release.keystore` for Google Play signed distribution.

---

## 🔗 Related Projects

| Project | Description |
|---------|-------------|
| [Paperphone-plus](https://github.com/619dev/Paperphone-plus) | Upstream main repository (frontend + backend) |
| [ppp-win](https://github.com/619dev/ppp-win) | Windows desktop client |
| [ppp-mac](https://github.com/619dev/ppp-mac) | Mac desktop client |

---

## 🔒 Security Model

```
On Registration:
  Device locally generates IK (Identity Key) + SPK (Signed Pre-Key) + 20x OPK (One-time Pre-Keys)
  Public keys uploaded to server; private keys persist in 4 layers, never leaving the device

On Sending Messages:
  Sender downloads recipient's IK public key
  Generates ephemeral ECDH key pair (unique per message)
  X25519 ECDH → shared secret → XSalsa20-Poly1305 encryption
  Ephemeral public key attached to message header; recipient destroys it after decryption

What the Server Sees:
  ✅ Ciphertext blob + routing metadata (sender/recipient UUID)
  ❌ Plaintext / private keys / ephemeral keys / call content
```

---

## 📄 License

This project is licensed under the [GNU Affero General Public License v3.0 (AGPL-3.0)](LICENSE), consistent with the upstream repository [619dev/Paperphone-plus](https://github.com/619dev/Paperphone-plus).

In summary:
- ✅ Free to deploy and use by individuals and enterprises
- ✅ Modification is permitted
- ⚠️ If you provide a modified version as a network service, you must release the modified source code
- ⚠️ Derivative works must use the same license (AGPL-3.0)
- ⚠️ Original copyright notices and license information must be preserved

See the full license text in the [LICENSE](LICENSE) file.

---

## 🤝 Contributing

Contributions via Issues and Pull Requests are welcome!

1. Fork this repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m 'Add your feature'`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

---

## 📬 Contact

- Telegram Group: https://t.me/+vHJtvWJY_gEyMTUx
- Upstream Issues: https://github.com/619dev/Paperphone-plus/issues
