import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.pekaboo.opensource"
version = "1.1.0"

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
            <b>1.1.0</b><br/>
            <b>New Features</b><br/><ul>
<li>switch to Gradle build, fix API compat, add CI/CD pipeline</li>
<li>implement IntelliJ IDEA plugin with toolbar, tool window, status bar and CI/CD pipeline</li>
<li>add common variable shortcuts for command input</li>
<li>add GitHub Actions auto-release workflow with version bumping</li>
<li>优化 UI 和体验，添加自动化发布功能</li>
</ul>
<b>Bug Fixes</b><br/><ul>
<li>configure IDE for plugin verification to resolve build failure</li>
<li>resolve regex group reference error in auto-release workflow</li>
</ul>
        """.trimIndent()
        vendor {
            name = "Pekaboo"
            url = "https://github.com/pekaboo"
        }
        ideaVersion {
            sinceBuild = "233"
            // untilBuild omitted → Marketplace accepts all future IDE versions
        }
    }

    pluginVerification {
        ides {
            recommended()
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
        // untilBuild omitted → no upper bound, compatible with future IDE releases
    }

    signPlugin {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
}
