package com.adriano.designsystem

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Test
import java.io.File
import java.io.FileReader

// Data class to match the token structure
data class TokenValue(val value: String, val type: String)

class TokenGenerationTest {

    @Test
    fun `debug token generation logic`() {
        // 1. LOCATE THE FILE
        // Unit tests usually run from the project root.
        // If this fails, replace the string below with the FULL ABSOLUTE PATH to your file
        // e.g. File("/Users/Adriano/StudioProjects/MyApp/tokens.json")
        var jsonFile = File("tokens.json")

        // Helper to find the file if the test runner starts inside 'app/'
        if (!jsonFile.exists()) {
            jsonFile = File("../tokens.json")
        }

        println("📂 Reading file from: ${jsonFile.absolutePath}")

        // Stop the test here if file isn't found
        assert(jsonFile.exists()) { "❌ Could not find tokens.json! Please check the path." }

        // 2. PARSE SAFELY (The Fix)
        val gson = Gson()
        // Parse as a generic Element first to avoid the "Expected BEGIN_OBJECT" crash
        val rootElement = JsonParser.parseReader(FileReader(jsonFile))
        val rootObject = rootElement.asJsonObject

        val sb = StringBuilder()

        // 3. EXTRACT "global" SET
        if (rootObject.has("global")) {
            println("✅ Found 'global' set. Processing tokens...")

            val globalSet = rootObject.getAsJsonObject("global")

            for (key in globalSet.keySet()) {
                val tokenElement = globalSet.get(key)

                // IGNORE metadata arrays (like $themes, $metadata) which caused your crash
                if (tokenElement.isJsonObject) {
                    try {
                        val token = gson.fromJson(tokenElement, TokenValue::class.java)

                        if (token.type == "color") {
                            val hex = token.value.removePrefix("#")
                            val colorName = key.replaceFirstChar { it.uppercase() }

                            // Fix Hex length
                            val fullHex = if (hex.length == 6) "FF$hex" else hex

                            val kotlinLine = "val $colorName = Color(0x$fullHex)"
                            sb.append(kotlinLine).append("\n")

                            // Print each line as it's generated to the console
                            println("   generated: $kotlinLine")
                        }
                    } catch (e: Exception) {
                        println("   ⚠️ Skipped key '$key': ${e.message}")
                    }
                } else {
                    println("   ℹ️ Skipped metadata/array: $key")
                }
            }
        } else {
            println("❌ Error: 'global' key not found in JSON.")
        }

        // 4. FINAL OUTPUT
        println("\n--- FINAL GENERATED CODE ---\n")
        println(sb.toString())
    }
}