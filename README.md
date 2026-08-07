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

## 📄 License

MIT
