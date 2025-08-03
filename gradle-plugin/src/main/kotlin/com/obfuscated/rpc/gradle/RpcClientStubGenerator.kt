package com.obfuscated.rpc.gradle

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

/**
 * Generator for RPC client stubs.
 */
class RpcClientStubGenerator(
    private val serviceInfo: RpcServiceInfo,
    private val obfuscationEnabled: Boolean
) {
    
    fun generate(): String {
        val fileSpec = FileSpec.builder(serviceInfo.packageName, "${serviceInfo.name}Client")
            .addImport("com.obfuscated.rpc.runtime", "RpcClient")
            .addImport("com.obfuscated.rpc.core", "RpcServiceMetadata", "RpcMethodMetadata")
            .addImport("kotlinx.coroutines.flow", "Flow")
            .addImport("kotlinx.serialization", "KSerializer")
            .addImport("kotlinx.serialization.serializer")
            .addType(generateClientClass())
            .build()
        
        return fileSpec.toString()
    }
    
    private fun generateClientClass(): TypeSpec {
        val clientClassName = "${serviceInfo.name}Client"
        
        return TypeSpec.classBuilder(clientClassName)
            .addSuperinterface(ClassName(serviceInfo.packageName, serviceInfo.name))
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("rpcClient", ClassName("com.obfuscated.rpc.runtime", "RpcClient"))
                    .build()
            )
            .addProperty(
                PropertySpec.builder("rpcClient", ClassName("com.obfuscated.rpc.runtime", "RpcClient"))
                    .initializer("rpcClient")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addProperty(generateServiceMetadataProperty())
            .addFunctions(serviceInfo.methods.map { generateMethodImplementation(it) })
            .build()
    }
    
    private fun generateServiceMetadataProperty(): PropertySpec {
        val methodMetadataList = serviceInfo.methods.map { method ->
            val methodId = if (obfuscationEnabled) generateMethodId(method.name) else method.name
            
            CodeBlock.of(
                "RpcMethodMetadata(%S, %S, %L, emptyList(), %S)",
                method.name,
                methodId,
                method.isStreaming,
                method.returnType
            )
        }
        
        val metadataInitializer = CodeBlock.builder()
            .add("RpcServiceMetadata(\n")
            .indent()
            .add("serviceName = %S,\n", serviceInfo.name)
            .add("methods = listOf(\n")
            .indent()
            .add(methodMetadataList.joinToString(",\n") { it.toString() })
            .unindent()
            .add("\n),\n")
            .add("obfuscated = %L\n", obfuscationEnabled)
            .unindent()
            .add(")")
            .build()
        
        return PropertySpec.builder(
            "serviceMetadata",
            ClassName("com.obfuscated.rpc.core", "RpcServiceMetadata")
        )
            .addModifiers(KModifier.OVERRIDE)
            .initializer(metadataInitializer)
            .build()
    }
    
    private fun generateMethodImplementation(method: RpcMethodInfo): FunSpec {
        val funSpecBuilder = FunSpec.builder(method.name)
            .addModifiers(KModifier.OVERRIDE)
        
        if (method.isSuspend) {
            funSpecBuilder.addModifiers(KModifier.SUSPEND)
        }
        
        // Parse return type
        val returnType = parseReturnType(method.returnType)
        funSpecBuilder.returns(returnType)
        
        // Generate method body
        val methodId = if (obfuscationEnabled) generateMethodId(method.name) else method.name
        
        if (method.isStreaming) {
            // Generate streaming method implementation
            funSpecBuilder.addCode(
                CodeBlock.of(
                    "return rpcClient.stream(%S, %S, emptyList(), serializer())",
                    serviceInfo.name,
                    methodId
                )
            )
        } else {
            // Generate regular method implementation
            funSpecBuilder.addCode(
                CodeBlock.of(
                    "return rpcClient.call(%S, %S, emptyList(), serializer())",
                    serviceInfo.name,
                    methodId
                )
            )
        }
        
        return funSpecBuilder.build()
    }
    
    private fun parseReturnType(returnTypeString: String): TypeName {
        return when {
            returnTypeString.startsWith("Flow<") -> {
                val innerType = returnTypeString.removePrefix("Flow<").removeSuffix(">")
                ClassName("kotlinx.coroutines.flow", "Flow").parameterizedBy(
                    parseSimpleType(innerType)
                )
            }
            else -> parseSimpleType(returnTypeString)
        }
    }
    
    private fun parseSimpleType(typeString: String): TypeName {
        return when (typeString.trim()) {
            "String" -> STRING
            "Int" -> INT
            "Long" -> LONG
            "Boolean" -> BOOLEAN
            "Unit" -> UNIT
            else -> ClassName.bestGuess(typeString.trim())
        }
    }
    
    private fun generateMethodId(methodName: String): String {
        // In a real implementation, this would use the same algorithm as the mapping task
        // For now, we'll use a simple hash-based approach
        return if (obfuscationEnabled) {
            "m${methodName.hashCode().toString(36).take(6)}"
        } else {
            methodName
        }
    }
}

