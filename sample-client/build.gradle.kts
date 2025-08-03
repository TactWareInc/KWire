plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("com.obfuscated.rpc.sample.client.ClientMainKt")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":runtime"))
    implementation(project(":serialization"))
    implementation(project(":obfuscation-support"))
    implementation(project(":ktor-integration"))
    
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.websockets)
    
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    
    implementation(libs.logback.classic)
}

kotlin {
    jvmToolchain(17)
}

