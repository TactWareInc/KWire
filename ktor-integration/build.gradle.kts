plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvmToolchain(17)
    
    jvm()
    
    js(IR) {
        browser()
        nodejs()
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":core"))
                api(project(":runtime"))
                api(project(":serialization"))
                api(project(":obfuscation-support"))
                
                // Ktor dependencies
                implementation("io.ktor:ktor-server-core:${rootProject.extra["ktorVersion"]}")
                implementation("io.ktor:ktor-client-core:${rootProject.extra["ktorVersion"]}")
                implementation("io.ktor:ktor-client-content-negotiation:${rootProject.extra["ktorVersion"]}")
                implementation("io.ktor:ktor-serialization-kotlinx-json:${rootProject.extra["ktorVersion"]}")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${rootProject.extra["serializationVersion"]}")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${rootProject.extra["coroutinesVersion"]}")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${rootProject.extra["coroutinesVersion"]}")
                implementation("io.ktor:ktor-server-test-host:${rootProject.extra["ktorVersion"]}")
                implementation("io.ktor:ktor-client-mock:${rootProject.extra["ktorVersion"]}")
            }
        }
        
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-server-netty:${rootProject.extra["ktorVersion"]}")
                implementation("io.ktor:ktor-client-cio:${rootProject.extra["ktorVersion"]}")
            }
        }
        
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("io.ktor:ktor-server-test-host:${rootProject.extra["ktorVersion"]}")
            }
        }
        
        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:${rootProject.extra["ktorVersion"]}")
            }
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}

