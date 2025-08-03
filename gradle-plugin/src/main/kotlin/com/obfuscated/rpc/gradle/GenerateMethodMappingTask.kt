package com.obfuscated.rpc.gradle

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import kotlin.random.Random

/**
 * Task for generating method ID mappings for obfuscation.
 */
@CacheableTask
abstract class GenerateMethodMappingTask : DefaultTask() {
    
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputSourceDirs: ConfigurableFileCollection
    
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    @get:Input
    abstract val obfuscationEnabled: Property<Boolean>
    
    @TaskAction
    fun generateMapping() {
        val mappingFile = outputFile.get().asFile
        mappingFile.parentFile.mkdirs()
        
        logger.info("Generating method mappings: ${mappingFile.absolutePath}")
        
        // Parse RPC services to extract methods
        val services = parseRpcServices()
        
        // Generate method mappings
        val mappings = generateMethodMappings(services)
        
        // Write mappings to JSON file
        val json = Json { prettyPrint = true }
        val jsonContent = json.encodeToString(mappings)
        mappingFile.writeText(jsonContent)
        
        logger.info("Generated ${mappings.services.size} service mappings")
    }
    
    private fun parseRpcServices(): List<RpcServiceInfo> {
        val services = mutableListOf<RpcServiceInfo>()
        
        inputSourceDirs.files.forEach { sourceDir ->
            if (sourceDir.exists() && sourceDir.isDirectory) {
                sourceDir.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .forEach { file ->
                        val serviceInfo = parseKotlinFile(file)
                        if (serviceInfo != null) {
                            services.add(serviceInfo)
                        }
                    }
            }
        }
        
        return services
    }
    
    private fun parseKotlinFile(file: File): RpcServiceInfo? {
        val content = file.readText()
        
        // Parse @RpcService annotation
        val serviceAnnotationRegex = """@RpcService(?:\([^)]*\))?\s*interface\s+(\w+)""".toRegex()
        val serviceMatch = serviceAnnotationRegex.find(content) ?: return null
        
        val serviceName = serviceMatch.groupValues[1]
        val packageName = extractPackageName(content)
        val methods = extractRpcMethods(content)
        
        return RpcServiceInfo(
            name = serviceName,
            packageName = packageName,
            methods = methods,
            sourceFile = file
        )
    }
    
    private fun extractPackageName(content: String): String {
        val packageRegex = """package\s+([\w.]+)""".toRegex()
        return packageRegex.find(content)?.groupValues?.get(1) ?: ""
    }
    
    private fun extractRpcMethods(content: String): List<RpcMethodInfo> {
        val methods = mutableListOf<RpcMethodInfo>()
        
        val methodRegex = """(?:@RpcMethod(?:\([^)]*\))?\s*)?(?:suspend\s+)?fun\s+(\w+)\s*\([^)]*\)\s*:\s*([^{]+)""".toRegex()
        
        methodRegex.findAll(content).forEach { match ->
            val methodName = match.groupValues[1]
            val returnType = match.groupValues[2].trim()
            val isStreaming = returnType.startsWith("Flow<")
            
            methods.add(RpcMethodInfo(
                name = methodName,
                returnType = returnType,
                isStreaming = isStreaming,
                isSuspend = match.value.contains("suspend")
            ))
        }
        
        return methods
    }
    
    private fun generateMethodMappings(services: List<RpcServiceInfo>): MethodMappings {
        val serviceMappings = mutableMapOf<String, Map<String, String>>()
        
        services.forEach { service ->
            val methodMappings = mutableMapOf<String, String>()
            
            service.methods.forEach { method ->
                val methodId = if (obfuscationEnabled.get()) {
                    generateObfuscatedMethodId()
                } else {
                    method.name
                }
                methodMappings[method.name] = methodId
            }
            
            serviceMappings[service.name] = methodMappings
        }
        
        return MethodMappings(serviceMappings)
    }
    
    private fun generateObfuscatedMethodId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars.random() }.joinToString("")
    }
}

/**
 * Serializable method mappings structure.
 */
@kotlinx.serialization.Serializable
data class MethodMappings(
    val services: Map<String, Map<String, String>>
)

