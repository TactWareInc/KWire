plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("java-gradle-plugin")
    id("maven-publish")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    
    // For code generation
    implementation("com.squareup:kotlinpoet:1.18.1")
    
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.2.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.2.0")
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("obfuscatedRpc") {
            id = "net.tactware.kwire.plugin"
            implementationClass = "net.tactware.kwire.gradle.ObfuscatedRpcPlugin"
            displayName = "KWire RPC Plugin"
            description = "Gradle plugin for RPC routings"
        }
    }
}

// Enable local plugin usage
group = "com.obfuscated.rpc"
version = "1.0.0-SNAPSHOT"

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

