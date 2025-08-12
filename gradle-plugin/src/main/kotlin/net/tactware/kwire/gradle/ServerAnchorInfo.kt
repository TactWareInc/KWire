package net.tactware.kwire.gradle

data class ServerAnchorInfo(
    val packageName: String,
    val abstractClassName: String,
    val interfaceFqn: String,
    val explicitServiceName: String?,
    val generateFactory: Boolean
)
