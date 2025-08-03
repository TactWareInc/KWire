pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "KWire"

include(
    ":core",
    ":runtime", 
    ":serialization",
    ":ktor-integration",
    ":obfuscation-support",
    ":gradle-plugin",
    ":sample-server",
    ":sample-client"
)

