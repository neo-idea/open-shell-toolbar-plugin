# Changelog

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
