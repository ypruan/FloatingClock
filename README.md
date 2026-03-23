# 悬浮时钟 ⏰

一个用于线上抢购的 Android 悬浮时钟应用，支持毫秒级时间显示和倒计时功能。

## ✨ 功能特性

- **毫秒级精度** - 时间显示精确到毫秒（如 `12:34:56.789`）
- **双模式切换** - 支持当前时间和倒计时两种模式
- **悬浮窗显示** - 可拖动的半透明卡片，不影响其他应用操作
- **智能提醒** - 支持提示音、震动和提前提醒功能
- **精美 UI** - Material 3 设计风格，简约美观
- **抢购优化** - 60fps 刷新率，低延迟显示

## 📱 截图

| 主界面 | 悬浮窗 | 时间设置 |
|--------|--------|----------|
| 模式选择、启动按钮 | 毫秒级时间显示 | 精确到毫秒的目标时间设置 |

## 🚀 使用方法

### 1. 下载安装

从 [Releases](../../releases) 页面下载最新的 APK 文件安装。

### 2. 授予权限

首次启动需要授予"悬浮窗权限"，按提示操作即可。

### 3. 使用步骤

1. 选择显示模式（当前时间 / 倒计时）
2. 如选择倒计时模式，设置目标时间
3. 配置提醒设置（提示音、震动等）
4. 点击"启动悬浮时钟"
5. 拖动悬浮窗到合适位置

## 🛠️ 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose + XML 混合
- **架构**: 服务（Service）+ 悬浮窗（WindowManager）
- **最低 SDK**: Android 7.0 (API 24)
- **目标 SDK**: Android 14 (API 34)

## 📦 自行编译

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34

### 编译步骤

```bash
# 克隆仓库
git clone https://github.com/yourusername/FloatingClock.git
cd FloatingClock

# 使用 Gradle 编译
./gradlew assembleDebug

# APK 输出路径
# app/build/outputs/apk/debug/app-debug.apk
```

## 📄 许可证

[MIT License](LICENSE)

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 💬 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 [GitHub Issue](../../issues)
- 发送邮件至: your-email@example.com

---

**注意**: 本应用仅用于辅助显示时间，请遵守各平台的抢购规则，理性消费。
