package com.nosefc27.launcher

import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object Downloader {
    fun fetch(url: String, target: File, expectedBytes: Long?, onProgress: (Long, Long) -> Unit) {
        target.parentFile?.mkdirs()
        var existing = if (target.exists()) target.length() else 0L
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000; readTimeout = 60_000
            if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
        }
        if (existing > 0 && connection.responseCode == HttpURLConnection.HTTP_OK) {
            target.delete(); existing = 0
        }
        check(connection.responseCode == HttpURLConnection.HTTP_OK || connection.responseCode == 206) {
            "HTTP ${connection.responseCode}"
        }
        val total = expectedBytes ?: (existing + connection.contentLengthLong.coerceAtLeast(0))
        RandomAccessFile(target, "rw").use { out ->
            out.seek(existing)
            connection.inputStream.use { input ->
                val buffer = ByteArray(1024 * 1024); var done = existing
                while (true) {
                    val count = input.read(buffer); if (count < 0) break
                    out.write(buffer, 0, count); done += count; onProgress(done, total)
                }
            }
        }
        connection.disconnect()
        if (expectedBytes != null) check(target.length() == expectedBytes) { "Tamaño incorrecto" }
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(1024 * 1024).use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
