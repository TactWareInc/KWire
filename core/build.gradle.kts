import org.jreleaser.model.internal.common.CommitAuthor

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
    id("org.jreleaser") version "1.20.0"

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
                api(project(":obfuscation-support"))
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        
        val jvmMain by getting {
            dependencies {
                // JVM-specific dependencies if needed
            }
        }
        
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.test.junit5)
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

tasks.named<Test>("jvmTest") {
    this.useJUnitPlatform()
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
        this.passphrase.set(findProperty("signing.password") as String?)

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
