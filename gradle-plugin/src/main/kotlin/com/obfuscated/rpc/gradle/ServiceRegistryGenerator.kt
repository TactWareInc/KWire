package com.obfuscated.rpc.gradle

import com.squareup.kotlinpoet.*

/**
 * Generator for service registry code.
 */
class ServiceRegistryGenerator(
    private val services: List<RpcServiceInfo>,
    private val obfuscationEnabled: Boolean
) {
    
    fun generate(): String {
        val fileSpec = FileSpec.builder("", "GeneratedServiceRegistry")
            .addImport("com.obfuscated.rpc.runtime", "RpcClient", "RpcServer", "MethodRegistry")
            .addImport("com.obfuscated.rpc.core", "RpcTransport")
            .addType(generateRegistryClass())
            .build()
        
        return fileSpec.toString()
    }
    
    private fun generateRegistryClass(): TypeSpec {
        return TypeSpec.objectBuilder("GeneratedServiceRegistry")
            .addFunction(generateMethodRegistryFunction())
            .addFunction(generateClientFactoryFunction())
            .addFunction(generateServerRegistrationFunction())
            .build()
    }
    
    private fun generateMethodRegistryFunction(): FunSpec {
        val codeBuilder = CodeBlock.builder()
            .add("return methodRegistry {\n")
            .indent()
        
        services.forEach { service ->
            codeBuilder.add("service(%S) {\n", service.name)
            codeBuilder.indent()
            
            service.methods.forEach { method ->
                val methodId = if (obfuscationEnabled) generateMethodId(method.name) else method.name
                codeBuilder.add("method(%S, %S)\n", method.name, methodId)
            }
            
            codeBuilder.unindent()
            codeBuilder.add("}\n")
        }
        
        codeBuilder.unindent()
        codeBuilder.add("}")
        
        return FunSpec.builder("createMethodRegistry")
            .returns(ClassName("com.obfuscated.rpc.runtime", "MethodRegistry"))
            .addCode(codeBuilder.build())
            .build()
    }
    
    private fun generateClientFactoryFunction(): FunSpec {
        val codeBuilder = CodeBlock.builder()
            .add("val rpcClient = RpcClient(transport)\n")
            .add("return object {\n")
            .indent()
        
        services.forEach { service ->
            val clientClassName = "${service.name}Client"
            val serviceInterface = ClassName(service.packageName, service.name)
            
            codeBuilder.add(
                "val %L: %T = %L(rpcClient)\n",
                service.name.replaceFirstChar { it.lowercase() },
                serviceInterface,
                clientClassName
            )
        }
        
        codeBuilder.unindent()
        codeBuilder.add("}")
        
        return FunSpec.builder("createClients")
            .addParameter("transport", ClassName("com.obfuscated.rpc.core", "RpcTransport"))
            .returns(ANY)
            .addCode(codeBuilder.build())
            .build()
    }
    
    private fun generateServerRegistrationFunction(): FunSpec {
        val codeBuilder = CodeBlock.builder()
        
        services.forEach { service ->
            val registrationFunction = "register${service.name}"
            codeBuilder.add("%L(server, services.%L)\n", 
                registrationFunction,
                service.name.replaceFirstChar { it.lowercase() }
            )
        }
        
        val parametersBuilder = FunSpec.builder("registerAllServices")
            .addParameter("server", ClassName("com.obfuscated.rpc.runtime", "RpcServer"))
        
        // Add service implementation parameters
        services.forEach { service ->
            parametersBuilder.addParameter(
                service.name.replaceFirstChar { it.lowercase() },
                ClassName(service.packageName, service.name)
            )
        }
        
        // Create services object parameter
        val servicesObjectCode = CodeBlock.builder()
            .add("val services = object {\n")
            .indent()
        
        services.forEach { service ->
            servicesObjectCode.add(
                "val %L = %L\n",
                service.name.replaceFirstChar { it.lowercase() },
                service.name.replaceFirstChar { it.lowercase() }
            )
        }
        
        servicesObjectCode.unindent()
            .add("}\n")
            .add(codeBuilder.build())
        
        return parametersBuilder
            .addCode(servicesObjectCode.build())
            .build()
    }
    
    private fun generateMethodId(methodName: String): String {
        // Use consistent method ID generation
        return if (obfuscationEnabled) {
            "m${methodName.hashCode().toString(36).take(6)}"
        } else {
            methodName
        }
    }
}

