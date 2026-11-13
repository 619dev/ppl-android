# PaperPhoneLite Android

[English](README_EN.md) · [更新历史](changelog.md)

> [PaperPhoneLite](https://github.com/619dev/PaperPhoneLite) 的 Android 客户端，使用 Capacitor 8 打包上游 React/TypeScript 前端，并内嵌 Tor。

[![Upstream](https://img.shields.io/badge/上游-619dev%2FPaperPhoneLite-blue?logo=github)](https://github.com/619dev/PaperPhoneLite)
[![Version](https://img.shields.io/badge/版本-3.1.5-orange)](package.json)
[![License](https://img.shields.io/badge/License-AGPL--3.0-blue.svg)](LICENSE)

## 项目范围

本仓库只包含 Android 客户端，不包含 Rust 服务端、MySQL、Redis 或 onion service 部署配置。服务端和自托管文档位于[上游仓库](https://github.com/619dev/PaperPhoneLite)。

当前客户端提供：

- 私聊、群聊、联系人、群组及好友申请。
- 文字、图片、视频、文档、语音、Emoji 和 Telegram 贴纸消息。
- X25519 密钥协商、XSalsa20-Poly1305 消息加密、Ed25519 消息及群密钥签名、安全号码与身份验证标识。
- 消息同步、本地离线缓存、持久化发件箱、已读状态和输入状态。
- 消息自动删除、拉黑、好友标签、二维码和 TOTP 两步验证。
- 额外消息密码、8 种文本外观、前后台自动锁定及启动解锁提示。
- Android 可选的 ntfy 通知注册与订阅流程。

PaperPhoneLite 3.x **不提供**朋友圈、公开时间线、单聊或群聊语音/视频通话、LiveKit、Cloudflare R2 或 Web/PWA 生产发行。

## Tor 与网络边界

- Release APK 内嵌 Guardian Project `tor-android 0.4.8.17.2`，仅面向 64 位 ARM 设备（`arm64-v8a`）。为避免反射/JNI 依赖在运行时闪退，Release 启用 R8 代码压缩，并保留反射和 JNI 入口；资源裁剪保持关闭。Debug 构建仍可用于本地开发测试。
- 原生 Tor 服务监听本机 SOCKS5 `127.0.0.1:9050`；只有 Tor 引导进度达到 100% 后，Android WebView 代理才切换到该端口，避免线路尚未可用时误报成功并发送登录请求。
- 直接 Tor 连接 20 秒内无法建立线路时，客户端会从 Tor Project 官方 circumvention settings 接口获取当前 WebTunnel bridge，通过内嵌 IPtProxy/Lyrebird 重启 Tor；bridge 会规范化为 `utls=none`，上次成功获取的 bridge 可在接口暂时不可用时回退使用。
- WebTunnel 重启前会清理可安全重建的 Tor 目录共识缓存，避免过期共识使引导长期停滞；不会清除账号、消息或密钥数据。
- 生产构建只接受规范的 v3 `.onion` 服务地址；Vite 开发模式额外允许回环地址。
- 原生代理桥拒绝清除 Tor 代理或替换为其他代理。浏览器开发模式不能由 JavaScript 强制代理，不属于受支持的生产发行方式。
- Tor 可隐藏网络来源，但不能保证绝对匿名，也不能消除设备、账号行为、通知服务或主动披露造成的关联。

## 通知与第三方服务

ntfy 是 Android 唯一支持的可选后台通知方案。启用时，应用从服务端取得专属主题、向服务端注册订阅并提供复制/下载入口；用户需在 ntfy App 中订阅该主题。客户端不包含 FCM、Firebase、OneSignal、Web Push 或 Google Services。

## 本地数据与密钥

- 身份密钥、Sender Key 和身份信任记录使用按账号隔离的浏览器秘密存储，以不可导出的 WebCrypto AES-GCM 密钥加密后存入 IndexedDB；这不等同于 Android Keystore。会话、设置、消息缓存与离线数据使用 localStorage。
- 消息缓存持久化前会移除已解密的明文字段，但当前 3.1.5 前端没有把整个聊天缓存交给 Android Keystore 加密。
- Android 系统备份已关闭，并明确排除云备份及设备迁移中的应用数据（包括 WebView 身份密钥和消息缓存）。
- 启用“文本外观加密”后，应用启动会要求输入额外密码；错误或取消解锁时只显示文本外观密文。该功能不会替代设备锁或 Android Keystore。
- 仓库保留原生 `SecureStoragePlugin` 和 `KeepAwakePlugin` 兼容代码，但同步后的 3.1.5 前端不调用它们，不能将其描述为当前密钥或缓存的系统级保护。
- 清理应用数据或卸载应用可能永久删除本地密钥及无法恢复的历史消息。

## Android 标识

| 项目 | 值 |
|---|---|
| 应用名称 | `PaperPhoneLite` |
| Application ID | `com.fm619.paperphonelite` |
| 版本 | `3.1.5` |
| Version Code | `30105` |
| 最低 Android API | 24 |

## 构建

需要 Node.js、npm、JDK 21 和 Android SDK。

```bash
npm ci
npm run build
npx cap sync android --deployment
cd android
./gradlew assembleDebug
```

调试 APK 位于 `android/app/build/outputs/apk/debug/app-debug.apk`。

## Release 构建与验证

为兼容 F-Droid 签名复制，正式发布先生成未签名 APK，再用脚本保留 ZIP 对齐进行签名。构建前取消签名环境变量：

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

未签名产物为 `android/app/build/outputs/apk/release/app-release-unsigned.apk`。在本机私有环境中配置 `ANDROID_SDK_ROOT`、`KEYSTORE_PASSWORD` 和 `KEY_PASSWORD` 后执行：

```bash
scripts/sign-release.sh android/app/build/outputs/apk/release/app-release-unsigned.apk release/3.1.5/app-release.apk
```

默认密钥库路径为 `paperphone-release.keystore`，可通过 `PPL_RELEASE_KEYSTORE` 指向外部私有文件。不要提交密钥库或密码。脚本使用 Build Tools 35.0.0，保留 ZIP 对齐并验证 v2/v3 签名。Release 仅支持 `arm64-v8a`，启用 R8，资源裁剪保持关闭。

## 当前发布与 F-Droid

- [3.1.5 Release](https://github.com/619dev/ppl-android/releases/tag/v3.1.5)：包含签名 APK 和 `SHA256SUMS`。
- 固定源码标签为 `v3.1.5`；上游客户端基线为 `2823996`。
- 初次收录 [MR !46295](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/46295) 已合并；[版本更新 MR !57182](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/57182) 保留现有构建条目，并追加 3.1.5（30105）。
- 本版 F-Droid 独立构建和签名复制结果以版本更新 MR 的 CI 为准。
- F-Droid 元数据启用了 `Binaries` 和允许的签名证书，用于验证可复现构建。跨渠道覆盖安装取决于最终 APK 证书是否相同，请先核对签名；本项目不发布到 Google Play。

提交、签名与验证流程见 [`fdroid/README.md`](fdroid/README.md)，构建元数据见 [`fdroid/com.fm619.paperphonelite.yml`](fdroid/com.fm619.paperphonelite.yml)。

## 3.1.5 升级与安全边界

双方客户端和服务端需配套升级。新版服务端启动时修复历史单向好友关系；Android 会在关系变更后刷新联系人。窄屏、短屏和横屏布局会避开系统安全区域，登录页可滚动。双方通过线下交流的文本外观密码完成身份握手；绿色身份标识需要有效证明及对方对本次挑战的回执，签名本身不证明陌生公钥属于目标联系人。身份密钥变化会撤销信任，需重新验证。

新群消息始终采用 Sender Key 加密和 Ed25519 签名，使用服务端发布的成员公钥，无须逐个完成私聊身份验证。文本外观密码只用于新私聊，旧群消息仍可用原密码解密。成员变动后使用新的密钥 epoch；离线客户端会恢复历史 epoch 的密钥。轮换无法撤回离群成员已持有的旧密钥或已读明文，也无法恢复此前已丢失的密钥。

旧版未签名消息会明确标记；联系人身份固定后拒绝未签名消息，包括旧历史。当前注册字段 `kem_pub` 实际承载 Ed25519 签名公钥，签名消息路径不使用 ML-KEM，不能宣称具备可工作的后量子密钥注册。消息签名不会对上传的附件文件本身提供端到端加密。每个会话的本地消息缓存最多保留 2,000 条，服务端自动删除和附件保留策略仍生效。

测试覆盖与手工验收步骤见 [`tests/README.md`](tests/README.md)。

## 与上游同步

上游前端目录为 `PaperPhoneLite/client`。同步时需保留 Android 原生 Tor 集成、Capacitor 配置和兼容插件，并重新执行前端构建与 `npx cap sync android`。上游服务端变更不应复制到本仓库。

## 许可证与问题反馈

本项目按 [AGPL-3.0](LICENSE) 发布，与[上游 PaperPhoneLite](https://github.com/619dev/PaperPhoneLite)一致。

- Android 客户端问题：[619dev/ppl-android/issues](https://github.com/619dev/ppl-android/issues)
- 上游协议、前端或服务端问题：[619dev/PaperPhoneLite/issues](https://github.com/619dev/PaperPhoneLite/issues)
