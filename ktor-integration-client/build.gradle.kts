plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    id("com.vanniktech.maven.publish") version "0.34.0"
    signing
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
                api(project(":ktor-integration-common"))

                // Ktor dependencies
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.websockets)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.kotlinx.datetime)
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
            }
        }
        
        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.websockets)
            }
        }
        
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.test.junit5)
                implementation(libs.ktor.server.test.host)
            }
        }
        
        val jsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
    }
}


mavenPublishing {
    // Configure publishing to Maven Central
    publishToMavenCentral()

    signAllPublications()

    // Configure project coordinates
    coordinates(rootProject.group as String, "ktor-integration-common", rootProject.version as String)

    // Configure POM metadata
    pom {
        name.set("KWire")
        description.set("RPC library alternative to gRPC, optimized for Kotlin Multiplatform")
        inceptionYear.set("2024")
        url.set("https://github.com/TactWareInc/KWire")

        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("kmbisset89")
                name.set("Kerry Bisset")
                url.set("https://github.com/kmbisset89")
            }
        }

        scm {
            url.set("https://github.com/TactWareInc/KWire")
            connection.set("scm:git:git://github.com/TactWareInc/KWire.git")
            developerConnection.set("scm:git:ssh://git@github.com/TactWareInc/KWire.git")
        }
    }
}

signing {
    sign(publishing.publications)
    useInMemoryPgpKeys(
        findProperty("signingKey") as String?,
        findProperty("signing.password") as String?
    )
}


tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}