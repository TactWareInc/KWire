package net.tactware.kwire.gradle

import com.squareup.kotlinpoet.ClassName

data class GenerationTarget(
    val packageName: String,
    val superType: ClassName,   // anchor base OR the service interface
    val returnType: ClassName,  // what the factory should return
    val clientSimpleName: String,
    val emitFactory: Boolean
)