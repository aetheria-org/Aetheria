plugins {
    id("gg.essential.multi-version.root")
    id("gg.essential.loom") version "1.15.50" apply false
    kotlin("jvm") version "2.0.10" apply false
}

group = "io.hamlook.aetheria"

preprocess {
    strictExtraMappings.set(false)
    val mc10809 = createNode("1.8.9-forge", 10809, "srg")
    val mc11605Forge = createNode("1.16.5-forge", 11605, "srg")
    val mc11605Fabric = createNode("1.16.5-fabric", 11605, "yarn")
    val mc12101Fabric = createNode("1.21.1-fabric", 12101, "yarn")

    mc11605Forge.link(mc10809, file("versions/mapping-1.16.5-forge-1.8.9.txt"))
    mc11605Fabric.link(mc11605Forge, file("versions/mapping-1.16.5-fabric-1.16.5-forge.txt"))
    mc12101Fabric.link(mc11605Fabric, file("versions/mapping-1.21.1-fabric-1.16.5-fabric.txt"))
}
