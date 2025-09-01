import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kwire.plugin) apply false
}

// Load secrets from local.properties (not committed) if present
val localProperties: Properties by lazy {
    val props = Properties()
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { props.load(it) }
    props
}

allprojects {
    group = "net.tactware.kwire"
    version = "1.0.0-SNAPSHOT"
    
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "maven-publish")

    // Apply signing only where we will publish to Maven Central
    if (name in listOf("core", "ktor-integration-client", "ktor-integration-server")) {
        apply(plugin = "signing")
    }

    afterEvaluate {
        extensions.configure<org.gradle.api.publish.PublishingExtension> {
            publications.withType<org.gradle.api.publish.maven.MavenPublication>().configureEach {
                if (project.name in listOf("core", "ktor-integration-client", "ktor-integration-server")) {
                    pom {
                        val moduleName = when (project.name) {
                            "core" -> "KWire Core"
                            "ktor-integration-client" -> "KWire Ktor Integration Client"
                            "ktor-integration-server" -> "KWire Ktor Integration Server"
                            else -> project.name
                        }
                        name.set(moduleName)
                        description.set("KWire: Kotlin Multiplatform RPC toolkit — ${project.name} module")
                        url.set("https://github.com/TactWare/KWire")
                        licenses {
                            license {
                                name.set("The Apache License, Version 2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                                distribution.set("repo")
                            }
                        }
                        developers {
                            developer {
                                id.set("tactware")
                                name.set("TactWare")
                                url.set("https://github.com/TactWare")
                            }
                        }
                        scm {
                            url.set("https://github.com/TactWare/KWire")
                            connection.set("scm:git:https://github.com/TactWare/KWire.git")
                            developerConnection.set("scm:git:ssh://git@github.com/TactWare/KWire.git")
                        }
                    }
                }
            }
            repositories {
                // Always keep local repository for testing
                maven {
                    name = "local"
                    url = uri("$rootDir/build/repository")
                }
                // Configure Sonatype (OSSRH) only for the selected modules
                if (project.name in listOf("core", "ktor-integration-client", "ktor-integration-server")) {
                    maven {
                        name = "OSSRH"
                        val releasesUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                        val snapshotsUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                        url = if (project.version.toString().endsWith("-SNAPSHOT")) snapshotsUrl else releasesUrl
                        credentials {
                            username = localProperties.getProperty("ossrhUsername")
                                ?: findProperty("ossrhUsername") as String?
                                ?: System.getenv("OSSRH_USERNAME")
                            password = localProperties.getProperty("ossrhPassword")
                                ?: findProperty("ossrhPassword") as String?
                                ?: System.getenv("OSSRH_PASSWORD")
                        }
                    }
                }
            }
        }

        // Configure signing for the selected modules when keys are available
        if (project.name in listOf("core", "ktor-integration-client", "ktor-integration-server")) {
            extensions.findByType<org.gradle.plugins.signing.SigningExtension>()?.apply {
                val signingKeyId = localProperties.getProperty("signing.keyId")
                    ?: findProperty("signing.keyId") as String?
                val signingKey = localProperties.getProperty("signing.key")
                    ?: findProperty("signing.key") as String?
                    ?: System.getenv("SIGNING_KEY")
                val signingPassword = localProperties.getProperty("signing.password")
                    ?: findProperty("signing.password") as String?
                    ?: System.getenv("SIGNING_PASSWORD")

                if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
                    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
                    sign(extensions.getByType<org.gradle.api.publish.PublishingExtension>().publications)
                }
            }
        }
    }
}

// All dependencies are now managed through the version catalog in gradle/libs.versions.toml

