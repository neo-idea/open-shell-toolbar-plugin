import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.pekaboo.opensource"
version = "1.0.0"

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
        instrumentationTools()
    }

    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        id = "com.pekaboo.opensource.open-shell-toolbar"
        name = "Open Shell Toolbar"
        version = project.version.toString()
        description = """
            Customizable toolbar plugin for IntelliJ IDEA.
            Add shell command buttons to toolbar, sidebar, and status bar for quick access to frequently used commands.
        """.trimIndent()
        changeNotes = """
            <b>1.0.0</b><br/>
            <ul>
                <li>Initial release: toolbar buttons, tool window, status bar widget</li>
                <li>Shell command execution with variable substitution</li>
                <li>JSON import/export for configurations</li>
            </ul>
        """.trimIndent()
        vendor {
            name = "Pekaboo"
            url = "https://github.com/pekaboo"
        }
        ideaVersion {
            sinceBuild = "233"
            untilBuild = "251.*"
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
        untilBuild = "251.*"
    }

    signPlugin {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
}
