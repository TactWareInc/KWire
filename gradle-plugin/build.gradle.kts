plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("java-gradle-plugin")
    id("maven-publish")
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib"))
    implementation(kotlin("gradle-plugin"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${rootProject.extra["serializationVersion"]}")
    
    // For code generation
    implementation("com.squareup:kotlinpoet:1.18.1")
    
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("obfuscatedRpc") {
            id = "com.obfuscated.rpc.plugin"
            implementationClass = "com.obfuscated.rpc.gradle.ObfuscatedRpcPlugin"
            displayName = "Obfuscated RPC Plugin"
            description = "Gradle plugin for generating obfuscated RPC stubs and mappings"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

