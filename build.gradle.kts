plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

allprojects {
    group = "com.obfuscated.rpc"
    version = "1.0.0-SNAPSHOT"
    
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "maven-publish")
    
    afterEvaluate {
        extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    name = "local"
                    url = uri("$rootDir/build/repository")
                }
            }
        }
    }
}

// All dependencies are now managed through the version catalog in gradle/libs.versions.toml

