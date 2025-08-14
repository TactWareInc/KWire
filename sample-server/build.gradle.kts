plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("net.tactware.kwire.plugin")
    application
}

application {
    mainClass.set("com.obfuscated.rpc.sample.server.ServerMainKt")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":obfuscation-support"))
    implementation(project(":ktor-integration-server"))
    implementation(project(":sample-api"))
    
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.websockets)
    implementation(libs.kotlinx.datetime)
    
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    
    implementation(libs.logback.classic)
}

kotlin {
    jvmToolchain(17)
}

obfuscatedRpc {
    this.apiSourcePath = rootProject.projectDir.resolve("sample-api/src/main/kotlin").absolutePath
    this.serverSourcePath = rootProject.projectDir.resolve("sample-server/src/main/kotlin").absolutePath
}

