import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.pekaboo.opensource"
version = "1.1.2"

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
        id = "com.openshell.idea.toolbar"
        name = "Open Shell Toolbar"
        version = project.version.toString()
        description = """
            Customizable toolbar plugin for IntelliJ IDEA.
            Add shell command buttons to toolbar, sidebar, and status bar for quick access to frequently used commands.
        """.trimIndent()
        changeNotes = """
            <b>1.1.2</b><br/>
            <b>Bug Fixes</b><br/><ul>
<li>align plugin id with Marketplace (com.openshell.idea.toolbar)</li>
</ul>
        """.trimIndent()
        vendor {
            name = "talentneo"
            url = "https://github.com/neo-idea/open-shell-toolbar-plugin"
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
