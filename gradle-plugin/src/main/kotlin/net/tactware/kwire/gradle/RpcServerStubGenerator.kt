package net.tactware.kwire.gradle

import com.squareup.kotlinpoet.*

/**
 * Generator for RPC server stubs.
 */
class RpcServerStubGenerator {
    
    fun generateServerStub(serviceName: String, interfaceName: String, packageName: String, obfuscationEnabled: Boolean): String {
        val fileSpec = FileSpec.builder(packageName, "${interfaceName}Server")
            .addImport("com.obfuscated.rpc.runtime", "RpcServer")
            .addImport("com.obfuscated.rpc.core", "RpcServiceMetadata", "RpcMethodMetadata")
            .addType(generateServerClass(serviceName, interfaceName, packageName, obfuscationEnabled))
            .build()
        
        return fileSpec.toString()
    }
    
    private fun generateServerClass(serviceName: String, interfaceName: String, packageName: String, obfuscationEnabled: Boolean): TypeSpec {
        val serverClassName = "${interfaceName}Server"
        
        return TypeSpec.classBuilder(serverClassName)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("implementation", ClassName(packageName, interfaceName))
                    .build()
            )
            .addProperty(
                PropertySpec.builder("implementation", ClassName(packageName, interfaceName))
                    .initializer("implementation")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addProperty(generateServiceMetadataProperty(serviceName, obfuscationEnabled))
            .build()
    }
    
    private fun generateServiceMetadataProperty(serviceName: String, obfuscationEnabled: Boolean): PropertySpec {
        val metadataInitializer = CodeBlock.builder()
            .add("RpcServiceMetadata(\n")
            .indent()
            .add("serviceName = %S,\n", serviceName)
            .add("methods = emptyMap(),\n") // Simplified for now
            .add("obfuscated = %L\n", obfuscationEnabled)
            .unindent()
            .add(")")
            .build()
        
        return PropertySpec.builder(
            "serviceMetadata",
            ClassName("com.obfuscated.rpc.core", "RpcServiceMetadata")
        )
            .initializer(metadataInitializer)
            .build()
    }
}

