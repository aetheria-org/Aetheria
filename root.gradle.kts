plugins {
    id("gg.essential.multi-version.root")
    id("gg.essential.loom") version "1.15.50" apply false
    kotlin("jvm") version "2.3.20" apply false
}

group = "io.hamlook.aetheria"
version = "1.4.0-alpha"

preprocess {
    strictExtraMappings.set(true)
    val mc10809 = createNode("1.8.9-forge", 10809, "srg")
}
