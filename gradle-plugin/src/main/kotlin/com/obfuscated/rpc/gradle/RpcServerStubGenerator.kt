package com.obfuscated.rpc.gradle

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

/**
 * Generator for RPC server stubs.
 */
class RpcServerStubGenerator(
    private val serviceInfo: RpcServiceInfo,
    private val obfuscationEnabled: Boolean
) {
    
    fun generate(): String {
        val fileSpec = FileSpec.builder(serviceInfo.packageName, "${serviceInfo.name}Server")
            .addImport("com.obfuscated.rpc.runtime", "RpcServer")
            .addImport("com.obfuscated.rpc.core", "RpcServiceMetadata", "RpcMethodMetadata")
            .addImport("kotlinx.coroutines.flow", "Flow")
            .addImport("kotlinx.serialization.json", "JsonElement", "decodeFromJsonElement", "encodeToJsonElement")
            .addImport("kotlinx.serialization", "serializer")
            .addType(generateServerHandlerClass())
            .addFunction(generateRegistrationFunction())
            .build()
        
        return fileSpec.toString()
    }
    
    private fun generateServerHandlerClass(): TypeSpec {
        val handlerClassName = "${serviceInfo.name}ServerHandler"
        
        return TypeSpec.classBuilder(handlerClassName)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("serviceImpl", ClassName(serviceInfo.packageName, serviceInfo.name))
                    .build()
            )
            .addProperty(
                PropertySpec.builder("serviceImpl", ClassName(serviceInfo.packageName, serviceInfo.name))
                    .initializer("serviceImpl")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addProperty(generateServiceMetadataProperty())
            .addFunction(generateInvokeMethod())
            .addFunction(generateInvokeStreamMethod())
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
            "metadata",
            ClassName("com.obfuscated.rpc.core", "RpcServiceMetadata")
        )
            .initializer(metadataInitializer)
            .build()
    }
    
    private fun generateInvokeMethod(): FunSpec {
        val codeBuilder = CodeBlock.builder()
            .add("return when (methodId) {\n")
            .indent()
        
        serviceInfo.methods.filter { !it.isStreaming }.forEach { method ->
            val methodId = if (obfuscationEnabled) generateMethodId(method.name) else method.name
            
            codeBuilder.add("%S -> {\n", methodId)
            codeBuilder.indent()
            
            if (method.isSuspend) {
                codeBuilder.add("val result = serviceImpl.%L()\n", method.name)
            } else {
                codeBuilder.add("val result = serviceImpl.%L()\n", method.name)
            }
            
            codeBuilder.add("result\n")
            codeBuilder.unindent()
            codeBuilder.add("}\n")
        }
        
        codeBuilder.add("else -> throw MethodNotFoundException(%S, methodId)\n", serviceInfo.name)
        codeBuilder.unindent()
        codeBuilder.add("}")
        
        return FunSpec.builder("invoke")
            .addModifiers(KModifier.SUSPEND)
            .addParameter("methodId", STRING)
            .addParameter("parameters", LIST.parameterizedBy(ClassName("kotlinx.serialization.json", "JsonElement")))
            .returns(ANY.copy(nullable = true))
            .addCode(codeBuilder.build())
            .build()
    }
    
    private fun generateInvokeStreamMethod(): FunSpec {
        val codeBuilder = CodeBlock.builder()
            .add("return when (methodId) {\n")
            .indent()
        
        serviceInfo.methods.filter { it.isStreaming }.forEach { method ->
            val methodId = if (obfuscationEnabled) generateMethodId(method.name) else method.name
            
            codeBuilder.add("%S -> {\n", methodId)
            codeBuilder.indent()
            codeBuilder.add("serviceImpl.%L()\n", method.name)
            codeBuilder.unindent()
            codeBuilder.add("}\n")
        }
        
        if (serviceInfo.methods.none { it.isStreaming }) {
            codeBuilder.add("else -> throw MethodNotFoundException(%S, methodId)\n", serviceInfo.name)
        } else {
            codeBuilder.add("else -> throw MethodNotFoundException(%S, methodId)\n", serviceInfo.name)
        }
        
        codeBuilder.unindent()
        codeBuilder.add("}")
        
        return FunSpec.builder("invokeStream")
            .addModifiers(KModifier.SUSPEND)
            .addParameter("methodId", STRING)
            .addParameter("parameters", LIST.parameterizedBy(ClassName("kotlinx.serialization.json", "JsonElement")))
            .returns(ClassName("kotlinx.coroutines.flow", "Flow").parameterizedBy(ANY))
            .addCode(codeBuilder.build())
            .build()
    }
    
    private fun generateRegistrationFunction(): FunSpec {
        return FunSpec.builder("register${serviceInfo.name}")
            .addParameter("server", ClassName("com.obfuscated.rpc.runtime", "RpcServer"))
            .addParameter("serviceImpl", ClassName(serviceInfo.packageName, serviceInfo.name))
            .addCode(
                CodeBlock.of(
                    "val handler = %L(serviceImpl)\nserver.registerService(%T::class, serviceImpl, handler.metadata)",
                    "${serviceInfo.name}ServerHandler",
                    ClassName(serviceInfo.packageName, serviceInfo.name)
                )
            )
            .build()
    }
    
    private fun generateMethodId(methodName: String): String {
        // Use the same algorithm as the client stub generator
        return if (obfuscationEnabled) {
            "m${methodName.hashCode().toString(36).take(6)}"
        } else {
            methodName
        }
    }
}

