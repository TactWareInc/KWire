plugins {
    kotlin("multiplatform") version "2.2.0" apply false
    kotlin("plugin.serialization") version "2.2.0" apply false
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

// Common dependency versions
extra["kotlinVersion"] = "2.2.0"
extra["ktorVersion"] = "3.1.0"
extra["serializationVersion"] = "1.6.3"
extra["coroutinesVersion"] = "1.8.0"

