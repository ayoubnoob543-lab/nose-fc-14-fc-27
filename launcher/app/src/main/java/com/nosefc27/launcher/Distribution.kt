package com.nosefc27.launcher

import org.json.JSONObject

data class DataPart(val url: String, val sha256: String, val sizeBytes: Long)
data class Distribution(val apkUrl: String, val apkSha256: String, val obbUrl: String, val obbSha256: String, val dataSha256: String, val parts: List<DataPart>) {
    companion object {
        fun fromJson(text: String): Distribution {
            val o = JSONObject(text); val array = o.getJSONArray("data_parts")
            val parts = (0 until array.length()).map { val p = array.getJSONObject(it); DataPart(p.getString("url"), p.getString("sha256"), p.getLong("size_bytes")) }
            return Distribution(o.getString("game_apk_url"), o.getString("game_apk_sha256"), o.getString("obb_zip_url"), o.getString("obb_zip_sha256"), o.getString("data_sha256"), parts)
        }
    }
}
