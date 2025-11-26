#!/usr/bin/env kotlin

@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.google.code.gson:gson:2.10.1")
@file:DependsOn("com.squareup:kotlinpoet:1.14.2")

import com.google.gson.JsonParser
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.MemberName
import java.io.File
import java.io.FileReader

println("🚀 Starting Token Generation Script")
println("📂 Working Directory: ${System.getProperty("user.dir")}")

// 1. LOAD JSON
val jsonFile = File("tokens.json")
if (!jsonFile.exists()) {
    println("❌ Error: tokens.json not found in ${System.getProperty("user.dir")}")
    System.exit(1)
}
println("✅ Found tokens.json")

// 2. PARSE SAFELY
val rootElement = JsonParser.parseReader(FileReader(jsonFile))
val rootObject = rootElement.asJsonObject

// 3. PREPARE KOTLINPOET BUILDERS
// --- Color ---
val colorClassName = ClassName("androidx.compose.ui.graphics", "Color")
val colorFileSpec = FileSpec.builder("com.adriano.designsystem.ui.theme", "Color")

// --- Typography ---
val textStyleClassName = ClassName("androidx.compose.ui.text", "TextStyle")
val fontFamilyClassName = ClassName("androidx.compose.ui.text.font", "FontFamily")
val spMember = MemberName("androidx.compose.ui.unit", "sp")
val typeFileSpec = FileSpec.builder("com.adriano.designsystem.ui.theme", "Type")

// --- Spacing ---
val dpClassName = ClassName("androidx.compose.ui.unit", "Dp")
val dpMember = MemberName("androidx.compose.ui.unit", "dp")
val spacingFileSpec = FileSpec.builder("com.adriano.designsystem.ui.theme", "Spacing")

// Extract ONLY the "global" set
if (rootObject.has("global")) {
    val globalSet = rootObject.getAsJsonObject("global")
    println("🔍 Processing 'global' token set with ${globalSet.size()} entries...")

    for (key in globalSet.keySet()) {
        val tokenElement = globalSet.get(key)

        if (tokenElement.isJsonObject) {
            val tokenObj = tokenElement.asJsonObject
            
            // Extract Type
            val typeElement = if (tokenObj.has("\$type")) tokenObj.get("\$type") else tokenObj.get("type")
            val type = if (typeElement != null && typeElement.isJsonPrimitive) typeElement.asString else null

            // Extract Value
            val valueElement = if (tokenObj.has("\$value")) tokenObj.get("\$value") else tokenObj.get("value")

            if (type != null && valueElement != null) {
                // =================================================================================
                // COLOR
                // =================================================================================
                if (type == "color" && valueElement.isJsonPrimitive) {
                    try {
                        val value = valueElement.asString
                        val hex = value.removePrefix("#")
                        val colorName = key.replaceFirstChar { it.uppercase() }
                        val fullHex = if (hex.length == 6) "FF$hex" else hex
                        
                        val property = PropertySpec.builder(colorName, colorClassName)
                            .initializer("%T(0x$fullHex)", colorClassName)
                            .build()
                        
                        colorFileSpec.addProperty(property)
                    } catch (e: Exception) {
                        println("⚠️ Error processing color $key: ${e.message}")
                    }
                }
                // =================================================================================
                // TYPOGRAPHY
                // =================================================================================
                else if (type == "typography" && valueElement.isJsonObject) {
                    try {
                        val valueObj = valueElement.asJsonObject
                        
                        // Convert "Body large" -> "BodyLarge"
                        val styleName = key.split(" ").joinToString("") { 
                            it.replaceFirstChar { c -> c.uppercase() } 
                        }

                        // Helper to safely parse numbers from strings like "16", "16px", "0.5px"
                        fun getFloat(k: String, default: Float): Float {
                            if (!valueObj.has(k)) return default
                            val s = valueObj.get(k).asString
                            return s.replace("px", "").toFloatOrNull() ?: default
                        }

                        val fontSize = getFloat("fontSize", 14f)
                        val lineHeight = getFloat("lineHeight", 20f)
                        val letterSpacing = getFloat("letterSpacing", 0f)
                        
                        val property = PropertySpec.builder(styleName, textStyleClassName)
                            .initializer(
                                "%T(\n  fontFamily = %T.Default,\n  fontSize = %L.%M,\n  lineHeight = %L.%M,\n  letterSpacing = %L.%M\n)",
                                textStyleClassName,
                                fontFamilyClassName,
                                fontSize, spMember,
                                lineHeight, spMember,
                                letterSpacing, spMember
                            )
                            .build()

                        typeFileSpec.addProperty(property)

                    } catch (e: Exception) {
                        println("⚠️ Error processing typography $key: ${e.message}")
                    }
                }
                // =================================================================================
                // SPACING
                // =================================================================================
                else if (type == "spacing" && valueElement.isJsonPrimitive) {
                    try {
                         val spacingName = key.replaceFirstChar { it.uppercase() }
                         val valueStr = valueElement.asString
                         val valueFloat = valueStr.replace("px", "").toFloatOrNull() ?: 0f
                         
                         val property = PropertySpec.builder(spacingName, dpClassName)
                            .initializer("%L.%M", valueFloat, dpMember)
                            .build()

                         spacingFileSpec.addProperty(property)

                    } catch (e: Exception) {
                        println("⚠️ Error processing spacing $key: ${e.message}")
                    }
                }
            }
        }
    }
} else {
    println("❌ Error: 'global' token set not found in JSON.")
    System.exit(1)
}

// 4. WRITE TO FILES
val outputDir = File("app/src/main/java")
if (!outputDir.exists()) {
    println("⚠️ Output directory does not exist, creating: ${outputDir.absolutePath}")
    outputDir.mkdirs()
}

println("💾 Writing files to: ${outputDir.absolutePath}")

try {
    colorFileSpec.build().writeTo(outputDir)
    println("   - Written Color.kt")
    
    typeFileSpec.build().writeTo(outputDir)
    println("   - Written Type.kt")
    
    spacingFileSpec.build().writeTo(outputDir)
    println("   - Written Spacing.kt")
    
    println("✅ Success! Generated Color.kt, Type.kt, and Spacing.kt using KotlinPoet.")
} catch (e: Exception) {
    println("❌ Error writing files: ${e.message}")
    e.printStackTrace()
    System.exit(1)
}