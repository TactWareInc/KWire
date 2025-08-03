package com.obfuscated.rpc.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

/**
 * Gradle task for generating RPC client and server stubs.
 */
@CacheableTask
abstract class GenerateRpcStubsTask : DefaultTask() {
    
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputSourceDirs: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @get:Input
    abstract val obfuscationEnabled: Property<Boolean>
    
    @get:Input
    abstract val generateClient: Property<Boolean>
    
    @get:Input
    abstract val generateServer: Property<Boolean>
    
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val methodMappingFile: Property<File>
    
    @TaskAction
    fun generateStubs() {
        val outputDirectory = outputDir.get().asFile
        outputDirectory.mkdirs()
        
        logger.info("Generating RPC stubs in: ${outputDirectory.absolutePath}")
        logger.info("Obfuscation enabled: ${obfuscationEnabled.get()}")
        logger.info("Generate client: ${generateClient.get()}")
        logger.info("Generate server: ${generateServer.get()}")
        
        // Parse source files to find RPC service interfaces
        val rpcServices = parseRpcServices()
        
        // Generate client stubs if requested
        if (generateClient.get()) {
            generateClientStubs(rpcServices, outputDirectory)
        }
        
        // Generate server stubs if requested
        if (generateServer.get()) {
            generateServerStubs(rpcServices, outputDirectory)
        }
        
        // Generate service registry
        generateServiceRegistry(rpcServices, outputDirectory)
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
        
        // Simple regex-based parsing for @RpcService annotation
        // In a real implementation, this would use a proper Kotlin parser
        val serviceAnnotationRegex = """@RpcService(?:\([^)]*\))?\s*interface\s+(\w+)""".toRegex()
        val serviceMatch = serviceAnnotationRegex.find(content) ?: return null
        
        val serviceName = serviceMatch.groupValues[1]
        val packageName = extractPackageName(content)
        
        // Extract methods
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
        
        // Extract suspend functions and Flow-returning functions
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
    
    private fun generateClientStubs(services: List<RpcServiceInfo>, outputDir: File) {
        services.forEach { service ->
            val clientStub = RpcClientStubGenerator(service, obfuscationEnabled.get()).generate()
            
            val packageDir = File(outputDir, service.packageName.replace('.', '/'))
            packageDir.mkdirs()
            
            val clientFile = File(packageDir, "${service.name}Client.kt")
            clientFile.writeText(clientStub)
            
            logger.info("Generated client stub: ${clientFile.absolutePath}")
        }
    }
    
    private fun generateServerStubs(services: List<RpcServiceInfo>, outputDir: File) {
        services.forEach { service ->
            val serverStub = RpcServerStubGenerator(service, obfuscationEnabled.get()).generate()
            
            val packageDir = File(outputDir, service.packageName.replace('.', '/'))
            packageDir.mkdirs()
            
            val serverFile = File(packageDir, "${service.name}Server.kt")
            serverFile.writeText(serverStub)
            
            logger.info("Generated server stub: ${serverFile.absolutePath}")
        }
    }
    
    private fun generateServiceRegistry(services: List<RpcServiceInfo>, outputDir: File) {
        val registryGenerator = ServiceRegistryGenerator(services, obfuscationEnabled.get())
        val registryCode = registryGenerator.generate()
        
        val registryFile = File(outputDir, "GeneratedServiceRegistry.kt")
        registryFile.writeText(registryCode)
        
        logger.info("Generated service registry: ${registryFile.absolutePath}")
    }
}

/**
 * Information about an RPC service parsed from source code.
 */
data class RpcServiceInfo(
    val name: String,
    val packageName: String,
    val methods: List<RpcMethodInfo>,
    val sourceFile: File
)

/**
 * Information about an RPC method.
 */
data class RpcMethodInfo(
    val name: String,
    val returnType: String,
    val isStreaming: Boolean,
    val isSuspend: Boolean
)

