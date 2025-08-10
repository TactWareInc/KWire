pluginManagement {
    includeBuild("gradle-plugin") {
        name = "kwire-gradle-plugin"
    }
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

rootProject.name = "kwire"

include(":core")
include(":obfuscation-support")
include(":ktor-integration")
include(":gradle-plugin")
include(":sample-api")
include(":sample-client")
include(":sample-server")

