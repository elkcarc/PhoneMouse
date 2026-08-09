package com.elk.phonemouse

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeHidManager(private val service: MouseHidService) : HidManager {
    private val _mouseHidService = MutableStateFlow<MouseHidService?>(null)
    override val mouseHidService = _mouseHidService.asStateFlow()

    override fun startAndBind() {
        _mouseHidService.value = service
    }

    override fun unbind() {
        _mouseHidService.value = null
    }
}
