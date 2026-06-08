package com.yunfan.douyincontrol.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerEngine {

    private var job: Job? = null

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun start(totalSeconds: Int, scope: CoroutineScope, onTimeUp: () -> Unit) {
        stop()
        _remainingSeconds.value = totalSeconds
        _isRunning.value = true

        job = scope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000)
                _remainingSeconds.value -= 1
            }
            _isRunning.value = false
            onTimeUp()
        }
    }

    fun stop() {
        job?.cancel()
        _isRunning.value = false
    }
}
