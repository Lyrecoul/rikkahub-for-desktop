<div align="center">
  <img src="desktop/src/main/resources/icon.png" alt="RikkaHub" width="100" />
  <h1>RikkaHub</h1>
  <p>A native desktop LLM chat client for Linux and Windows.</p>

  [简体中文](README_ZH_CN.md) | English
</div>

<div align="center">
  <img src="docs/img/desktop.png" alt="RikkaHub desktop" width="450" />
</div>

RikkaHub is a native Linux and Windows chat client built with Kotlin and Compose Desktop. It connects to OpenAI-compatible model services and keeps models, conversations, tools, and personal preferences in one desktop application.

## Features

- **OpenAI-compatible APIs**: Add a custom service URL and API key, test the connection, and discover available models. Presets are included for services such as OpenAI, DeepSeek, OpenRouter, and SiliconFlow.
- **Multi-model conversations**: Select models per conversation or assistant, with streaming responses, reasoning output, model capability indicators, context compression, and conversation branches.
- **Attachments and rich content**: Add images, audio, text, and documents to messages. Markdown, code highlighting, LaTeX math, and Mermaid diagrams are supported.
- **Tools and agents**: Configure MCP services and optionally enable web search, local time, long-term memory, and workspace agent tools. Agent actions involving local files, commands, or network access require confirmation.
- **Conversation management**: Search, pin, rename, and organize chats; export an individual conversation as Markdown or back up providers, preferences, and chats as JSON.
- **Localization and appearance**: Simplified Chinese, English, and additional interface languages are available, alongside system-aware light/dark modes and several accent themes.

## Quick Start

RikkaHub requires **JDK 17 or newer**. Clone the repository and run this from its root:

```bash
./gradlew :desktop:run
```

On first launch, open Settings and add a model provider. Enter an OpenAI-compatible base URL and API key, then use **Test and fetch models** to confirm the connection. Keys are handled by the desktop secure store and are not included in conversation exports.

## Build and Package

Development, native distribution, and test commands:

```bash
# Run the desktop app (Linux/macOS)
./gradlew :desktop:run

# Run the desktop app (Windows PowerShell)
.\gradlew.bat :desktop:run

# Create a distributable app for the current platform
./gradlew :desktop:createDistributable

# Run tests
./gradlew :desktop:test

# Build and install the Arch Linux package
cd packaging/arch && makepkg -si

# Create a Windows installer (run on Windows)
.\gradlew.bat :desktop:packageMsi
```

Gradle is configured with DEB, RPM, EXE, and MSI native package targets. Build Windows installers on Windows; generated files are placed under `desktop/build/compose/binaries/`. The Arch package recipe lives in `packaging/arch/` and uses the system Java runtime.

## Local Data

Configuration is stored in `$XDG_CONFIG_HOME/rikkahub/desktop.json` (or `~/.config/rikkahub/desktop.json`) on Linux and `%APPDATA%\RikkaHub\desktop.json` on Windows, while each conversation is saved separately under `conversations/` in the same directory. Settings can export a JSON backup. Resetting local data permanently removes local providers, assistants, preferences, and conversations. Linux uses Secret Service for API keys; Windows encrypts them with DPAPI for the current Windows user.

## Technology

RikkaHub is built with Kotlin, Compose Desktop, Ktor/OkHttp, and Kotlinx Serialization, and integrates the Model Context Protocol (MCP) Kotlin SDK. The Gradle Wrapper is included, so a separate Gradle installation is unnecessary.

## License

This project is licensed under the [GNU Affero General Public License v3.0](LICENSE) (AGPL-3.0).
