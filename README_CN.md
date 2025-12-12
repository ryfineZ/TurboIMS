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
</p>

[English](README.md)

## 截图

<p align="center">
  <img src="docs/Screenshot1.png" width="400"/>
  <img src="docs/Screenshot2.png" width="400"/>
</p>

## 关于

TurboIMS 是一个允许您在 Google Pixel 手机上启用或禁用 VoLTE（高清语音通话）、VoWiFi（Wi-Fi 通话）、VT（视频通话）和 VoNR（5G 语音）等 IMS 功能的工具。它需要 [Shizuku](https://shizuku.rikka.app/zh-CN/) 才能工作。

## 功能

- **系统信息**: 显示您设备的应用版本、Android 版本和安全补丁版本。
- **Shizuku 状态**: 显示 Shizuku 的当前状态，并允许刷新权限。
- **Logcat 查看器**: 查看和导出应用日志以进行调试。
- **SIM 卡选择**: 将设置应用于特定的 SIM 卡或一次性应用于所有 SIM 卡。
- **可定制的 IMS 功能**:
    - **运营商名称**: 覆盖设备上显示的运营商名称。
    - **VoLTE (高清语音通话)**: 开启 4G 高清语音通话。
    - **VoWiFi (Wi-Fi 通话)**: 通过 Wi-Fi 网络拨打电话，并可选择仅 Wi-Fi 模式。
    - **VT (视频通话)**: 开启基于 IMS 的视频通话。
    - **VoNR (5G 语音)**: 开启 5G 高清语音通话（需要 Android 14+）。
    - **Cross-SIM Calling (跨卡通话)**: 开启双卡互连功能。
    - **UT (补充业务)**: 通过 UT 开启呼叫转移、呼叫等待等补充服务。
    - **5G NR**: 开启 5G NSA（非独立组网）和 SA（独立组网）网络。
    - **5G 信号强度阈值**: 可选择是否应用自定义的 5G 信号强度阈值。

## 要求

- Google Pixel 设备
- Android 13 或更高版本
- 已安装并运行 [Shizuku](https://shizuku.rikka.app/zh-CN/)

## 安装

1.  从 [Releases](https://github.com/Mystery00/TurboIMS/releases) 页面下载最新的 APK。
2.  在您的设备上安装 APK。
3.  打开应用并授予 Shizuku 权限。

## 使用

1.  **检查状态**: 确保 Shizuku 正在运行且应用已获得权限。
2.  **选择 SIM 卡**: 选择您要配置的 SIM 卡。
3.  **切换功能**: 打开或关闭所需的 IMS 功能。
4.  **应用**: 点击“应用配置”按钮。


## 鸣谢

- App 图标源于 [iconfont](https://www.iconfont.cn/collections/detail?cid=28924) 平台的设计，并在此基础上进行了修改以适配本项目。

## 免责声明

本应用会修改您设备的运营商配置。请自行承担使用风险。开发者对任何功能损坏或损失概不负责。

## 许可证

本项目使用 Apache License 2.0 许可证。有关详细信息，请参阅 [LICENSE](LICENSE) 文件。
