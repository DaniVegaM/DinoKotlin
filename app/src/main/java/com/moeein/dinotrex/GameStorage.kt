package com.moeein.dinotrex

import android.content.Context
import android.webkit.JavascriptInterface
import org.json.JSONObject
import java.io.*

class GameStorage(private val context: Context) {

    @JavascriptInterface
    fun saveGame(jsonState: String, format: String = "json") {
        try {
            when (format) {
                "json" -> saveAsJson(jsonState)
                "txt" -> saveAsText(jsonState)
                "bin" -> saveAsBinary(jsonState)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun loadGame(format: String = "json"): String {
        return try {
            when (format) {
                "json" -> loadFromJson()
                "txt" -> loadFromText()
                "bin" -> loadFromBinary()
                else -> ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun saveAsJson(jsonState: String) {
        context.openFileOutput("game_save.json", Context.MODE_PRIVATE).use {
            it.write(jsonState.toByteArray())
        }
    }

    private fun saveAsText(jsonState: String) {
        val textState = convertJsonToText(jsonState)
        context.openFileOutput("game_save.txt", Context.MODE_PRIVATE).use {
            it.write(textState.toByteArray())
        }
    }

    private fun saveAsBinary(jsonState: String) {
        context.openFileOutput("game_save.bin", Context.MODE_PRIVATE).use {
            it.write(jsonState.toByteArray())
        }
    }

    private fun loadFromJson(): String {
        return try {
            FileInputStream(context.getFileStreamPath("game_save.json")).bufferedReader().use {
                it.readText()
            }
        } catch (e: FileNotFoundException) {
            ""
        }
    }

    private fun loadFromText(): String {
        return try {
            val text = FileInputStream(context.getFileStreamPath("game_save.txt")).bufferedReader().use {
                it.readText()
            }
            convertTextToJson(text)
        } catch (e: FileNotFoundException) {
            ""
        }
    }

    private fun loadFromBinary(): String {
        return try {
            FileInputStream(context.getFileStreamPath("game_save.bin")).bufferedReader().use {
                it.readText()
            }
        } catch (e: FileNotFoundException) {
            ""
        }
    }

    private fun convertJsonToText(json: String): String {
        // Implementación simple - puedes personalizar esto
        val obj = JSONObject(json)
        return """Juego Guardado:
            |Puntuación: ${obj.optInt("score")}
            |Récord: ${obj.optInt("highScore")}
            |Velocidad: ${obj.optDouble("speed")}
        """.trimMargin()
    }

    private fun convertTextToJson(text: String): String {
        // Implementación simple - necesitarás ajustar según tu formato de texto
        val lines = text.split("\n")
        val score = lines.find { it.contains("Puntuación") }?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
        val highScore = lines.find { it.contains("Récord") }?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
        val speed = lines.find { it.contains("Velocidad") }?.substringAfter(":")?.trim()?.toDoubleOrNull() ?: 1.0

        return """{"score":$score,"highScore":$highScore,"speed":$speed}"""
    }
}