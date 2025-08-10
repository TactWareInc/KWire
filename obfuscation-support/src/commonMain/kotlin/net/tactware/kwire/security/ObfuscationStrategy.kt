package net.tactware.kwire.security

import kotlinx.serialization.Serializable

/**
 * Obfuscation strategies available.
 */
@Serializable
enum class ObfuscationStrategy {
    RANDOM,      // Generate completely random names
    HASH_BASED,  // Use hash of original name as base
    SEQUENTIAL   // Use sequential numbering
}
