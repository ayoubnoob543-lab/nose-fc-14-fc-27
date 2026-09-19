package com.nosefc27.launcher

import org.json.JSONObject

/** Public JSON contract served over HTTPS by the future CDN/repository. */
data class UpdateManifest(
    val version: String,
    val apkUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val mandatory: Boolean,
    val notes: String
) {
    companion object {
        fun fromJson(json: String): UpdateManifest {
            val o = JSONObject(json)
            return UpdateManifest(
                version = o.getString("version"),
                apkUrl = o.getString("apk_url"),
                sha256 = o.getString("sha256"),
                sizeBytes = o.getLong("size_bytes"),
                mandatory = o.optBoolean("mandatory", false),
                notes = o.optString("notes", "")
            )
        }
    }
}

fun isNewerVersion(remote: String, current: String): Boolean {
    fun parts(v: String) = v.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
    val a = parts(remote); val b = parts(current)
    for (i in 0 until maxOf(a.size, b.size)) {
        val av = a.getOrElse(i) { 0 }; val bv = b.getOrElse(i) { 0 }
        if (av != bv) return av > bv
    }
    return false
}
