package net.tactware.kwire.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PluginApplyTest {

    @TempDir
    lateinit var testProjectDir: File

    private fun writeFile(relativePath: String, content: String): File {
        val target = File(testProjectDir, relativePath)
        target.parentFile.mkdirs()
        target.writeText(content)
        return target
    }

    @Test
    fun `plugin applies and registers generateRpcStubs task`() {
        // Minimal settings and build files. Apply Kotlin JVM so SourceSetContainer exists.
        writeFile(
            "settings.gradle.kts",
            """
            rootProject.name = "test-project"
            """.trimIndent()
        )

        writeFile(
            "build.gradle.kts",
            """
            plugins {
                kotlin("jvm") version "2.2.0"
                id("net.tactware.kwire.plugin")
            }

            repositories { mavenCentral() }

            kotlin { jvmToolchain(17) }
            """.trimIndent()
        )

        // Create a no-op source file (the task can still run even if no services are found)
        writeFile(
            "src/main/kotlin/com/example/Empty.kt",
            "package com.example\n\nclass Empty\n"
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--all")
            .build()

        // The output should list our task registered and grouped correctly
        val out = result.output
        assertTrue(out.contains("generateRpcStubs"), "Expected tasks output to contain generateRpcStubs, but was:\n$out")

        // Now try running the task to ensure it's actionable
        val run = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("generateRpcStubs", "--stacktrace")
            .build()

        val task = run.task(":generateRpcStubs")
        assertEquals(TaskOutcome.SUCCESS, task?.outcome)
    }
}
