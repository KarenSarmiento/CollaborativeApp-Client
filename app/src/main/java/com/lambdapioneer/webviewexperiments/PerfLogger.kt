package com.lambdapioneer.webviewexperiments

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
        // TODO: consider removing event from hash table
    }
}
