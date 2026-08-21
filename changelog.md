# Changelog / 更新历史

本文件只记录 `ppl-android` 基于 [619dev/PaperPhoneLite](https://github.com/619dev/PaperPhoneLite) 的 Android 发行历史。旧文档中从 PaperPhonePlus Android 复制而来的 2.x 历史已移除，因为其中的 LiveKit 通话、朋友圈、时间线和 Android Keystore 缓存等内容不属于当前 PaperPhoneLite 3.x 客户端。

This file records Android releases of `ppl-android` based on [619dev/PaperPhoneLite](https://github.com/619dev/PaperPhoneLite). The copied PaperPhonePlus Android 2.x history was removed because its LiveKit calling, Moments, Timeline, and Android-Keystore-cache descriptions do not apply to the current PaperPhoneLite 3.x client.

## 3.0.15 — 2026-08-21

- 修复私聊或群聊 Sender Key 解密后，收到的附件仍停留在文本外观密文状态的问题，恢复跨设备文件名显示和下载访问。
- 已登录账号启用文本外观加密时，Android App 启动后提示输入额外密码；取消或密码错误时继续只显示文本外观密文。
- 为全部 8 种界面语言补充启动解锁和密码错误提示。
- npm、Android 原生版本、F-Droid 元数据及个人信息页底部版本统一更新为 `3.0.15`（`versionCode 30015`）。

- Fixed received attachments remaining as styled ciphertext after private-chat or group Sender Key decryption, restoring cross-device filenames and download access.
- Added a startup password prompt for signed-in accounts with text-appearance encryption enabled; cancelling or entering an incorrect password leaves only styled ciphertext visible.
- Added startup-unlock and incorrect-password text in all eight UI languages.
- Updated npm, Android native, F-Droid metadata, and Profile-footer versions to `3.0.15` (`versionCode 30015`).

---

## 3.0.13 — 2026-08-21

- 修复 Android 聊天中的文件消息点击下载按钮没有反应：鉴权下载完成后，附件会分块写入原生临时文件并打开系统分享/保存面板，不再依赖 WebView 对 `blob:` 下载的支持。
- 防止附件保存、文件上传和消息发送被快速重复触发，并在附件保存期间显示处理中状态。
- npm、Android 原生版本、F-Droid 元数据及个人信息页底部版本统一更新为 `3.0.13`（`versionCode 30013`）。

- Fixed file-message download buttons appearing unresponsive in Android chats. Authenticated attachments are now streamed into a native temporary file before opening the system share/save sheet, without relying on WebView `blob:` downloads.
- Prevented rapid duplicate attachment saves, file uploads, and message sends, and added an in-progress state while an attachment is being saved.
- Updated npm, Android native, F-Droid metadata, and Profile-footer versions to `3.0.13` (`versionCode 30013`).

---

## 3.0.12 — 2026-08-21

- 附件改为通过当前配置的 PaperPhone 服务在应用内鉴权下载，避免 Android 将 onion 文件链接交给系统浏览器。
- 下载后优先打开 Android 原生分享/保存面板，并保留浏览器下载回退。
- npm、Android 原生版本及个人信息页底部版本统一更新为 `3.0.12`（`versionCode 30012`）。

- Attachments are now downloaded through the configured PaperPhone server with in-app authentication, preventing Android from handing onion file links to the system browser.
- Downloads prefer the Android share/save sheet and retain a browser-download fallback.
- Updated npm, Android native, and Profile-footer versions to `3.0.12` (`versionCode 30012`).

---

## 3.0.11 — 2026-08-20

- 为 F-Droid 上架补充中英文 Fastlane 手机截图和预填的 RFP 材料。
- 将 F-Droid 官网字段统一为 `https://paperphone.app`。
- npm、Android 原生版本及个人信息页底部版本统一更新为 `3.0.11`（`versionCode 30011`）。

- Added English and Chinese Fastlane phone screenshots and a pre-filled RFP draft for the F-Droid submission.
- Standardized the F-Droid website field on `https://paperphone.app`.
- Updated npm, Android native, and Profile-footer versions to `3.0.11` (`versionCode 30011`).

---

## 3.0.10 — 2026-08-20

- 移除依赖 Google ML Kit/Play Services 的原生扫码插件，改用 WebView 摄像头与内置开源 `jsQR` 解码，满足 F-Droid 纯自由软件要求。
- 修正 F-Droid Node 22 构建环境、初始化目录和自动更新元数据，并为 Guardian Project 开源 Maven 仓库添加最小范围扫描例外。
- npm、Android 原生版本及个人信息页底部版本统一更新为 `3.0.10`（`versionCode 30010`）。

- Removed the native scanner plugin that depends on Google ML Kit/Play Services and replaced it with WebView camera capture plus the bundled FOSS `jsQR` decoder for F-Droid compliance.
- Corrected the F-Droid Node 22 environment, initialization directory, and auto-update metadata, with a narrowly scoped scanner exception for Guardian Project's FOSS Maven repository.
- Updated npm, Android native, and Profile-footer versions to `3.0.10` (`versionCode 30010`).

---

## 3.0.9 — 2026-08-20

- 为 F-Droid 源码构建移除发布签名密码回退值和本机 JDK 绝对路径；无密钥环境现在会生成未签名 APK，交由 F-Droid 签名。
- 增加 F-Droid 构建元数据、中英文商店说明和发布检查清单。
- 移除 Google Fonts 运行时请求，改用现有系统字体回退。
- npm、Android 原生版本及个人信息页底部版本统一更新为 `3.0.9`（`versionCode 30009`）。

- Prepared source builds for F-Droid by removing signing-password fallbacks and the machine-specific JDK path; environments without a key now produce an unsigned APK for F-Droid to sign.
- Added F-Droid build metadata, English and Chinese store listings, and a release checklist.
- Removed the runtime Google Fonts request in favor of the existing system-font fallback.
- Updated npm, Android native, and Profile-footer versions to `3.0.9` (`versionCode 30009`).

---

## 3.0.8 — 2026-08-18

- 修复已授予 PaperPhoneLite 摄像头权限后，扫码仍要求为 Google Play 服务单独授予摄像头权限的问题。
- 移除 Google Play 服务原生扫码界面及 `barcode_ui` 模块声明，统一使用应用自身权限控制的 CameraX 扫描。
- 补充 HTML 根层透明状态，避免 Capacitor WebView 背景遮挡位于其后的原生摄像头预览。
- npm、Android 原生版本及个人信息页底部版本统一更新为 `3.0.8`（`versionCode 30008`）。

- Fixed scanning still requesting separate Google Play services camera access after PaperPhoneLite itself had already been granted camera permission.
- Removed the Google Play services scanner UI and `barcode_ui` module declaration; scanning now consistently uses app-permission-controlled CameraX.
- Made the HTML root transparent during scanning so the Capacitor WebView cannot cover the native camera preview behind it.
- Updated npm, Android native, and Profile-footer versions to `3.0.8` (`versionCode 30008`).

---

## 3.0.7 — 2026-08-18

- 修复“扫一扫”启动后只显示应用界面和扫描框、没有可见摄像头画面的问题。
- Android 优先使用 ML Kit 完整原生扫码界面直接调用摄像头；缺少 Google 扫码模块时回退到 CameraX 内嵌扫描。
- 回退模式会隐藏其余 WebView 界面并保持扫描页透明，同时完善取消、识别结果及扫描资源清理流程。
- npm、Android 原生版本及个人信息页底部版本统一更新为 `3.0.7`（`versionCode 30007`）。

- Fixed QR scanning opening over the app UI without a visible camera preview.
- Android now prefers ML Kit's full native scanner UI to invoke the camera directly, with embedded CameraX scanning as a fallback when the Google scanner module is unavailable.
- The fallback hides the remaining WebView UI and keeps the scanner page transparent, with improved cancellation, result handling, and scanner cleanup.
- Updated npm, Android native, and Profile-footer versions to `3.0.7` (`versionCode 30007`).

---

## 3.0.6 — 2026-08-18

- 修复 Android 打开“扫一扫”时可能因 WebView 相机接口崩溃的问题，改用与 Capacitor 8 兼容的原生 ML Kit 二维码扫描。
- 增加相机权限、设备支持检查、扫码关闭及监听器清理；浏览器环境保留 Web 扫码回退，Android 最低 API 仍为 24。
- 更新隐私政策和使用条款，明确 iOS 不使用 APNs、当前不提供系统后台远程通知；Android 可选择通过 ntfy 接收后台通知。
- 明确 Android 与 iOS 均不集成 APNs、FCM、Firebase、OneSignal 或 Web Push，并修正通知数据处理与保留范围的表述。
- npm、Android 原生版本及个人信息页底部版本统一更新为 `3.0.6`（`versionCode 30006`）。

- Fixed a crash that could occur when opening QR scanning on Android by replacing WebView camera capture with native ML Kit QR scanning compatible with Capacitor 8.
- Added camera-permission and device-support checks plus safe scanner/listener cleanup; browsers retain the web fallback, and Android minimum API remains 24.
- Updated the Privacy Policy and Terms of Use to state that iOS does not use APNs and currently has no system background remote notifications; Android can optionally use ntfy for background notifications.
- Clarified that neither Android nor iOS integrates APNs, FCM, Firebase, OneSignal, or Web Push, and corrected notification-data processing and retention wording.
- Updated npm, Android native, and Profile-footer versions to `3.0.6` (`versionCode 30006`).

---

## 3.0.5 — 2026-08-18

- 修复内嵌 Tor 已就绪后，登录和注册访问 `http://` v3 onion 服务仍显示 `Failed to fetch` 的问题。
- 允许运行于安全 Capacitor 源的 Android WebView 承载 HTTP onion 请求，并允许该请求进入内嵌 Tor SOCKS 代理；onion 服务的认证与链路加密仍由 Tor 提供。
- 生产环境继续只接受规范的 v3 `.onion` 服务地址，拒绝明网地址和清除或替换内嵌 Tor 代理。
- npm、Android 原生版本及个人信息页底部版本统一更新为 `3.0.5`（`versionCode 30005`）。

- Fixed `Failed to fetch` during login and registration against `http://` v3 onion services after bundled Tor had become ready.
- Allowed the Android WebView running from the secure Capacitor origin to carry HTTP onion requests into the bundled Tor SOCKS proxy; Tor continues to provide onion-service authentication and link encryption.
- Production builds remain restricted to valid v3 `.onion` server addresses and still reject clearnet servers or attempts to clear or replace the bundled Tor proxy.
- Updated npm, Android native, and Profile-footer versions to `3.0.5` (`versionCode 30005`).

---

## 3.0.4 — 2026-08-18

- Tor 直连 20 秒内无法建立线路时，自动从 Tor Project 官方 circumvention settings 接口获取当前推荐的 WebTunnel bridge，并通过内嵌 IPtProxy/Lyrebird 重试启动。
- 缓存上次成功获取的 WebTunnel bridge，补充 45 秒启动超时、失败重试和登录页实时状态提示；登录与注册仍只在 Tor 和 WebView 代理就绪后开放。
- 使用上游 PaperPhoneLite 官方青绿色电话图标，重新生成 Android 启动器、自适应图标和启动画面资源，并将整体界面强调色调整为与图标一致的青绿色。
- 移除登录页“密钥只保存在本地、端到端加密、前向加密、抗量子加密”四个提示卡片。
- npm、Android 原生版本及个人信息页底部版本统一更新为 `3.0.4`（`versionCode 30004`）。

- When direct Tor cannot establish a circuit within 20 seconds, the app now fetches a currently recommended WebTunnel bridge from Tor Project's official circumvention-settings endpoint and retries through bundled IPtProxy/Lyrebird.
- The last successfully fetched WebTunnel bridge is cached, with a 45-second startup timeout, retry handling, and live login-page status messages; login and registration remain gated until Tor and the WebView proxy are ready.
- Adopted the official teal PaperPhoneLite telephone icon from upstream, regenerated Android launcher, adaptive-icon, and splash resources, and aligned the interface accent palette with the icon.
- Removed the four login-page cards for local-only keys, end-to-end encryption, forward secrecy, and post-quantum encryption.
- Updated npm, Android native, and Profile-footer versions to `3.0.4` (`versionCode 30004`).

---

## 3.0.3 — 2026-08-18

- Android 登录页新增内嵌 Tor 启动按钮和实时状态显示。
- 登录和注册现在必须等待 Tor 建立线路且 WebView 代理成功切换到内嵌 Tor 后才能提交。
- Android 登录页移除容易误导的 `127.0.0.1:7890/10808` 手动代理选择；系统 VPN 可直接承载 Tor 的外连流量。
- Tor 不再随原生插件加载自动启动，并修复 Activity/WebView 重建后恢复 Tor 状态的流程。
- 为全部 8 种界面语言补充 Tor 启动、就绪和失败提示。
- npm、Android 原生版本及个人信息页底部版本统一更新为 `3.0.3`（`versionCode 30003`）。

- Added an explicit bundled-Tor start button and live status display to the Android login page.
- Login and registration now remain disabled until Tor establishes a circuit and the WebView proxy is successfully routed through bundled Tor.
- Removed the misleading `127.0.0.1:7890/10808` manual proxy selector from Android login; a system VPN may carry Tor's outbound traffic transparently.
- Tor no longer starts automatically when the native plugin loads, and Tor state is restored after Activity/WebView recreation.
- Added Tor starting, ready, and failure messages in all eight UI languages.
- Updated npm, Android native, and Profile-footer versions to `3.0.3` (`versionCode 30003`).

---

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
