# TurboIMS

<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="200" alt="TurboIMS Logo"/>
</p>

<p align="center">
  <strong>在 Google Pixel 设备上开启 VoLTE、VoWiFi 和其他 IMS 功能。</strong>
</p>

<p align="center">
    <a href="https://github.com/Mystery00/TurboIMS/releases"><img src="https://img.shields.io/github/v/release/Mystery00/TurboIMS" alt="GitHub release"></a>
    <a href="LICENSE"><img src="https://img.shields.io/github/license/Mystery00/TurboIMS" alt="License"></a>
    <a href="https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/Mystery00/TurboIMS"><img src="https://img.shields.io/badge/Obtainium-Import-blue?logo=obtainium&logoColor=white" alt="Obtainium"></a>
</p>

[English](README.md)

## 截图

<p align="center">
  <img src="docs/Screenshot1.png" width="400"/>
  <img src="docs/Screenshot2.png" width="400"/>
</p>

## 关于

TurboIMS 是一个允许您在 Google Pixel 手机上启用或禁用 VoLTE（高清语音通话）、VoWiFi（Wi-Fi 通话）、VT（视频通话）和 VoNR（5G 语音）等 IMS 功能的工具。它需要 [Shizuku](https://shizuku.rikka.app/zh-CN/) 才能工作。
本项目 fork 自 [Turbo1123/TurboIMS](https://github.com/Turbo1123/TurboIMS)，并在此基础上持续维护与增强。

## 功能

- **系统信息**: 显示您设备的应用版本、Android 版本和安全补丁版本。
- **Shizuku 状态**: 显示 Shizuku 的当前状态，并允许刷新权限。
- **Logcat 查看器**: 查看和导出应用日志以进行调试。
- **SIM 卡选择**: 将设置应用于特定的 SIM 卡或一次性应用于所有 SIM 卡。
- **可定制的 IMS 功能**:
    - **运营商名称**: 覆盖设备上显示的运营商名称。
  - **运营商国家码 (Country ISO)**: 强制修改运营商国家码 (需 Android 14+)。
  - **IMS User Agent**: 自定义 IMS User Agent 字符串。
    - **VoLTE (高清语音通话)**: 开启 4G 高清语音通话。
    - **VoWiFi (Wi-Fi 通话)**: 通过 Wi-Fi 网络拨打电话，并可选择仅 Wi-Fi 模式。
    - **VT (视频通话)**: 开启基于 IMS 的视频通话。
    - **VoNR (5G 语音)**: 开启 5G 高清语音通话（需要 Android 14+）。
    - **Cross-SIM Calling (跨卡通话)**: 开启双卡互连功能。
    - **UT (补充业务)**: 通过 UT 开启呼叫转移、呼叫等待等补充服务。
    - **5G NR**: 开启 5G NSA（非独立组网）和 SA（独立组网）网络。
    - **5G 信号强度阈值**: 可选择是否应用自定义的 5G 信号强度阈值。
- **配置持久化**: 自动保存每张 SIM 卡的配置。

## 要求

- **支持设备**: 搭载 Google Tensor 芯片 (GS101, GS201, Zuma, Zuma Pro) 的 Pixel 设备。
    - Pixel 6, 6 Pro, 6a
    - Pixel 7, 7 Pro, 7a
    - Pixel 8, 8 Pro, 8a
    - Pixel 9, 9 Pro, 9 Pro XL, 9 Pro Fold
    - Pixel 10, 10 Pro, 10 Pro XL
    - Pixel Fold, Pixel Tablet
    - **注意:** 搭载 Qualcomm Snapdragon 芯片的设备（Pixel 5 及更早机型）**不支持**。
- Android 13 或更高版本
- 已安装并运行 [Shizuku](https://shizuku.rikka.app/zh-CN/)

## 安装

<a href="https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/Mystery00/TurboIMS"><img src="https://raw.githubusercontent.com/ImranR98/Obtainium/refs/heads/main/assets/graphics/badge_obtainium.png" alt="Obtainium" height="96"></a>

1.  从 [Releases](https://github.com/Mystery00/TurboIMS/releases) 页面下载最新的 APK。
2.  在您的设备上安装 APK。
3.  打开应用并授予 Shizuku 权限。

## 使用

1.  **检查状态**: 确保 Shizuku 正在运行且应用已获得权限。
2.  **选择 SIM 卡**: 选择您要配置的 SIM 卡。
3.  **切换功能**: 打开或关闭所需的 IMS 功能。
4.  **应用**: 点击“应用配置”按钮。


## 项目说明

本项目最初 fork 自 [Turbo1123/TurboIMS](https://github.com/Turbo1123/TurboIMS)。在使用原项目的过程中，由于遇到诸多问题，我决定对整个代码库进行彻底重构。这包括重写 SIM 卡读取、运营商配置的核心逻辑，以及重新设计 UI 和图标。

此外，本项目还引入了**运营商名称修改**和**日志查看**等新功能。尽管本项目保留了原有的 fork 关系和名称，但代码逻辑已发生根本性变化，几乎与原项目无关，因此后续不计划合并上游代码。作为我自己和朋友们日常使用的工具，只要大家还有需求，我就会持续维护下去。

## 鸣谢

- **[vvb2060/Ims](https://github.com/vvb2060/Ims)**
- **[nullbytepl/CarrierVanityName](https://github.com/nullbytepl/CarrierVanityName)**: 运营商名称修改功能的代码参考自此项目。
- **[kyujin-cho/pixel-volte-patch](https://github.com/kyujin-cho/pixel-volte-patch)**
- App 图标源于 [iconfont](https://www.iconfont.cn/collections/detail?cid=28924) 平台的设计，并在此基础上进行了修改以适配本项目。

## 免责声明

本应用会修改您设备的运营商配置。请自行承担使用风险。开发者对任何功能损坏或损失概不负责。

## 许可证

本项目使用 Apache License 2.0 许可证。有关详细信息，请参阅 [LICENSE](LICENSE) 文件。
