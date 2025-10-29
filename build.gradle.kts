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

group = "net.tactware.kwire"
version = "1.0.4"

allprojects {

    
    repositories {
        mavenCentral()
    }
}

subprojects {
    plugins.withId("maven-publish") {
        the<org.gradle.api.publish.PublishingExtension>().repositories {
            maven {
                name = "staging"
                url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
            }
        }
    }
}



