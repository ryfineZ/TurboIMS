### New Features / 新功能
- **Logcat Viewer / Logcat 查看器**: Added a new page to view and export application logs for easier debugging. / 新增 Logcat 页面，支持查看和导出应用日志。

### Important Changes / 重要变更
- **No Configuration Persistence / 不再保存配置**: Configuration is no longer stored locally. Each application applies a fresh configuration. Configuration state represents current selection properly. / 配置不再保存在本地，每一次应用配置都是全新的数据。
- **Permission Removal / 移除权限**: Removed `READ_PHONE_STATE` permission. SIM card information is now accessed via Shizuku with shell privileges. / 移除了 `READ_PHONE_STATE` 权限，现在通过 Shizuku 以 Shell 权限读取 SIM 卡信息。

### Optimizations / 优化
- **UI Refactor / UI 重构**: Refactored SIM selection UI and optimized hint formats. / 重构 SIM 卡选择 UI，调整提示信息格式。
- **Architecture & Cleanup / 架构与清理**: Refactored application architecture and removed unused code/resources. / 重构应用架构，优化代码和资源配置，移除未使用的代码。
- **CI & Dependencies / CI 与依赖**: Updated CI configuration and dependencies. / 更新 CI 配置和依赖。
