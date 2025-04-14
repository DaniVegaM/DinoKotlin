package com.example.t_rex

import android.content.Context
import android.webkit.JavascriptInterface
import org.json.JSONObject

class WebAppInterface(private val context: Context) {
    // Método que JavaScript puede llamar para obtener el estado guardado
    @JavascriptInterface
    fun getSavedGameState(): String {
        // Implementaremos esto luego - devuelve el estado guardado como JSON
        return ""
    }

    // Método que JavaScript puede llamar para guardar el estado
    @JavascriptInterface
    fun saveGameState(stateJson: String) {
        // Implementaremos esto luego - guarda el estado recibido
    }
}