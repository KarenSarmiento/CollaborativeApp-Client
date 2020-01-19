package com.karensarmiento.collaborationapp.messaging

import android.util.Log

object MessageBuffer {
    private const val TAG = "MessageBuffer"
    // Holds requests that are awaiting responses.
    private val waitingRequests = mutableSetOf<String>()

    fun registerWaitingRequest(requestId: String) {
        waitingRequests.add(requestId)
        Log.i(TAG, "Registered request as waiting: $requestId.")
        Log.i(TAG, waitingRequests.toString())
    }

    fun tryResolveRequest(requestId: String): Boolean {
        return if (waitingRequests.contains(requestId)) {
            Log.w(TAG, "A response has been received for the waiting request $requestId.")
            waitingRequests.remove(requestId)
            true
        } else {
            Log.w(TAG, "No request was waiting with id $requestId. Will ignore response.")
            false
        }
    }
}