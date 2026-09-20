package com.nosefc27.launcher

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StatFs
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
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class MainActivity : AppCompatActivity() {
    private companion object {
        const val DISTRIBUTION_URL = "https://raw.githubusercontent.com/ayoubnoob543-lab/nose-fc-14-fc-27/master/launcher/distribution.json"
        const val UPDATE_MANIFEST_URL = "https://raw.githubusercontent.com/ayoubnoob543-lab/nose-fc-14-fc-27/master/launcher/update.json"
        const val MAX_MANIFEST_BYTES = 1_048_576L
        const val MAX_ZIP_ENTRIES = 500_000
        const val MAX_UNCOMPRESSED_BYTES = 12_000_000_000L
        const val MIN_WORKING_SPACE = 11_000_000_000L
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
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(title); addView(status)
            addView(progress, LinearLayout.LayoutParams(-1, 48))
            addView(action)
            if (Build.VERSION.SDK_INT >= 23) setPadding(24, 24, 24, 24)
        }
        setContentView(root)
        lifecycleScope.launch { loadDistribution() }
    }

    private suspend fun loadDistribution() {
        val distribution = withContext(Dispatchers.IO) { fetchText(DISTRIBUTION_URL)?.let { Distribution.fromJson(it) } }
        withContext(Dispatchers.Main) {
            if (distribution == null) {
                status.text = "No se pudo cargar el manifiesto público. Comprueba la conexión y pulsa reintentar."
                action.text = "Reintentar"
                action.isEnabled = true
                action.setOnClickListener { lifecycleScope.launch { loadDistribution() } }
                return@withContext
            }
            status.text = "Recursos disponibles.\n\nSe necesitan al menos 11 GB libres durante la preparación."
            action.isEnabled = true
            checkForUpdate()
        }
    }

    private fun downloadDistribution() {
        action.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                check(availableBytes() >= MIN_WORKING_SPACE) {
                    "Se necesitan al menos 11 GB libres para preparar los recursos"
                }
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
                val assembledPart = File(dir, "Fifa16ModFC27.assembled.zip.part")
                assembledPart.delete()
                FileOutputStream(assembledPart, false).use { output ->
                    d.parts.indices.forEach { i ->
                        File(dir, "data-$i.part").inputStream().use { input -> input.copyTo(output) }
                    }
                }
                check(Downloader.sha256(assembledPart) == d.dataSha256) { "Hash DATA incorrecto" }
                check(assembledPart.renameTo(assembled)) { "No se pudo promover el ZIP ensamblado" }
                val staging = File(filesDir, "prepared-staging").apply { deleteRecursively(); mkdirs() }
                extractZip(obb, File(staging, "obb"))
                extractZip(assembled, File(staging, "data"))
                val prepared = File(filesDir, "prepared")
                prepared.deleteRecursively()
                check(staging.renameTo(prepared)) { "No se pudo activar la preparación" }
                withContext(Dispatchers.Main) {
                    status.text = "Descarga verificada y recursos preparados.\n\nPulsa para abrir la descarga del APK del juego e instalarlo manualmente."
                    action.text = "Abrir descarga del juego"
                    action.setOnClickListener { openExternal(d.apkUrl) }
                    action.isEnabled = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    status.text = "Error recuperable: ${e.message ?: "desconocido"}\n\nPuedes reintentar sin perder archivos verificados."
                    action.text = "Reintentar"
                    action.isEnabled = true
                    action.setOnClickListener { downloadDistribution() }
                }
            }
        }
    }

    private suspend fun download(label: String, url: String, file: File, bytes: Long?, hash: String) {
        require(url.startsWith("https://")) { "URL no segura para $label" }
        withContext(Dispatchers.Main) { status.text = "Descargando $label..." }
        if (file.exists() && (bytes == null || file.length() == bytes) && Downloader.sha256(file).equals(hash, true)) return
        val partial = File(file.parentFile, file.name + ".part")
        Downloader.fetch(url, partial, bytes) { done, total ->
            lifecycleScope.launch(Dispatchers.Main) { progress.progress = if (total > 0) ((done * 100) / total).toInt().coerceIn(0, 100) else 0 }
        }
        check(Downloader.sha256(partial).equals(hash, ignoreCase = true)) { "Hash incorrecto: $label" }
        file.delete()
        check(partial.renameTo(file)) { "No se pudo guardar $label" }
    }

    private fun openExternal(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun extractZip(zip: File, destination: File) {
        destination.mkdirs()
        var entries = 0
        var total = 0L
        ZipInputStream(zip.inputStream().buffered()).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                check(++entries <= MAX_ZIP_ENTRIES) { "ZIP con demasiadas entradas" }
                val output = File(destination, entry.name)
                check(output.canonicalPath.startsWith(destination.canonicalPath + File.separator)) { "Ruta ZIP insegura" }
                if (entry.isDirectory) output.mkdirs() else {
                    output.parentFile?.mkdirs()
                    output.outputStream().use { out ->
                        val buffer = ByteArray(1024 * 1024)
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            total += n
                            check(total <= MAX_UNCOMPRESSED_BYTES) { "ZIP demasiado grande" }
                            out.write(buffer, 0, n)
                        }
                    }
                }
            }
        }
    }

    private fun checkForUpdate() {
        lifecycleScope.launch {
            val manifest = withContext(Dispatchers.IO) { fetchText(UPDATE_MANIFEST_URL)?.let { UpdateManifest.fromJson(it) } } ?: return@launch
            val installed = BuildConfig.VERSION_CODE
            if (manifest.versionCode <= installed) return@launch
            val message = "Hay una nueva versión de FC 27: ${manifest.version}\n\n${manifest.notes}\n\nTamaño: ${formatBytes(manifest.sizeBytes)}"
                val dialog = AlertDialog.Builder(this@MainActivity).setTitle("Actualizar FC 27").setMessage(message)
                .setPositiveButton("Descargar") { _, _ -> downloadUpdate(manifest) }
            if (!manifest.mandatory) dialog.setNegativeButton("Ahora no", null)
            dialog.setCancelable(!manifest.mandatory).show()
        }
    }

    private fun downloadUpdate(manifest: UpdateManifest) {
        action.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val target = File(filesDir, "downloads/FC27-update-${manifest.version}.apk")
                download("actualización ${manifest.version}", manifest.apkUrl, target, manifest.sizeBytes, manifest.sha256)
                withContext(Dispatchers.Main) {
                    status.text = "Actualización descargada y verificada. Ábrela desde el navegador/gestor de archivos para instalarla manualmente."
                    action.text = "Abrir página de actualización"
                    action.isEnabled = true
                    action.setOnClickListener { openExternal(manifest.apkUrl) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    status.text = "No se pudo actualizar: ${e.message ?: "error desconocido"}"
                    action.text = "Reintentar actualización"
                    action.isEnabled = true
                    action.setOnClickListener { downloadUpdate(manifest) }
                }
            }
        }
    }

    private fun fetchText(url: String): String? = runCatching {
        require(url.startsWith("https://"))
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000; connection.readTimeout = 15_000; connection.instanceFollowRedirects = true
        check(connection.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${connection.responseCode}" }
        check(connection.contentLengthLong <= MAX_MANIFEST_BYTES || connection.contentLengthLong < 0) { "Manifiesto demasiado grande" }
        connection.inputStream.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0L
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                total += n
                check(total <= MAX_MANIFEST_BYTES) { "Manifiesto demasiado grande" }
                out.write(buffer, 0, n)
            }
            out.toByteArray().toString(Charsets.UTF_8)
        }
    }.getOrNull()

    private fun availableBytes(): Long = StatFs(filesDir.absolutePath).availableBytes
    private fun formatBytes(bytes: Long) = if (bytes >= 1_000_000_000) "%.2f GB".format(bytes / 1_000_000_000.0) else "%.1f MB".format(bytes / 1_000_000.0)
}
