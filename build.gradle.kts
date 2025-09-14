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
version = "1.0.0"

allprojects {
    group = "net.tactware.kwire"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
    }
}

//nexusPublishing {
//    repositories {
//        sonatype {
//            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
//            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
//            username.set(findProperty("sonatypeUsername") as String)
//            password.set(findProperty("sonatypePassword") as String)
//        }
//    }
//}


