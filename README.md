# PaperPhoneLite Android

[English](README_EN.md) · [更新历史](changelog.md)

> [PaperPhoneLite](https://github.com/619dev/PaperPhoneLite) 的 Android 客户端，使用 Capacitor 8 打包上游 React/TypeScript 前端，并内嵌 Tor。

[![Upstream](https://img.shields.io/badge/上游-619dev%2FPaperPhoneLite-blue?logo=github)](https://github.com/619dev/PaperPhoneLite)
[![Version](https://img.shields.io/badge/版本-3.0.12-orange)](package.json)
[![License](https://img.shields.io/badge/License-AGPL--3.0-blue.svg)](LICENSE)

## 项目范围

本仓库只包含 Android 客户端，不包含 Rust 服务端、MySQL、Redis 或 onion service 部署配置。服务端和自托管文档位于[上游仓库](https://github.com/619dev/PaperPhoneLite)。

当前客户端提供：

- 私聊、群聊、联系人、群组及好友申请。
- 文字、图片、视频、文档、语音、Emoji 和 Telegram 贴纸消息。
- X25519 + ML-KEM-768 混合密钥协商、XSalsa20-Poly1305 消息加密、群聊 Sender Key 和安全号码。
- 消息同步、本地离线缓存、持久化发件箱、已读状态和输入状态。
- 消息自动删除、拉黑、好友标签、二维码和 TOTP 两步验证。
- 额外消息密码、8 种文本外观及前后台自动锁定。
- Android 必需的 ntfy 通知注册与订阅流程。

PaperPhoneLite 3.x **不提供**朋友圈、公开时间线、单聊或群聊语音/视频通话、LiveKit、Cloudflare R2 或 Web/PWA 生产发行。

## Tor 与网络边界

- APK 内嵌 Guardian Project `tor-android 0.4.8.17.2`，支持 `arm64-v8a`、`armeabi-v7a`、`x86` 和 `x86_64`。
- 原生 Tor 服务监听本机 SOCKS5 `127.0.0.1:9050`；建立线路后，Android WebView 代理切换到该端口。
- 直接 Tor 连接 20 秒内无法建立线路时，客户端会从 Tor Project 官方 circumvention settings 接口获取当前 WebTunnel bridge，通过内嵌 IPtProxy/Lyrebird 重启 Tor；上次成功获取的 bridge 可在接口暂时不可用时回退使用。
- 生产构建只接受规范的 v3 `.onion` 服务地址；Vite 开发模式额外允许回环地址。
- 原生代理桥拒绝清除 Tor 代理或替换为其他代理。浏览器开发模式不能由 JavaScript 强制代理，不属于受支持的生产发行方式。
- Tor 可隐藏网络来源，但不能保证绝对匿名，也不能消除设备、账号行为、通知服务或主动披露造成的关联。

## 通知与第三方服务

ntfy 是 Android 的保留且必需通知方案。应用从服务端取得专属主题、向服务端注册订阅并提供复制/下载入口；用户需在 ntfy App 中订阅该主题。客户端不包含 FCM、Firebase、OneSignal、Web Push 或 Google Services。

## 本地数据与密钥

- 上游前端通过 IndexedDB 保存身份密钥和 Sender Key，通过 localStorage 保存会话、设置、消息缓存与离线数据。
- 消息缓存持久化前会移除已解密的明文字段，但当前 3.0.12 前端没有把整个聊天缓存交给 Android Keystore 加密。
- 仓库保留原生 `SecureStoragePlugin` 和 `KeepAwakePlugin` 兼容代码，但同步后的 3.0 前端不调用它们，不能将其描述为当前密钥或缓存的系统级保护。
- 清理应用数据或卸载应用可能永久删除本地密钥及无法恢复的历史消息。

## Android 标识

| 项目 | 值 |
|---|---|
| 应用名称 | `PaperPhoneLite` |
| Application ID | `com.fm619.paperphonelite` |
| 版本 | `3.0.12` |
| Version Code | `30012` |
| 最低 Android API | 24 |

## 构建

需要 Node.js、npm、JDK 21 和 Android SDK。

```bash
npm install
npm run build
npx cap sync android
cd android
./gradlew assembleDebug
```

调试 APK 位于 `android/app/build/outputs/apk/debug/app-debug.apk`。

Release 构建需要未提交到 Git 的 `paperphone-release.keystore`，并应通过环境变量提供签名密码：

```bash
export KEYSTORE_PASSWORD='...'
export KEY_PASSWORD='...'
cd android
./gradlew clean assembleRelease
```

Release APK 位于 `android/app/build/outputs/apk/release/app-release.apk`。正式 Android APK 发布到本仓库的 [GitHub Releases](https://github.com/619dev/ppl-android/releases)，并计划提交至 F-Droid；不发布到 Google Play。F-Droid 会使用自己的密钥签名，因此其 APK 与 GitHub Release APK 不能互相覆盖安装。

F-Droid 提交与验证流程见 [`fdroid/README.md`](fdroid/README.md)，候选构建元数据见 [`fdroid/com.fm619.paperphonelite.yml`](fdroid/com.fm619.paperphonelite.yml)。

## 与上游同步

上游前端目录为 `PaperPhoneLite/client`。同步时需保留 Android 原生 Tor 集成、Capacitor 配置和兼容插件，并重新执行前端构建与 `npx cap sync android`。上游服务端变更不应复制到本仓库。

## 许可证与问题反馈

本项目按 [AGPL-3.0](LICENSE) 发布，与[上游 PaperPhoneLite](https://github.com/619dev/PaperPhoneLite)一致。

- Android 客户端问题：[619dev/ppl-android/issues](https://github.com/619dev/ppl-android/issues)
- 上游协议、前端或服务端问题：[619dev/PaperPhoneLite/issues](https://github.com/619dev/PaperPhoneLite/issues)
