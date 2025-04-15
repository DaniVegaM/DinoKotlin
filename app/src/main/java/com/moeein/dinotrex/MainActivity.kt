package com.moeein.dinotrex

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.moeein.dinotrex.databinding.ActivityMainBinding
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    var url: String = "file:///android_asset/index.html"

    // Tipos de archivo para guardar
    private val saveFormats = arrayOf("JSON", "Binary", "Text")
    private val saveFileExtensions = arrayOf(".json", ".bin", ".txt")


    companion object {
        private const val STORAGE_PERMISSION_CODE = 100
    }

    @SuppressLint("SetJavaScriptEnabled")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Verificar permisos primero
        if (!checkStoragePermissions()) {
            requestStoragePermissions()
        }

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.addJavascriptInterface(WebAppInterface(this), "Android")
        binding.webView.loadUrl(url)

        // Configurar botones
        findViewById<Button>(R.id.btnSaveGame).setOnClickListener {
            showSaveFormatDialog()
        }

        findViewById<Button>(R.id.btnLoadGame).setOnClickListener {
            showLoadDialog()
        }
    }

    private fun checkStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, STORAGE_PERMISSION_CODE)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivityForResult(intent, STORAGE_PERMISSION_CODE)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permisos denegados", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Método actualizado para obtener el directorio de guardado
    private fun getSaveDirectory(): File {
        // Usamos siempre el directorio de documentos de la app para mejor compatibilidad
        return File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "DinoGameSaves").apply {
            if (!exists()) mkdirs()
        }
    }

    private fun showSaveFormatDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Seleccione formato para guardar")
            .setItems(saveFormats) { _, which ->
                showNameInputDialog(which)
            }
        builder.create().show()
    }

    private fun showNameInputDialog(formatIndex: Int) {
        val input = EditText(this)
        input.hint = "Nombre de la partida"

        AlertDialog.Builder(this)
            .setTitle("Guardar partida")
            .setMessage("Ingrese un nombre para la partida")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    saveGameData(formatIndex, name)
                } else {
                    Toast.makeText(this, "Debe ingresar un nombre", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showLoadDialog() {
        val savedGames = getSavedGamesInfo()
        if (savedGames.isEmpty()) {
            Toast.makeText(this, "No hay partidas guardadas", Toast.LENGTH_SHORT).show()
            return
        }

        val gameNames = savedGames.map {
            "${it.name} (High Score: ${it.highScore})"
        }.toTypedArray()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Seleccione partida a cargar")
            .setItems(gameNames) { _, which ->
                // Cambiar esto para usar el filePath correcto
                loadGameData(File(savedGames[which].filePath))
            }
        builder.create().show()
    }

    private fun getSavedGamesInfo(): List<SavedGameInfo> {
        return try {
            val saveDir = getSaveDirectory()
            if (!saveDir.exists() || !saveDir.isDirectory) return emptyList()

            saveDir.listFiles()?.filter { file ->
                file.isFile && saveFileExtensions.any { file.name.endsWith(it) }
            }?.mapNotNull { file ->
                try {
                    val data = when {
                        file.name.endsWith(".json") || file.name.endsWith(".txt") -> {
                            FileInputStream(file).bufferedReader().use { it.readText() }
                        }
                        file.name.endsWith(".bin") -> {
                            ObjectInputStream(FileInputStream(file)).use { it.readObject() as String }
                        }
                        else -> null
                    } ?: return@mapNotNull null

                    val json = JSONObject(data)
                    SavedGameInfo(
                        fileName = file.name,
                        filePath = file.absolutePath,
                        name = json.optString("saveName", file.nameWithoutExtension),
                        highScore = json.optInt("highestScore", 0),
                        saveDate = json.optString("saveDate", "")
                    )
                } catch (e: Exception) {
                    null
                }
            }?.sortedByDescending { it.highScore } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun debugSaveDirectory() {
        val saveDir = getSaveDirectory()
        val files = saveDir.listFiles()?.joinToString("\n") { it.name } ?: "No hay archivos"
        Log.d("SaveGame", "Directorio: ${saveDir.absolutePath}\nArchivos:\n$files")
    }

    private fun saveGameData(formatIndex: Int, saveName: String) {
        binding.webView.evaluateJavascript("""
            (function() {
                return {
                    distanceRan: Runner.instance_.distanceRan,
                    highestScore: Runner.instance_.highestScore,
                    currentSpeed: Runner.instance_.currentSpeed,
                    time: Runner.instance_.time,
                    obstacles: Runner.instance_.obstacles.map(ob => ({
                        type: ob.typeConfig.type,
                        xPos: ob.xPos,
                        yPos: ob.yPos,
                        width: ob.width,
                        height: ob.height
                    })),
                    tRexStatus: Runner.instance_.tRex.status
                };
            })()
        """.trimIndent()) { result ->
            if (result == "null") {
                Toast.makeText(this, "Error al obtener datos del juego", Toast.LENGTH_SHORT).show()
                return@evaluateJavascript
            }

            val fileName = "dino_save_${saveName.replace(" ", "_")}_${System.currentTimeMillis()}${saveFileExtensions[formatIndex]}"

            try {
                val saveDir = getSaveDirectory()
                val file = File(saveDir, fileName)

                FileOutputStream(file).use { fos ->
                    when (formatIndex) {
                        0 -> fos.write(result.toByteArray()) // JSON
                        1 -> ObjectOutputStream(fos).writeObject(result) // Binary
                        2 -> fos.write(result.toByteArray()) // Text
                    }
                }
                Toast.makeText(this, "Partida guardada: $saveName", Toast.LENGTH_SHORT).show()

                // Depuración: Mostrar ruta real
                debugSaveDirectory()
            } catch (e: Exception) {
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadGameData(file: File) {
        try {
            val data = when {
                file.name.endsWith(".json") || file.name.endsWith(".txt") -> {
                    FileInputStream(file).bufferedReader().use { it.readText() }
                }
                file.name.endsWith(".bin") -> {
                    ObjectInputStream(FileInputStream(file)).use { it.readObject() as String }
                }
                else -> throw Exception("Formato de archivo no soportado")
            }

            binding.webView.evaluateJavascript("""
                (function() {
                    var gameData = $data;
                    Runner.instance_.distanceRan = gameData.distanceRan;
                    Runner.instance_.highestScore = gameData.highestScore;
                    Runner.instance_.currentSpeed = gameData.currentSpeed;
                    Runner.instance_.time = gameData.time;
                    Runner.instance_.obstacles = gameData.obstacles;
                    Runner.instance_.tRex.status = gameData.tRexStatus;
                    
                    // Reiniciar el juego con los datos cargados
                    Runner.instance_.restart();
                    return "Partida cargada";
                })()
            """.trimIndent()) {
                Toast.makeText(this, "Partida cargada", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Actualiza la clase SavedGameInfo
    data class SavedGameInfo(
        val fileName: String,
        val filePath: String,  // Nueva propiedad para la ruta completa
        val name: String,
        val highScore: Int,
        val saveDate: String

    )

    inner class WebAppInterface(private val context: Context) {
        @JavascriptInterface
        fun showToast(toast: String) {
            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
        }
    }
}