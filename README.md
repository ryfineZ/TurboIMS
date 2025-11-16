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

## About

TurboIMS is a tool that allows you to enable or disable IMS features like Voice over LTE (VoLTE), Wi-Fi Calling (VoWiFi), Video Calling (VT), and 5G Voice (VoNR) on Google Pixel phones. It requires [Shizuku](https://shizuku.rikka.app/) to work.

## Features

- **IMS Feature Control**: Enable or disable the following features:
    - VoLTE (Voice over LTE)
    - VoWiFi (Wi-Fi Calling)
    - VT (Video Calling)
    - VoNR (Voice over 5G)
    - Cross-SIM Calling
    - UT (Supplementary Services)
    - 5G NR
- **SIM Card Selection**: Apply settings for a specific SIM card.
- **System Info**: View your Android version and Shizuku status.

## Requirements

- Google Pixel device
- Android 14 or higher
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

## Permissions

- **READ_PHONE_STATE**: This permission is required to get the list of available SIM cards, allowing you to choose which one to configure. For more details on its usage, you can refer to the [source code](https://github.com/Mystery00/TurboIMS/blob/master/app/src/main/java/io/github/vvb2060/ims/MainViewModel.kt#L121-L138).

## Credits

- The app icon is based on an original design from [iconfont](https://www.iconfont.cn/collections/detail?cid=28924), modified for use in this project.

## Disclaimer

This application modifies your device'''s carrier configuration. Use it at your own risk. The developers are not responsible for any damage or loss of functionality.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
