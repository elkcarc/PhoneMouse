package com.example.phonemouse

/**
 * Data class representing an automation strategy configuration.
 * Defines the parameters for randomized clicking behavior to simulate human interaction.
 */
data class AutomationConfig(
    /** Minimum delay between mouse clicks (ms). */
    val minInterval: Int,
    /** Maximum delay between mouse clicks (ms). */
    val maxInterval: Int,
    /** Minimum duration the button is held down (ms). */
    val minPressDuration: Int,
    /** Maximum duration the button is held down (ms). */
    val maxPressDuration: Int,
    /** Minimum duration of a long 'break' pause (ms). */
    val minBreakDelay: Int,
    /** Maximum duration of a long 'break' pause (ms). */
    val maxBreakDelay: Int,
    /** Chance of a long break occurring (1 in X clicks). */
    val delayFrequency: Int,
) {
    /**
     * Converts the config to a comma-separated string for persistence in SharedPreferences.
     */
    override fun toString(): String {
        return "$minInterval,$maxInterval,$minPressDuration,$maxPressDuration,$minBreakDelay,$maxBreakDelay,$delayFrequency"
    }

    companion object {
        /**
         * Creates a config from a comma-separated string.
         * @return An [AutomationConfig] or null if the string format is invalid.
         */
        fun fromString(data: String): AutomationConfig? {
            val parts = data.split(",")
            return try {
                AutomationConfig(
                    minInterval = parts[0].toInt(),
                    maxInterval = parts[1].toInt(),
                    minPressDuration = parts[2].toInt(),
                    maxPressDuration = parts[3].toInt(),
                    minBreakDelay = parts[4].toInt(),
                    maxBreakDelay = parts[5].toInt(),
                    delayFrequency = parts[6].toInt(),
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}