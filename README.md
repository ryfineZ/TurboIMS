# Turbo IMS Mod

> TurboIMS 的改版分支，增加诊断与兼容能力，适配 Android 16 之后的限制。

## 为什么要改

Android 16（尤其 2025-01 安全补丁之后）对 `CarrierConfig` 的 **persistent 覆盖**做了更严格限制：
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
- 中国移动建议 APN：`cmnet`，类型 `default,supl`

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
