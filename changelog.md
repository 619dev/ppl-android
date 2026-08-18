# Changelog / 更新历史

本文件只记录 `ppl-android` 基于 [619dev/PaperPhoneLite](https://github.com/619dev/PaperPhoneLite) 的 Android 发行历史。旧文档中从 PaperPhonePlus Android 复制而来的 2.x 历史已移除，因为其中的 LiveKit 通话、朋友圈、时间线和 Android Keystore 缓存等内容不属于当前 PaperPhoneLite 3.x 客户端。

This file records Android releases of `ppl-android` based on [619dev/PaperPhoneLite](https://github.com/619dev/PaperPhoneLite). The copied PaperPhonePlus Android 2.x history was removed because its LiveKit calling, Moments, Timeline, and Android-Keystore-cache descriptions do not apply to the current PaperPhoneLite 3.x client.

## 3.0.2 — 2026-08-18

- Application ID、Gradle namespace、Capacitor appId、Java 包名、资源包名和自定义 scheme 统一更正为 `com.fm619.paperphonelite`。
- Android 应用显示名称统一为 `PaperPhoneLite`。
- 保留 ntfy 作为 Android 必需通知方案，继续支持主题获取、服务端注册及 ntfy App 订阅流程。
- 修正文档中的上游、功能范围、Tor、通知、本地存储、发行渠道及构建说明。
- npm、Android 原生版本及个人信息页底部版本统一更新为 `3.0.2`（`versionCode 30002`）。

- Corrected the Application ID, Gradle namespace, Capacitor appId, Java package, resource package, and custom scheme to `com.fm619.paperphonelite`.
- Standardized the Android display name as `PaperPhoneLite`.
- Retained ntfy as the required Android notification path, including topic retrieval, server registration, and ntfy-app subscription.
- Corrected documentation covering upstream ownership, feature scope, Tor, notifications, local storage, distribution, and builds.
- Updated npm, Android native, and Profile-footer versions to `3.0.2` (`versionCode 30002`).

---

## 3.0.1 — 2026-08-18

### Android 客户端

- 前端同步至上游 PaperPhoneLite 3.0 系列，应用内版本、npm 版本及 Android `versionName` 统一为 `3.0.1`，`versionCode` 为 `30001`。
- 内嵌 Guardian Project `tor-android 0.4.8.17.2`；APK 包含 `arm64-v8a`、`armeabi-v7a`、`x86` 与 `x86_64` 原生 Tor 库。
- Tor 建立线路后，将 Android WebView 代理设置为本机 `127.0.0.1:9050`。
- 生产地址限制为 v3 `.onion`；原生代理桥拒绝清除或替换内嵌 Tor 代理。
- 移除 Capacitor Push、FCM、Firebase、OneSignal、Web Push、Google Services 配置、依赖和 Android 通知权限。
- ntfy 作为 Android 必需通知方案保留：客户端取得主题、向服务端注册，并引导用户在 ntfy App 中订阅。
- Android 应用显示名称更正为 `PaperPhoneLite`，Application ID 更正为 `com.fm619.paperphonelite`。

### 文档更正

- 上游仓库统一更正为 `https://github.com/619dev/PaperPhoneLite`。
- 删除 Google Play 发布链接与说明；Android 正式版仅通过 `ppl-android` GitHub Releases 发布。
- 删除已不存在的 LiveKit、私聊/群聊通话、朋友圈、公开时间线和 Cloudflare R2 功能说明。
- 更正本地存储说明：当前同步后的 3.0 前端使用 IndexedDB/localStorage；没有启用旧版 Android Keystore 整体聊天缓存方案。
- 明确 ntfy 是 Android 保留的通知方案，并记录完整注册/订阅流程。
- 更新目录结构、构建、签名、Tor 边界和问题反馈链接。

### Android client

- Synchronized the frontend with the upstream PaperPhoneLite 3.0 series. The in-app, npm, and Android `versionName` values are `3.0.1`; `versionCode` is `30001`.
- Bundled Guardian Project `tor-android 0.4.8.17.2`, including native Tor libraries for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`.
- Configures the Android WebView to use local SOCKS5 `127.0.0.1:9050` after Tor establishes a circuit.
- Restricts production server addresses to v3 `.onion` and rejects attempts to clear or replace the bundled Tor proxy.
- Removed Capacitor Push, FCM, Firebase, OneSignal, Web Push, Google Services configuration, dependencies, and Android notification permissions.
- Retained ntfy as the required Android notification path: the client obtains and registers a topic, then guides the user to subscribe in the ntfy app.
- Corrected the Android display name to `PaperPhoneLite` and the Application ID to `com.fm619.paperphonelite`.

### Documentation corrections

- Corrected every upstream reference to `https://github.com/619dev/PaperPhoneLite`.
- Removed Google Play links and distribution claims; official Android builds are distributed only through `ppl-android` GitHub Releases.
- Removed claims for deleted LiveKit, direct/group calls, Moments, public Timeline, and Cloudflare R2 features.
- Corrected local-storage documentation: the synchronized 3.0 frontend uses IndexedDB/localStorage and does not enable the former Android-Keystore whole-chat-cache design.
- Clarified that ntfy remains the Android notification solution and documented its registration/subscription flow.
- Updated the repository layout, build/signing instructions, Tor boundary, and issue links.
