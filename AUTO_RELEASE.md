# Auto Release & Publish

## CI/CD Pipeline

Push to `main` branch triggers an automated pipeline that:

1. **Bumps version** - Minor version (second segment) auto-increments: `1.0.0` → `1.1.0`
2. **Updates files** - Syncs version in `pom.xml` and `plugin.xml`
3. **Generates changelog** - Parses conventional commits since last tag
4. **Builds plugin** - Maven build + distribution ZIP packaging
5. **Verifies structure** - Checks JAR contains `plugin.xml`, ZIP has correct layout
6. **Creates tag** - Pushes `v{version}` git tag
7. **Updates CHANGELOG.md** - Prepends new release entry
8. **Creates GitHub Release** - With changelog, installation instructions, plugin metadata
9. **Publishes to JetBrains Marketplace** - Uploads distribution ZIP via API (optional)

## Conventional Commits

Use these prefixes for automatic changelog categorization:

| Prefix | Changelog Section |
|--------|-------------------|
| `feat:` | 🚀 New Features |
| `fix:` | 🐛 Bug Fixes |
| `improve:` / `enhance:` / `refactor:` | 🔧 Improvements |
| `docs:` | 📝 Documentation |
| `chore:` / `ci:` | (omitted from changelog) |

Example:
```
feat: add search filter to tool window
fix: resolve NPE in command executor
docs: update installation guide
```

## Setup

### Required GitHub Secrets

| Secret | Description | Required |
|--------|-------------|----------|
| `GITHUB_TOKEN` | Auto-provided by GitHub Actions | Yes |
| `JETBRAINS_TOKEN` | JetBrains Marketplace API token | No (optional) |

### JetBrains Marketplace Token

1. Go to [JetBrains Marketplace Profile](https://plugins.jetbrains.com/author/me/tokens)
2. Generate a new token
3. Add as `JETBRAINS_TOKEN` in **Settings > Secrets and variables > Actions**

### Optional GitHub Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `JETBRAINS_CHANNEL` | `stable` | Marketplace channel (`stable`, `eap`, etc.) |

## Versioning

Follows [Semantic Versioning](https://semver.org/):

- **Patch** (`1.0.x`): Bug fixes (manual)
- **Minor** (`1.x.0`): New features (auto on push to main)
- **Major** (`x.0.0`): Breaking changes (manual)

## Distribution Structure

The built ZIP follows IntelliJ plugin convention:

```
Open Shell Toolbar/
├── lib/
│   ├── open-shell-toolbar-plugin-{version}.jar
│   └── gson-2.10.1.jar
├── README.md
├── CHANGELOG.md
└── LICENSE
```

## Manual Installation

1. Download ZIP from [GitHub Releases](../../releases)
2. IntelliJ IDEA → **Settings → Plugins → ⚙️ → Install Plugin from Disk...**
3. Select the ZIP file
4. Restart IDE
