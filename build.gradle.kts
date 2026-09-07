import org.apache.commons.lang3.SystemUtils

plugins {
    idea
    java
    id("com.gradleup.shadow") version "9.4.1"
    id("gg.essential.multi-version")
    id("gg.essential.loom")
    kotlin("jvm") version "2.3.20"
    id("dev.architectury.architectury-pack200")
}

//Constants:

val baseGroup = (project.findProperty("baseGroup") as? String) ?: "io.hamlook.aetheria"
val mcVersion = (project.findProperty("mcVersion") as? String) ?: "1.8.9"
val modid = (project.findProperty("modid") as? String) ?: "aetheria"
val transformerFile = rootProject.file("src/main/resources/accesstransformer.cfg")

// Minecraft configuration:
loom {
    runConfigs {
        "client" {
            property("mixin.debug", "true")
            programArgs("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
            if (SystemUtils.IS_OS_MAC_OSX) {
                vmArgs.remove("-XstartOnFirstThread")
            }
        }
        remove(getByName("server"))
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        mixinConfig("mixins.$modid.json")
        if (transformerFile.exists()) {
            println("Installing access transformer")
            accessTransformer(transformerFile)
        }
    }
    @Suppress("UnstableApiUsage")
    mixin {
        useLegacyMixinAp.set(true)
        defaultRefmapName.set("mixins.$modid.refmap.json")
    }
}

sourceSets.main {
    java.srcDirs(rootProject.file("src/main/java"), rootProject.file("src/main/kotlin"))
    resources.srcDirs(rootProject.file("src/main/resources"))
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
}

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
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
    shadowImpl("org.java-websocket:Java-WebSocket:1.6.0") { isTransitive = false }
    shadowImpl("org.slf4j:slf4j-api:1.7.36") { isTransitive = false }
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")
    annotationProcessor("com.google.code.gson:gson:2.10.1")
    //noinspection VulnerableGradleDependency — Guava 17.0 is MC 1.8.9's bundled version
    annotationProcessor("com.google.guava:guava:17.0")

    // Kotlin standard library, must be shadowed into the JAR
    shadowImpl(kotlin("stdlib"))
    shadowImpl("com.tianscar.imageio:imageio-apng:1.0.1") { isTransitive = false }
    shadowImpl("dev.matrixlab.webp4j:webp4j-core:2.5.0") { isTransitive = false }
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.2.1")
    shadowImpl("com.google.code.gson:gson:2.10.1") { isTransitive = false }
    shadowImpl("com.fifesoft:rsyntaxtextarea:3.3.3") { isTransitive = false }
    shadowImpl("com.mojang:brigadier:1.0.18") { isTransitive = false }
    implementation("moe.nea:hotswapagent-forge:1.0.1")
}

// Tasks:

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

tasks.withType(Jar::class) {
    archiveBaseName.set(modid)
    manifest.attributes.run {
        this["Main-Class"] = "io.hamlook.aetheria.ModInstaller"
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"

        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
        this["MixinConfigs"] = "mixins.$modid.json"
        if (transformerFile.exists()) this["FMLAT"] = "${modid}_at.cfg"
    }
}

tasks.processResources {
    val versionStr = project.version.toString()
    @Suppress("REDUNDANT_CALL_OF_CONVERSION_METHOD") val mcVersionStr = mcVersion.toString()
    @Suppress("REDUNDANT_CALL_OF_CONVERSION_METHOD") val modidStr = modid.toString()
    @Suppress("REDUNDANT_CALL_OF_CONVERSION_METHOD") val baseGroupStr = baseGroup.toString()

    inputs.property("version", versionStr)
    inputs.property("mcversion", mcVersionStr)
    inputs.property("modid", modidStr)
    inputs.property("basePackage", baseGroupStr)

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    filesMatching(listOf("mcmod.info", "mixins.$modidStr.json")) {
        expand("version" to versionStr, "mcversion" to mcVersionStr, "modid" to modidStr, "basePackage" to baseGroupStr)
    }

    rename("accesstransformer.cfg", "META-INF/${modidStr}_at.cfg")
}


val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    archiveClassifier.set("")
    input.set(tasks.shadowJar.get().archiveFile)
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