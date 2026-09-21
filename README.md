# Open Shell Toolbar

[![Build & Release](https://github.com/neo-idea/open-shell-toolbar-plugin/actions/workflows/auto-release.yml/badge.svg)](https://github.com/neo-idea/open-shell-toolbar-plugin/actions/workflows/auto-release.yml)
[![Latest Release](https://img.shields.io/github/v/release/neo-idea/open-shell-toolbar-plugin)](https://github.com/neo-idea/open-shell-toolbar-plugin/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![IntelliJ Platform](https://img.shields.io/badge/IntelliJ-2023.3%2B-blue)](https://plugins.jetbrains.com/docs/intellij/api-changes-list.html)

**One-click shell commands, right where you work — the IntelliJ toolbar.**

Open Shell Toolbar is a lightweight plugin for IntelliJ IDEA (and other
IntelliJ-Platform IDEs) that puts your frequently-used shell commands onto the
main toolbar, a tool window, and the status bar. Stop retyping the same
`pnpm dev`, `docker compose up`, or `git pull --rebase` in a terminal — pin
them as buttons and run them with a single click.

## ✨ Features

- **Toolbar buttons for shell commands** — run any command with one click,
  from the main toolbar
- **Two display modes** (switchable live in Settings):
  - **Popup** *(default)* — a single icon; click to open a dropdown with all commands
  - **Flat** — one button per command directly on the toolbar
    (3 commands = 3 buttons, no dropdown)
- **Flexible icons** — each command's icon can be an
  **emoji**, **inline SVG code**, or an **image URL**
  (`http(s)://`, `file://`, `data:` URI; PNG / JPEG / GIF / SVG)
  with a live preview in the edit dialog
- **Built-in Terminal by default** — commands run in the IDE's terminal tool
  window (modern API on 2025.2+, classic fallback on older versions);
  an external-terminal fallback keeps things working everywhere
- **Tool window manager** — search, add, edit, duplicate, delete, enable/disable,
  import/export JSON
- **Status bar widget** — quick popup from the bottom-right status bar
- **Variable substitution** — `{{rootPath}}`, `{{workspaceFolder}}`, `$HOME`,
  `$USER`, `$(pwd)`, and more
- **Live refresh** — changes apply immediately everywhere, no IDE restart
- **Persistent** — configurations survive IDE restarts and updates

## 📦 Installation

### From GitHub Releases (recommended)

1. Download the latest `open-shell-toolbar-plugin-*.zip` from
   [Releases](https://github.com/neo-idea/open-shell-toolbar-plugin/releases)
2. In your IDE: **Settings/Preferences → Plugins → ⚙ → Install Plugin from Disk…**
3. Select the downloaded zip, restart when prompted

### From source

```bash
./gradlew build
# zip is produced in build/distributions/
```

Then install it via *Install Plugin from Disk…* as above.

## 🚀 Quick Start

1. After installation, find the shell icon on the main toolbar
   (left of the settings gear in the New UI)
2. Open it → **Configure…** (or **Settings → Tools → Shell Toolbar**)
3. Add a command, for example:

   | Field | Value |
   |---|---|
   | Title | `Dev Server` |
   | Command | `pnpm dev` |
   | Working Directory | *(empty = project root)* |
   | Icon | 🚀 — or an SVG / image URL |
   | Open in built-in Terminal | ✔ |

4. Click **OK** — the command is now a button on your toolbar.

## 📖 Usage

### Toolbar display modes

Switch in **Settings → Tools → Shell Toolbar → Toolbar display mode**:

| Mode | Looks like |
|---|---|
| **Popup** (default) | One icon → dropdown with all commands + *Configure* |
| **Flat** | Every enabled command as its own button on the toolbar |

With no enabled commands, the popup icon always stays visible so the
*Configure* entry remains reachable.

### Running commands

- Commands with **Open in built-in Terminal** (the default) run inside the
  IDE Terminal tool window — each run opens a new terminal tab and executes
  there
- Without it, commands run in the background and finish with a notification
- `$ProjectFileDir$`-style variables, `{{rootPath}}`, `$HOME`, `$(pwd)` etc.
  are substituted before execution

### Managing commands

- **Settings → Tools → Shell Toolbar** — table editor with import/export
- **Shell Toolbar tool window** — same features plus search and context menus
- **Status bar** — click the widget for a quick popup

### Where is my configuration stored?

Commands are persisted per IDE instance via
`PersistentStateComponent` (XML under
`options/shell-toolbar.xml` in the IDE config directory), and can be
exported/imported as JSON from the settings page.

## ❓ FAQ

**The buttons are not visible on the toolbar?**

- New UI hides the main toolbar by default — enable it via
  **View → Appearance → Toolbar** (or **Settings → Appearance & Behavior →
  Appearance → Show toolbar in the main window**)
- On narrow windows the buttons may fold into the toolbar overflow menu —
  click the **⋮** at the right end of the toolbar and look for the
  *Shell Commands* group
- The tool window and status bar widget work regardless of toolbar visibility

**My URL/SVG icon shows the default 💻?**

The image failed to load (bad URL, non-image content, or invalid SVG).
Check `idea.log` (Help → Show Log in Finder…) for `CommandIconManager`
warnings. Icons are cached — fix the source and restart the IDE to retry.

**Which IDEs are supported?**

Anything on IntelliJ Platform **2023.3+**: IDEA (Community/Ultimate), WebStorm,
PyCharm, GoLand, etc. Verified against 2023.3 and 2025.2 in CI.

## 🛠 Development

```bash
./gradlew build          # compile + tests + verification
./gradlew verifyPlugin   # compatibility check (IC-2023.3, IC-2025.2)
./gradlew runIde         # sandbox IDE with the plugin installed
```

- CI (GitHub Actions) builds, verifies, bumps the version by commit type
  (`feat:` minor / `fix:` patch), generates the changelog, and publishes a
  GitHub Release on every push to `main`
- See [CHANGELOG.md](CHANGELOG.md) for release history

Contributions are welcome — PRs, issues, and icon presets alike.

## 📄 License

[MIT](LICENSE) © neo-idea
