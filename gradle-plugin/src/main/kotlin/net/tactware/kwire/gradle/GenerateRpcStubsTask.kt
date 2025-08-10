package net.tactware.kwire.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

/**
 * Gradle task for generating RPC client and server stubs using annotation parsing.
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

    @TaskAction
    fun generateStubs() {
        val outputDirectory = outputDir.get().asFile
        outputDirectory.deleteRecursively()
        outputDirectory.mkdirs()

        logger.lifecycle("Generating RPC stubs in: ${outputDirectory.absolutePath}")
        logger.lifecycle("Obfuscation enabled: ${obfuscationEnabled.get()}")
        logger.lifecycle("Generate client: ${generateClient.get()}")
        logger.lifecycle("Generate server: ${generateServer.get()}")

        // Debug: List all source files being processed
        val sourceFiles = collectSourceFiles()
        logger.lifecycle("Processing ${sourceFiles.files.size} source files:")
        sourceFiles.files.forEach { file ->
            logger.lifecycle("  - ${file.absolutePath}")
        }

        try {
            // Parse source files to find @RpcService annotated interfaces
            val parser = AnnotationParser()
            val services = parser.parseServices(sourceFiles)

            if (services.isEmpty()) {
                logger.warn("No @RpcService annotated interfaces found in source files")
                logger.warn("Checked files:")
                sourceFiles.files.forEach { file ->
                    logger.warn("  - ${file.name}: ${if (file.readText().contains("@RpcService")) "HAS @RpcService" else "no @RpcService"}")
                }
                return
            }

            logger.lifecycle("Found ${services.size} RPC services: ${services.map { "${it.interfaceName} (${it.serviceName})" }}")

            // Validate services before processing
            services.forEach { service ->
                logger.lifecycle("Validating service: ${service.interfaceName} -> ${service.serviceName}")
                logger.lifecycle("  Package: ${service.packageName}")
                logger.lifecycle("  Methods (${service.methods.size}):")
                
                service.methods.forEach { method ->
                    logger.lifecycle("    - ${method.methodName} -> ${method.rpcMethodId}")
                    logger.lifecycle("      Return type: ${method.returnType} (streaming: ${method.isStreaming})")
                    logger.lifecycle("      Parameters: ${method.parameters.map { "${it.name}: ${it.type}" }}")
                }
                
                // Verify that the interface name is valid
                if (!service.interfaceName.matches(Regex("""\w+"""))) {
                    throw IllegalArgumentException("Invalid interface name: ${service.interfaceName}")
                }
            }

            services.forEach { service ->
                logger.lifecycle("Generating code for service: ${service.interfaceName} -> ${service.serviceName}")

                try {
                    if (generateClient.get()) {
                        generateCleanApiClient(service, outputDirectory)
                    }

                    if (generateServer.get()) {
//                        generateServerStub(service, outputDirectory)
                    }
                } catch (e: Exception) {
                    logger.error("Error generating code for service ${service.interfaceName}: ${e.message}")
                    e.printStackTrace()
                    throw e
                }
            }

            if (generateClient.get() && services.isNotEmpty()) {
                try {
//                    generateApiClientFactory(services, outputDirectory)
                } catch (e: Exception) {
                    logger.error("Error generating API client factory: ${e.message}")
                    e.printStackTrace()
                    throw e
                }
            }
        } catch (e: Exception) {
            logger.error("Error generating RPC stubs: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun collectSourceFiles(): org.gradle.api.file.FileCollection {
        val project = this.project

        // Debug: Check what inputSourceDirs contains
        logger.info("Input source dirs: ${inputSourceDirs.files}")

        val allFiles = project.files()

        inputSourceDirs.files.forEach { sourceDir ->
            logger.info("Checking source dir: ${sourceDir.absolutePath}")
            if (sourceDir.exists() && sourceDir.isDirectory) {
                val kotlinFiles = project.fileTree(sourceDir) {
                    it.include("**/*.kt")
                }
                logger.info("Found ${kotlinFiles.files.size} Kotlin files in ${sourceDir.name}")
                kotlinFiles.files.forEach { file ->
                    logger.info("  - ${file.absolutePath}")
                }
                allFiles.from(kotlinFiles)
            } else {
                logger.warn("Source dir does not exist or is not a directory: ${sourceDir.absolutePath}")
            }
        }

        return allFiles
    }



//    private fun generateServerStub(service: ServiceInfo, outputDir: File) {
//        val generator = RpcServerStubGenerator()
//        val content = generator.generateServerStub(
//            serviceName = service.serviceName,
//            interfaceName = service.interfaceName,
//            packageName = service.packageName,
//            obfuscationEnabled = obfuscationEnabled.get()
//        )
//
//        val packageDir = File(outputDir, service.packageName.replace('.', '/'))
//        packageDir.mkdirs()
//        val outputFile = File(packageDir, "${service.interfaceName}Server.kt")
//        outputFile.writeText(content)
//
//        logger.info("Generated server stub: ${outputFile.relativeTo(outputDir)}")
//    }

    private fun generateCleanApiClient(service: ServiceInfo, outputDir: File) {
        val generator = ServiceClientGenerator()
        val content = generator.generate(service)

        val packageDir = File(outputDir, service.packageName.replace('.', '/'))
        packageDir.mkdirs()
        val outputFile = File(packageDir, "${service.interfaceName}ClientImpl.kt")
        outputFile.writeText(content)

        logger.info("Generated clean API client: ${outputFile.relativeTo(outputDir)}")
    }

//    private fun generateApiClientFactory(services: List<ServiceInfo>, outputDir: File) {
//        val generator = ApiClientFactoryGenerator()
//        val content = generator.generateApiClientFactory(services)
//
//        // Use a common generated package for the factory
//        val packageDir = File(outputDir, "com/obfuscated/rpc/generated")
//        packageDir.mkdirs()
//        val outputFile = File(packageDir, "GeneratedApiClient.kt")
//        outputFile.writeText(content)
//
//        logger.info("Generated API client factory: ${outputFile.relativeTo(outputDir)}")
//    }
}
