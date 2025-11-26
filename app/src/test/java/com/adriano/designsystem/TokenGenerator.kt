package com.adriano.designsystem

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

class TokenGenerationTest {

    @Test
    fun `generate tokens via script`() {
        val scriptName = "convert_tokens.main.kts"
        val potentialDirs = listOf(File("."), File(".."), File("../.."))
        val scriptFile = potentialDirs
            .map { File(it, scriptName) }
            .firstOrNull { it.exists() }
            ?: run {
                fail("Script $scriptName not found in ${potentialDirs.map { it.absolutePath }}")
                return
            }

        val workingDir = scriptFile.parentFile
        println("Found script at: ${scriptFile.absolutePath}")
        
        scriptFile.setExecutable(true)

        // Enhance PATH
        val currentPath = System.getenv("PATH") ?: ""
        val extendedPath = "$currentPath:/usr/local/bin:/opt/homebrew/bin:/opt/local/bin:/usr/bin:/bin"
        
        val command = listOf("kotlin", scriptName)
        println("🚀 Executing: $command in ${workingDir.absolutePath}")

        try {
            val processBuilder = ProcessBuilder(command)
                .directory(workingDir)
                .redirectErrorStream(true)
            
            processBuilder.environment()["PATH"] = extendedPath

            val process = processBuilder.start()
            val completed = process.waitFor(2, TimeUnit.MINUTES)
            val output = process.inputStream.bufferedReader().readText()
            
            println("📜 Script Output:\n$output")

            if (!completed) {
                process.destroy()
                fail("Script timed out")
            }
            
            if (process.exitValue() != 0) {
                 if (output.contains("No such file or directory") || output.contains("not found")) {
                     fail("Script failed execution. Ensure 'kotlin' is in PATH. Output: $output")
                 } else {
                     fail("Script failed with exit code ${process.exitValue()}. Output: $output")
                 }
            }

            // --- Verify Color.kt ---
            val colorFile = File(workingDir, "app/src/main/java/com/adriano/designsystem/ui/theme/Color.kt")
            assertTrue("Color.kt not found", colorFile.exists())
            val colorContent = colorFile.readText()
            assertTrue("Should contain Primary color", colorContent.contains("val Primary: Color = Color(0xFF3A693B)"))

            // --- Verify Type.kt ---
            val typeFile = File(workingDir, "app/src/main/java/com/adriano/designsystem/ui/theme/Type.kt")
            assertTrue("Type.kt not found", typeFile.exists())
            val typeContent = typeFile.readText()
            assertTrue("Should contain BodyLarge", typeContent.contains("val BodyLarge: TextStyle"))

            // --- Verify Spacing.kt ---
            val spacingFile = File(workingDir, "app/src/main/java/com/adriano/designsystem/ui/theme/Spacing.kt")
            assertTrue("Spacing.kt not found", spacingFile.exists())
            val spacingContent = spacingFile.readText()
            println("✅ Generated Spacing Content:\n$spacingContent")
            
            // Check content with correct floating point format (e.g., "One" -> 4.0.dp)
            assertTrue("Should contain One", spacingContent.contains("val One: Dp = 4.0.dp"))
            assertTrue("Should contain Two", spacingContent.contains("val Two: Dp = 8.0.dp"))

        } catch (e: Exception) {
             throw e
        }
    }
}