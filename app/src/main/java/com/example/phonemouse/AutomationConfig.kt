package com.example.phonemouse

import org.json.JSONObject

/** Domain model representing a randomized click strategy profile. */
data class AutomationConfig(
    val name: String,
    val minInterval: Int,
    val maxInterval: Int,
    val minPressDuration: Int,
    val maxPressDuration: Int,
    val minBreakDelay: Int,
    val maxBreakDelay: Int,
    val delayFrequency: Int,
) {
    /** Serializes the profile into a JSON string for persistence. */
    fun toJson(): String {
        return JSONObject().apply {
            put("name", name)
            put("minInt", minInterval)
            put("maxInt", maxInterval)
            put("minPress", minPressDuration)
            put("maxPress", maxPressDuration)
            put("minBreak", minBreakDelay)
            put("maxBreak", maxBreakDelay)
            put("freq", delayFrequency)
        }.toString()
    }

    companion object {
        /** Creates an [AutomationConfig] from a JSON string. Returns null on parse error. */
        fun fromJson(json: String): AutomationConfig? {
            return try {
                val obj = JSONObject(json)
                AutomationConfig(
                    obj.getString("name"),
                    obj.getInt("minInt"),
                    obj.getInt("maxInt"),
                    obj.getInt("minPress"),
                    obj.getInt("maxPress"),
                    obj.getInt("minBreak"),
                    obj.getInt("maxBreak"),
                    obj.getInt("freq")
                )
            } catch (_: Exception) {
                // Fallback to legacy CSV format if JSON parsing fails
                val parts = json.split(",")
                if (parts.size >= 7) {
                    AutomationConfig(
                        "Autoclicker Profile",
                        parts[0].toInt(), parts[1].toInt(), parts[2].toInt(),
                        parts[3].toInt(), parts[4].toInt(), parts[5].toInt(), parts[6].toInt()
                    )
                } else null
            }
        }
    }
}