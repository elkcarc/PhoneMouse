package com.example.phonemouse

import org.json.JSONObject

/** Domain model for a captured sequence of mouse events. */
data class InputRecording(
    val name: String,
    val timestamp: Long,
    val durationMs: Long,
    val clickCount: Int,
    val data: String // Semicolon-separated timing/report data
) {
    /** Serializes the recording into a JSON string for persistence. */
    fun toJson(): String {
        return JSONObject().apply {
            put("name", name)
            put("timestamp", timestamp)
            put("duration", durationMs)
            put("clicks", clickCount)
            put("data", data)
        }.toString()
    }

    companion object {
        /** Creates an [InputRecording] from a JSON string. */
        fun fromJson(json: String): InputRecording? {
            return try {
                val obj = JSONObject(json)
                InputRecording(
                    obj.getString("name"),
                    obj.getLong("timestamp"),
                    obj.getLong("duration"),
                    obj.getInt("clicks"),
                    obj.getString("data")
                )
            } catch (_: Exception) { null }
        }
    }
}