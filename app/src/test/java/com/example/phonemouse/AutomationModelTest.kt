package com.example.phonemouse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AutomationModelTest {

    /**
     * Purpose: Ensure that AutomationConfig objects can be correctly converted to JSON and back.
     * Before State: A valid AutomationConfig instance with diverse numeric parameters.
     * During Test: Calls toJson() and then fromJson() on the resulting string.
     * After State: All fields in the deserialized object match the original exactly.
     */
    @Test
    fun `AutomationConfig serialization and deserialization`() {
        val config = AutomationConfig(
            name = "Test Profile",
            minInterval = 100,
            maxInterval = 200,
            minPressDuration = 50,
            maxPressDuration = 150,
            minBreakDelay = 1000,
            maxBreakDelay = 2000,
            delayFrequency = 50
        )

        val json = config.toJson()
        val deserialized = AutomationConfig.fromJson(json)

        assertNotNull(deserialized)
        assertEquals(config.name, deserialized?.name)
        assertEquals(config.minInterval, deserialized?.minInterval)
        assertEquals(config.maxInterval, deserialized?.maxInterval)
        assertEquals(config.minPressDuration, deserialized?.minPressDuration)
        assertEquals(config.maxPressDuration, deserialized?.maxPressDuration)
        assertEquals(config.minBreakDelay, deserialized?.minBreakDelay)
        assertEquals(config.maxBreakDelay, deserialized?.maxBreakDelay)
        assertEquals(config.delayFrequency, deserialized?.delayFrequency)
    }

    /**
     * Purpose: Ensure that InputRecording objects (macros) correctly persist their event data through JSON.
     * Before State: An InputRecording instance containing a semi-colon delimited movement string.
     * During Test: Serializes the macro to JSON and reconstructs the object from it.
     * After State: Verification that the complex event string and metadata remain intact.
     */
    @Test
    fun `InputRecording serialization and deserialization`() {
        val recording = InputRecording(
            name = "Macro 1",
            timestamp = 123456789L,
            durationMs = 5000L,
            clickCount = 10,
            data = "0:0,0,0,0;100:1,0,0,0;200:0,0,0,0",
            loopPlayback = true
        )

        val json = recording.toJson()
        val deserialized = InputRecording.fromJson(json)

        assertNotNull(deserialized)
        assertEquals(recording.name, deserialized?.name)
        assertEquals(recording.timestamp, deserialized?.timestamp)
        assertEquals(recording.durationMs, deserialized?.durationMs)
        assertEquals(recording.clickCount, deserialized?.clickCount)
        assertEquals(recording.data, deserialized?.data)
        assertEquals(recording.loopPlayback, deserialized?.loopPlayback)
    }
}
