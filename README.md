# TurboIMS

<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="200" alt="TurboIMS Logo"/>
</p>

<p align="center">
  <strong>Enable VoLTE, VoWiFi, and other IMS features on Google Pixel devices.</strong>
</p>

<p align="center">
    <a href="https://github.com/Mystery00/TurboIMS/releases"><img src="https://img.shields.io/github/v/release/Mystery00/TurboIMS" alt="GitHub release"></a>
    <a href="LICENSE"><img src="https://img.shields.io/github/license/Mystery00/TurboIMS" alt="License"></a>
    <a href="obtainium://add/https://github.com/Mystery00/TurboIMS"><img src="https://img.shields.io/badge/Obtainium-Import-blue?logo=obtainium&logoColor=white" alt="Obtainium"></a>
</p>

[简体中文](README_CN.md)

## Screenshots

<p align="center">
  <img src="docs/Screenshot1.png" width="400"/>
  <img src="docs/Screenshot2.png" width="400"/>
</p>

## About

TurboIMS is a tool that allows you to enable or disable IMS features like Voice over LTE (VoLTE), Wi-Fi Calling (VoWiFi), Video Calling (VT), and 5G Voice (VoNR) on Google Pixel phones. It requires [Shizuku](https://shizuku.rikka.app/) to work.

## Features

- **System Information**: Displays your device's app version, Android version, and security patch version.
- **Shizuku Status**: Shows the current status of Shizuku and allows for refreshing permissions.
- **Logcat Viewer**: View and expert application logs for debugging purposes.
- **Sim Card Selection**: Apply settings to a specific SIM card or all SIM cards at once.
- **Customizable IMS Features**:
    - **Carrier Name**: Override the carrier name displayed on your device.
  - **Country ISO**: Force modify operator country code (Requires Android 14+).
  - **IMS User Agent**: Override the IMS User Agent string.
    - **VoLTE (Voice over LTE)**: Enable high-definition voice calls over 4G.
    - **VoWiFi (Wi-Fi Calling)**: Make calls over Wi-Fi networks, with options for Wi-Fi only mode.
    - **VT (Video Calling)**: Enable IMS-based video calls.
    - **VoNR (Voice over 5G)**: Enable high-definition voice calls over 5G (Requires Android 14+).
    - **Cross-SIM Calling**: Enable dual-SIM interconnection features.
    - **UT (Supplementary Services)**: Enable call forwarding, call waiting, and other supplementary services over UT.
    - **5G NR**: Enable 5G NSA (Non-Standalone) and SA (Standalone) networks.
    - **5G Signal Strength Thresholds**: Option to apply custom 5G signal strength thresholds.
- **Configuration Persistence**: Automatically saves configuration per SIM card.

## Requirements

- **Supported Devices**: Google Pixel devices with Tensor chips (GS101, GS201, Zuma, Zuma Pro).
    - Pixel 6, 6 Pro, 6a
    - Pixel 7, 7 Pro, 7a
    - Pixel 8, 8 Pro, 8a
    - Pixel 9, 9 Pro, 9 Pro XL, 9 Pro Fold
    - Pixel 10, 10 Pro, 10 Pro XL
    - Pixel Fold, Pixel Tablet
    - **Note:** Devices with Qualcomm Snapdragons (Pixel 5 and older) are NOT supported.
- Android 13 or higher
- [Shizuku](https://shizuku.rikka.app/) installed and running

## Installation

<a href="obtainium://add/https://github.com/Mystery00/TurboIMS"><img src="https://raw.githubusercontent.com/ImranR98/Obtainium/refs/heads/main/assets/graphics/badge_obtainium.png" alt="Obtainium"></a>

1.  Download the latest APK from the [Releases](https://github.com/Mystery00/TurboIMS/releases) page.
2.  Install the APK on your device.
3.  Open the app and grant Shizuku permission.

## Usage

1.  **Check Status**: Ensure Shizuku is running and the app has permission.
2.  **Select SIM**: Choose the SIM card you want to configure.
3.  **Toggle Features**: Turn the desired IMS features on or off.
4.  **Apply**: Tap the "Apply Configuration" button.


## About this Project

This project originated as a fork of [Turbo1123/TurboIMS](https://github.com/Turbo1123/TurboIMS). However, due to various stability issues encountered during usage, the codebase has undergone a complete refactoring. This includes rewriting the core logic for SIM card reading and carrier configuration, as well as redesigning the UI and icons.

Major new features, such as **Carrier Name modification** and **Logcat viewer**, have also been introduced. Although the project retains the original name and fork history, the code has diverged significantly, and there are no plans to merge upstream changes. As an active user myself along with friends, I am committed to maintaining this project for as long as it serves a purpose.

## Credits

- **[vvb2060/Ims](https://github.com/vvb2060/Ims)**
- **[nullbytepl/CarrierVanityName](https://github.com/nullbytepl/CarrierVanityName)**: Carrier name modification logic is derived from this project.
- **[kyujin-cho/pixel-volte-patch](https://github.com/kyujin-cho/pixel-volte-patch)**
- The app icon is based on an original design from [iconfont](https://www.iconfont.cn/collections/detail?cid=28924), modified for use in this project.

## Disclaimer

This application modifies your device's carrier configuration. Use it at your own risk. The developers are not responsible for any damage or loss of functionality.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
