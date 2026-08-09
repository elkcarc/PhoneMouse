package com.example.phonemouse

import kotlinx.coroutines.flow.StateFlow

/** Interface defining the contract for managing the mouse HID service connection. */
interface HidManager {
    /** Stream of the active HID service instance (null if not bound). */
    val mouseHidService: StateFlow<MouseHidService?>
    /** Initiates the background service and binds to it. */
    fun startAndBind()
    /** Unbinds and stops the background service. */
    fun unbind()
}