plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("java-gradle-plugin")
    id("maven-publish")
}

dependencies {
    implementation(gradleApi())
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlinx.serialization.json)
    
    // For code generation
    implementation(libs.kotlinpoet)
    
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit5)
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

