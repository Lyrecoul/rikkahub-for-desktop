<div align="center">
  <img src="desktop/src/main/resources/icon.png" alt="RikkaHub" width="100" />
  <h1>RikkaHub</h1>
  <p>原生 Linux 与 Windows 桌面 LLM 聊天客户端。</p>

  简体中文 | [English](README.md)
</div>

<div align="center">
  <img src="docs/img/desktop.png" alt="RikkaHub 桌面版" width="450" />
</div>

RikkaHub 是一个以 Kotlin 和 Compose Desktop 构建的 Linux 与 Windows 原生聊天客户端。它可连接兼容 OpenAI API 的模型服务，让模型、对话、工具和个人偏好集中在一个桌面应用中管理。

## 功能

- **兼容 OpenAI API**：添加自定义服务地址与密钥，自动测试连接并获取可用模型；内置 OpenAI、DeepSeek、OpenRouter、硅基流动等常用服务预设。
- **面向多模型的对话**：按会话或助手选择模型，支持流式回复、推理内容、模型能力标识、上下文压缩及对话分支。
- **附件与富文本**：可在消息中添加图片、音频、文本和文档；支持 Markdown、代码高亮、LaTeX 数学公式与 Mermaid 图表渲染。
- **工具与智能体**：可配置 MCP 服务，并按需启用联网搜索、当前时间、长期记忆和工作区智能体工具。涉及本地文件、命令或网络的智能体操作需要用户确认。
- **对话整理与迁移**：搜索、置顶、重命名和归档会话，导出单个对话为 Markdown，或将服务配置、偏好和聊天记录备份为 JSON。
- **本地化与外观**：提供简体中文、英语及多种界面语言，支持跟随系统的明暗主题和多套主题色。

## 快速开始

需要 **JDK 17 或更新版本**。克隆仓库后，在项目根目录执行：

```bash
./gradlew :desktop:run
```

首次启动后，前往设置添加一个模型服务。填写 OpenAI 兼容的基础 URL 和 API 密钥，再使用“测试并获取模型”确认连接。密钥由桌面端的安全存储管理，不会写入会话导出文件。

## 构建与打包

开发构建和原生发行包：

```bash
# 运行桌面应用（Linux/macOS）
./gradlew :desktop:run

# 运行桌面应用（Windows PowerShell）
.\gradlew.bat :desktop:run

# 生成当前平台的应用分发目录
./gradlew :desktop:createDistributable

# 运行测试
./gradlew :desktop:test

# 构建并安装 Arch Linux 软件包
cd packaging/arch && makepkg -si

# 创建 Windows 安装程序（在 Windows 上运行）
.\gradlew.bat :desktop:packageMsi
```

Gradle 配置了 DEB、RPM、EXE 与 MSI 原生包目标；Windows 安装包需在 Windows 上构建，生成的产物位于 `desktop/build/compose/binaries/`。Arch 软件包构建脚本位于 `packaging/arch/`，并使用系统 Java 运行时。

## 本地数据

Linux 上应用设置保存在 `$XDG_CONFIG_HOME/rikkahub/desktop.json`（默认路径为 `~/.config/rikkahub/desktop.json`），Windows 上保存在 `%APPDATA%\RikkaHub\desktop.json`；每个会话保存在同一目录下 `conversations/` 的独立文件中。可在设置中导出 JSON 备份；重置本地数据会永久删除本机的服务、助手、偏好与对话记录。Linux 使用 Secret Service 保存 API 密钥；Windows 使用仅当前 Windows 用户可解密的 DPAPI。

## 技术栈

RikkaHub 使用 Kotlin、Compose Desktop、Ktor/OkHttp 和 Kotlinx Serialization 构建，并集成 Model Context Protocol（MCP）Kotlin SDK。项目采用 Gradle Wrapper，因此无需预先安装 Gradle。

## 许可证

本项目基于 [GNU Affero General Public License v3.0](LICENSE) (AGPL-3.0) 开源。
