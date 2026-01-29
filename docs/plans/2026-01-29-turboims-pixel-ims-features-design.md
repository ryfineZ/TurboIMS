# TurboIMS: Pixel IMS 优势功能整合设计

日期：2026-01-29

## 目标
在保持 TurboIMS 现有结构与易用性的前提下，引入 Pixel IMS 的核心优势：
- 高版本系统写入兜底（Broker instrumentation）
- 读取真实 CarrierConfig 回显到 UI
- 一键重启 IMS 注册
- QS 快捷开关/状态图块
- 全量配置 Dump（只读导出）

## 范围
包含以上 5 项能力，不包含“全量编辑器”。

## 架构与组件
维持现有分层：UI → ViewModel → ShizukuProvider → privileged instrumentation。
新增组件：
1) BrokerInstrumentation：使用 shell 权限写入 CarrierConfig，必要时从 persistent=true 降级为 false。
2) ConfigReader：读取 CarrierConfig 当前值（真实状态）。
3) ImsStatusReader：查询 IMS 注册状态（ITelephony.isImsRegistered）。
4) QS Tile Services：VoLTE Toggle + IMS Status，分别提供 SIM1/SIM2。
5) DumpActivity/VM：读取全量 CarrierConfig 并导出/复制。

## 数据流
- 写入：MainViewModel → ShizukuProvider.overrideImsConfig → ImsModifier；失败/高版本兜底到 BrokerInstrumentation。
- 读取：ConfigReader → Feature 映射 → UI 回显；失败回退到本地历史配置。
- QS：Tile → ShizukuProvider（写入/读取/IMS 重启）→ 更新 tile 状态。
- Dump：ConfigReader 全量扫描 KEY_*，输出文本。

## 兼容性策略
- 当 overrideConfig 抛出 SecurityException（persistent 限制）或写入失败时，切换到 BrokerInstrumentation。
- ConfigReader 与 ImsStatusReader 失败时 UI 退回历史配置或显示“未知”。

## 错误处理
- 所有特权调用返回明确错误消息，UI 提示并允许重试。
- Shizuku 未授权时：UI 入口提示授权，QS 显示不可用。

## 测试与验证
- 手动验证：
  - VoLTE 开关与 IMS 重启在 SIM1/SIM2 均生效
  - QS tile 状态更新与点击行为
  - Dump 内容可导出且含关键 KEY_* 值
- 回归：主界面应用配置、历史恢复、日志功能不受影响

