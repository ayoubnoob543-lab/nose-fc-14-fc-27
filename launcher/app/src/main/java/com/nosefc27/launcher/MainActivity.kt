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
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class MainActivity : AppCompatActivity() {
    private companion object {
        const val CURRENT_VERSION = "0.2.0"
        const val DISTRIBUTION_URL = "https://raw.githubusercontent.com/ayoubnoob543-lab/nose-fc-14-fc-27/master/launcher/distribution.json"
        const val UPDATE_MANIFEST_URL = "https://raw.githubusercontent.com/ayoubnoob543-lab/nose-fc-14-fc-27/master/launcher/update.json"
    }

    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var action: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = TextView(this).apply { text = "FC 27"; textSize = 30f; setPadding(24, 32, 24, 16) }
        status = TextView(this).apply { text = "Comprobando archivos..."; textSize = 16f; setPadding(24, 8, 24, 16) }
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        action = Button(this).apply { text = "Descargar recursos"; isEnabled = false }
        action.setOnClickListener { downloadDistribution() }
        setContentView(LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(title); addView(status); addView(progress, LinearLayout.LayoutParams(-1, 48)); addView(action) })
        lifecycleScope.launch { loadDistribution() }
    }

    private suspend fun loadDistribution() {
        val distribution = withContext(Dispatchers.IO) { fetchText(DISTRIBUTION_URL)?.let { Distribution.fromJson(it) } }
        withContext(Dispatchers.Main) {
            if (distribution == null) { status.text = "No se pudo cargar el manifiesto público."; return@withContext }
            status.text = "Recursos listos para descargar.\n\nNecesita aproximadamente 6,2 GB."
            action.isEnabled = true
            checkForUpdate()
        }
    }

    private fun downloadDistribution() {
        action.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val d = fetchText(DISTRIBUTION_URL)?.let { Distribution.fromJson(it) } ?: error("Manifiesto no disponible")
                val dir = File(filesDir, "downloads").apply { mkdirs() }
                val apk = File(dir, "FC27-game.apk")
                val obb = File(dir, "obb.zip")
                download("APK", d.apkUrl, apk, null, d.apkSha256)
                download("OBB", d.obbUrl, obb, null, d.obbSha256)
                d.parts.forEachIndexed { index, part ->
                    val partFile = File(dir, "data-$index.part")
                    download("DATA ${index + 1}/${d.parts.size}", part.url, partFile, part.sizeBytes, part.sha256)
                }
                val assembled = File(dir, "Fifa16ModFC27.assembled.zip")
                assembled.delete()
                FileOutputStream(assembled, true).use { output ->
                    d.parts.indices.forEach { i -> File(dir, "data-$i.part").inputStream().use { input -> input.copyTo(output) } }
                }
                check(Downloader.sha256(assembled) == d.dataSha256) { "Hash DATA incorrecto" }
                extractZip(obb, File(filesDir, "prepared/obb"))
                extractZip(assembled, File(filesDir, "prepared/data"))
                withContext(Dispatchers.Main) { status.text = "Descarga verificada y recursos preparados.\n\nAndroid pedirá instalar el APK del juego."; action.text = "Instalar juego"; action.setOnClickListener { installApk(apk) }; action.isEnabled = true }
            } catch (e: Exception) { withContext(Dispatchers.Main) { status.text = "Error: ${e.message}"; action.isEnabled = true } }
        }
    }

    private suspend fun download(label: String, url: String, file: File, bytes: Long?, hash: String) {
        withContext(Dispatchers.Main) { status.text = "Descargando $label..." }
        Downloader.fetch(url, file, bytes) { done, total -> lifecycleScope.launch(Dispatchers.Main) { progress.progress = if (total > 0) ((done * 100) / total).toInt() else 0 } }
        check(Downloader.sha256(file).equals(hash, ignoreCase = true)) { "Hash incorrecto: $label" }
    }

    private fun installApk(apk: File) {
        val uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.files", apk)
        startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) })
    }

    private fun extractZip(zip: File, destination: File) {
        destination.mkdirs()
        ZipInputStream(zip.inputStream().buffered()).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                val output = File(destination, entry.name)
                check(output.canonicalPath.startsWith(destination.canonicalPath + File.separator)) { "Ruta ZIP insegura" }
                if (entry.isDirectory) output.mkdirs() else {
                    output.parentFile?.mkdirs()
                    output.outputStream().use { input.copyTo(it) }
                }
            }
        }
    }

    private fun checkForUpdate() {
        lifecycleScope.launch {
            val manifest = withContext(Dispatchers.IO) { fetchText(UPDATE_MANIFEST_URL)?.let { UpdateManifest.fromJson(it) } } ?: return@launch
            if (!isNewerVersion(manifest.version, CURRENT_VERSION)) return@launch
            val message = "Hay una nueva versión de FC 27: ${manifest.version}\n\n${manifest.notes}\n\nTamaño: ${formatBytes(manifest.sizeBytes)}"
            val dialog = AlertDialog.Builder(this@MainActivity).setTitle("Actualizar FC 27").setMessage(message).setPositiveButton("Actualizar") { _, _ -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(manifest.apkUrl))) }
            if (!manifest.mandatory) dialog.setNegativeButton("Ahora no", null)
            dialog.setCancelable(!manifest.mandatory).show()
        }
    }

    private fun fetchText(url: String): String? = runCatching { (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 10_000; readTimeout = 15_000 }.inputStream.bufferedReader().use { it.readText() } }.getOrNull()
    private fun formatBytes(bytes: Long) = if (bytes >= 1_000_000_000) "%.2f GB".format(bytes / 1_000_000_000.0) else "%.1f MB".format(bytes / 1_000_000.0)
}
