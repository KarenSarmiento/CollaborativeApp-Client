package com.karensarmiento.collaborationapp.collaboration

/**
 * Simple performance logger using a startEvent/endEvent with string identifier paradigm
 */
class PerfLogger(val logEvent: (String, Long) -> Unit) {

    private val events = HashMap<String, Long>()
    private val nsPerMs = 1_000_000

    fun startEvent(name: String) {
        events[name] = System.nanoTime()
    }

    fun endEvent(name: String) {
        events[name]?.also { startTime ->
            val deltaTimeMs = (System.nanoTime() - startTime) / nsPerMs
            logEvent(name, deltaTimeMs)
        }
    }
}
