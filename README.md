<div align="center">
  <img src="desktop/src/main/resources/icon.png" alt="RikkaHub" width="100" />
  <h1>RikkaHub</h1>
  <p>A native Linux desktop LLM chat client.</p>

  [简体中文](README_ZH_CN.md) | English
</div>

<div align="center">
  <img src="docs/img/desktop.png" alt="RikkaHub desktop" width="450" />
</div>

## Build

Requires JDK 17 or newer.

```bash
./gradlew :desktop:run
./gradlew :desktop:createDistributable
cd packaging/arch && makepkg -si
```

The Arch package bundles a Java runtime. Configuration is stored in
`$XDG_CONFIG_HOME/rikkahub/desktop.json` (or `~/.config/rikkahub/desktop.json`), and conversations are stored
separately under `conversations/`.

## License

This project is licensed under the [GNU Affero General Public License v3.0](LICENSE) (AGPL-3.0).
