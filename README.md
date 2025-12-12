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
    - **VoLTE (Voice over LTE)**: Enable high-definition voice calls over 4G.
    - **VoWiFi (Wi-Fi Calling)**: Make calls over Wi-Fi networks, with options for Wi-Fi only mode.
    - **VT (Video Calling)**: Enable IMS-based video calls.
    - **VoNR (Voice over 5G)**: Enable high-definition voice calls over 5G (Requires Android 14+).
    - **Cross-SIM Calling**: Enable dual-SIM interconnection features.
    - **UT (Supplementary Services)**: Enable call forwarding, call waiting, and other supplementary services over UT.
    - **5G NR**: Enable 5G NSA (Non-Standalone) and SA (Standalone) networks.
    - **5G Signal Strength Thresholds**: Option to apply custom 5G signal strength thresholds.

## Requirements

- Google Pixel device
- Android 13 or higher
- [Shizuku](https://shizuku.rikka.app/) installed and running

## Installation

1.  Download the latest APK from the [Releases](https://github.com/Mystery00/TurboIMS/releases) page.
2.  Install the APK on your device.
3.  Open the app and grant Shizuku permission.

## Usage

1.  **Check Status**: Ensure Shizuku is running and the app has permission.
2.  **Select SIM**: Choose the SIM card you want to configure.
3.  **Toggle Features**: Turn the desired IMS features on or off.
4.  **Apply**: Tap the "Apply Configuration" button.


## Credits

- The app icon is based on an original design from [iconfont](https://www.iconfont.cn/collections/detail?cid=28924), modified for use in this project.

## Disclaimer

This application modifies your device's carrier configuration. Use it at your own risk. The developers are not responsible for any damage or loss of functionality.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
