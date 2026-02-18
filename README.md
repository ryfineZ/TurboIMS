# Turbo IMS

> TurboIMS 的改版分支，增加诊断与兼容能力，适配 Android 16 之后的限制。
>
> 本项目 fork 自 [Turbo1123/TurboIMS](https://github.com/Turbo1123/TurboIMS)，并在此基础上持续维护与增强。

## 为什么要改

Android 16（尤其 2026-01 安全补丁之后）对 `CarrierConfig` 的 **persistent 覆盖**做了更严格限制：
- 非系统应用调用 `overrideConfig(..., persistent=true)` 可能直接触发 `com.android.phone` 崩溃
- 即使 UI 显示 5G 图标，IMS 也可能不注册，数据无法上网

因此本改版做了以下调整：
1. **Broker 兜底写入**：默认使用非 persistent 写入，避免系统崩溃
2. **读取真实系统配置**：UI 开关展示当前 CarrierConfig 实际值
3. **一键重启 IMS**：便于配置快速生效
4. **全量配置 Dump + 过滤**：快速诊断当前系统配置
5. **QS 快捷图块**：VoLTE 开关 / IMS 状态快捷入口
6. **SIM 读取增强**：加入 ISub 读取路径，兼容 eSIM/双卡
7. **包名改为 `io.github.vvb2060.ims.mod`**：可与原版共存安装

## 功能概览

- 系统信息、Shizuku 状态
- SIM 选择（单卡/全卡）
- IMS 功能开关：VoLTE / VoWiFi / VT / VoNR / UT / Cross‑SIM / 5G NR / 5G 阈值 / 显示 4G 等
- **读取当前系统配置（同步按钮）**
- **一键重启 IMS**
- **Dump 当前 CarrierConfig（可过滤关键字）**
- **QS 快捷图块**：VoLTE Toggle / IMS Status

## 最新版本更新（3.8.3）

- 新增「Wi-Fi 异常修复」独立卡片，提供小白可用的一键修复入口
- 新增「一键修复 Wi-Fi 网络」能力：修复国内 Wi‑Fi 显示网络受限/感叹号且无法上网
- 优化修复文案与交互反馈，降低技术门槛
- 统一系统信息、Wi‑Fi 修复卡片与 SIM 区块间距，布局更一致
- 发布说明与文档同步更新

完整变更请见 [CHANGELOG.md](CHANGELOG.md) 与 [Releases](https://github.com/ryfineZ/TurboIMS/releases)。

## 系统要求

- Pixel Tensor 机型（Pixel 6/7/8/9/10 系列、Fold/Tablet）
- Android 13+（建议 14/15/16）
- Shizuku 运行并授权

## 构建与安装

```
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> 注意：Debug 构建需要签名。项目已通过 `local.properties` 指定 keystore。若报签名错误，请创建本地 keystore 并写入：

```
SIGN_KEY_STORE_FILE=/path/to/your.keystore
SIGN_KEY_STORE_PASSWORD=***
SIGN_KEY_ALIAS=***
SIGN_KEY_PASSWORD=***
```

## 使用步骤（建议顺序）

1. 确认 Shizuku 运行并授权
2. 选择 SIM 卡
3. 点击“同步”读取当前配置
4. 按需开启/关闭功能并“应用”
5. 如需立即生效，可点击“重启 IMS”
6. 排障时进入“Dump 配置”并使用过滤

## 常见问题

### 1. 有信号但无法上网
系统更新后可能清空 APN。请先检查 APN 是否为空：
- 设置 → 网络与互联网 → SIM → APN
- MCC/MNC 一般由系统自动填写，无需手动修改
- 中国移动（China Mobile）
  - 名称（Name）：`CMNET`（名称可自定义，建议用此便于识别）
  - APN：`cmnet`
  - MCC/MNC：系统自动读取，保持默认（MNC 可能为 00/02/07）
  - APN 类型（APN Type）：`default,supl`（如无法上网可尝试 `default,supl,net`）
  - APN 协议（APN Protocol）：`IPv4/IPv6`
- 中国联通（China Unicom）
  - 名称（Name）：`3GNET`
  - APN：`3gnet`
  - MCC/MNC：系统自动读取，保持默认（MNC 可能为 01/06）
  - APN 类型（APN Type）：`default,supl`
  - APN 协议（APN Protocol）：`IPv4/IPv6`
- 中国电信（China Telecom）
  - 名称（Name）：`CTNET`
  - APN：`ctnet`（注意小写）
  - MCC/MNC：系统自动读取，保持默认（MNC 可能为 03/11）
  - APN 类型（APN Type）：`default,supl`
  - APN 协议（APN Protocol）：`IPv4/IPv6`
  - 漫游协议（Roaming Protocol）：`IPv4`

### 2. 5G 图标有但数据不通
Android 16 之后 CarrierConfig 覆盖可能无法真正让 IMS 注册成功。建议：
- 查看 `*#*#4636#*#*` 的 IMS Registration 状态
- 若为 Not registered，则需要运营商支持或更高权限（system app/root）

## 免责声明

本应用会修改系统运营商配置，仅供学习/测试使用。请自行承担风险。

## 致谢

- vvb2060/Ims
- kyujin-cho/pixel-volte-patch
- nullbytepl/CarrierVanityName

## License

Apache-2.0
