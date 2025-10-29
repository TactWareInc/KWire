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

rootProject.name = "kwire"

include(":core")
include(":obfuscation-support")
include(":ktor-integration-client")
include(":ktor-integration-common")
include(":ktor-integration-server")
include(":sample-api")
include(":sample-client")
include(":sample-server")

