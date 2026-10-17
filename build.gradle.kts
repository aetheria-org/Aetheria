import io.hamlook.aetheria.sharedvariables.ProjectTarget
import com.replaymod.gradle.preprocess.PreprocessExtension
import org.apache.commons.lang3.SystemUtils

plugins {
    idea
    java
    id("com.gradleup.shadow") version "9.4.1"
    kotlin("jvm") version "2.0.10"
    id("gg.essential.multi-version")
    id("gg.essential.loom")
    id("dev.architectury.architectury-pack200")
}

// Constants:

val target = ProjectTarget.values().find { it.projectPath == project.path }!!

val baseGroup = (project.findProperty("baseGroup") as? String) ?: "io.hamlook.aetheria"
val modid = (project.findProperty("modid") as? String) ?: "aetheria"
val transformerFile = rootProject.file("src/main/resources/accesstransformer.cfg")

// Minecraft configuration:

loom {
    runConfigs {
        "client" {
            property("mixin.debug", "true")
            if (target.isForge) {
                programArgs("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
            }
            if (SystemUtils.IS_OS_MAC_OSX) {
                vmArgs.remove("-XstartOnFirstThread")
            }
        }
        remove(getByName("server"))
    }
    if (target.isForge) {
        forge {
            pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
            mixinConfig("mixins.$modid.json")
            if (transformerFile.exists()) {
                println("Installing access transformer")
                accessTransformer(transformerFile)
            }
        }
    } else {
        accessWidenerPath.set(rootProject.file("src/main/resources/aetheria.accesswidener"))
    }
    @Suppress("UnstableApiUsage")
    mixin {
        useLegacyMixinAp.set(true)
        defaultRefmapName.set("mixins.$modid.refmap.json")
    }
}

sourceSets.main {
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
}

extensions.configure<PreprocessExtension> {
    vars.put("MC", target.minecraftVersion.versionNumber)
    vars.put("FORGE", if (target.isForge) 1 else 0)
    vars.put("FABRIC", if (target.isFabric) 1 else 0)
    vars.put("JAVA", target.minecraftVersion.javaVersion)
}

pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    the<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>().sourceSets.named("main") {
        if (target == ProjectTarget.MAIN) {
            kotlin.srcDir(rootProject.file("src/main/kotlin"))
            kotlin.srcDir(rootProject.file("src/main/java"))
        }
    }
    val compileKotlin = tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin")
    tasks.named<JavaCompile>("compileJava") {
        dependsOn(compileKotlin)
        classpath += files(compileKotlin.get().destinationDirectory)
        // options.isFork = true
    }
}

// Files listed in versions/<target>/buildpaths-excluded.txt are excluded from compilation on that target.

fun excludeBuildPaths(buildPathsFile: File, sourceSet: Provider<SourceSet>) {
    if (buildPathsFile.exists()) {
        sourceSet.get().apply {
            val buildPaths = buildPathsFile.readText().lineSequence()
                .map { it.substringBefore("#").trim().replace(Regex("\\.(?!kt|java|\\()"), "/") }
                .filter { it.isNotBlank() }
                .toSet()
            kotlin.exclude(buildPaths)
            java.exclude(buildPaths)
        }
    } else {
        logger.warn("buildpaths-excluded.txt not found: ${buildPathsFile.path} — no files excluded")
    }
}
val buildPathsFile = file("buildpaths-excluded.txt")
excludeBuildPaths(buildPathsFile, sourceSets.main)
excludeBuildPaths(buildPathsFile, sourceSets.test)

// Dependencies:

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
    maven("https://maven.minecraftforge.net/")
    maven("https://files.minecraftforge.net/maven/")
    maven("https://repo.essential.gg/repository/maven-public/")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://repo.nea.moe/releases")
}

val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    val versionName = target.minecraftVersion.versionName
    minecraft("com.mojang:minecraft:$versionName")

    if (target.mappingDependency == "official") {
        mappings(loom.officialMojangMappings())
    } else {
        mappings(target.mappingDependency)
    }

    if (target.forgeDep != null) {
        "forge"(target.forgeDep!!)
    }

    val mixinVersion = if (target == ProjectTarget.MAIN) "0.7.11-SNAPSHOT" else "0.8.2"

    if (target.isForge) {
        shadowImpl("org.spongepowered:mixin:$mixinVersion") {
            isTransitive = false
        }
        annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")
        annotationProcessor("com.google.code.gson:gson:2.10.1")
        //noinspection VulnerableGradleDependency — Guava 17.0 is MC 1.8.9's bundled version
        annotationProcessor("com.google.guava:guava:17.0")
    } else {
        target.fabricLoaderVersion?.let { modImplementation(it) }
        target.fabricApiVersion?.let { modImplementation(it) }
        modImplementation("net.fabricmc:fabric-language-kotlin:1.12.0+kotlin.2.0.10")
        modImplementation("io.github.llamalad7:mixinextras-fabric:0.4.1")
        annotationProcessor("io.github.llamalad7:mixinextras-fabric:0.4.1")
    }

    shadowImpl("org.java-websocket:Java-WebSocket:1.6.0") { isTransitive = false }
    shadowImpl("org.slf4j:slf4j-api:1.7.36") { isTransitive = false }

    shadowImpl(kotlin("stdlib"))
    shadowImpl("com.tianscar.imageio:imageio-apng:1.0.1") { isTransitive = false }
    shadowImpl("dev.matrixlab.webp4j:webp4j-core:2.5.0") { isTransitive = false }
    shadowImpl("com.google.code.gson:gson:2.10.1") { isTransitive = false }
    shadowImpl("com.fifesoft:rsyntaxtextarea:3.3.3") { isTransitive = false }

    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    if (target == ProjectTarget.MAIN) {
        shadowImpl("com.mojang:brigadier:1.0.18") { isTransitive = false }
        runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.2.1")
        implementation("moe.nea:hotswapagent-forge:1.0.1")
    }
    if (target.isFabric) {
        runtimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.1")
    }
}

// Tasks:

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000", "-Xmaxwarns", "10000"))
}

tasks.withType(Jar::class) {
    archiveBaseName.set(modid)
    manifest.attributes.run {
        this["Main-Class"] = "io.hamlook.aetheria.ModInstaller"
        if (target.isForge) {
            this["FMLCorePluginContainsFMLMod"] = "true"
            this["ForceLoadAsMod"] = "true"
            this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
            this["MixinConfigs"] = "mixins.$modid.json"
            if (transformerFile.exists()) this["FMLAT"] = "${modid}_at.cfg"
        }
    }
}

tasks.processResources {
    val versionStr = project.version.toString()
    val modidStr = modid
    val baseGroupStr = baseGroup

    inputs.property("version", versionStr)
    inputs.property("mcversion", target.minecraftVersion.versionName)
    inputs.property("modid", modidStr)
    inputs.property("basePackage", baseGroupStr)

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    filesMatching(listOf("mcmod.info", "mixins.$modidStr.json")) {
        expand("version" to versionStr, "mcversion" to target.minecraftVersion.versionName, "modid" to modidStr, "basePackage" to baseGroupStr)
    }

    rename("accesstransformer.cfg", "META-INF/${modidStr}_at.cfg")
}

val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    archiveClassifier.set("")
    inputFile.set(tasks.shadowJar.get().archiveFile)
}

tasks.jar {
    archiveClassifier.set("without-deps")
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
}

tasks.shadowJar {
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    archiveClassifier.set("non-obfuscated-with-deps")
    configurations = listOf(shadowImpl)
    from(rootProject.file("LICENSE.txt"))
    mergeServiceFiles()
    doLast {
        println("Shadow jar built: ${archiveFile.get().asFile} (${archiveFile.get().asFile.length()} bytes)")
    }

    fun relocate(name: String) = relocate(name, "$baseGroup.deps.$name")
    relocate("kotlin")
    relocate("com.google.gson")
    relocate("org.java_websocket")
    relocate("org.slf4j")
    relocate("com.tianscar.imageio")
    relocate("org.fife")
}

tasks.assemble.get().dependsOn(tasks.remapJar)


