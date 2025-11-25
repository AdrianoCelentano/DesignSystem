#!/usr/bin/env kotlin

@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.google.code.gson:gson:2.10.1")

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

// 1. DATA CLASSES (To match your JSON structure)
data class TokenValue(val value: String, val type: String)
// We use a Map because token names (primary, secondary) are dynamic
typealias TokenSet = Map<String, TokenValue>

// 2. LOAD JSON
val jsonFile = File("tokens.json")
if (!jsonFile.exists()) {
    println("Error: tokens.json not found!")
    System.exit(1)
}

val gson = Gson()
val type = object : TypeToken<Map<String, TokenSet>>() {}.type
val data: Map<String, TokenSet> = gson.fromJson(jsonFile.readText(), type)

// 3. GENERATE KOTLIN CODE
val sb = StringBuilder()
sb.append("package com.example.myapp.ui.theme\n\n")
sb.append("import androidx.compose.ui.graphics.Color\n\n")

val globalTokens = data["global"] ?: emptyMap()

globalTokens.forEach { (name, token) ->
    if (token.type == "color") {
        // Convert hex string (#FF0000) to Color(0xFFFF0000)
        val hex = token.value.removePrefix("#")
        val colorName = name.replaceFirstChar { it.uppercase() } // primary -> Primary

        // Simple logic to ensure we have full 8 digits for ARGB
        val fullHex = if (hex.length == 6) "FF$hex" else hex

        sb.append("val $colorName = Color(0x$fullHex)\n")
    }
}

// 4. WRITE TO FILE
val outputPath = "app/src/main/java/com/example/myapp/ui/theme/Color.kt"
val outputFile = File(outputPath)

// Ensure directory exists
outputFile.parentFile.mkdirs()
outputFile.writeText(sb.toString())

println("✅ Success! Generated Color.kt with Kotlin Script.")

