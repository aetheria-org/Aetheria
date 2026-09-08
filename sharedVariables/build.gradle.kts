plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

sourceSets.main {
    kotlin.srcDir(file("src"))
}

gradlePlugin {
    plugins {
        create("aetheriaShared") {
            id = "io.hamlook.aetheria.shared-variables"
            implementationClass = "io.hamlook.aetheria.sharedvariables.NoOp"
        }
    }
}
