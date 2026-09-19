package com.nosefc27.launcher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    private companion object {
        const val CURRENT_VERSION = "0.1.0"
        // Replace with the public HTTPS manifest when the CDN/repository is published.
        const val UPDATE_MANIFEST_URL = "https://example.com/fc27/update.json"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = TextView(this).apply { text = "FC 27"; textSize = 30f; setPadding(24, 32, 24, 16) }
        val status = TextView(this).apply {
            text = "Preparado para descargar los recursos del juego.\n\nLa descarga ocupará aproximadamente 6,2 GB."
            textSize = 16f; setPadding(24, 8, 24, 16)
        }
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progress = 0 }
        val action = Button(this).apply { text = "Comprobar y descargar recursos" }
        action.setOnClickListener {
            status.text = "El manifiesto de distribución aún no está publicado.\n\nSe configurará cuando esté disponible el alojamiento público."
            action.isEnabled = false
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(status)
            addView(progress, LinearLayout.LayoutParams(-1, 48))
            addView(action)
        })
        checkForUpdate()
    }

    private fun checkForUpdate() {
        lifecycleScope.launch {
            val manifest = withContext(Dispatchers.IO) { fetchManifest() } ?: return@launch
            if (!isNewerVersion(manifest.version, CURRENT_VERSION)) return@launch
            withContext(Dispatchers.Main) {
                val message = buildString {
                    append("Hay una nueva versión de FC 27: ").append(manifest.version)
                    if (manifest.notes.isNotBlank()) append("\n\n").append(manifest.notes)
                    append("\n\nTamaño: ").append(formatBytes(manifest.sizeBytes))
                }
                val dialog = AlertDialog.Builder(this@MainActivity)
                    .setTitle("Actualizar FC 27")
                    .setMessage(message)
                    .setPositiveButton("Actualizar") { _, _ -> openUpdate(manifest) }
                if (!manifest.mandatory) dialog.setNegativeButton("Ahora no", null)
                dialog.setCancelable(!manifest.mandatory).show()
            }
        }
    }

    private fun fetchManifest(): UpdateManifest? = runCatching {
        val connection = (URL(UPDATE_MANIFEST_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 8_000; readTimeout = 8_000
            setRequestProperty("Accept", "application/json")
        }
        connection.inputStream.bufferedReader().use { UpdateManifest.fromJson(it.readText()) }
    }.getOrNull()

    private fun openUpdate(manifest: UpdateManifest) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(manifest.apkUrl)))
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        else -> "$bytes bytes"
    }
}
