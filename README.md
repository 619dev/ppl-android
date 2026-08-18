# PaperPhonePlus — Android 客户端

🌐 **其他语言 / Other Languages:** [English](README_EN.md)

> 本仓库是 [PaperPhonePlus](https://github.com/619dev/Paperphone-plus) 的 **Android 原生客户端**（Capacitor 打包），基于上游前端代码构建，适用于 Google Play 及侧载分发。

[![Upstream](https://img.shields.io/badge/上游仓库-619dev%2FPaperphone--plus-blue?logo=github)](https://github.com/619dev/Paperphone-plus)
[![React](https://img.shields.io/badge/React-19-blue)](#)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.7-blue)](#)
[![Capacitor](https://img.shields.io/badge/Capacitor-8-green)](#)
[![Version](https://img.shields.io/badge/版本-3.0.1-orange)](package.json)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](LICENSE)

[![Google Play](https://img.shields.io/badge/Google%20Play-下载-green?logo=google-play)](https://play.google.com/store/apps/details?id=com.fm619.paperphoneplus)

---

## 📖 项目简介

PaperPhonePlus 是一款微信风格的端对端加密即时通讯应用。本仓库为其 Android 原生客户端，使用 [Capacitor](https://capacitorjs.com/) 将 React + TypeScript 前端打包为 Android APK/AAB。

### 核心特性

| 功能 | 说明 |
|------|------|
| 🔐 端对端加密 | 无状态 ECDH + XSalsa20-Poly1305，逐消息临时密钥，前向保密 |
| 🗝️ 安全本地密钥 | 身份私钥和群聊 Sender Keys 由 Android Keystore 保护，聊天缓存使用设备专属 AES-256-GCM 密钥加密 |
| 📹 视频/语音通话 | 私聊与群组会议统一使用 LiveKit SFU（最多 100 人），支持自由讨论与讲课模式 |
| 🎙️ 实时变声 | 语音消息 / 通话支持 3 档变声（0.8x / 1.0x / 1.2x） |
| 📱 会话保持 | 网络中断、普通鉴权失败或服务器地址变化时保留登录状态，仅在服务器明确撤销会话时退出 |
| 📴 离线访问 | 按账户缓存联系人、群组、聊天记录、朋友圈、时间线及媒体，断网时仍可浏览已缓存内容 |
| 👥 群聊 | 最多 2000 人群组，支持加密与未加密两种模式 |
| 💬 消息功能 | 文字、图片、视频、文件、语音消息、消息引用、Emoji 面板、Telegram 贴纸包、已读状态 |
| 🌐 朋友圈 | 发动态（文字+图片/视频）、点赞、评论、标签可见性控制 |
| 📰 时间线 | 小红书风格公开发帖区，双列瀑布流布局，支持匿名发帖 |
| 🔔 消息通知 | Android 使用 ntfy，不集成 FCM、Firebase 或 OneSignal |
| 🌐 多语言 | 中文、英文、日语、韩语、法语、德语、俄语、西班牙语 |
| 🔑 两步验证 | Google Authenticator 兼容 TOTP，8 个恢复码 |
| 📷 扫码 | 扫二维码添加好友、加入群聊 |

### 更新日志

完整版本更新记录已迁移至 [changelog.md](changelog.md)。

### 最近更新（v3.0.1）

- 前端整体同步至 PaperPhoneLite 3.0 系列，并统一应用、Android 原生平台和个人信息页版本号。
- 移除 OneSignal、FCM、Firebase、Web Push 及 Google Services 集成；Android 通知仅保留 ntfy。
- 内嵌 Guardian Project Tor 客户端，Tor 建立线路后将 WebView 流量路由至本地 SOCKS5，并禁止明网回退。
- 生产环境仅接受 v3 `.onion` 服务地址。

---

## 🔐 额外加密文本外观：工作原理与安全边界

这项功能是**建立在原有端到端加密（E2EE）之上的额外保险**，不是用文本外观代替 E2EE，也不会绕过或降低原有加密。私聊仍由 X25519 / ML-KEM-768 密钥协商及原有消息加密链路保护；群聊仍使用 Sender Key 协议。身份私钥和群聊 Sender Key 继续由 Android Keystore 保护。

启用后，每条消息按以下顺序处理：

1. 发送方先用双方或群内全员约定的额外密码处理消息正文。密码通过 PBKDF2-SHA-256（210,000 次迭代及随机盐）派生 AES-256-GCM 密钥；每条消息使用独立随机 IV，并通过认证标签校验完整性。
2. 额外加密后的完整数据帧（版本、盐、IV 和密文）再转换成所选的 8 种文本外观之一。这不是单纯替换字符的装饰效果，外观字符实际承载的是额外加密密文。
3. 该外观密文随后才进入项目原有加密链路：私聊使用 E2EE，群聊使用 Sender Key；服务器接收到的仍是原有 E2EE／Sender Key 密文及投递所需元数据。
4. 接收端执行相反流程：先用原有 E2EE／Sender Key 解密消息，再还原文本外观数据，并用额外密码解密出正文。

额外密码不会上传、自动同步或由服务器分发。私聊双方必须设置相同密码；群聊中希望阅读正文的所有成员也必须设置相同密码。文本外观不需要一致：每条消息都会携带自己的外观类型标记，接收端会自动识别并还原发送方选择的外观。例如一方发送“与佛论禅”、另一方发送“韩文”，只要额外密码相同，双方都能正常解密；每个人的外观设置只决定自己发出的密文样式。密码缺失、仍处于锁定状态或密码不一致时，消息依然能够正常发送、接收并完成原有 E2EE 解密，但应用只能显示文本外观密文，无法显示原文。

应用不会持久保存额外密码：解锁后密码只保留在当前运行内存中，本地仅保存随机盐和用于验证密码是否正确的 AES-GCM 验证数据。用户可以立即锁定，也可在应用离开前台 5、15、30 或 60 分钟后自动锁定。此额外层用于在原有 E2EE 之外增加一个独立的共享秘密；它不能替代强密码、设备锁、系统安全存储，也不能在设备已被完全控制且密码仍驻留内存时提供绝对保护。

## 🏗️ 技术架构

```
ppp-android/
├── android/                  # Capacitor Android 原生工程
├── src/                      # React + TypeScript 前端源码
│   ├── App.tsx               # 路由 + 鉴权守卫
│   ├── index.css             # 设计系统（暗色/亮色，玻璃拟态）
│   ├── main.tsx              # React 入口
│   ├── api/                  # HTTP 客户端 + WebSocket 客户端
│   ├── components/           # UI 组件（TabBar、通话覆盖层、QR码等）
│   ├── contexts/             # React Context
│   ├── crypto/               # 加密模块
│   │   ├── ratchet.ts        # ECDH + XSalsa20-Poly1305 加密
│   │   ├── keystore.ts       # Android Keystore 私钥持久化
│   │   └── groupCrypto.ts    # 群组加密（Sender Key 协议）
│   ├── hooks/                # 自定义 Hooks
│   │   └── useGroupCall.ts   # LiveKit 群组会议与主持控制
│   ├── i18n/                 # 多语言支持
│   ├── pages/                # 页面组件
│   ├── store/                # Zustand 状态管理
│   └── utils/                # 工具函数
│       ├── messagePayload.ts # 消息正文与引用信息的兼容编码
│       ├── offlineCache.ts   # 按账户隔离的数据与媒体离线缓存
│       ├── stickerCache.ts   # Telegram 贴纸持久化缓存
│       └── session.ts        # 会话终止信号识别与安全退出
├── capacitor.config.ts       # Capacitor 配置
├── vite.config.ts            # Vite 构建配置
├── package.json              # 依赖管理
└── android/                  # Capacitor Android 与内嵌 Tor 客户端
```

### 技术栈

- **前端框架**：React 19 + TypeScript 5.7
- **构建工具**：Vite 6
- **状态管理**：Zustand 5
- **原生桥接**：Capacitor 8（Android）
- **加密库**：libsodium-wrappers-sumo（WebAssembly，Curve25519 / XSalsa20-Poly1305）
- **抗量子加密**：crystals-kyber-js（CRYSTALS-Kyber 后量子密钥封装）
- **视频通话**：LiveKit SFU（私聊与群组会议）
- **网络隐私**：应用内嵌 Guardian Project Tor，仅允许 Tor onion 服务
- **Android 通知**：ntfy（无 Google 推送框架）
- **UI 图标**：Lucide React
- **动画**：Lottie Web
- **二维码**：qrcode + jsqr

---

## 🚀 快速开始

### 前置要求

- [Node.js](https://nodejs.org/) ≥ 18
- [Android Studio](https://developer.android.com/studio) + Android SDK
- JDK 17+

### 安装与构建

```bash
# 1. 克隆仓库
git clone <repo-url> && cd ppp-android

# 2. 安装依赖
npm install

# 3. 构建前端
npm run build

# 4. 同步到 Android 工程
npx cap sync android

# 5. 在 Android Studio 中打开
npx cap open android
```

### 开发模式

```bash
# 启动前端开发服务器
npm run dev

# 在另一个终端运行 Android 实时重载
npx cap run android --livereload --external
```

> **注意**：开发模式下需要配置后端服务器地址。可在登录页面手动输入后端地址，例如 `https://your-server.com`。

---

## 🔧 配置说明

### Capacitor 配置

核心配置位于 [capacitor.config.ts](capacitor.config.ts)：

| 配置项 | 值 | 说明 |
|--------|-----|------|
| `appId` | `com.fm619.paperphoneplus` | Android 应用包名 |
| `appName` | `PaperPhonePlus` | 应用显示名称 |
| `webDir` | `dist` | 前端构建输出目录 |
| `androidScheme` | `https` | WebRTC 和 crypto API 需要 HTTPS |

### Android 网络与通知

Android APK 内嵌 Tor，并在 Tor 建立线路后将 WebView 流量路由到本地 SOCKS5 端口。通知使用 ntfy；无需 Firebase 或 `google-services.json`。

---

## 📦 构建发布

### 构建 Release APK/AAB

```bash
# 构建前端
npm run build

# 同步到 Android 工程
npx cap sync android

# 在 Android Studio 中构建签名包
# Build → Generate Signed Bundle / APK
```

### 签名配置

项目包含发布签名密钥库 `paperphone-release.keystore`，用于 Google Play 签名分发。

---

## 🔗 相关项目

| 项目 | 说明 |
|------|------|
| [Paperphone-plus](https://github.com/619dev/Paperphone-plus) | 上游主仓库（前端 + 后端） |
| [ppp-win](https://github.com/619dev/ppp-win) | Windows 桌面客户端 |
| [ppp-mac](https://github.com/619dev/ppp-mac) | Mac 桌面客户端 |

---

## 🔒 安全模型

```
注册时:
  设备本地生成 IK（身份密钥）+ SPK（签名预密钥）+ 20x OPK（一次性预密钥）
  公钥上传服务器，私钥四层持久化，永不离开设备

发送消息时:
  发送方下载接收方 IK 公钥
  生成临时 ECDH 密钥对（每条消息独立）
  X25519 ECDH → 共享秘密 → XSalsa20-Poly1305 加密
  临时公钥附在消息 header 中，接收方解密后销毁

服务器所见:
  ✅ 密文 blob + 路由元数据（发件人/收件人 UUID）
  ❌ 明文 / 私钥 / 临时密钥 / 通话内容
```

---

## 📄 开源许可

本项目基于 [GNU Affero General Public License v3.0 (AGPL-3.0)](LICENSE) 开源，与上游仓库 [619dev/Paperphone-plus](https://github.com/619dev/Paperphone-plus) 保持一致。

简而言之：
- ✅ 个人和企业均可自由部署和使用
- ✅ 允许修改代码
- ⚠️ 修改后通过网络提供服务时，必须公开修改后的源代码
- ⚠️ 衍生作品必须使用相同协议（AGPL-3.0）
- ⚠️ 必须保留原始版权声明和许可信息

完整协议文本请参阅 [LICENSE](LICENSE) 文件。

---

## 🤝 贡献

欢迎通过 Issue 和 Pull Request 参与贡献！

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/your-feature`)
3. 提交更改 (`git commit -m 'Add your feature'`)
4. 推送到分支 (`git push origin feature/your-feature`)
5. 创建 Pull Request

---

## 📬 联系方式

- Telegram 群组：https://t.me/+vHJtvWJY_gEyMTUx
- 上游仓库 Issues：https://github.com/619dev/Paperphone-plus/issues
