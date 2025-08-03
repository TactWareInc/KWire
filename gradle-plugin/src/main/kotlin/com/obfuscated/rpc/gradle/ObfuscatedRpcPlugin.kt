package com.obfuscated.rpc.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Main Gradle plugin for Obfuscated RPC code generation.
 */
class ObfuscatedRpcPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        // Create extension for plugin configuration
        val extension = project.extensions.create("obfuscatedRpc", ObfuscatedRpcExtension::class.java)
        
        // Register code generation tasks
        registerCodeGenerationTasks(project, extension)
        
        // Configure dependencies
        configureDependencies(project)
    }
    
    private fun registerCodeGenerationTasks(project: Project, extension: ObfuscatedRpcExtension) {
        // Register the main code generation task
        val generateStubsTask = project.tasks.register("generateRpcStubs", GenerateRpcStubsTask::class.java) { task ->
            task.group = "obfuscated-rpc"
            task.description = "Generate RPC client and server stubs"
            
            // Configure task properties using afterEvaluate to ensure extension is configured
            project.afterEvaluate {
                task.obfuscationEnabled.set(extension.obfuscationEnabled.getOrElse(true))
                task.generateClient.set(extension.generateClient.getOrElse(true))
                task.generateServer.set(extension.generateServer.getOrElse(true))
                
                // Set input and output directories
                task.outputDir.set(project.layout.buildDirectory.dir("generated/rpc"))
                
                // Find source directories
                val sourceFiles = project.fileTree("src/main/kotlin").matching {
                    it.include("**/*.kt")
                }
                task.inputSourceDirs.from(sourceFiles)
            }
        }
        
        // Register method mapping generation task
        val generateMappingTask = project.tasks.register("generateMethodMapping", GenerateMethodMappingTask::class.java) { task ->
            task.group = "obfuscated-rpc"
            task.description = "Generate method ID mappings for obfuscation"
            
            project.afterEvaluate {
                task.obfuscationEnabled.set(extension.obfuscationEnabled.getOrElse(true))
                task.outputFile.set(project.layout.buildDirectory.file("generated/rpc/method-mappings.json"))
                
                val sourceFiles = project.fileTree("src/main/kotlin").matching {
                    it.include("**/*.kt")
                }
                task.inputSourceDirs.from(sourceFiles)
            }
        }
        
        // Make compile tasks depend on code generation
        project.tasks.configureEach { task ->
            if (task.name == "compileKotlin" || task.name == "compileJava") {
                task.dependsOn(generateStubsTask, generateMappingTask)
            }
        }
    }
    
    private fun configureDependencies(project: Project) {
        project.afterEvaluate {
            project.dependencies.add("implementation", "com.obfuscated.rpc:core:${getLibraryVersion()}")
            project.dependencies.add("implementation", "com.obfuscated.rpc:runtime:${getLibraryVersion()}")
            project.dependencies.add("implementation", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            project.dependencies.add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
        }
    }
    
    private fun getLibraryVersion(): String {
        return "1.0.0-SNAPSHOT"
    }
}

