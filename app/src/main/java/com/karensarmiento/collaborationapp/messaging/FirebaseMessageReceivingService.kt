package com.karensarmiento.collaborationapp.messaging

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.content.Intent
import com.karensarmiento.collaborationapp.utils.JsonKeyword as Jk
import com.karensarmiento.collaborationapp.utils.Utils


/**
 * This makes use of the Google FirebaseMessagingService to:
 * 1) Handle incoming messages
 * 2) Handle client registration tokens.
 */
class FirebaseMessageReceivingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FirebaseReceiptService"
        // Maps requestID to a callback to apply on the received response.
        private val waitingRequests = mutableMapOf<String, (Any?)->Any?>()

        /**
         *  Registers the requestId as a waiting request. Callback is applied on result if valid.
         */
        fun applyCallbackOnResponseToRequest(requestId: String, callback: (Any?)->(Any?)) {
            waitingRequests[requestId] = callback
        }
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.i(TAG, "Received message!! ${remoteMessage.data}")

        // Handle data payload if one exists.
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)

            when(val downstreamType = remoteMessage.data[Jk.DOWNSTREAM_TYPE.text]) {
                // TODO: Do not accept null here (server should add downstream type.)
                null -> Log.w(TAG, "No downstream type was specified in received message.")
                Jk.JSON_UPDATE.text-> handleJsonUpdateMessage(remoteMessage)
                else -> handleResponseMessage(downstreamType, remoteMessage)
            }
        }
    }

    /**
     *  Send intent containing Json update to trigger automerge update.
     *
     *  @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    private fun handleJsonUpdateMessage(remoteMessage: RemoteMessage) {
        // Ignore messages sent from self.
        // TODO: Could this be a security threat? You set a message as coming from someone else so
        // that they don't see certain changes. Maybe only the server should be allowed to do this.
        // Or maybe you should do this by message id or something.
        if (remoteMessage.data[Jk.EMAIL.text] == Utils.getGoogleEmail()) {
            Log.i(TAG, "Ignoring message sent from self.")
            return
        }

        val jsonUpdate = remoteMessage.data[Jk.JSON_UPDATE.text]

        val updateIntent = Intent()
        updateIntent.action = Jk.JSON_UPDATE.text
        updateIntent.putExtra(Jk.VALUE.text, jsonUpdate)
        sendBroadcast(updateIntent)
    }

    /**
     *  Resolve corresponding request ID from waitingRequests maps and handle.
     *
     *  @param downstreamType The type of downstream message.
     *  @param responseMessage Object representing the message received from Firebase Cloud Messaging.
     */
    private fun handleResponseMessage(downstreamType: String, responseMessage: RemoteMessage) {
        val requestId = responseMessage.data[Jk.REQUEST_ID.text]
        if (requestId == null) {
            Log.w(TAG, "No request id was found in the packet. It will be ignored.")
            return
        }
        val callback = waitingRequests[requestId]
        if (callback == null) {
            Log.w(TAG, "No request was waiting with id $requestId. Will ignore response")
            return
        }
        waitingRequests.remove(requestId)

        when(downstreamType) {
            Jk.GET_NOTIFICATION_KEY_RESPONSE.text -> handleNotificationKeyResponse(responseMessage, callback)
            else -> Log.w(TAG, "Downstream type $downstreamType not yet supported.")
        }
    }

    /**
     *  Apply the callback to the received notification key or report an error.
     *
     *  @param responseMessage Object representing the message received from Firebase Cloud Messaging.
     *  @param callback The callback to apply to the received notification key.
     */
    private fun handleNotificationKeyResponse(responseMessage: RemoteMessage, callback: (Any?)->(Any?)) {
        val notificationKey = responseMessage.data[Jk.NOTIFICATION_KEY.text]
        if (notificationKey == null) {
            Log.w(TAG, "No notification key was found in a notification key response message.")
        }
        callback(notificationKey)
    }

    /**
     * Called when FCM does not deliver a message. This may be because there are too many messages
     * or if the device hasn't connected to FCM for a month. When the app instance receives this
     * callback, we must perform a full sync with the app server.
     */
    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.d(TAG, "Message Deleted")
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        // TODO: Notify server of updated registration ID.
        Log.d(TAG, "Refreshed token: $token")
    }
}