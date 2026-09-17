package com.openwispr.app.service

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

enum class DictationOperation {
    DICTATION,
    TRANSFORMATION,
}

data class DictationState(
    val sessionId: Long = 0,
    val phase: DictationPhase = DictationPhase.IDLE,
    val operation: DictationOperation = DictationOperation.DICTATION,
    val transcript: String = "",
    val message: String? = null,
) {
    val isActive: Boolean
        get() = phase == DictationPhase.CONNECTING ||
            phase == DictationPhase.LISTENING ||
            phase == DictationPhase.PROCESSING

    internal fun acceptsActiveUpdate(sessionId: Long): Boolean =
        this.sessionId == sessionId && isActive

    internal fun acceptsCancellation(sessionId: Long): Boolean =
        sessionId != 0L && acceptsActiveUpdate(sessionId)
}

object DictationStateBus {
    private val mutableState = MutableStateFlow(DictationState())
    val state = mutableState.asStateFlow()

    fun set(value: DictationState) {
        mutableState.value = value
    }
}
