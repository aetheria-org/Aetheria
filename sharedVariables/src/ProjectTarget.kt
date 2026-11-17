package io.hamlook.aetheria.sharedvariables

private fun yarn(version: String): String = "net.fabricmc:yarn:${version}:v2"

enum class ProjectTarget(
    val projectName: String,
    val minecraftVersion: MinecraftVersion,
    val mappingDependency: String,
    val mappingStyle: MappingStyle,
    val forgeDep: String? = null,
    val fabricLoaderVersion: String? = null,
    val fabricApiVersion: String? = null,
    val isModern: Boolean = false,
    linkTo: String?,
) {
    MAIN(
        "1.8.9-forge",
        MinecraftVersion.MC189,
        "de.oceanlabs.mcp:mcp_stable:22-1.8.9@zip",
        MappingStyle.SEARGE,
        forgeDep = "net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9",
        linkTo = "BRIDGE116FORGE",
    ),
    BRIDGE116FORGE(
        "1.16.5-forge",
        MinecraftVersion.MC11605,
        "official",
        MappingStyle.SEARGE,
        forgeDep = "net.minecraftforge:forge:1.16.5-36.2.39",
        linkTo = "BRIDGE116FABRIC",
    ),
    BRIDGE116FABRIC(
        "1.16.5-fabric",
        MinecraftVersion.MC11605,
        yarn("1.16.5+build.10"),
        MappingStyle.YARN,
        linkTo = "MODERN_12101FABRIC",
    ),
    MODERN_12101FABRIC(
        "1.21.1-fabric",
        MinecraftVersion.MC12101,
        yarn("1.21.1+build.3"),
        MappingStyle.YARN,
        fabricLoaderVersion = "net.fabricmc:fabric-loader:0.16.9",
        fabricApiVersion = "net.fabricmc.fabric-api:fabric-api:0.116.17+1.21.1",
        isModern = true,
        linkTo = null,
    ),
    ;

    val isForge get() = forgeDep != null
    val isFabric get() = forgeDep == null
    val isBridge get() = name.contains("BRIDGE")

    val linkTo by lazy {
        if (linkTo == null) null
        else ProjectTarget.values().find { it.name == linkTo }!!
    }

    val projectPath get() = ":$projectName"
}
