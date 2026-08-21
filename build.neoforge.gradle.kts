import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("java-library")
    id("maven-publish")
    id("dev.kikugie.stonecutter")
    id("net.neoforged.moddev")
    id("idea")
}

// ===== 按版本配置（新增版本时在这里加一行） =====
val sc = extensions.getByType<StonecutterBuildExtension>()
val mcVersion = sc.current.version

data class VersionInfo(
    val neoForge: String,
    val neoForgeRange: String,
    val mcRange: String,
    val parchmentMc: String? = null,
    val parchmentMappings: String? = null,
    val java: Int,
    val packFormat: Int,
)

val versionInfo = mapOf(
    "1.21.1" to VersionInfo(neoForge = "21.1.244", neoForgeRange = "[21.1,)", mcRange = "[1.21.1]", parchmentMc = "1.21.1", parchmentMappings = "2024.11.17", java = 21, packFormat = 34),
    "1.21.8" to VersionInfo(neoForge = "21.8.54", neoForgeRange = "[21.8,)", mcRange = "[1.21.8]", java = 21, packFormat = 69),
    "1.21.10" to VersionInfo(neoForge = "21.10.64", neoForgeRange = "[21.10,)", mcRange = "[1.21.10]", java = 21, packFormat = 77),
)
val info = versionInfo[mcVersion] ?: error("Unsupported Minecraft version: $mcVersion (configured versions: ${versionInfo.keys})")

// ===== 通用配置 =====
version = "${property("mod_version")}+" + mcVersion
group = property("mod_group_id").toString()

base {
    archivesName.set(property("mod_id").toString())
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    inputs.property("modId", modId)
    inputs.property("modName", modName)
    inputs.property("modLicense", modLicense)
    inputs.property("modVersion", modVersion)
    inputs.property("mcRange", info.mcRange)
    inputs.property("neoVersion", info.neoForge)
    inputs.property("neoVersionRange", info.neoForgeRange)
    inputs.property("loaderVersionRange", loaderVersionRange)
    from(rootProject.file("src/main/templates"))
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(
            "mod_id" to modId,
            "mod_name" to modName,
            "mod_license" to modLicense,
            "mod_version" to modVersion,
            "minecraft_version_range" to info.mcRange,
            "neo_version" to info.neoForge,
            "neo_version_range" to info.neoForgeRange,
            "loader_version_range" to loaderVersionRange,
        )
    }
}

sourceSets.main {
    resources.srcDir("src/generated/resources")
    resources.srcDir(generateModMetadata)
    resources.exclude("**/*.bbmodel")
    resources.exclude("src/generated/**/.cache")
}

repositories {}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(info.java)
}

neoForge {
    version = info.neoForge

    if (info.parchmentMappings != null) {
        parchment {
            mappingsVersion.set(info.parchmentMappings)
            minecraftVersion.set(info.parchmentMc!!)
        }
    }

    runs {
        register("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", property("mod_id").toString())
        }
        register("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", property("mod_id").toString())
        }
        register("gameTestServer") {
            type = "gameTestServer"
            systemProperty("neoforge.enabledGameTestNamespaces", property("mod_id").toString())
        }
        register("data") {
            data()
            programArguments.addAll(
                "--mod", property("mod_id").toString(),
                "--all",
                "--output", file("src/generated/resources/").absolutePath,
                "--existing", file("src/main/resources/").absolutePath,
            )
        }
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        register(property("mod_id").toString()) {
            sourceSet(sourceSets["main"])
        }
    }
    ideSyncTask(generateModMetadata)
}

// 一次构建全部版本：把各版本 jar 收集到 build/libs/<mc版本>/ 下
tasks.register<Copy>("buildAndCollect") {
    group = "build"
    dependsOn("build")
    from(tasks.named("jar"))
    into(rootProject.layout.buildDirectory.dir("libs/" + mcVersion))
}

// 提前读取项目属性：在任务 lambda 里调 property() 会解析到任务对象上（报 unknown property）
val modId = property("mod_id").toString()
val modName = property("mod_name").toString()
val modLicense = property("mod_license").toString()
val modVersion = property("mod_version").toString()
val loaderVersionRange = property("loader_version_range").toString()

tasks.named<ProcessResources>("processResources") {
    dependsOn("stonecutterGenerate")
    // expand() 的值不参与 up-to-date 检查，显式声明为输入，改版本表后能自动重跑
    inputs.property("packFormat", info.packFormat)
    inputs.property("modId", modId)
    inputs.property("modName", modName)
    inputs.property("modLicense", modLicense)
    inputs.property("modVersion", modVersion)
    inputs.property("mcRange", info.mcRange)
    inputs.property("neoVersion", info.neoForge)
    inputs.property("loaderVersionRange", loaderVersionRange)
    filesMatching("pack.mcmeta") {
        expand("pack_format" to info.packFormat, "mod_id" to modId)
    }
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(
            "mod_id" to modId,
            "mod_name" to modName,
            "mod_license" to modLicense,
            "mod_version" to modVersion,
            "minecraft_version_range" to info.mcRange,
            "neo_version" to info.neoForge,
            "neo_version_range" to info.neoForgeRange,
            "loader_version_range" to loaderVersionRange,
        )
    }
}

// ModDevGradle 生成 artifact 前确保 Stonecutter 分版本源码已生成
tasks.matching { it.name == "createMinecraftArtifacts" }.configureEach {
    dependsOn("stonecutterGenerate")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}