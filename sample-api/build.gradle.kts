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
    implementation(project(":runtime"))
    implementation(project(":serialization"))
    implementation(project(":obfuscation-support"))
    implementation(project(":ktor-integration"))
    
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.websockets)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.atomicfu)
    
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    
    implementation(libs.logback.classic)
}

//// Configure the local RPC plugin
//obfuscatedRpc {
//    obfuscationEnabled.set(false) // Disable obfuscation for cleaner generated code
//    generateClient.set(true)      // Enable client generation
//    generateServer.set(false)     // We don't need server stubs in the API package
//}


kotlin {
    jvmToolchain(17)
}

