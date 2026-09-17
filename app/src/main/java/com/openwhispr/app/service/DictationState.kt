package com.openwhispr.app.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DictationPhase {
    IDLE,
    CONNECTING,
    LISTENING,
    PROCESSING,
    COMPLETED,
    ERROR,
}

data class DictationState(
    val sessionId: Long = 0,
    val phase: DictationPhase = DictationPhase.IDLE,
    val transcript: String = "",
    val message: String? = null,
) {
    val isActive: Boolean
        get() = phase == DictationPhase.CONNECTING ||
            phase == DictationPhase.LISTENING ||
            phase == DictationPhase.PROCESSING
}

object DictationStateBus {
    private val mutableState = MutableStateFlow(DictationState())
    val state = mutableState.asStateFlow()

    fun set(value: DictationState) {
        mutableState.value = value
    }
}

