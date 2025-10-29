package net.tactware.kwire.ktor

/**
 * JSON configuration builder
 */
class JsonBuilder {
    var ignoreUnknownKeys: Boolean = true
    var isLenient: Boolean = true
    var encodeDefaults: Boolean = true
    var prettyPrint: Boolean = false
}