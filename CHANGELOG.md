# Changelog

## [1.3.3] - 2026-08-08

## What's Changed

**1 commits** since previous release.

### 🐛 Bug Fixes
- run commands in user's login shell with full PATH (fix pnpm/node not found) (c0bb041)

---

**Full Changelog**: https://github.com/neo-idea/open-shell-toolbar-plugin/compare/v1.3.2...v1.3.3


## [1.3.2] - 2026-08-08

## What's Changed

**1 commits** since previous release.

### 🐛 Bug Fixes
- harden production reliability (async execution, service resolution, empty config persistence) (cd3e39c)

---

**Full Changelog**: https://github.com/neo-idea/open-shell-toolbar-plugin/compare/v1.3.1...v1.3.2


## [1.3.1] - 2026-08-08

## What's Changed

**1 commits** since previous release.

### 🐛 Bug Fixes
- register MainToolBarRight dynamically to avoid PluginException on 2026.1 (4aa3f78)

---

**Full Changelog**: https://github.com/neo-idea/open-shell-toolbar-plugin/compare/v1.3.0...v1.3.1


## [1.3.0] - 2026-08-08

## What's Changed

**1 commits** since previous release.

### 🚀 New Features
- show toolbar buttons in both MainToolBar and MainToolBarRight (b4bf56c)

---

**Full Changelog**: https://github.com/neo-idea/open-shell-toolbar-plugin/compare/v1.2.1...v1.3.0


## [1.2.1] - 2026-08-08

## What's Changed

**2 commits** since previous release.

### 🐛 Bug Fixes
- register toolbar buttons on MainToolBar for reliable rendering (8de59e4)

---

**Full Changelog**: https://github.com/neo-idea/open-shell-toolbar-plugin/compare/v1.2.0...v1.2.1


## [1.2.0] - 2026-08-07

## What's Changed

**1 commits** since previous release.

### 🚀 New Features
- render shell command buttons inline on main toolbar (951ea1e)

---

**Full Changelog**: https://github.com/neo-idea/open-shell-toolbar-plugin/compare/v1.1.3...v1.2.0


## [1.1.3] - 2026-08-07

## What's Changed

**2 commits** since previous release.

### 🐛 Bug Fixes
- pin plugin verification to resolvable IDE builds (4f40ee3)
- remove until-build cap and harden release pipeline (76714b3)

---

**Full Changelog**: https://github.com/neo-idea/open-shell-toolbar-plugin/compare/v1.1.2...v1.1.3


## [1.1.2] - 2026-08-07

## What's Changed

**1 commits** since previous release.

### 🐛 Bug Fixes
- align plugin id with Marketplace (com.openshell.idea.toolbar) (e21ed58)

---

**Full Changelog**: https://github.com/neo-idea/open-shell-toolbar-plugin/compare/v1.1.1...v1.1.2


## [1.1.1] - 2026-08-07

## What's Changed

**2 commits** since previous release.

### 🐛 Bug Fixes
- guard marketplace publish with shell check instead of secrets in if (21d562f)
- use secrets context for marketplace publish condition (cd6da7d)

---

**Full Changelog**: https://github.com/neo-idea/open-shell-toolbar-plugin/compare/v1.1.0...v1.1.1


## [1.1.0] - 2026-08-07

## What's Changed

**11 commits** since previous release.

### 🚀 New Features
- switch to Gradle build, fix API compat, add CI/CD pipeline (f112588)
- implement IntelliJ IDEA plugin with toolbar, tool window, status bar and CI/CD pipeline (9f364ef)
- add common variable shortcuts for command input (f82f78d)
- add GitHub Actions auto-release workflow with version bumping (3d2e9a8)
- 优化 UI 和体验，添加自动化发布功能 (2c905c9)

### 🐛 Bug Fixes
- configure IDE for plugin verification to resolve build failure (45ca95a)
- resolve regex group reference error in auto-release workflow (c2ccc79)

### 📦 Other Changes
- Title: Add supported variables documentation and quick insert buttons (1f91fd1)

---

**Full Changelog**: https://github.com/neo-idea/open-shell-toolbar-plugin/compare/2c905c9...v1.1.0


All notable changes to the **Open Shell Toolbar** plugin are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-04-10

### 🚀 New Features
- Customizable toolbar buttons that execute shell commands
- Tool window panel with search, add, edit, delete, reorder capabilities
- Status bar widget with quick command popup menu
- Settings integration (Settings > Tools > Shell Toolbar)
- Variable substitution in commands (`{{rootPath}}`, `$HOME`, `$USER`, `{{workspaceFolder}}`, `$(pwd)`)
- JSON import/export for configurations
- Emoji icon picker with preset icons
- Right-click context menu in tool window
- Double-click to execute commands
- Cross-platform support (macOS, Linux, Windows)
