plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
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
    
    implementation("io.ktor:ktor-server-core:3.1.0")
    implementation("io.ktor:ktor-server-netty:3.1.0")
    implementation("io.ktor:ktor-server-content-negotiation:3.1.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.0")
    implementation("io.ktor:ktor-server-cors:3.1.0")
    implementation("io.ktor:ktor-server-websockets:3.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
    
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

kotlin {
    jvmToolchain(17)
}

