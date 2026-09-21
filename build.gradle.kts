import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.openshell.idea"
version = "1.6.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2023.3")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.plugins.terminal")
        instrumentationTools()
    }

    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        id = "com.openshell.idea.toolbar"
        name = "Open Shell Toolbar"
        version = project.version.toString()
        description = """
            Customizable toolbar plugin for IntelliJ IDEA.
            Add shell command buttons to toolbar, sidebar, and status bar for quick access to frequently used commands.
        """.trimIndent()
        changeNotes = """
            <b>1.6.0</b><br/>
            <b>New Features</b><br/><ul>
<li>toolbar display modes: Popup (single icon with dropdown, default) or Flat (one button per command directly on the toolbar) — switchable in Settings &gt; Tools &gt; Shell Toolbar</li>
</ul>
            <b>1.5.2 / 1.5.1</b><br/>
            <b>Bug Fixes</b><br/><ul>
<li>fix: command configurations are now truly persisted across IDE restarts (new commands were silently lost)</li>
<li>fix: toolbar dropdown and status bar popup now render consistently (emoji icon + title + command preview, shared renderer, empty state with Configure entry)</li>
<li>fix: "Open in Terminal" reliably opens the IntelliJ built-in terminal — modern terminal API on 2025.2+ with classic API fallback, instead of silently dropping to the OS terminal</li>
<li>fix: new commands open in the built-in terminal by default</li>
<li>fix: settings page and tool window refresh live when commands change elsewhere</li>
</ul>
        """.trimIndent()
        vendor {
            name = "talentneo"
            url = "https://github.com/neo-idea/open-shell-toolbar-plugin"
        }
        ideaVersion {
            sinceBuild = "233"
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        ides {
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2023.3")
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2025.2")
        }
    }

    buildSearchableOptions = true

    publishing {
        token = providers.environmentVariable("JETBRAINS_TOKEN")
        channels = listOf(providers.environmentVariable("JETBRAINS_CHANNEL").getOrElse("stable"))
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    patchPluginXml {
        sinceBuild = "233"
    }

    signPlugin {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
}
