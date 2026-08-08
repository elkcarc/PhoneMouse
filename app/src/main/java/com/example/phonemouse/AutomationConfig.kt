package com.example.phonemouse

/** Domain model representing a randomized click strategy. */
data class AutomationConfig(
    val minInterval: Int,
    val maxInterval: Int,
    val minPressDuration: Int,
    val maxPressDuration: Int,
    val minBreakDelay: Int,
    val maxBreakDelay: Int,
    val delayFrequency: Int,
) {
    /** Serializes the configuration into a CSV string for persistence. */
    override fun toString() = "$minInterval,$maxInterval,$minPressDuration,$maxPressDuration,$minBreakDelay,$maxBreakDelay,$delayFrequency"

    companion object {
        /** Creates an [AutomationConfig] from a CSV string. Returns null on parse error. */
        fun fromString(data: String): AutomationConfig? {
            val p = data.split(",")
            return try {
                AutomationConfig(p[0].toInt(), p[1].toInt(), p[2].toInt(), p[3].toInt(), p[4].toInt(), p[5].toInt(), p[6].toInt())
            } catch (_: Exception) { null }
        }
    }
}