plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
    id("org.jreleaser") version "1.20.0"

}

kotlin {
    jvmToolchain(17)
    
    jvm()

    
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":core"))

                // Ktor dependencies
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.websockets)
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
                implementation(libs.ktor.server.test.host)
                implementation(libs.ktor.client.mock)
            }
        }
        
        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.server.netty)
                implementation(libs.ktor.server.websockets)
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
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {

        version = rootProject.version as String

        pom {
            name.set("KWire")
            description.set("RPC library alternative to gRPC, optimized for Kotlin Multiplatform")
            url.set("https://github.com/TactWareInc/KWire") // or site

            licenses {
                license {
                    name.set("Apache-2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("kmbisset89")
                    name.set("Kerry Bisset")
                    email.set("kerry.bisset@tactware.net")
                }
            }
            scm {
                url.set("https://github.com/TactWareInc/KWire")
            }
        }
    }
}

jreleaser {
    release{
        github{
            this.name.set("KWire")
            commitAuthor {
                name.set("Kerry Bisset")
                email.set("kerry.bisset@tactware.net")
            }
            tagName.set("v${project.version}")
        }
    }
    signing{
        setActive("ALWAYS")
        this.armored.set(true)
        this.secretKey.set(findProperty("signing.key") as String?)
        this.publicKey.set(findProperty("signing.publickey") as String?)
    }
    deploy{
        maven {
            mavenCentral {
                create("sonatype") {
                    setActive("ALWAYS")
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository("target/staging-deploy")
                    this.password.set(findProperty("sonatypePassword") as String?)
                    this.username.set(findProperty("sonatypeUsername") as String?)
                }
            }
        }
    }
}


tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}

