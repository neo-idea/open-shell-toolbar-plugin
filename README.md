# Open Shell Toolbar Plugin

🚀 IntelliJ IDEA plugin that adds a customizable shell command toolbar, tool window, and status bar widget for quick command execution.

## ✨ Features

- **Toolbar Action Group**: Dynamic shell command buttons in the main toolbar
- **Tool Window Panel**: Full-featured command manager with search, add/edit/delete, and context menu
- **Status Bar Widget**: Quick command popup accessible from the status bar
- **Settings Integration**: Configure commands via Settings > Tools > Shell Toolbar
- **Variable Substitution**: Support for `{{rootPath}}`, `{{workspaceFolder}}`, `$HOME`, `$USER`, `$API_KEY`, `$(pwd)` and more
- **JSON Import/Export**: Save and load configurations as JSON files
- **Emoji Icon Picker**: Preset emoji icons for command buttons
- **CI/CD Pipeline**: Auto version bump, changelog generation, and GitHub Release

## 🔧 Build

```bash
# Build with Gradle
./gradlew build

# The plugin zip will be in build/distributions/
```

## 📦 Installation

1. Build the plugin: `./gradlew build`
2. In IntelliJ IDEA, go to **Settings > Plugins > ⚙️ > Install Plugin from Disk...**
3. Select the built `.zip` file from `build/distributions/`

## 🎯 Usage

1. Configure shell commands in **Settings > Tools > Shell Toolbar**
2. Use the toolbar buttons for quick command execution
3. Open the **Shell Toolbar** tool window for full command management
4. Click the status bar widget for quick access

## ❓ FAQ

### Why are the command buttons not visible on the main toolbar?

The buttons are registered on the right side of the main toolbar
(`MainToolBarRight`). If they are missing:

- **New UI hides the main toolbar by default** — enable it via
  `View > Appearance > Toolbar` (or `Settings > Appearance & Behavior >
  Appearance > Show toolbar in the main window`).
- On narrow windows the buttons may be folded into the toolbar overflow
  menu — click the **⋮** (three dots) at the right end of the toolbar and
  look for the "Shell Commands" group.
- Confirm the installed version is **1.2.0+** (`Settings > Plugins >
  Installed > Open Shell Toolbar`), since versions before 1.2.0 render as a
  single dropdown icon instead of individual buttons.

The plugin also provides two other entry points that do not depend on the
toolbar being visible: the **Shell Toolbar** tool window on the right edge and
the **Shell Commands** status bar widget.

## 📄 License

MIT
