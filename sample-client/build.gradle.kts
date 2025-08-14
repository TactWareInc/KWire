plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("net.tactware.kwire.plugin")
    application
}

application {
    mainClass.set("com.obfuscated.rpc.sample.client.GeneratedApiClientExampleKt")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":obfuscation-support"))
    implementation(project(":ktor-integration-client"))
    implementation(project(":sample-api")) // Add dependency on sample-api for generated code
    
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.websockets)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.atomicfu)
    
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    
    implementation(libs.logback.classic)
}

obfuscatedRpc {
    this.apiSourcePath = "../sample-api/src/main/kotlin"
    this.clientSourcePath = "src/main/kotlin"
}