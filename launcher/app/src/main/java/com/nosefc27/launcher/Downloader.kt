package com.nosefc27.launcher

import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object Downloader {
    fun fetch(url: String, target: File, expectedBytes: Long?, onProgress: (Long, Long) -> Unit) {
        require(url.startsWith("https://")) { "Solo se permiten descargas HTTPS" }
        target.parentFile?.mkdirs()
        var existing = if (target.exists()) target.length() else 0L
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
            }
            val code = connection.responseCode
            if (existing > 0 && code == 416) {
                if (expectedBytes != null && existing == expectedBytes) return
                target.delete()
                existing = 0L
                connection.disconnect()
                connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000
                    readTimeout = 60_000
                    instanceFollowRedirects = true
                }
            } else if (existing > 0 && code == HttpURLConnection.HTTP_OK) {
                // Server ignored Range: restart explicitly instead of appending duplicate bytes.
                RandomAccessFile(target, "rw").use { it.setLength(0L) }
                existing = 0L
            }
            val finalCode = connection.responseCode
            check(finalCode == HttpURLConnection.HTTP_OK || finalCode == HttpURLConnection.HTTP_PARTIAL) {
                "HTTP $finalCode"
            }
            if (existing > 0) {
                val contentRange = connection.getHeaderField("Content-Range")
                    ?: error("Respuesta parcial sin Content-Range")
                check(contentRange.startsWith("bytes $existing-")) {
                    "Rango recibido no coincide con el desplazamiento local"
                }
            }
            val responseLength = connection.contentLengthLong.coerceAtLeast(0L)
            val total = expectedBytes ?: if (finalCode == HttpURLConnection.HTTP_PARTIAL) existing + responseLength else responseLength
            RandomAccessFile(target, "rw").use { out ->
                out.seek(existing)
                connection.inputStream.use { input ->
                    val buffer = ByteArray(1024 * 1024)
                    var done = existing
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        check(count > 0) { "Lectura HTTP inválida" }
                        out.write(buffer, 0, count)
                        done += count
                        onProgress(done, total)
                    }
                }
            }
            if (expectedBytes != null) check(target.length() == expectedBytes) {
                "Tamaño incorrecto: ${target.length()} / $expectedBytes"
            }
        } finally {
            connection?.disconnect()
        }
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(1024 * 1024).use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
