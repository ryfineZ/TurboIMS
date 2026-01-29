## 3.7 (2026-01-29)

### 新增
- Android 16 下的 CarrierConfig 写入兜底（避免 persistent 限制导致崩溃）
- 读取系统当前配置并回显到 UI
- 一键重启 IMS 注册
- CarrierConfig 全量 Dump + 过滤
- QS 快捷图块：VoLTE 开关 / IMS 状态
- SIM 读取增强（ISub 路径，兼容 eSIM/双卡）
- 包名调整为 `io.github.vvb2060.ims.mod`，可与原版共存

### 优化
- 功能开关默认值与实际配置对齐
- 诊断入口集中到工具卡片，排障更直观

---

## 新增

- 增强系统信息显示与交互反馈，更新支持设备文档
- 实现 SIM 卡配置的持久化存储与回溯
- 支持自定义 SIM_COUNTRY_ISO_OVERRIDE_STRING

## 优化

- 使用 FeatureValue 封装功能配置数据，优化 Compose 重组性能
- 升级依赖库并调整配置读取逻辑
- 优化构建配置并启用代码混淆
