# Changelog / 更新日志

All notable changes and new features are recorded here. Historical entries below were migrated from the repository documentation.

所有重要版本改动和新特性统一记录于此。下方历史条目由仓库原有文档迁移而来。

## 3.0.1 — 2026-08-18

- Synchronized the Android frontend with the upstream PaperPhoneLite 3.0 codebase.
- Removed OneSignal, FCM, Firebase, Web Push, Google Services configuration, and their Android permissions and dependencies.
- Retained ntfy as the only Android notification path.
- Embedded Guardian Project Tor for all supported Android ABIs and automatically routed WebView traffic through the local Tor SOCKS5 endpoint after bootstrap.
- Disabled clearing or replacing the embedded Tor proxy, preventing clearnet fallback; production server addresses remain restricted to v3 `.onion` services.
- Updated the package, Android native platform, documentation, and Profile-page version display to `3.0.1` (`versionCode 30001`).

- Android 前端同步至上游 PaperPhoneLite 3.0 代码。
- 移除 OneSignal、FCM、Firebase、Web Push、Google Services 配置及对应 Android 权限和依赖。
- Android 通知仅保留 ntfy 通道。
- 为 Android 全部支持的 ABI 内嵌 Guardian Project Tor，完成启动后自动将 WebView 流量路由至本地 Tor SOCKS5。
- 禁止清除或替换内嵌 Tor 代理，避免明网回退；生产服务器地址继续仅允许 v3 `.onion` 服务。
- npm 包、Android 原生平台、说明文档及个人信息页底部版本统一更新为 `3.0.1`（`versionCode 30001`）。

---

## 2.4.7

- Fixed E2EE safety-number mismatches by deriving both views from the same pair of published identity keys; text appearance and its extra password remain independent of the E2EE safety number.
- Fixed one-to-one video calls that could play audio while leaving the remote video black; remote LiveKit tracks now use native track attachment and explicit mobile playback.
- Fixed the call-duration race that could leave an established call at `00:00`.
- Added ordered multi-image sending with a maximum of 20 images per selection and per-image upload progress.
- Added per-account, per-conversation scroll-position memory and a one-tap button to jump to the latest message.
- Updated the application and native platform versions to `2.4.7`.

- 修复 E2EE 安全号码不一致：双方现在基于服务器发布的同一对身份公钥计算；文本外观及其额外密码仍与 E2EE 安全号码相互独立。
- 修复私聊视频通话只有声音、远端画面黑屏的问题；远端 LiveKit 媒体改用原生轨道绑定，并显式兼容移动端播放。
- 修复通话已经接通但计时器停留在 `00:00` 的事件竞态。
- 新增多图片发送：一次最多选择 20 张，保持选择顺序并显示逐张上传进度。
- 新增按账号、按会话保存屏幕滚动位置，以及一键跳到最新消息按钮。
- 应用及原生平台版本统一更新为 `2.4.7`。

---

# Historical entries from README.md

### 最近更新（v2.4.6）

- 文本外观现已明确定位为原有端对端加密之上的额外保险：消息正文先由共享额外密码加密并转换为所选外观，再进入私聊 E2EE（X25519 / ML-KEM-768）或群聊 Sender Key 加密链路。
- 私聊双方或群内所有成员需要自行约定并设置相同的额外密码；密码不会上传服务器或自动同步。
- 密码不一致时，原有 E2EE 和消息送达仍正常，但接收方只能看到文本外观密文，无法查看原文。
- 该功能不会替代、绕过或降级原有 E2EE；个人信息 > 消息隐私页面的 8 种语言说明已同步更新。

### 最近更新（v2.4.4）

- 修复额外加密锁定状态下错误显示“设置密码”的问题；现在显示“输入解锁密码”，并同步全部 8 种语言。

- 修复关闭额外文本外观加密时未验证密码的安全问题；现在即使已解锁，也必须重新输入正确的额外密码才能关闭。
- 文本外观现已隐藏协议元数据，发送中的本地缓存不再保留消息原文。
- 额外聊天记录加密已移至个人信息 > 消息隐私，并全局应用于所有聊天。

- 加密发送改为失败即停止，不再因加密或密钥分发错误回退为明文；消息会显示实际采用的 `PQ v2`、`X25519 ↓` 或 `SK vN`。
- 新增可选聊天记录额外密码、8 种文本外观编码，以及应用离开前台 5/15/30/60 分钟后的自动锁定。
- 未解锁或密码错误时仅显示文本外观密文；身份私钥与 Sender Key 由 Android Keystore 保护，并补齐 8 种语言界面。

#### v2.3.9

- 当服务端确认双方已是好友时，立即刷新好友列表并显示正确提示，兼容修复后的好友请求接口。

#### v2.3.8

- 修复扫码启动摄像头后左上角返回按钮无法退出的问题，退出时会立即停止并释放摄像头。
- 修复向已有好友重复发送添加请求会破坏好友关系的问题；搜索结果现在会明确标记“已是好友”。

- 新增本地聊天记录静态加密：使用 Android Keystore 中的设备专属密钥和 AES-256-GCM 加密，密文保存至独立 IndexedDB。
- 身份私钥及群聊 Sender Keys 迁移至系统安全存储，密钥无法随应用备份迁移到其他设备。
- 聊天明文仅保留在运行内存中，持久化前强制移除解密字段；私聊发送中的消息也不会短暂写入明文。
- 自动迁移并删除旧版 localStorage、sessionStorage 和 IndexedDB 中的明文密钥与聊天缓存，同时清理旧的未加密媒体缓存。
- 加密缓存按账号隔离并进行完整性验证，检测到损坏或篡改时安全丢弃，不回退到明文存储。

#### v2.3.3

- 发送语音消息录音期间保持屏幕唤醒，防止自动熄屏、锁屏后出现录音界面无法操作的问题。
- 语音消息最长限制为 120 秒，到达上限后自动停止；变声后的最终音频同样不会超过 120 秒。
- 离开聊天页面时自动释放录音器、计时器和麦克风资源，提高录音流程稳定性。

- 新增通话防休眠功能：私聊及群组的语音、视频通话期间保持屏幕常亮。
- 覆盖来电、呼出、连接中和已接通状态；通话结束或失败后自动恢复系统锁屏策略。
- 使用 Android 原生窗口唤醒标志实现，无需申请额外的后台唤醒锁权限。
- 全面更新会话保持策略，加入短期访问令牌自动刷新与旧会话平滑升级。
- WebSocket 增加鉴权确认、心跳超时检测、指数退避重连，并在网络切换或应用恢复前台时主动重连。
- 新增消息补偿同步与本地发送队列，离线消息可在恢复连接后续传，并通过客户端消息 ID 去重、对账。
- 保持离线状态下的本地账号和缓存数据；只有服务端明确撤销会话时才自动退出。
- 保留 v2.3.0 的 Google Pixel 全系列 WindowInsets 与安全区域适配。
- 应用版本更新至 2.3.2。

---

---

# Historical entries from README_EN.md

### What's New in v2.4.6

- Text appearance is now clearly documented as extra insurance above the existing end-to-end encryption: the shared extra password encrypts and renders the body first, followed by private-chat E2EE (X25519 / ML-KEM-768) or group Sender Key encryption.
- Both private-chat participants, or every group member, must agree on and configure the same extra password; it is never uploaded or synchronized.
- If passwords differ, E2EE and delivery still work, but recipients see only styled ciphertext and cannot read the original body.
- This feature never replaces, bypasses, or downgrades the original E2EE; the Profile > Message privacy explanation is updated in all eight UI languages.

### What's New in v2.4.4

- Fixed the locked extra-encryption dialog so it requests the unlock password instead of asking users to set one, across all eight languages.

- Fixed a security issue that allowed extra text-appearance encryption to be disabled without password verification; the correct extra password must now be re-entered even while unlocked.
- Text appearance now hides protocol metadata and optimistic caches no longer retain original message bodies.
- Extra message-history encryption moved to Profile > Message privacy and applies globally to all chats.

- Encrypted sends now fail closed instead of falling back to plaintext, and each message reports its actual `PQ v2`, `X25519 ↓`, or `SK vN` protocol.
- Added an optional chat-history password, eight presentation codecs, and automatic locking 5/15/30/60 minutes after leaving the foreground.
- Locked or incorrectly unlocked histories show presentation ciphertext only; identity private keys and Sender Keys are protected by Android Keystore, with complete UI copy in all eight languages.

#### v2.3.9

- Refreshes the friends list and shows the correct message when the server confirms the users are already friends, matching the repaired friend-request API.

#### v2.3.8

- Fixed the unresponsive back button after the QR scanner starts the camera; closing now stops and releases the camera immediately.
- Fixed duplicate friend requests to existing friends corrupting the friendship; search results now clearly show “Already friends.”

- Added encryption at rest for local chat history using a device-bound Android Keystore key and AES-256-GCM, with ciphertext stored in a dedicated IndexedDB database.
- Moved identity private keys and group Sender Keys into secure system-backed storage that cannot be restored onto another device with an app backup.
- Chat plaintext now remains in memory only; decrypted fields are stripped before persistence, including optimistic outgoing private messages.
- Added one-time migration and removal of legacy plaintext keys and chat caches from localStorage, sessionStorage, and IndexedDB, plus cleanup of the former unencrypted media cache.
- Encrypted caches are isolated per account and authenticated against tampering; invalid data is discarded without falling back to plaintext storage.

#### v2.3.3

- Keeps the screen awake while recording voice messages, preventing the recording UI from becoming unresponsive after automatic locking.
- Limits voice messages to 120 seconds and stops automatically at the limit; processed voice effects are also capped at 120 seconds.
- Releases the recorder, timers, and microphone when leaving a chat to improve recording stability.

- Added call sleep prevention: the screen stays awake during direct and group voice/video calls.
- Covers incoming, outgoing, connecting, and connected states, then restores the system screen-timeout policy when the call ends or fails.
- Uses Android's native window keep-screen-on flag without requesting an additional background wake-lock permission.
- Overhauled session persistence with automatic short-lived access-token refresh and seamless upgrades for legacy sessions.
- Added authenticated WebSocket readiness, heartbeat timeout detection, exponential-backoff reconnects, and recovery after network changes or app resume.
- Added catch-up message synchronization and a durable local outbox so queued messages resume after reconnect and reconcile by client message ID.
- Preserved local accounts and cached data while offline; automatic sign-out now occurs only after explicit server-side session revocation.
- Retained the v2.3.0 Google Pixel lineup WindowInsets and safe-area adaptations.
- Updated the app version to 2.3.2.

---
