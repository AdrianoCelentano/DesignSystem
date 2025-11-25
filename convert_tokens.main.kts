#!/usr/bin/env kotlin

@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.google.code.gson:gson:2.10.1")

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import java.io.File
import java.io.FileReader

// 1. DATA CLASS (For a single token)
data class TokenValue(
    @SerializedName("\$value") val value: String,
    @SerializedName("\$type") val type: String
)

// 2. LOAD JSON
val jsonFile = File("tokens.json")
if (!jsonFile.exists()) {
    println("Error: tokens.json not found in ${System.getProperty("user.dir")}")
    System.exit(1)
}

// 3. PARSE SAFELY
val gson = Gson()
// Parse root as a generic JSON Object first (avoids crashing on Arrays like $themes)
val rootElement = JsonParser.parseReader(FileReader(jsonFile))
val rootObject = rootElement.asJsonObject

// 4. GENERATE KOTLIN CONTENT
val sb = StringBuilder()
sb.append("package com.adriano.designsystem.ui.theme\n\n")
sb.append("import androidx.compose.ui.graphics.Color\n\n")

// Extract ONLY the "global" set (or whatever you named your set in Figma)
if (rootObject.has("global")) {
    val globalSet = rootObject.getAsJsonObject("global")

    // Iterate manually through the keys in "global"
    for (key in globalSet.keySet()) {
        val tokenElement = globalSet.get(key)

        // Safety check: ensure the token is actually an object (not an array or string)
        if (tokenElement.isJsonObject) {
            try {
                val token = gson.fromJson(tokenElement, TokenValue::class.java)

                if (token.type == "color") {
                    val hex = token.value.removePrefix("#")
                    val colorName = key.replaceFirstChar { it.uppercase() }

                    // Ensure 8 chars for Color(0xFF...)
                    val fullHex = if (hex.length == 6) "FF$hex" else hex
                    sb.append("val $colorName = Color(0x$fullHex)\n")
                }
            } catch (e: Exception) {
                // Ignore parsing errors for non-matching objects
            }
        }
    }
} else {
    println("⚠️ Warning: 'global' token set not found in JSON.")
}

// 5. WRITE TO FILE
val outputPath = "app/src/main/java/com/adriano/designsystem/ui/theme/Color.kt"
val outputFile = File(outputPath)

outputFile.parentFile.mkdirs()
outputFile.writeText(sb.toString())

println("✅ Success! Generated Color.kt safely.")