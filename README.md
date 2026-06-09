# SillyTavern Lite Android App

这是一个基于原型页面重构的原生 Android 项目，使用 Kotlin 与 Jetpack Compose 实现。

## 功能

- 角色广场首页，不再以聊天列表作为主入口
- 独立角色详情页，展示大头像、简介、标签、开场白、最近互动和开始聊天入口
- 沉浸式角色聊天页，顶部展示角色头像、名称和关系状态
- 互动历史页，每个角色独立保存聊天记录
- 设置页集中承载 API 配置、推理参数预留、调试预留与本地数据管理预留
- 聊天房间与系统返回键路由
- 个人资料编辑
- OpenAI 兼容 API 配置
- JSON 角色卡导入
- 酒馆 PNG 角色卡导入
- 本地保存角色、聊天记录、长期记忆预留、好感度预留、世界书预留、API 配置与用户资料

## 产品原则

- 角色优先：用户从角色广场进入详情，再开始或继续聊天。
- 沉浸优先：模型、参数、API Key 不出现在主要聊天路径中。
- 本地优先：角色卡、聊天记录、长期记忆、世界书和 API Key 默认只保存在本机。
- 数据包预留：未来导出 ZIP 可包含 `characters/`、`chats/`、`memories/`、`worldbooks/`、`settings.json`。

## 构建

```powershell
.\gradlew.bat assembleDebug --no-daemon
```

## 安装包

调试安装包位于：

```text
release/app-debug.apk
```

Flutter 版安装包位于：

```text
release/aichat_flutter-release.apk
```

Flutter 版源码位于：

```text
aichat_flutter/
```
